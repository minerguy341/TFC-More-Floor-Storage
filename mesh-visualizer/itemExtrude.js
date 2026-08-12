/**
 * Minecraft-style generated item mesh: extrude opaque texels into a thin 3D slab
 * (same idea as ItemModelGenerator — front/back + rim faces on silhouette edges).
 */

import * as THREE from "three";

const imageCache = new Map();
const geometryCache = new Map();

function loadImage(path) {
  if (imageCache.has(path)) return imageCache.get(path);
  const promise = new Promise((resolve, reject) => {
    const img = new Image();
    img.decoding = "async";
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(`Failed to load ${path}`));
    img.src = path;
  });
  imageCache.set(path, promise);
  return promise;
}

function isOpaque(data, size, x, y, alphaCutoff = 16) {
  if (x < 0 || y < 0 || x >= size || y >= size) return false;
  return data[(y * size + x) * 4 + 3] > alphaCutoff;
}

/**
 * Build a BufferGeometry in model space [-0.5,0.5]^2 on XY, thickness 1/size on Z
 * (matches MC: 16×16 item, 1 texel deep, centered).
 */
function buildExtrudedGeometry(imageData, size) {
  const data = imageData.data;
  const px = 1 / size;
  const z0 = -px / 2;
  const z1 = px / 2;

  const positions = [];
  const normals = [];
  const uvs = [];
  const indices = [];
  let v = 0;

  const pushQuad = (corners, uvCorners, normal) => {
    for (const c of corners) positions.push(c[0], c[1], c[2]);
    for (const uv of uvCorners) uvs.push(uv[0], uv[1]);
    for (let i = 0; i < 4; i++) normals.push(normal[0], normal[1], normal[2]);
    indices.push(v, v + 1, v + 2, v, v + 2, v + 3);
    v += 4;
  };

  for (let ty = 0; ty < size; ty++) {
    for (let tx = 0; tx < size; tx++) {
      if (!isOpaque(data, size, tx, ty)) continue;

      const x0 = tx * px - 0.5;
      const x1 = (tx + 1) * px - 0.5;
      const y1 = 0.5 - ty * px;
      const y0 = 0.5 - (ty + 1) * px;

      const u0 = tx / size;
      const u1 = (tx + 1) / size;
      const vBot = 1 - (ty + 1) / size;
      const vTop = 1 - ty / size;
      const uC = (u0 + u1) * 0.5;
      const vC = (vBot + vTop) * 0.5;

      pushQuad(
        [
          [x0, y0, z1],
          [x1, y0, z1],
          [x1, y1, z1],
          [x0, y1, z1],
        ],
        [
          [u0, vBot],
          [u1, vBot],
          [u1, vTop],
          [u0, vTop],
        ],
        [0, 0, 1]
      );

      pushQuad(
        [
          [x1, y0, z0],
          [x0, y0, z0],
          [x0, y1, z0],
          [x1, y1, z0],
        ],
        [
          [u1, vBot],
          [u0, vBot],
          [u0, vTop],
          [u1, vTop],
        ],
        [0, 0, -1]
      );

      const rimUV = [
        [uC, vC],
        [uC, vC],
        [uC, vC],
        [uC, vC],
      ];

      if (!isOpaque(data, size, tx, ty - 1)) {
        pushQuad(
          [
            [x0, y1, z1],
            [x1, y1, z1],
            [x1, y1, z0],
            [x0, y1, z0],
          ],
          rimUV,
          [0, 1, 0]
        );
      }
      if (!isOpaque(data, size, tx, ty + 1)) {
        pushQuad(
          [
            [x1, y0, z1],
            [x0, y0, z1],
            [x0, y0, z0],
            [x1, y0, z0],
          ],
          rimUV,
          [0, -1, 0]
        );
      }
      if (!isOpaque(data, size, tx - 1, ty)) {
        pushQuad(
          [
            [x0, y0, z0],
            [x0, y0, z1],
            [x0, y1, z1],
            [x0, y1, z0],
          ],
          rimUV,
          [-1, 0, 0]
        );
      }
      if (!isOpaque(data, size, tx + 1, ty)) {
        pushQuad(
          [
            [x1, y0, z1],
            [x1, y0, z0],
            [x1, y1, z0],
            [x1, y1, z1],
          ],
          rimUV,
          [1, 0, 0]
        );
      }
    }
  }

  const geo = new THREE.BufferGeometry();
  geo.setAttribute("position", new THREE.Float32BufferAttribute(positions, 3));
  geo.setAttribute("normal", new THREE.Float32BufferAttribute(normals, 3));
  geo.setAttribute("uv", new THREE.Float32BufferAttribute(uvs, 2));
  geo.setIndex(indices);
  geo.computeBoundingBox();
  geo.computeBoundingSphere();
  return geo;
}

async function getGeometry(path) {
  if (geometryCache.has(path)) return geometryCache.get(path);
  const promise = (async () => {
    const img = await loadImage(path);
    const canvas = document.createElement("canvas");
    canvas.width = img.width;
    canvas.height = img.height;
    const ctx = canvas.getContext("2d", { willReadFrequently: true });
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.drawImage(img, 0, 0);
    const dim = Math.min(canvas.width, canvas.height);
    const imageData = ctx.getImageData(0, 0, dim, dim);
    const geo = buildExtrudedGeometry(imageData, dim);
    geo.userData.shared = true;
    return geo;
  })();
  geometryCache.set(path, promise);
  return promise;
}

/**
 * @param {string} path texture URL
 * @param {THREE.Texture} threeTex already-configured Three texture (nearest, srgb)
 * @param {{width?: number, height?: number, thicknessScale?: number}} opts
 */
export async function createExtrudedItemMesh(path, threeTex, opts = {}) {
  const width = opts.width ?? 1;
  const height = opts.height ?? 1;
  const thicknessScale = opts.thicknessScale ?? 1;

  const geo = await getGeometry(path);
  const mat = new THREE.MeshStandardMaterial({
    map: threeTex,
    transparent: true,
    alphaTest: 0.1,
    roughness: 0.72,
    metalness: 0.18,
    side: THREE.FrontSide,
  });

  const mesh = new THREE.Mesh(geo, mat);
  mesh.scale.set(width, height, thicknessScale);
  mesh.castShadow = true;
  mesh.receiveShadow = true;
  mesh.userData.sharedGeometry = true;
  return mesh;
}

export function clearExtrusionCache() {
  for (const geo of geometryCache.values()) {
    Promise.resolve(geo).then((g) => g.dispose?.());
  }
  geometryCache.clear();
  imageCache.clear();
}
