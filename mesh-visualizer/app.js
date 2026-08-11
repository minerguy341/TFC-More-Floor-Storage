/**
 * TFC Lean Mesh Visualizer
 * Pose math mirrors LeaningToolRenderer from TFC-More-Floor-Storage.
 *
 * Local frame after yaw: -Z toward wall. Wall face at z = -0.5.
 * Order: translate(lateral, centerY, wallOffset) → XP(-lean) → ZP(flip?) → ZP(upright?) → extras → scale
 */

import * as THREE from "three";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";

const STORAGE_KEY = "tfc-lean-mesh-visualizer-v1";
const NUMBER_KEYS = [
  "leanAngle",
  "wallOffset",
  "centerY",
  "scale",
  "uprightTurn",
  "flipFacingTurn",
  "slotSpacing",
  "extraRotX",
  "extraRotY",
  "extraRotZ",
  "nudgeX",
  "nudgeY",
  "nudgeZ",
  "toolLength",
  "toolWidth",
];
const BOOL_KEYS = ["flatSprite", "flipFacing"];

/** Fallback when defaults.json cannot be fetched (e.g. some file:// setups). */
const EMBEDDED_DEFAULTS = {
  version: 1,
  source: "LeaningToolRenderer (cursor/tool-leaning-6673)",
  notes: [
    "Local frame after yaw: -Z points toward the wall. Wall face sits at z = -0.5.",
    "Pose order matches the Java renderer: translate → lean (XP) → flip facing (ZP) → upright (ZP) → scale.",
  ],
  globals: { slotCount: 4, facing: "north", showAllSlots: true },
  categories: [
    {
      id: "default",
      name: "Default tools",
      description: "Axes, picks, shovels, hoes — stock lean distance.",
      examples: ["tfc:metal/axe/wrought_iron", "tfc:metal/pickaxe/wrought_iron", "minecraft:iron_shovel"],
      tags: [],
      flatSprite: true,
      flipFacing: false,
      leanAngle: 28,
      wallOffset: -0.34,
      centerY: 0.42,
      scale: 0.72,
      uprightTurn: -45,
      flipFacingTurn: 90,
      slotSpacing: 0.2,
      extraRotX: 0,
      extraRotY: 0,
      extraRotZ: 0,
      nudgeX: 0,
      nudgeY: 0,
      nudgeZ: 0,
      toolLength: 1.0,
      toolWidth: 0.35,
      color: "#8b7355",
    },
    {
      id: "clear_wall",
      name: "Clear wall (long tools)",
      description: "Swords, maces, rods, spindle, firestarter — seated farther from the wall face.",
      examples: ["tfc:metal/sword/wrought_iron", "tfc:metal/mace/wrought_iron", "tfc:spindle", "tfc:firestarter", "minecraft:fishing_rod"],
      tags: ["lean_clear_wall"],
      flatSprite: true,
      flipFacing: false,
      leanAngle: 28,
      wallOffset: -0.27,
      centerY: 0.42,
      scale: 0.72,
      uprightTurn: -45,
      flipFacingTurn: 90,
      slotSpacing: 0.2,
      extraRotX: 0,
      extraRotY: 0,
      extraRotZ: 0,
      nudgeX: 0,
      nudgeY: 0,
      nudgeZ: 0,
      toolLength: 1.15,
      toolWidth: 0.28,
      color: "#6e7f8d",
    },
    {
      id: "flip_facing",
      name: "Flip facing (saw / chisel)",
      description: "Saws & chisels — 90° CW after lean, clear-wall offset.",
      examples: ["tfc:metal/saw/wrought_iron", "tfc:metal/chisel/wrought_iron"],
      tags: ["lean_clear_wall", "lean_flip_facing"],
      flatSprite: true,
      flipFacing: true,
      leanAngle: 28,
      wallOffset: -0.27,
      centerY: 0.42,
      scale: 0.72,
      uprightTurn: -45,
      flipFacingTurn: 90,
      slotSpacing: 0.2,
      extraRotX: 0,
      extraRotY: 0,
      extraRotZ: 0,
      nudgeX: 0,
      nudgeY: 0,
      nudgeZ: 0,
      toolLength: 1.05,
      toolWidth: 0.32,
      color: "#9a7b4f",
    },
    {
      id: "closer_wall",
      name: "Closer wall (knife / tuyere)",
      description: "Knives & tuyeres — flip facing, 0.05 closer than clear-wall.",
      examples: ["tfc:metal/knife/wrought_iron", "tfc:metal/tuyere/wrought_iron"],
      tags: ["lean_clear_wall", "lean_closer_wall", "lean_flip_facing"],
      flatSprite: true,
      flipFacing: true,
      leanAngle: 28,
      wallOffset: -0.32,
      centerY: 0.42,
      scale: 0.72,
      uprightTurn: -45,
      flipFacingTurn: 90,
      slotSpacing: 0.2,
      extraRotX: 0,
      extraRotY: 0,
      extraRotZ: 0,
      nudgeX: 0,
      nudgeY: 0,
      nudgeZ: 0,
      toolLength: 0.85,
      toolWidth: 0.3,
      color: "#c4a574",
    },
  ],
};

/** Minecraft Direction.toYRot() for horizontal facings. */
const FACING_YROT = { south: 0, west: 90, north: 180, east: 270 };

let config;
let baseline;
let activeId;
const toolRoots = [];

const canvas = document.getElementById("viewport");
const catList = document.getElementById("cat-list");
const catTitle = document.getElementById("cat-title");
const catDesc = document.getElementById("cat-desc");
const exportOut = document.getElementById("export-out");
const wallMeter = document.getElementById("wall-meter");
const fileInput = document.getElementById("file-input");
const showAllSlotsEl = document.getElementById("showAllSlots");

const scene = new THREE.Scene();
scene.background = new THREE.Color(0x171410);
scene.fog = new THREE.Fog(0x171410, 8, 18);

const camera = new THREE.PerspectiveCamera(42, 1, 0.05, 50);
camera.position.set(2.4, 1.6, 2.8);

const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.shadowMap.enabled = true;

const controls = new OrbitControls(camera, canvas);
controls.target.set(0.5, 0.45, 0.15);
controls.enableDamping = true;
controls.maxPolarAngle = Math.PI * 0.49;

const hemi = new THREE.HemisphereLight(0xf0e6d8, 0x2a2218, 0.85);
scene.add(hemi);
const key = new THREE.DirectionalLight(0xffe2b8, 1.05);
key.position.set(2.5, 4, 1.5);
key.castShadow = true;
scene.add(key);
const fill = new THREE.DirectionalLight(0x8aa0b8, 0.35);
fill.position.set(-2, 1.5, 2);
scene.add(fill);

const world = new THREE.Group();
scene.add(world);

function makeBlock(color, x, y, z) {
  const geo = new THREE.BoxGeometry(1, 1, 1);
  const mat = new THREE.MeshStandardMaterial({
    color,
    roughness: 0.92,
    metalness: 0.05,
  });
  const mesh = new THREE.Mesh(geo, mat);
  mesh.position.set(x + 0.5, y + 0.5, z + 0.5);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  const edges = new THREE.LineSegments(
    new THREE.EdgesGeometry(geo),
    new THREE.LineBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.25 })
  );
  mesh.add(edges);
  return mesh;
}

function buildEnvironment() {
  // Floor under leaning cell
  world.add(makeBlock(0x5a4a38, 0, -1, 0));
  // Neighboring floor for context
  world.add(makeBlock(0x4e4132, -1, -1, 0));
  world.add(makeBlock(0x4e4132, 1, -1, 0));
  // Wall the tools lean against (north, local -Z after facing yaw)
  const wall = makeBlock(0x6d6254, 0, 0, -1);
  world.add(wall);
  world.add(makeBlock(0x61574b, -1, 0, -1));
  world.add(makeBlock(0x61574b, 1, 0, -1));
  world.add(makeBlock(0x6d6254, 0, 1, -1));

  // Ghost outline of leaning block volume
  const leanOutline = new THREE.LineSegments(
    new THREE.EdgesGeometry(new THREE.BoxGeometry(1, 1, 1)),
    new THREE.LineBasicMaterial({ color: 0xd4a05a, transparent: true, opacity: 0.35 })
  );
  leanOutline.position.set(0.5, 0.5, 0.5);
  world.add(leanOutline);

  // Wall face plane helper at local z=-0.5 after transform — draw in world for NORTH facing.
  // For facing=north, yaw = 180 - 180 = 0, so local == world after center translate.
  const faceGeo = new THREE.PlaneGeometry(1, 1);
  const faceMat = new THREE.MeshBasicMaterial({
    color: 0x7fad7a,
    transparent: true,
    opacity: 0.18,
    side: THREE.DoubleSide,
  });
  const face = new THREE.Mesh(faceGeo, faceMat);
  face.position.set(0.5, 0.5, 0); // world z=0 is the south face of the north wall block (= local -0.5 from cell center)
  world.add(face);

  const grid = new THREE.GridHelper(6, 6, 0x3a3228, 0x2a241e);
  grid.position.set(0.5, 0.001, 0.5);
  world.add(grid);
}

function createToolProxy(cat) {
  const root = new THREE.Group();
  const length = cat.toolLength;
  const width = cat.toolWidth;

  // Flat FIXED sprite proxy: thin card in XY, tip (+Y) / handle (-Y), blade hint on +X
  const body = new THREE.Mesh(
    new THREE.BoxGeometry(width, length, 0.04),
    new THREE.MeshStandardMaterial({
      color: new THREE.Color(cat.color),
      roughness: 0.55,
      metalness: 0.25,
    })
  );
  body.castShadow = true;

  const handle = new THREE.Mesh(
    new THREE.BoxGeometry(width * 0.28, length * 0.35, 0.05),
    new THREE.MeshStandardMaterial({ color: 0x3b2a1c, roughness: 0.9 })
  );
  handle.position.y = -length * 0.28;

  const tip = new THREE.Mesh(
    new THREE.ConeGeometry(width * 0.28, length * 0.18, 4),
    new THREE.MeshStandardMaterial({ color: 0xd8d2c4, metalness: 0.6, roughness: 0.35 })
  );
  tip.position.y = length * 0.42;
  tip.rotation.y = Math.PI / 4;

  const axes = new THREE.AxesHelper(0.35);

  const sprite = new THREE.Group();
  sprite.add(body, handle, tip);
  root.add(sprite);
  root.add(axes);
  root.userData.sprite = sprite;
  root.userData.axes = axes;
  return root;
}

/**
 * Apply LeaningToolRenderer pose for one slot.
 * facing defaults to north so local frame aligns with world after center translate.
 */
function applyLeanPose(root, cat, slot, slotCount, facing = "north") {
  const large = false;
  const lateral = large ? 0 : (slot - (slotCount - 1) / 2) * cat.slotSpacing;
  const yRot = 180 - (FACING_YROT[facing] ?? 180);

  root.position.set(0, 0, 0);
  root.rotation.set(0, 0, 0);
  root.scale.set(1, 1, 1);
  root.updateMatrix();

  // Reproduce PoseStack ops with a matrix chain (same order as Java mulPose/translate/scale).
  const m = new THREE.Matrix4();
  const tmp = new THREE.Matrix4();
  const quat = new THREE.Quaternion();
  const euler = new THREE.Euler();

  const mulT = (x, y, z) => {
    m.multiply(tmp.makeTranslation(x, y, z));
  };
  const mulR = (axis, deg) => {
    quat.setFromAxisAngle(axis, THREE.MathUtils.degToRad(deg));
    m.multiply(tmp.makeRotationFromQuaternion(quat));
  };
  const mulS = (s) => {
    m.multiply(tmp.makeScale(s, s, s));
  };

  m.identity();
  mulT(0.5, 0, 0.5);
  mulR(new THREE.Vector3(0, 1, 0), yRot);
  mulT(lateral + cat.nudgeX, cat.centerY + cat.nudgeY, cat.wallOffset + cat.nudgeZ);
  mulR(new THREE.Vector3(1, 0, 0), -cat.leanAngle);
  if (cat.flipFacing) {
    mulR(new THREE.Vector3(0, 0, 1), cat.flipFacingTurn);
  }
  if (cat.flatSprite) {
    mulR(new THREE.Vector3(0, 0, 1), cat.uprightTurn);
  }
  if (cat.extraRotX) mulR(new THREE.Vector3(1, 0, 0), cat.extraRotX);
  if (cat.extraRotY) mulR(new THREE.Vector3(0, 1, 0), cat.extraRotY);
  if (cat.extraRotZ) mulR(new THREE.Vector3(0, 0, 1), cat.extraRotZ);
  mulS(cat.scale);

  root.matrixAutoUpdate = false;
  root.matrix.copy(m);
  root.matrixWorldNeedsUpdate = true;
}

function rebuildTools() {
  for (const t of toolRoots) {
    world.remove(t);
    t.traverse((o) => {
      if (o.geometry) o.geometry.dispose();
      if (o.material) {
        if (Array.isArray(o.material)) o.material.forEach((m) => m.dispose());
        else o.material.dispose();
      }
    });
  }
  toolRoots.length = 0;

  const cat = activeCategory();
  if (!cat) return;

  const slots = showAllSlotsEl.checked ? config.globals.slotCount : 1;
  const start = showAllSlotsEl.checked ? 0 : 1; // single tool in a mid-ish slot feel: use slot 1 of 0..3? Use centered: only slot index mapped
  const indices = showAllSlotsEl.checked
    ? [...Array(slots).keys()]
    : [Math.floor((config.globals.slotCount - 1) / 2)];

  for (const slot of indices) {
    const proxy = createToolProxy(cat);
    applyLeanPose(proxy, cat, slot, config.globals.slotCount, config.globals.facing);
    // Refresh proxy mesh sizes if length/width changed — recreate already uses cat.
    world.add(proxy);
    toolRoots.push(proxy);
  }

  updateWallMeter(cat);
}

function updateWallMeter(cat) {
  const z = cat.wallOffset + cat.nudgeZ;
  const gap = Math.abs(z - -0.5);
  wallMeter.textContent = `wall gap: ${gap.toFixed(3)} · foot z: ${z.toFixed(3)}`;
}

function activeCategory() {
  return config.categories.find((c) => c.id === activeId);
}

function baselineCategory(id) {
  return baseline.categories.find((c) => c.id === id);
}

function selectCategory(id) {
  activeId = id;
  [...catList.children].forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.id === id);
  });
  const cat = activeCategory();
  catTitle.textContent = cat.name;
  catDesc.textContent = `${cat.description}  ·  examples: ${cat.examples.join(", ")}`;
  syncControlsFromCategory(cat);
  rebuildTools();
}

function syncControlsFromCategory(cat) {
  for (const key of NUMBER_KEYS) {
    const el = document.getElementById(key);
    el.value = cat[key];
    const label = document.querySelector(`.val[data-for="${key}"]`);
    if (label) label.textContent = formatNum(cat[key]);
  }
  for (const key of BOOL_KEYS) {
    document.getElementById(key).checked = !!cat[key];
  }
}

function formatNum(n) {
  const v = Number(n);
  if (Number.isInteger(v)) return String(v);
  return String(Math.round(v * 100) / 100);
}

function bindControls() {
  for (const key of NUMBER_KEYS) {
    const el = document.getElementById(key);
    el.addEventListener("input", () => {
      const cat = activeCategory();
      cat[key] = parseFloat(el.value);
      const label = document.querySelector(`.val[data-for="${key}"]`);
      if (label) label.textContent = formatNum(cat[key]);
      // length/width need remesh
      if (key === "toolLength" || key === "toolWidth") rebuildTools();
      else {
        refreshPosesOnly();
        persist();
      }
    });
  }
  for (const key of BOOL_KEYS) {
    document.getElementById(key).addEventListener("change", (e) => {
      activeCategory()[key] = e.target.checked;
      refreshPosesOnly();
      persist();
    });
  }
  showAllSlotsEl.addEventListener("change", () => {
    config.globals.showAllSlots = showAllSlotsEl.checked;
    rebuildTools();
    persist();
  });
}

function refreshPosesOnly() {
  const cat = activeCategory();
  // If geometry params unchanged, still recreate when color? Keep simple: re-apply poses.
  // Proxies already built with old length — only rebuild when needed. For pose params:
  const slots = toolRoots.length;
  const indices = showAllSlotsEl.checked
    ? [...Array(config.globals.slotCount).keys()]
    : [Math.floor((config.globals.slotCount - 1) / 2)];
  if (slots !== indices.length) {
    rebuildTools();
    persist();
    return;
  }
  toolRoots.forEach((root, i) => {
    applyLeanPose(root, cat, indices[i], config.globals.slotCount, config.globals.facing);
    // Update color live
    root.userData.sprite?.traverse((o) => {
      if (o.isMesh && o.material && o.material.color && o !== root) {
        /* leave handle/tip */
      }
    });
  });
  updateWallMeter(cat);
  persist();
}

function renderCatList() {
  catList.innerHTML = "";
  for (const cat of config.categories) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "cat-btn";
    btn.dataset.id = cat.id;
    btn.innerHTML = `<strong><span class="swatch" style="background:${cat.color}"></span>${cat.name}</strong><span>${cat.tags.length ? cat.tags.join(" · ") : "no special tags"} · z=${cat.wallOffset}</span>`;
    btn.addEventListener("click", () => selectCategory(cat.id));
    catList.appendChild(btn);
  }
}

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
}

function deepClone(o) {
  return JSON.parse(JSON.stringify(o));
}

async function loadDefaultsFile() {
  try {
    const res = await fetch("./defaults.json", { cache: "no-store" });
    if (res.ok) return await res.json();
  } catch {
    /* file:// or offline — use embedded baseline */
  }
  return EMBEDDED_DEFAULTS;
}

async function loadInitial() {
  const saved = localStorage.getItem(STORAGE_KEY);
  baseline = await loadDefaultsFile();
  if (saved) {
    try {
      config = JSON.parse(saved);
      for (const b of baseline.categories) {
        if (!config.categories.find((c) => c.id === b.id)) {
          config.categories.push(deepClone(b));
        }
      }
    } catch {
      config = deepClone(baseline);
    }
  } else {
    config = deepClone(baseline);
  }
  showAllSlotsEl.checked = config.globals.showAllSlots !== false;
}

function exportJava() {
  const byId = Object.fromEntries(config.categories.map((c) => [c.id, c]));
  const d = byId.default;
  const clear = byId.clear_wall;
  const closer = byId.closer_wall;
  const flip = byId.flip_facing;

  const lines = [];
  lines.push("// Generated by TFC Lean Mesh Visualizer");
  lines.push("// Paste into LeaningToolRenderer (review before committing).");
  lines.push(`private static final float LEAN_ANGLE = ${num(d.leanAngle)}f;`);
  lines.push(`private static final float SLOT_SPACING = ${num(d.slotSpacing)}f;`);
  lines.push(`private static final float CENTER_Y = ${num(d.centerY)}f;`);
  lines.push(`private static final float WALL_OFFSET = ${num(d.wallOffset)}f;`);
  lines.push(`private static final float CLEAR_WALL_OFFSET = ${num(clear.wallOffset)}f;`);
  lines.push(`private static final float CLOSER_WALL_OFFSET = ${num(closer.wallOffset)}f;`);
  lines.push(`private static final float SCALE = ${num(d.scale)}f;`);
  lines.push(`private static final float UPRIGHT_TURN = ${num(d.uprightTurn)}f;`);
  lines.push(`private static final float FLIP_FACING_TURN = ${num(flip.flipFacingTurn)}f;`);
  lines.push("");
  lines.push("// Per-category extras (not in stock renderer — wire up if needed):");
  for (const c of config.categories) {
    lines.push(
      `// ${c.id}: lean=${num(c.leanAngle)} wallZ=${num(c.wallOffset)} y=${num(c.centerY)} scale=${num(c.scale)} ` +
        `upright=${num(c.uprightTurn)} flip=${c.flipFacing}?${num(c.flipFacingTurn)} ` +
        `extra=(${num(c.extraRotX)},${num(c.extraRotY)},${num(c.extraRotZ)}) ` +
        `nudge=(${num(c.nudgeX)},${num(c.nudgeY)},${num(c.nudgeZ)})`
    );
  }
  exportOut.value = lines.join("\n");
}

function num(v) {
  const n = Number(v);
  return Number.isInteger(n) ? String(n) : String(Math.round(n * 1000) / 1000);
}

function exportJson() {
  exportOut.value = JSON.stringify(config, null, 2);
}

function downloadJson() {
  const blob = new Blob([JSON.stringify(config, null, 2)], { type: "application/json" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = "lean-mesh-poses.json";
  a.click();
  URL.revokeObjectURL(a.href);
  exportJson();
}

function setView(mode) {
  document.querySelectorAll("#hud .chip[data-view]").forEach((c) => {
    c.classList.toggle("active", c.dataset.view === mode);
  });
  if (mode === "side") {
    camera.position.set(3.2, 0.7, 0.5);
    controls.target.set(0.5, 0.45, 0.15);
  } else if (mode === "room") {
    camera.position.set(0.5, 1.2, 3.2);
    controls.target.set(0.5, 0.45, 0.2);
  } else if (mode === "top") {
    camera.position.set(0.5, 4.2, 0.55);
    controls.target.set(0.5, 0, 0.2);
  } else {
    camera.position.set(2.4, 1.6, 2.8);
    controls.target.set(0.5, 0.45, 0.15);
  }
  controls.update();
}

function onResize() {
  const wrap = document.getElementById("stage-wrap");
  const w = wrap.clientWidth;
  const h = wrap.clientHeight;
  camera.aspect = w / Math.max(h, 1);
  camera.updateProjectionMatrix();
  renderer.setSize(w, h, false);
}

function animate() {
  requestAnimationFrame(animate);
  controls.update();
  renderer.render(scene, camera);
}

document.getElementById("btn-reset-cat").addEventListener("click", () => {
  const b = baselineCategory(activeId);
  if (!b) return;
  const idx = config.categories.findIndex((c) => c.id === activeId);
  config.categories[idx] = deepClone(b);
  selectCategory(activeId);
  persist();
});

document.getElementById("btn-reset-all").addEventListener("click", () => {
  config = deepClone(baseline);
  renderCatList();
  selectCategory(config.categories[0].id);
  persist();
});

document.getElementById("btn-save").addEventListener("click", downloadJson);
document.getElementById("btn-load").addEventListener("click", () => fileInput.click());
fileInput.addEventListener("change", async () => {
  const file = fileInput.files?.[0];
  if (!file) return;
  const text = await file.text();
  config = JSON.parse(text);
  renderCatList();
  selectCategory(config.categories[0]?.id || activeId);
  persist();
  exportJson();
});

document.getElementById("btn-export-java").addEventListener("click", exportJava);
document.getElementById("btn-copy").addEventListener("click", async () => {
  if (!exportOut.value) exportJson();
  await navigator.clipboard.writeText(exportOut.value);
});

document.querySelectorAll("#hud .chip[data-view]").forEach((chip) => {
  chip.addEventListener("click", () => setView(chip.dataset.view));
});

window.addEventListener("resize", onResize);

await loadInitial();
buildEnvironment();
renderCatList();
bindControls();
selectCategory(config.categories[0].id);
onResize();
animate();
exportJson();
