// Voxel concept renders of the future Murim structures, evaluated inside an isolated Blockbench (three.js) instance by
// render-concepts.cjs. 1 voxel = 1 Minecraft block. Nothing here is a Blockbench project: voxels are drawn as
// instanced cubes straight into the scene, lit, fogged, rendered offscreen and removed again.
window.ST = (() => {
  const U = 16; // scene units per block
  let vox = new Map();
  const key = (x, y, z) => `${x},${y},${z}`;

  // ---------------------------------------------------------------- materials (colour + pixel texture)
  const MATS = {
    grass: ['#6FA04A', 'grass'], grass_dry: ['#A3A55C', 'grass'], grass_lush: ['#5E9E3C', 'grass'], dirt: ['#7A5A3C', 'noise'],
    stone: ['#8C8C88', 'noise'], stone_brick: ['#8E8D88', 'brick'], cobble: ['#7E7E7A', 'cobble'], limestone: ['#B4B3A8', 'streak'],
    limestone_moss: ['#8A9A70', 'noise'], granite: ['#C2BCB0', 'speck'], sandstone: ['#D2B98A', 'bed'], sandstone_layer: ['#C2A070', 'bed'],
    loess: ['#C8A86B', 'noise'], red_loess: ['#A8603E', 'noise'], rammed: ['#BFA37A', 'bed'], mud_brick: ['#9C7B5A', 'brick'],
    gravel: ['#5E4E40', 'cobble'], sand: ['#D8C59A', 'noise'], water: ['#4E8FA0', 'water', 0.82], water_jade: ['#3E8F7E', 'water', 0.85],
    tile: ['#46484C', 'tile'], tile_green: ['#3F7D5A', 'tile'], tile_yellow: ['#D4A93A', 'tile'], ridge: ['#2F3134', 'noise'],
    red: ['#A8352A', 'plank'], red_wall: ['#9E4A3C', 'noise'], wood_dark: ['#4A3426', 'plank'], wood: ['#8A6440', 'plank'],
    wood_light: ['#B08A5A', 'plank'], white: ['#E8E4DA', 'plaster'], plaster_grey: ['#CFCAC0', 'plaster'], thatch: ['#B89A5A', 'thatch'],
    bamboo: ['#7FA048', 'bamboo'], bamboo_dark: ['#5C7E30', 'bamboo'], leaves: ['#3F7A34', 'leaves'], leaves_dark: ['#2C4A33', 'leaves'],
    leaves_pink: ['#E86A9A', 'leaves'], leaves_light: ['#8DB45A', 'leaves'], leaves_gold: ['#D8A92E', 'leaves'], leaves_osm: ['#2F5A2E', 'leaves'],
    bark: ['#5F5A50', 'noise'], bark_red: ['#6B4A34', 'noise'], lantern: ['#E0503A', 'glow'], gold: ['#E2B94A', 'noise'],
    lotus: ['#5E9A3A', 'leaves'], lotus_flower: ['#F4A0B9', 'noise'], field: ['#8DAA4A', 'field'], paddy: ['#6E9A4E', 'water', 0.9],
    snow: ['#F2F4F2', 'noise'], hay: ['#C9A54A', 'thatch'], fire: ['#F2A33A', 'glow'], banner: ['#B8322A', 'noise'],
    cloud: ['#F4F6F8', 'plaster', 0.8], smoke: ['#8E8C88', 'noise', 0.55], skin: ['#D9A882', 'noise'], hair: ['#2A2320', 'noise'],
    robe_white: ['#E9E4D6', 'plaster'], robe_blue: ['#3B5E8C', 'noise'], robe_black: ['#2B2B2E', 'noise'], robe_red: ['#9E2E2A', 'noise'],
    armor: ['#6E6A62', 'brick'], teal: ['#1D7A84', 'noise'], camel: ['#C8A06A', 'noise'], rice: ['#9CBF5A', 'field'],
  };
  const textures = {};
  function tex(name) {
    if (textures[name]) return textures[name];
    const [hex, style] = MATS[name];
    const c = document.createElement('canvas'); c.width = c.height = 16;
    const g = c.getContext('2d');
    const base = new THREE.Color(hex);
    let seed = [...name].reduce((a, ch) => a * 31 + ch.charCodeAt(0), 7);
    const rnd = () => ((seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
    const px = (x, y, f) => { const col = base.clone().multiplyScalar(f); g.fillStyle = `rgb(${col.r * 255 | 0},${col.g * 255 | 0},${col.b * 255 | 0})`; g.fillRect(x, y, 1, 1); };
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      let f = 0.9 + rnd() * 0.18;
      switch (style) {
        case 'brick': if (y % 4 === 3 || (x + (y >> 2) * 4) % 8 === 0) f = 0.72; break;
        case 'tile': if (x % 4 === 0) f = 0.7; else if (y % 4 === 3) f = 0.82; else f = 1.0 + (x % 4) * 0.04; break;
        case 'plank': if (y % 4 === 3) f = 0.75; break;
        case 'bed': if (y % 5 === 4) f = 0.82; break;
        case 'streak': if ((x * 7) % 11 === 0) f = 0.78; break;
        case 'speck': if (rnd() < 0.08) f = rnd() < 0.5 ? 0.45 : 1.18; break;
        case 'cobble': if ((x % 5 === 0) || (y % 5 === 0)) f = 0.72; break;
        case 'leaves': if (rnd() < 0.18) f = 0.68; else if (rnd() < 0.15) f = 1.18; break;
        case 'thatch': f = 0.85 + ((x * 3 + y) % 5) * 0.06; break;
        case 'bamboo': if (y % 8 === 0) f = 1.2; else if (x < 5) f = 0.85; break;
        case 'water': f = 0.92 + Math.sin((x + y * 2) * 0.8) * 0.06; break;
        case 'plaster': f = 0.96 + rnd() * 0.06; break;
        case 'field': f = (x % 3 === 0) ? 0.8 : 1.05; break;
        case 'glow': f = 1.0; break;
        case 'grass': f = 0.88 + rnd() * 0.22; break;
      }
      px(x, y, f);
    }
    const t = new THREE.CanvasTexture(c);
    t.magFilter = THREE.NearestFilter; t.minFilter = THREE.NearestFilter;
    // The canvas holds sRGB colours: say so, or the renderer brightens them as if they were linear.
    if (THREE.SRGBColorSpace) t.colorSpace = THREE.SRGBColorSpace; else if (THREE.sRGBEncoding) t.encoding = THREE.sRGBEncoding;
    return textures[name] = t;
  }

  // ---------------------------------------------------------------- voxel editing
  function set(x, y, z, m) { if (m) vox.set(key(x, y, z), m); else vox.delete(key(x, y, z)); }
  function get(x, y, z) { return vox.get(key(x, y, z)); }
  function fill(x0, y0, z0, x1, y1, z1, m) {
    for (let x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) for (let y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
      for (let z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, m);
  }
  function walls(x0, y0, z0, x1, y1, z1, m) {
    for (let y = y0; y <= y1; y++) for (let x = x0; x <= x1; x++) for (let z = z0; z <= z1; z++)
      if (x === x0 || x === x1 || z === z0 || z === z1) set(x, y, z, m);
  }
  function clear() { vox = new Map(); }
  /** Ground: height(x, z) gives the top y; top material, then soil `depth` deep. */
  function ground(x0, z0, x1, z1, height, top, sub, depth = 4) {
    for (let x = x0; x <= x1; x++) for (let z = z0; z <= z1; z++) {
      const h = Math.round(height(x, z));
      for (let y = h - depth; y <= h; y++) set(x, y, z, y === h ? (typeof top === 'function' ? top(x, z, h) : top) : sub);
    }
  }
  function water(x0, z0, x1, z1, y, m = 'water') { fill(x0, y, z0, x1, y, z1, m); }

  /**
   * Chinese roof over a rectangle: concave stepped profile (flat near the eaves, steep near the ridge), upturned
   * corners, dark ridge with raised ends. type: 'hip' (xieshan/wudian look), 'gable' (xuanshan), 'pyramid' (pavilion).
   */
  function roof(x0, z0, x1, z1, y, { type = 'hip', mat = 'tile', ridge = 'ridge', overhang = 1, lift = true } = {}) {
    x0 -= overhang; z0 -= overhang; x1 += overhang; z1 += overhang;
    const w = x1 - x0, d = z1 - z0, alongX = w >= d;
    const steps = Math.ceil(Math.min(w, d) / 2);
    let yy = y, lastY = y;
    for (let i = 0; i <= steps; i++) {
      const ix = type === 'gable' && alongX ? 0 : i, iz = type === 'gable' && !alongX ? 0 : i;
      const ax0 = x0 + (type === 'gable' && alongX ? 0 : ix), ax1 = x1 - (type === 'gable' && alongX ? 0 : ix);
      const az0 = z0 + (type === 'gable' && !alongX ? 0 : iz), az1 = z1 - (type === 'gable' && !alongX ? 0 : iz);
      if (ax0 > ax1 || az0 > az1) break;
      lastY = yy;
      for (let x = ax0; x <= ax1; x++) for (let z = az0; z <= az1; z++) {
        const edge = x === ax0 || x === ax1 || z === az0 || z === az1;
        if (edge) set(x, yy, z, mat);
      }
      // Concave curve: the first steps barely rise, the upper ones rise faster.
      const t = i / steps;
      yy += t < 0.3 ? (i % 2 === 0 ? 1 : 0) : t < 0.7 ? 1 : 1;
      if (t >= 0.7 && i % 2 === 1) { // steeper near the ridge: an extra course
        for (let x = ax0 + 1; x <= ax1 - 1; x++) for (let z = az0 + 1; z <= az1 - 1; z++)
          if (x === ax0 + 1 || x === ax1 - 1 || z === az0 + 1 || z === az1 - 1) set(x, yy, z, mat);
        lastY = yy;
        yy++;
      }
    }
    yy = lastY + 1; // ridge sits on the last course
    // Ridge along the long axis with raised ends (chiwen).
    const cx0 = alongX ? x0 + (type === 'hip' ? steps : 0) : Math.round((x0 + x1) / 2);
    const cx1 = alongX ? x1 - (type === 'hip' ? steps : 0) : Math.round((x0 + x1) / 2);
    const cz0 = alongX ? Math.round((z0 + z1) / 2) : z0 + (type === 'hip' ? steps : 0);
    const cz1 = alongX ? Math.round((z0 + z1) / 2) : z1 - (type === 'hip' ? steps : 0);
    if (type !== 'pyramid') {
      fill(Math.min(cx0, cx1), yy, Math.min(cz0, cz1), Math.max(cx0, cx1), yy, Math.max(cz0, cz1), ridge);
      set(cx0, yy + 1, cz0, ridge); set(cx1, yy + 1, cz1, ridge);
    } else {
      fill(Math.round((x0 + x1) / 2), yy, Math.round((z0 + z1) / 2), Math.round((x0 + x1) / 2), yy + 1, Math.round((z0 + z1) / 2), ridge);
    }
    if (lift) for (const [x, z] of [[x0, z0], [x1, z0], [x0, z1], [x1, z1]]) set(x, y + 1, z, mat); // flying eaves
    return yy;
  }
  /** Timber hall: stone platform, columns on a bay grid, walls between, a roof. Faces south (+z). */
  function hall(x0, z0, bays, depth, y, { bay = 4, wall = 'white', column = 'red', roofMat = 'tile', roofType = 'hip', storey = 5, platform = 1, base = 'stone_brick', doors = true } = {}) {
    const x1 = x0 + bays * bay, z1 = z0 + depth;
    fill(x0 - 1, y, z0 - 1, x1 + 1, y + platform - 1, z1 + 1, base);
    const fy = y + platform;
    // Steps on the axis.
    const mid = Math.round((x0 + x1) / 2);
    for (let s = 0; s < platform; s++) fill(mid - 1, y + s, z1 + 2 + (platform - 1 - s), mid + 1, y + s, z1 + 2 + (platform - 1 - s), base);
    for (let yy = fy; yy < fy + storey; yy++) {
      for (let x = x0; x <= x1; x++) for (let z = z0; z <= z1; z++) {
        const onEdge = x === x0 || x === x1 || z === z0 || z === z1;
        if (!onEdge) continue;
        const isCol = ((x - x0) % bay === 0 && (z === z0 || z === z1)) || ((z - z0) % Math.max(2, Math.floor(depth / 2)) === 0 && (x === x0 || x === x1));
        set(x, yy, z, isCol ? column : wall);
      }
    }
    if (doors) for (let yy = fy; yy < fy + 3; yy++) for (let x = mid - 1; x <= mid + 1; x++) set(x, yy, z1, 'wood_dark');
    // Lattice windows on the side bays.
    for (let b = 0; b < bays; b++) {
      const wx = x0 + b * bay + Math.floor(bay / 2);
      if (Math.abs(wx - mid) <= 1) continue;
      set(wx, fy + 2, z1, 'wood'); set(wx, fy + 3, z1, 'wood');
    }
    // Beam band under the eaves.
    for (let x = x0; x <= x1; x++) { set(x, fy + storey, z0, 'wood_dark'); set(x, fy + storey, z1, 'wood_dark'); }
    for (let z = z0; z <= z1; z++) { set(x0, fy + storey, z, 'wood_dark'); set(x1, fy + storey, z, 'wood_dark'); }
    return roof(x0, z0, x1, z1, fy + storey + 1, { type: roofType, mat: roofMat });
  }
  function pavilion(cx, cz, y, r = 2, { column = 'red', roofMat = 'tile', h = 4 } = {}) {
    fill(cx - r - 1, y, cz - r - 1, cx + r + 1, y, cz + r + 1, 'stone_brick');
    for (const [dx, dz] of [[-r, -r], [r, -r], [-r, r], [r, r]]) fill(cx + dx, y + 1, cz + dz, cx + dx, y + h, cz + dz, column);
    roof(cx - r, cz - r, cx + r, cz + r, y + h + 1, { type: 'pyramid', mat: roofMat });
  }
  /** Round-ish crown of leaves. */
  function blob(cx, cy, cz, rx, ry, rz, m, holes = 0.12, rnd = Math.random) {
    for (let x = -rx; x <= rx; x++) for (let y = -ry; y <= ry; y++) for (let z = -rz; z <= rz; z++) {
      const d = (x * x) / (rx * rx + 0.5) + (y * y) / (ry * ry + 0.5) + (z * z) / (rz * rz + 0.5);
      if (d <= 1 && rnd() > holes) set(cx + x, cy + y, cz + z, m);
    }
  }
  function tree(x, y, z, { trunk = 'bark', leaves = 'leaves', h = 5, r = 3, flat = false, rnd = Math.random } = {}) {
    fill(x, y + 1, z, x, y + h, z, trunk);
    blob(x, y + h + (flat ? 0 : 1), z, r, flat ? 1 : Math.max(2, r - 1), r, leaves, 0.12, rnd);
  }
  function willow(x, y, z, rnd = Math.random) {
    tree(x, y, z, { trunk: 'bark', leaves: 'leaves_light', h: 5, r: 3, rnd });
    for (let i = 0; i < 26; i++) {
      const dx = Math.round((rnd() - 0.5) * 7), dz = Math.round((rnd() - 0.5) * 7), len = 2 + Math.floor(rnd() * 5);
      for (let k = 0; k < len; k++) if (!get(x + dx, y + 4 - k, z + dz)) set(x + dx, y + 4 - k, z + dz, 'leaves_light');
    }
  }
  function pine(x, y, z, h = 6, rnd = Math.random) {
    fill(x, y + 1, z, x, y + h, z, 'bark_red');
    blob(x + 1, y + h, z, 4, 0, 3, 'leaves_dark', 0.15, rnd);
    blob(x - 1, y + h + 2, z, 3, 0, 2, 'leaves_dark', 0.15, rnd);
    blob(x, y + h - 2, z + 1, 3, 0, 3, 'leaves_dark', 0.2, rnd);
  }
  function bamboo(x, y, z, h, rnd = Math.random) {
    fill(x, y + 1, z, x, y + h, z, rnd() < 0.5 ? 'bamboo' : 'bamboo_dark');
    for (let k = Math.floor(h * 0.6); k <= h; k++) for (const [dx, dz] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) if (rnd() < 0.45) set(x + dx, y + k, z + dz, 'leaves_light');
  }
  /** Karst tower or sandstone pillar for backgrounds. */
  function peak(cx, cz, y0, r, h, mat, top = 'leaves', rnd = Math.random) {
    for (let y = y0; y <= y0 + h; y++) {
      const t = (y - y0) / h;
      const rr = t < 0.85 ? r : r * Math.sqrt(Math.max(0, 1 - ((t - 0.85) / 0.15) ** 2));
      for (let x = -r - 1; x <= r + 1; x++) for (let z = -r - 1; z <= r + 1; z++)
        if (Math.hypot(x, z) <= rr + (rnd() - 0.5) * 0.8) set(cx + x, y, cz + z, mat);
    }
    if (top) blob(cx, y0 + h, cz, Math.max(1, r - 1), 1, Math.max(1, r - 1), top, 0.25, rnd);
  }
  function seeded(seed) { let s = seed; return () => ((s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff); }

  // ---------------------------------------------------------------- rendering
  function render(view, w, h, sky = '#BFD3E0', fogNear = 140, fogFar = 420) {
    const group = new THREE.Group();
    const byMat = {};
    for (const [k, m] of vox) (byMat[m] = byMat[m] || []).push(k);
    const geo = new THREE.BoxGeometry(U, U, U);
    const dummy = new THREE.Object3D();
    for (const [m, keys] of Object.entries(byMat)) {
      const [, , opacity] = MATS[m];
      const material = new THREE.MeshLambertMaterial({ map: tex(m), transparent: !!opacity, opacity: opacity || 1,
        emissive: MATS[m][1] === 'glow' ? new THREE.Color(MATS[m][0]).multiplyScalar(0.6) : new THREE.Color(0) });
      const mesh = new THREE.InstancedMesh(geo, material, keys.length);
      keys.forEach((k, i) => {
        const [x, y, z] = k.split(',').map(Number);
        dummy.position.set(x * U + U / 2, y * U + U / 2, z * U + U / 2);
        dummy.updateMatrix();
        mesh.setMatrixAt(i, dummy.matrix);
        // Cheap ambient occlusion: darker when covered.
        let cover = 0;
        for (let dy = 1; dy <= 3; dy++) if (vox.has(key(x, y + dy, z))) cover++;
        mesh.setColorAt(i, new THREE.Color(1, 1, 1).multiplyScalar(1 - cover * 0.1));
      });
      group.add(mesh);
    }
    const ambient = new THREE.AmbientLight(0xffffff, 0.42);
    const sun = new THREE.DirectionalLight(0xfff1dc, 0.55);
    sun.position.set(-0.6, 1, 0.45);
    const fill2 = new THREE.DirectionalLight(0xcfe0ff, 0.12);
    fill2.position.set(0.7, 0.4, -0.6);
    group.add(ambient, sun, fill2);
    Canvas.scene.add(group);
    const oldFog = Canvas.scene.fog, oldBg = Canvas.scene.background;
    Canvas.scene.fog = new THREE.Fog(new THREE.Color(sky), fogNear * U, fogFar * U);
    Canvas.scene.background = new THREE.Color(sky);
    const p = new Preview({ id: 'st_' + Math.random().toString(36).slice(2), offscreen: true, antialias: true });
    p.resize(w, h);
    const spec = { position: view.position.map(v => v * U), target: view.target.map(v => v * U), projection: 'perspective', fov: view.fov || 45 };
    p.loadAnglePreset(spec);
    p.camera.far = 100000;
    p.camera.updateProjectionMatrix();
    p.camera.lookAt(...spec.target);
    return new Promise(res => p.screenshot({ crop: false }, data => {
      Canvas.scene.remove(group);
      Canvas.scene.fog = oldFog; Canvas.scene.background = oldBg;
      group.traverse(o => { if (o.material) o.material.dispose(); });
      geo.dispose();
      p.delete();
      res(data);
    }));
  }

  /** A small figure for scale: legs, robe, head (2 blocks tall, like a player). */
  function person(x, y, z, robe = 'robe_blue', hat = 'hair') {
    set(x, y + 1, z, robe === 'armor' ? 'robe_black' : 'robe_black');
    set(x, y + 2, z, robe);
    set(x, y + 3, z, 'skin');
    set(x, y + 4, z, hat);
  }
  function camel(x, y, z) {
    fill(x, y + 1, z, x, y + 2, z, 'camel'); fill(x + 2, y + 1, z, x + 2, y + 2, z, 'camel');
    fill(x, y + 3, z, x + 2, y + 3, z, 'camel'); set(x + 1, y + 4, z, 'camel'); set(x + 3, y + 4, z, 'camel'); set(x + 3, y + 5, z, 'camel');
  }

  return { set, get, fill, walls, clear, person, camel, ground, water, roof, hall, pavilion, blob, tree, willow, pine, bamboo, peak, seeded, render,
    count: () => vox.size };
})();
true;
