/**
 * TFC Lean Mesh Visualizer
 * Pose math mirrors LeaningToolRenderer.
 * One material + per-type pose config for each tool type.
 */

import * as THREE from "three";
import { OrbitControls } from "three/addons/controls/OrbitControls.js";
import { createExtrudedItemMesh } from "./itemExtrude.js";

const STORAGE_KEY = "tfc-lean-mesh-visualizer-v4";
const NUMBER_KEYS = [
  "leanAngle", "wallOffset", "centerY", "scale", "uprightTurn", "flipFacingTurn",
  "slotSpacing", "extraRotX", "extraRotY", "extraRotZ", "nudgeX", "nudgeY", "nudgeZ",
  "toolLength", "toolWidth", "thicknessScale",
];
const BOOL_KEYS = ["flatSprite", "flipFacing", "extrude3d"];

const GROUP_LABELS = {
  default: "Default lean",
  clear_wall: "Clear wall",
  flip_facing: "Flip facing",
  closer_wall: "Closer wall",
};

const FACING_YROT = { south: 0, west: 90, north: 180, east: 270 };

const EMBEDDED_DEFAULTS = {
  "version": 3,
  "source": "LeaningToolRenderer (cursor/tool-leaning-6673)",
  "assetNotice": "Item/block textures copied from TerraFirmaCraft 1.20.x for private local preview only \u2014 do not redistribute.",
  "globals": {
    "slotCount": 4,
    "facing": "north",
    "showAllSlots": false,
    "showAxes": false,
    "wallTexture": "textures/tfc/block/rock/raw/andesite.png",
    "floorTexture": "textures/tfc/block/rock/raw/granite.png"
  },
  "toolTypes": [
    {
      "id": "axe",
      "name": "Axe",
      "group": "default",
      "itemId": "tfc:metal/axe/wrought_iron",
      "texture": "textures/tfc/item/metal/axe/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "pickaxe",
      "name": "Pickaxe",
      "group": "default",
      "itemId": "tfc:metal/pickaxe/wrought_iron",
      "texture": "textures/tfc/item/metal/pickaxe/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "shovel",
      "name": "Shovel",
      "group": "default",
      "itemId": "tfc:metal/shovel/wrought_iron",
      "texture": "textures/tfc/item/metal/shovel/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "hoe",
      "name": "Hoe",
      "group": "default",
      "itemId": "tfc:metal/hoe/wrought_iron",
      "texture": "textures/tfc/item/metal/hoe/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "hammer",
      "name": "Hammer",
      "group": "default",
      "itemId": "tfc:metal/hammer/wrought_iron",
      "texture": "textures/tfc/item/metal/hammer/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "propick",
      "name": "Propick",
      "group": "default",
      "itemId": "tfc:metal/propick/wrought_iron",
      "texture": "textures/tfc/item/metal/propick/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "scythe",
      "name": "Scythe",
      "group": "default",
      "itemId": "tfc:metal/scythe/wrought_iron",
      "texture": "textures/tfc/item/metal/scythe/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "javelin",
      "name": "Javelin",
      "group": "default",
      "itemId": "tfc:metal/javelin/wrought_iron",
      "texture": "textures/tfc/item/metal/javelin/wrought_iron.png",
      "tags": [],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.34,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "sword",
      "name": "Sword",
      "group": "clear_wall",
      "itemId": "tfc:metal/sword/wrought_iron",
      "texture": "textures/tfc/item/metal/sword/wrought_iron.png",
      "tags": [
        "lean_clear_wall"
      ],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.27,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "mace",
      "name": "Mace",
      "group": "clear_wall",
      "itemId": "tfc:metal/mace/wrought_iron",
      "texture": "textures/tfc/item/metal/mace/wrought_iron.png",
      "tags": [
        "lean_clear_wall"
      ],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.27,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "fishing_rod",
      "name": "Fishing rod",
      "group": "clear_wall",
      "itemId": "tfc:metal/fishing_rod/wrought_iron",
      "texture": "textures/tfc/item/metal/fishing_rod/wrought_iron.png",
      "tags": [
        "lean_clear_wall"
      ],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.27,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "spindle",
      "name": "Spindle",
      "group": "clear_wall",
      "itemId": "tfc:spindle",
      "texture": "textures/tfc/item/spindle.png",
      "tags": [
        "lean_clear_wall"
      ],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.27,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "firestarter",
      "name": "Firestarter",
      "group": "clear_wall",
      "itemId": "tfc:firestarter",
      "texture": "textures/tfc/item/firestarter.png",
      "tags": [
        "lean_clear_wall"
      ],
      "flatSprite": true,
      "flipFacing": false,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.27,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "saw",
      "name": "Saw",
      "group": "flip_facing",
      "itemId": "tfc:metal/saw/wrought_iron",
      "texture": "textures/tfc/item/metal/saw/wrought_iron.png",
      "tags": [
        "lean_clear_wall",
        "lean_flip_facing"
      ],
      "flatSprite": true,
      "flipFacing": true,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.27,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "chisel",
      "name": "Chisel",
      "group": "flip_facing",
      "itemId": "tfc:metal/chisel/wrought_iron",
      "texture": "textures/tfc/item/metal/chisel/wrought_iron.png",
      "tags": [
        "lean_clear_wall",
        "lean_flip_facing"
      ],
      "flatSprite": true,
      "flipFacing": true,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.27,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "knife",
      "name": "Knife",
      "group": "closer_wall",
      "itemId": "tfc:metal/knife/wrought_iron",
      "texture": "textures/tfc/item/metal/knife/wrought_iron.png",
      "tags": [
        "lean_clear_wall",
        "lean_closer_wall",
        "lean_flip_facing"
      ],
      "flatSprite": true,
      "flipFacing": true,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.32,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    },
    {
      "id": "tuyere",
      "name": "Tuyere",
      "group": "closer_wall",
      "itemId": "tfc:metal/tuyere/wrought_iron",
      "texture": "textures/tfc/item/metal/tuyere/wrought_iron.png",
      "tags": [
        "lean_clear_wall",
        "lean_closer_wall",
        "lean_flip_facing"
      ],
      "flatSprite": true,
      "flipFacing": true,
      "extrude3d": true,
      "leanAngle": 28,
      "wallOffset": -0.32,
      "centerY": 0.42,
      "scale": 0.72,
      "uprightTurn": -45,
      "flipFacingTurn": 90,
      "slotSpacing": 0.2,
      "extraRotX": 0,
      "extraRotY": 0,
      "extraRotZ": 0,
      "nudgeX": 0,
      "nudgeY": 0,
      "nudgeZ": 0,
      "toolLength": 1,
      "toolWidth": 1,
      "thicknessScale": 1
    }
  ]
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
const texturePickerEl = document.getElementById("texture-picker");

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
    ? new THREE.MeshStandardMaterial({ map: tex, roughness: 0.95, metalness: 0.02 })
    : new THREE.MeshStandardMaterial({ color: fallbackColor, roughness: 0.92, metalness: 0.05 });
  const mesh = new THREE.Mesh(geo, mat);
  mesh.position.set(x + 0.5, y + 0.5, z + 0.5);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  mesh.add(
    new THREE.LineSegments(
      new THREE.EdgesGeometry(geo),
      new THREE.LineBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.28 })
    )
  );
  return mesh;
}

async function buildEnvironment() {
  while (world.children.length) world.remove(world.children[0]);
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

async function createToolSprite(tool) {
  const root = new THREE.Group();
  const path = tool.texture;
  const tex = await loadTex(path);
  const w = tool.toolWidth || 1;
  const h = tool.toolLength || 1;
  const thickness = tool.thicknessScale ?? 1;
  const useExtrude = tool.extrude3d !== false;

  let group;
  if (useExtrude && tex && path) {
    try {
      const mesh = await createExtrudedItemMesh(path, tex, {
        width: w,
        height: h,
        thicknessScale: thickness,
      });
      group = new THREE.Group();
      group.add(mesh);
    } catch (err) {
      console.warn("Extrude failed, falling back to flat", path, err);
      group = null;
    }
  }

  if (!group) {
    const geo = new THREE.PlaneGeometry(w, h);
    const mat = tex
      ? new THREE.MeshStandardMaterial({
          map: tex,
          transparent: true,
          alphaTest: 0.1,
          side: THREE.DoubleSide,
          roughness: 0.7,
          metalness: 0.15,
        })
      : new THREE.MeshStandardMaterial({
          color: 0x888888,
          roughness: 0.6,
          metalness: 0.2,
          side: THREE.DoubleSide,
        });
    group = new THREE.Group();
    group.add(new THREE.Mesh(geo, mat));
  }

  root.add(group);
  if (config.globals.showAxes) {
    root.add(new THREE.AxesHelper(0.35));
  }
  root.userData.texturePath = path;
  return root;
}

function applyLeanPose(root, tool, slot, slotCount, facing = "north") {
  const lateral = (slot - (slotCount - 1) / 2) * tool.slotSpacing;
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
  mulT(lateral + tool.nudgeX, tool.centerY + tool.nudgeY, tool.wallOffset + tool.nudgeZ);
  mulR(new THREE.Vector3(1, 0, 0), -tool.leanAngle);
  if (tool.flipFacing) mulR(new THREE.Vector3(0, 0, 1), tool.flipFacingTurn);
  if (tool.flatSprite) mulR(new THREE.Vector3(0, 0, 1), tool.uprightTurn);
  if (tool.extraRotX) mulR(new THREE.Vector3(1, 0, 0), tool.extraRotX);
  if (tool.extraRotY) mulR(new THREE.Vector3(0, 1, 0), tool.extraRotY);
  if (tool.extraRotZ) mulR(new THREE.Vector3(0, 0, 1), tool.extraRotZ);
  mulS(tool.scale);

  root.matrixAutoUpdate = false;
  root.matrix.copy(m);
  root.matrixWorldNeedsUpdate = true;
}

async function rebuildTools() {
  for (const t of toolRoots) {
    world.remove(t);
    t.traverse((o) => {
      if (o.geometry && !o.userData?.sharedGeometry && !o.geometry.userData?.shared) {
        o.geometry.dispose();
      }
      if (o.material) {
        if (Array.isArray(o.material)) o.material.forEach((m) => m.dispose());
        else o.material.dispose();
      }
    });
  }
  toolRoots.length = 0;

  const tool = activeTool();
  if (!tool) return;

  const indices = showAllSlotsEl.checked
    ? [...Array(config.globals.slotCount).keys()]
    : [Math.floor((config.globals.slotCount - 1) / 2)];

  for (const slot of indices) {
    const sprite = await createToolSprite(tool);
    applyLeanPose(sprite, tool, slot, config.globals.slotCount, config.globals.facing);
    world.add(sprite);
    toolRoots.push(sprite);
  }
  updateWallMeter(tool);
}

function updateWallMeter(tool) {
  const z = tool.wallOffset + tool.nudgeZ;
  const gap = Math.abs(z - -0.5);
  wallMeter.textContent = `wall gap: ${gap.toFixed(3)} · foot z: ${z.toFixed(3)}`;
}

function activeTool() {
  return config.toolTypes.find((t) => t.id === activeId);
}

function baselineTool(id) {
  return baseline.toolTypes.find((t) => t.id === id);
}

function formatNum(n) {
  const v = Number(n);
  if (Number.isInteger(v)) return String(v);
  return String(Math.round(v * 100) / 100);
}

function syncControlsFromTool(tool) {
  for (const key of NUMBER_KEYS) {
    const el = document.getElementById(key);
    el.value = tool[key];
    const label = document.querySelector(`.val[data-for="${key}"]`);
    if (label) label.textContent = formatNum(tool[key]);
  }
  for (const key of BOOL_KEYS) {
    document.getElementById(key).checked = !!tool[key];
  }
  if (texturePickerEl) {
    texturePickerEl.querySelector("img").src = tool.texture;
    texturePickerEl.querySelector(".tex-path").textContent = tool.texture.replace(/^textures\/tfc\/item\//, "");
  }
}

function selectTool(id) {
  activeId = id;
  [...catList.querySelectorAll(".cat-btn")].forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.id === id);
  });
  const tool = activeTool();
  catTitle.textContent = tool.name;
  catDesc.textContent = `${tool.itemId} · group ${GROUP_LABELS[tool.group] || tool.group}` +
    (tool.tags?.length ? ` · ${tool.tags.join(", ")}` : "");
  syncControlsFromTool(tool);
  void rebuildTools();
}

function renderToolList() {
  catList.innerHTML = "";
  const groups = [];
  for (const tool of config.toolTypes) {
    if (!groups.includes(tool.group)) groups.push(tool.group);
  }
  for (const group of groups) {
    const header = document.createElement("div");
    header.className = "group-header";
    header.textContent = GROUP_LABELS[group] || group;
    catList.appendChild(header);
    for (const tool of config.toolTypes.filter((t) => t.group === group)) {
      const btn = document.createElement("button");
      btn.type = "button";
      btn.className = "cat-btn";
      btn.dataset.id = tool.id;
      btn.innerHTML =
        `<strong><img class="cat-thumb" src="${tool.texture}" alt="" width="20" height="20" />${tool.name}</strong>` +
        `<span>z=${tool.wallOffset}${tool.flipFacing ? " · flip" : ""}</span>`;
      btn.addEventListener("click", () => selectTool(tool.id));
      catList.appendChild(btn);
    }
  }
}

function bindControls() {
  for (const key of NUMBER_KEYS) {
    const el = document.getElementById(key);
    el.addEventListener("input", () => {
      const tool = activeTool();
      tool[key] = parseFloat(el.value);
      const label = document.querySelector(`.val[data-for="${key}"]`);
      if (label) label.textContent = formatNum(tool[key]);
      if (key === "toolLength" || key === "toolWidth" || key === "thicknessScale") rebuildTools();
      else {
        refreshPosesOnly();
        persist();
      }
    });
  }
  for (const key of BOOL_KEYS) {
    document.getElementById(key).addEventListener("change", (e) => {
      activeTool()[key] = e.target.checked;
      if (key === "extrude3d") rebuildTools();
      else refreshPosesOnly();
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
  const tool = activeTool();
  const indices = showAllSlotsEl.checked
    ? [...Array(config.globals.slotCount).keys()]
    : [Math.floor((config.globals.slotCount - 1) / 2)];
  if (toolRoots.length !== indices.length) {
    rebuildTools();
    persist();
    return;
  }
  toolRoots.forEach((root, i) => {
    applyLeanPose(root, tool, indices[i], config.globals.slotCount, config.globals.facing);
  });
  updateWallMeter(tool);
  persist();
}

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(config));
}

function deepClone(o) {
  return JSON.parse(JSON.stringify(o));
}

function mergeTool(saved, base) {
  return { ...deepClone(base), ...saved, texture: saved.texture || base.texture };
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
  // Migrate old category-based saves by ignoring them (v4 key).
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      config = deepClone(baseline);
      config.globals = { ...config.globals, ...(parsed.globals || {}) };
      const list = parsed.toolTypes || [];
      config.toolTypes = baseline.toolTypes.map((b) => {
        const s = list.find((t) => t.id === b.id);
        return s ? mergeTool(s, b) : deepClone(b);
      });
      for (const s of list) {
        if (!config.toolTypes.find((t) => t.id === s.id)) config.toolTypes.push(deepClone(s));
      }
    } catch {
      config = deepClone(baseline);
    }
  } else {
    config = deepClone(baseline);
  }
  showAllSlotsEl.checked = !!config.globals.showAllSlots;
  if (showAxesEl) showAxesEl.checked = !!config.globals.showAxes;
}

function num(v) {
  const n = Number(v);
  return Number.isInteger(n) ? String(n) : String(Math.round(n * 1000) / 1000);
}

function exportJava() {
  const byId = Object.fromEntries(config.toolTypes.map((t) => [t.id, t]));
  const axe = byId.axe;
  const sword = byId.sword;
  const knife = byId.knife;
  const saw = byId.saw;

  const lines = [];
  lines.push("// Generated by TFC Lean Mesh Visualizer (per tool-type configs)");
  lines.push("// Shared tag defaults taken from representative types: axe / sword / saw / knife.");
  lines.push(`private static final float LEAN_ANGLE = ${num(axe.leanAngle)}f;`);
  lines.push(`private static final float SLOT_SPACING = ${num(axe.slotSpacing)}f;`);
  lines.push(`private static final float CENTER_Y = ${num(axe.centerY)}f;`);
  lines.push(`private static final float WALL_OFFSET = ${num(axe.wallOffset)}f;`);
  lines.push(`private static final float CLEAR_WALL_OFFSET = ${num(sword.wallOffset)}f;`);
  lines.push(`private static final float CLOSER_WALL_OFFSET = ${num(knife.wallOffset)}f;`);
  lines.push(`private static final float SCALE = ${num(axe.scale)}f;`);
  lines.push(`private static final float UPRIGHT_TURN = ${num(axe.uprightTurn)}f;`);
  lines.push(`private static final float FLIP_FACING_TURN = ${num(saw.flipFacingTurn)}f;`);
  lines.push("");
  lines.push("// Per tool-type overrides (wire into renderer / config if needed):");
  for (const t of config.toolTypes) {
    lines.push(
      `// ${t.id}: lean=${num(t.leanAngle)} wallZ=${num(t.wallOffset)} y=${num(t.centerY)} scale=${num(t.scale)} ` +
        `upright=${num(t.uprightTurn)} flip=${t.flipFacing}?${num(t.flipFacingTurn)} ` +
        `extra=(${num(t.extraRotX)},${num(t.extraRotY)},${num(t.extraRotZ)}) ` +
        `nudge=(${num(t.nudgeX)},${num(t.nudgeY)},${num(t.nudgeZ)})`
    );
  }
  exportOut.value = lines.join("\n");
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
  const b = baselineTool(activeId);
  if (!b) return;
  const idx = config.toolTypes.findIndex((t) => t.id === activeId);
  config.toolTypes[idx] = deepClone(b);
  selectTool(activeId);
  persist();
});

document.getElementById("btn-reset-all").addEventListener("click", () => {
  config = deepClone(baseline);
  renderToolList();
  selectTool(config.toolTypes[0].id);
  persist();
});

document.getElementById("btn-save").addEventListener("click", downloadJson);
document.getElementById("btn-load").addEventListener("click", () => fileInput.click());
fileInput.addEventListener("change", async () => {
  const file = fileInput.files?.[0];
  if (!file) return;
  const parsed = JSON.parse(await file.text());
  config = deepClone(baseline);
  config.globals = { ...config.globals, ...(parsed.globals || {}) };
  const list = parsed.toolTypes || [];
  config.toolTypes = (list.length ? list : baseline.toolTypes).map((t) => {
    const b = baselineTool(t.id);
    return b ? mergeTool(t, b) : deepClone(t);
  });
  renderToolList();
  selectTool(config.toolTypes[0]?.id || activeId);
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
renderToolList();
bindControls();
selectTool(config.toolTypes[0].id);
onResize();
animate();
exportJson();
