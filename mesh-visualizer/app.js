/**
 * TFC Lean Mesh Visualizer
 * Pose math mirrors LeaningToolRenderer from TFC-More-Floor-Storage.
 *
 * Local frame after yaw: -Z toward wall. Wall face at z = -0.5.
 * Order: translate(lateral, centerY, wallOffset) → XP(-lean) → ZP(flip?) → ZP(upright?) → extras → scale
 *
 * Item sprites: TerraFirmaCraft textures (private local use — see NOTICE.txt).
 */

import * as THREE from "three";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";

const STORAGE_KEY = "tfc-lean-mesh-visualizer-v2";
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

const FACING_YROT = { south: 0, west: 90, north: 180, east: 270 };

/** Fallback when defaults.json cannot be fetched. */
const EMBEDDED_DEFAULTS = {
  version: 2,
  source: "LeaningToolRenderer (cursor/tool-leaning-6673)",
  assetNotice: "TFC textures for private local preview only — do not redistribute.",
  globals: {
    slotCount: 4,
    facing: "north",
    showAllSlots: true,
    showAxes: false,
    wallTexture: "textures/tfc/block/rock/raw/andesite.png",
    floorTexture: "textures/tfc/block/rock/raw/granite.png",
  },
  textureLibrary: [
    "textures/tfc/item/metal/axe/wrought_iron.png",
    "textures/tfc/item/metal/pickaxe/wrought_iron.png",
    "textures/tfc/item/metal/shovel/wrought_iron.png",
    "textures/tfc/item/metal/hoe/wrought_iron.png",
    "textures/tfc/item/metal/sword/wrought_iron.png",
    "textures/tfc/item/metal/mace/wrought_iron.png",
    "textures/tfc/item/metal/knife/wrought_iron.png",
    "textures/tfc/item/metal/chisel/wrought_iron.png",
    "textures/tfc/item/metal/saw/wrought_iron.png",
    "textures/tfc/item/metal/tuyere/wrought_iron.png",
    "textures/tfc/item/spindle.png",
    "textures/tfc/item/firestarter.png",
  ],
  categories: [
    {
      id: "default",
      name: "Default tools",
      description: "Axes, picks, shovels, hoes — stock lean distance.",
      examples: [],
      tags: [],
      textures: [
        "textures/tfc/item/metal/axe/wrought_iron.png",
        "textures/tfc/item/metal/pickaxe/wrought_iron.png",
        "textures/tfc/item/metal/shovel/wrought_iron.png",
        "textures/tfc/item/metal/hoe/wrought_iron.png",
      ],
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
      toolLength: 1,
      toolWidth: 1,
      color: "#8b7355",
    },
    {
      id: "clear_wall",
      name: "Clear wall (long tools)",
      description: "Swords, maces, spindle, firestarter.",
      examples: [],
      tags: ["lean_clear_wall"],
      textures: [
        "textures/tfc/item/metal/sword/wrought_iron.png",
        "textures/tfc/item/metal/mace/wrought_iron.png",
        "textures/tfc/item/spindle.png",
        "textures/tfc/item/firestarter.png",
      ],
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
      toolLength: 1,
      toolWidth: 1,
      color: "#6e7f8d",
    },
    {
      id: "flip_facing",
      name: "Flip facing (saw / chisel)",
      description: "Saws & chisels — 90° CW after lean.",
      examples: [],
      tags: ["lean_clear_wall", "lean_flip_facing"],
      textures: [
        "textures/tfc/item/metal/saw/wrought_iron.png",
        "textures/tfc/item/metal/chisel/wrought_iron.png",
        "textures/tfc/item/metal/saw/steel.png",
        "textures/tfc/item/metal/chisel/wrought_iron.png",
      ],
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
      toolLength: 1,
      toolWidth: 1,
      color: "#9a7b4f",
    },
    {
      id: "closer_wall",
      name: "Closer wall (knife / tuyere)",
      description: "Knives & tuyeres — closer wall offset + flip.",
      examples: [],
      tags: ["lean_clear_wall", "lean_closer_wall", "lean_flip_facing"],
      textures: [
        "textures/tfc/item/metal/knife/wrought_iron.png",
        "textures/tfc/item/metal/tuyere/wrought_iron.png",
        "textures/tfc/item/metal/knife/steel.png",
        "textures/tfc/item/metal/knife/copper.png",
      ],
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
      toolLength: 1,
      toolWidth: 1,
      color: "#c4a574",
    },
  ],
};

let config;
let baseline;
let activeId;
const toolRoots = [];
const textureCache = new Map();
const loader = new THREE.TextureLoader();

const canvas = document.getElementById("viewport");
const catList = document.getElementById("cat-list");
const catTitle = document.getElementById("cat-title");
const catDesc = document.getElementById("cat-desc");
const exportOut = document.getElementById("export-out");
const wallMeter = document.getElementById("wall-meter");
const fileInput = document.getElementById("file-input");
const showAllSlotsEl = document.getElementById("showAllSlots");
const showAxesEl = document.getElementById("showAxes");
const slotTexturesEl = document.getElementById("slot-textures");

const scene = new THREE.Scene();
scene.background = new THREE.Color(0x171410);
scene.fog = new THREE.Fog(0x171410, 8, 18);

const camera = new THREE.PerspectiveCamera(42, 1, 0.05, 50);
camera.position.set(2.4, 1.6, 2.8);

const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false });
renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
renderer.shadowMap.enabled = true;

const controls = new OrbitControls(camera, canvas);
controls.target.set(0.5, 0.45, 0.15);
controls.enableDamping = true;
controls.maxPolarAngle = Math.PI * 0.49;

scene.add(new THREE.HemisphereLight(0xf0e6d8, 0x2a2218, 0.9));
const key = new THREE.DirectionalLight(0xffe2b8, 1.1);
key.position.set(2.5, 4, 1.5);
key.castShadow = true;
scene.add(key);
const fill = new THREE.DirectionalLight(0x8aa0b8, 0.4);
fill.position.set(-2, 1.5, 2);
scene.add(fill);

const world = new THREE.Group();
scene.add(world);

function textureLabel(path) {
  return path
    .replace(/^textures\/tfc\/item\//, "")
    .replace(/^textures\/tfc\//, "")
    .replace(/\.png$/, "");
}

function loadTex(path) {
  if (!path) return Promise.resolve(null);
  if (textureCache.has(path)) return textureCache.get(path);
  const promise = new Promise((resolve) => {
    loader.load(
      path,
      (tex) => {
        tex.magFilter = THREE.NearestFilter;
        tex.minFilter = THREE.NearestFilter;
        tex.colorSpace = THREE.SRGBColorSpace;
        tex.needsUpdate = true;
        resolve(tex);
      },
      undefined,
      () => {
        console.warn("Missing texture", path);
        resolve(null);
      }
    );
  });
  textureCache.set(path, promise);
  return promise;
}

async function makeBlock(texPath, x, y, z, fallbackColor) {
  const geo = new THREE.BoxGeometry(1, 1, 1);
  const tex = await loadTex(texPath);
  const mat = tex
    ? new THREE.MeshStandardMaterial({
        map: tex,
        roughness: 0.95,
        metalness: 0.02,
      })
    : new THREE.MeshStandardMaterial({
        color: fallbackColor,
        roughness: 0.92,
        metalness: 0.05,
      });
  const mesh = new THREE.Mesh(geo, mat);
  mesh.position.set(x + 0.5, y + 0.5, z + 0.5);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  const edges = new THREE.LineSegments(
    new THREE.EdgesGeometry(geo),
    new THREE.LineBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.28 })
  );
  mesh.add(edges);
  return mesh;
}

async function buildEnvironment() {
  // Clear previous env meshes except tools (tools managed separately)
  while (world.children.length) {
    const child = world.children[0];
    world.remove(child);
  }
  toolRoots.length = 0;

  const wallTex = config.globals.wallTexture;
  const floorTex = config.globals.floorTexture;

  world.add(await makeBlock(floorTex, 0, -1, 0, 0x5a4a38));
  world.add(await makeBlock(floorTex, -1, -1, 0, 0x4e4132));
  world.add(await makeBlock(floorTex, 1, -1, 0, 0x4e4132));
  world.add(await makeBlock(wallTex, 0, 0, -1, 0x6d6254));
  world.add(await makeBlock(wallTex, -1, 0, -1, 0x61574b));
  world.add(await makeBlock(wallTex, 1, 0, -1, 0x61574b));
  world.add(await makeBlock(wallTex, 0, 1, -1, 0x6d6254));

  const leanOutline = new THREE.LineSegments(
    new THREE.EdgesGeometry(new THREE.BoxGeometry(1, 1, 1)),
    new THREE.LineBasicMaterial({ color: 0xd4a05a, transparent: true, opacity: 0.35 })
  );
  leanOutline.position.set(0.5, 0.5, 0.5);
  world.add(leanOutline);

  const face = new THREE.Mesh(
    new THREE.PlaneGeometry(1, 1),
    new THREE.MeshBasicMaterial({
      color: 0x7fad7a,
      transparent: true,
      opacity: 0.16,
      side: THREE.DoubleSide,
      depthWrite: false,
    })
  );
  face.position.set(0.5, 0.5, 0);
  world.add(face);

  const grid = new THREE.GridHelper(6, 6, 0x3a3228, 0x2a241e);
  grid.position.set(0.5, 0.001, 0.5);
  world.add(grid);
}

/**
 * Minecraft FIXED flat item: textured card in local XY (face +Z), 1×1 model units.
 * TFC tool sprites are drawn corner-to-corner; uprightTurn (-45 ZP) stands the handle down.
 */
async function createToolSprite(cat, slot) {
  const root = new THREE.Group();
  const textures = cat.textures || [];
  const path = textures[slot % Math.max(textures.length, 1)] || textures[0];
  const tex = await loadTex(path);

  const w = cat.toolWidth || 1;
  const h = cat.toolLength || 1;
  const geo = new THREE.PlaneGeometry(w, h);
  let mat;
  if (tex) {
    mat = new THREE.MeshBasicMaterial({
      map: tex,
      transparent: true,
      alphaTest: 0.1,
      side: THREE.DoubleSide,
      depthWrite: false,
    });
  } else {
    mat = new THREE.MeshStandardMaterial({
      color: new THREE.Color(cat.color || "#888888"),
      roughness: 0.6,
      metalness: 0.2,
      side: THREE.DoubleSide,
    });
  }

  const sprite = new THREE.Mesh(geo, mat);
  // Slight thickness cue: duplicate backplane offset
  const back = new THREE.Mesh(
    geo,
    new THREE.MeshBasicMaterial({
      color: 0x1a1510,
      transparent: true,
      opacity: 0.35,
      side: THREE.FrontSide,
      depthWrite: false,
    })
  );
  back.position.z = -0.01;

  const group = new THREE.Group();
  group.add(back);
  group.add(sprite);

  // Tiny label under slot for identification in orbit view
  root.add(group);

  if (config.globals.showAxes) {
    const axes = new THREE.AxesHelper(0.35);
    root.add(axes);
    root.userData.axes = axes;
  }

  root.userData.sprite = group;
  root.userData.texturePath = path;
  return root;
}

function applyLeanPose(root, cat, slot, slotCount, facing = "north") {
  const lateral = (slot - (slotCount - 1) / 2) * cat.slotSpacing;
  const yRot = 180 - (FACING_YROT[facing] ?? 180);

  const m = new THREE.Matrix4();
  const tmp = new THREE.Matrix4();
  const quat = new THREE.Quaternion();

  const mulT = (x, y, z) => m.multiply(tmp.makeTranslation(x, y, z));
  const mulR = (axis, deg) => {
    quat.setFromAxisAngle(axis, THREE.MathUtils.degToRad(deg));
    m.multiply(tmp.makeRotationFromQuaternion(quat));
  };
  const mulS = (s) => m.multiply(tmp.makeScale(s, s, s));

  m.identity();
  mulT(0.5, 0, 0.5);
  mulR(new THREE.Vector3(0, 1, 0), yRot);
  mulT(lateral + cat.nudgeX, cat.centerY + cat.nudgeY, cat.wallOffset + cat.nudgeZ);
  mulR(new THREE.Vector3(1, 0, 0), -cat.leanAngle);
  if (cat.flipFacing) mulR(new THREE.Vector3(0, 0, 1), cat.flipFacingTurn);
  if (cat.flatSprite) mulR(new THREE.Vector3(0, 0, 1), cat.uprightTurn);
  if (cat.extraRotX) mulR(new THREE.Vector3(1, 0, 0), cat.extraRotX);
  if (cat.extraRotY) mulR(new THREE.Vector3(0, 1, 0), cat.extraRotY);
  if (cat.extraRotZ) mulR(new THREE.Vector3(0, 0, 1), cat.extraRotZ);
  mulS(cat.scale);

  root.matrixAutoUpdate = false;
  root.matrix.copy(m);
  root.matrixWorldNeedsUpdate = true;
}

async function rebuildTools() {
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

  const indices = showAllSlotsEl.checked
    ? [...Array(config.globals.slotCount).keys()]
    : [Math.floor((config.globals.slotCount - 1) / 2)];

  for (const slot of indices) {
    const sprite = await createToolSprite(cat, slot);
    applyLeanPose(sprite, cat, slot, config.globals.slotCount, config.globals.facing);
    world.add(sprite);
    toolRoots.push(sprite);
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
  catDesc.textContent = `${cat.description}${
    cat.examples?.length ? `  ·  ${cat.examples.join(", ")}` : ""
  }`;
  syncControlsFromCategory(cat);
  renderSlotTexturePickers(cat);
  void rebuildTools();
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

function renderSlotTexturePickers(cat) {
  if (!slotTexturesEl) return;
  slotTexturesEl.innerHTML = "";
  const lib = config.textureLibrary || baseline.textureLibrary || [];
  if (!cat.textures) cat.textures = [];
  while (cat.textures.length < config.globals.slotCount) {
    cat.textures.push(cat.textures[0] || lib[0] || "");
  }

  for (let i = 0; i < config.globals.slotCount; i++) {
    const row = document.createElement("div");
    row.className = "tex-row";

    const preview = document.createElement("img");
    preview.className = "tex-preview";
    preview.src = cat.textures[i];
    preview.alt = "";
    preview.width = 32;
    preview.height = 32;

    const label = document.createElement("label");
    label.textContent = `Slot ${i}`;

    const select = document.createElement("select");
    for (const path of lib) {
      const opt = document.createElement("option");
      opt.value = path;
      opt.textContent = textureLabel(path);
      if (path === cat.textures[i]) opt.selected = true;
      select.appendChild(opt);
    }
    select.addEventListener("change", () => {
      cat.textures[i] = select.value;
      preview.src = select.value;
      rebuildTools();
      persist();
    });

    row.append(preview, label, select);
    slotTexturesEl.appendChild(row);
  }
}

function bindControls() {
  for (const key of NUMBER_KEYS) {
    const el = document.getElementById(key);
    el.addEventListener("input", () => {
      const cat = activeCategory();
      cat[key] = parseFloat(el.value);
      const label = document.querySelector(`.val[data-for="${key}"]`);
      if (label) label.textContent = formatNum(cat[key]);
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
  showAxesEl?.addEventListener("change", () => {
    config.globals.showAxes = showAxesEl.checked;
    rebuildTools();
    persist();
  });
}

function refreshPosesOnly() {
  const cat = activeCategory();
  const indices = showAllSlotsEl.checked
    ? [...Array(config.globals.slotCount).keys()]
    : [Math.floor((config.globals.slotCount - 1) / 2)];
  if (toolRoots.length !== indices.length) {
    rebuildTools();
    persist();
    return;
  }
  toolRoots.forEach((root, i) => {
    applyLeanPose(root, cat, indices[i], config.globals.slotCount, config.globals.facing);
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
    const thumb = cat.textures?.[0]
      ? `<img class="cat-thumb" src="${cat.textures[0]}" alt="" width="20" height="20" />`
      : `<span class="swatch" style="background:${cat.color}"></span>`;
    btn.innerHTML = `<strong>${thumb}${cat.name}</strong><span>${
      cat.tags.length ? cat.tags.join(" · ") : "no special tags"
    } · z=${cat.wallOffset}</span>`;
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

function mergeCategory(saved, base) {
  return { ...deepClone(base), ...saved, textures: saved.textures || base.textures };
}

async function loadDefaultsFile() {
  try {
    const res = await fetch("./defaults.json", { cache: "no-store" });
    if (res.ok) return await res.json();
  } catch {
    /* embedded */
  }
  return EMBEDDED_DEFAULTS;
}

async function loadInitial() {
  const saved = localStorage.getItem(STORAGE_KEY);
  baseline = await loadDefaultsFile();
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      config = deepClone(baseline);
      config.globals = { ...config.globals, ...(parsed.globals || {}) };
      config.categories = baseline.categories.map((b) => {
        const s = (parsed.categories || []).find((c) => c.id === b.id);
        return s ? mergeCategory(s, b) : deepClone(b);
      });
      for (const s of parsed.categories || []) {
        if (!config.categories.find((c) => c.id === s.id)) config.categories.push(deepClone(s));
      }
    } catch {
      config = deepClone(baseline);
    }
  } else {
    config = deepClone(baseline);
  }
  showAllSlotsEl.checked = config.globals.showAllSlots !== false;
  if (showAxesEl) showAxesEl.checked = !!config.globals.showAxes;
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
  const parsed = JSON.parse(text);
  config = deepClone(baseline);
  config.globals = { ...config.globals, ...(parsed.globals || {}) };
  config.categories = (parsed.categories || baseline.categories).map((c) => {
    const b = baselineCategory(c.id);
    return b ? mergeCategory(c, b) : deepClone(c);
  });
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
await buildEnvironment();
renderCatList();
bindControls();
selectCategory(config.categories[0].id);
onResize();
animate();
exportJson();
