#!/usr/bin/env python3
"""
Compiles the shared sources with javac and reports only the errors that are real here.

Minecraft, NeoForge and TerraFirmaCraft are not on the classpath in this checkout, so most of what
javac says is noise: every reference to one of their types is an unresolved symbol. Filtering all of
that out is what lets the rest be read at all, but filtering by message alone throws away genuine
mistakes - a deleted field reads exactly like a missing Minecraft class, which is how one once got
committed.

What separates the two is the *name* that could not be found:

  * a type javac cannot see is one it was asked to import, so the name appears in the file's imports
  * a member inherited from a Minecraft superclass is unresolvable too, but there are few of those
    and they are listed below
  * anything else that is missing was declared in this repo, or should have been

Uppercase names are treated as types regardless: javac reports a static access on an unknown class
(`Minecraft.getInstance()`) as a missing *variable*, so the reported kind cannot be trusted.

This is a net, not a compiler. javac gives up on a call whose arguments are themselves unresolvable,
so a mistake buried inside an expression full of Minecraft types will still only show up in a real
build. What it does catch is a name that has gone missing from code it can otherwise read.

Run from the repository root:

    python3 tools/check_sources.py
"""

from __future__ import annotations

import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE_DIR = ROOT / "common" / "src" / "main" / "java"
OUR_PACKAGE = "com.minerguy341.morefloorstorage"

# Members inherited from Minecraft superclasses. Ours by use but not by declaration, so javac cannot
# resolve them and neither can this script; each one has to be vouched for by hand.
KNOWN_INHERITED = {
    "super",         # A superclass that does not exist has no members
    "this",          # Likewise, for an implicit outer reference into one
    "level",         # BlockEntity
    "worldPosition", # BlockEntity
}

# Every class here extends or implements something from Minecraft, so javac cannot check a single
# @Override. Nothing useful survives, and it drowns out what does.
ALWAYS_NOISE = "method does not override or implement a method from a supertype"

ERROR = re.compile(r"^(?P<file>.+\.java):(?P<line>\d+): error: (?P<message>.*)$")
SYMBOL = re.compile(r"^\s*symbol:\s*(?P<kind>\w+)\s*(?P<name>.*)$")
LOCATION = re.compile(r"^\s*location:\s*(?P<location>.*)$")
IMPORT = re.compile(r"^import\s+(?:static\s+)?([\w.]+)\s*;")
DECLARATION = re.compile(r"\b(?:class|interface|record|enum)\s+(?P<name>\w+)(?P<header>[^{]*)\{")
SUPERTYPE = re.compile(r"\b(?:extends|implements)\s+(?P<supertypes>[^{]+)")


class Problem:
    def __init__(self, file: str, line: str, message: str) -> None:
        self.file = file
        self.line = line
        self.message = message
        self.kind = ""
        self.name = ""
        self.location = ""

    def __str__(self) -> str:
        detail = f" [{self.kind} {self.name}]" if self.kind else ""
        where = f" in {self.location}" if self.location else ""
        return f"{self.file}:{self.line}: {self.message}{detail}{where}"


def parse(output: str) -> list[Problem]:
    problems: list[Problem] = []
    for raw in output.splitlines():
        match = ERROR.match(raw)
        if match:
            problems.append(Problem(match["file"], match["line"], match["message"]))
            continue
        if not problems:
            continue
        match = SYMBOL.match(raw)
        if match:
            problems[-1].kind = match["kind"]
            problems[-1].name = match["name"].strip()
            continue
        match = LOCATION.match(raw)
        if match:
            problems[-1].location = match["location"].strip()
    return problems


def imported_names(path: str, cache: dict[str, set[str]]) -> set[str]:
    """The simple names a file imports, which are exactly the ones missing jars can account for."""
    if path not in cache:
        names: set[str] = set()
        try:
            for line in Path(path).read_text(encoding="utf-8").splitlines():
                match = IMPORT.match(line)
                if match:
                    names.add(match[1].rsplit(".", 1)[-1])
                elif line.startswith(("public ", "final ", "class ", "@")):
                    break # Past the imports
        except OSError:
            pass
        cache[path] = names
    return cache[path]


def supertypes_by_class() -> dict[str, set[str]]:
    """Every type declared in this repo, mapped to the simple names of what it extends."""
    declared: dict[str, set[str]] = {}
    for path in SOURCE_DIR.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        for match in DECLARATION.finditer(text):
            supertypes: set[str] = set()
            for clause in SUPERTYPE.finditer(match["header"]):
                for name in clause["supertypes"].split(","):
                    name = name.strip().split("<")[0].rsplit(".", 1)[-1]
                    if name:
                        supertypes.add(name)
            declared.setdefault(match["name"], set()).update(supertypes)
    return declared


def inherits_from_outside(name: str, declared: dict[str, set[str]], seen: set[str]) -> bool:
    """
    True if this type has an ancestor javac cannot see, and so members it cannot check.

    Anything extending a Minecraft type inherits methods that are simply not here, and a call to one
    is indistinguishable from a call to a method that does not exist. Types built only out of what is
    in this repo have no such excuse, and missing members in those are reported.
    """
    if name not in declared or name in seen:
        return name not in declared
    seen.add(name)
    return any(inherits_from_outside(parent, declared, seen) for parent in declared[name])


def is_external(problem: Problem, cache: dict[str, set[str]], declared: dict[str, set[str]]) -> bool:
    """True if this error is only here because someone else's jar is missing."""
    message = problem.message

    if message == ALWAYS_NOISE:
        return True

    # Unresolvable imports and package references
    if "does not exist" in message:
        return not message.startswith(f"package {OUR_PACKAGE}")

    if message == "cannot find symbol":
        name = problem.name
        if name in KNOWN_INHERITED:
            return True
        if name[:1].isupper():
            return True # A type, or a static access javac has mistaken for a variable
        if problem.kind == "method":
            # `location: class Foo` or `location: variable bar of type Foo`
            owner = problem.location.replace("class ", "").split(" of type ")[-1].split("<")[0]
            return inherits_from_outside(owner.rsplit(".", 1)[-1], declared, set())
        return name in imported_names(problem.file, cache)

    # Anything else - a syntax error, a duplicate declaration, a bad assignment - is ours
    return False


def main() -> int:
    javac = shutil.which("javac")
    if javac is None:
        print("javac not found", file=sys.stderr)
        return 2

    sources = sorted(str(path) for path in SOURCE_DIR.rglob("*.java"))
    if not sources:
        print(f"No sources under {SOURCE_DIR}", file=sys.stderr)
        return 2

    with tempfile.TemporaryDirectory() as out:
        result = subprocess.run(
            # Almost every error is noise, so the default cap of 100 is reached long before anything
            # worth seeing; without this the real problem is simply never reported.
            [javac, "-proc:none", "-nowarn", "-Xmaxerrs", "100000", "-d", out, *sources],
            capture_output=True,
            text=True,
        )

    cache: dict[str, set[str]] = {}
    declared = supertypes_by_class()
    problems = [problem for problem in parse(result.stderr)
                if not is_external(problem, cache, declared)]
    if problems:
        print(f"{len(problems)} problem(s) that are not missing-dependency noise:\n")
        for problem in problems:
            print(f"  {problem}")
        return 1

    print(f"Checked {len(sources)} files: nothing but missing-dependency noise.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
