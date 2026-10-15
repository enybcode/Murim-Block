// The blood crystal (a design, in Blockbench: art/blockbench/blood_crystal_body.bbmodel): not an armour but the player's
// own body changed, all of it but the head taken by blood turned to crystal. Under the skin, veins of glowing blood; through
// it, crusts of dark red crystal (the sternum round a glowing heart, the shoulders, the forearms); out of it, shards:
// clusters on the shoulders, great blades from the upper back, spines along the outer forearms and over the knuckles.
// The forearms' shards grow on their outer side and on their back (what faces up and out when the arms are held
// forward), so they show in first person.
// Apart from the body: ten small crystals of blood floating behind the back in a fan, each its own group (its pivot at its
// middle) to be animated on its own: drifting, turning, flying off.
// It is drawn over the player's own skin, whatever it is: a layer a quarter pixel over the body, clear where the skin
// shows, the crusts and shards standing on it. A plain body (pieces body_*) is in the project to see it on, hidden and
// not exported; it is not part of the change.
// With --kit, the kit to implement it (build/blood-crystal-kit, zipped): the overlay as an Epic Fight skinned mesh on the
// biped's joints (assets/murimblock/animmodels/overlay/blood_crystal_body.json), the floating crystals as a GeckoLib
// model and its hover animation (geo/, animations/), the texture, previews, a README and a prompt for Codex.
// On a player's body (Blockbench pixels: x the wearer's right, y up, z its back; it faces north, -z), each piece on the
// biped joint that carries it, as the armours (chief-armor.cjs): the waist bends at 18, the elbows at 17.
// Usage: node scripts/animation/blood-crystal-body.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/** X the darkest crystal, Y deep blood, Z blood red, Q bright red, H the glint, V black veins; S skin, N trousers (the plain body). */
const BLOOD = { key: 'blood_crystal', c: { X: '#2e040b', Y: '#5c0a18', Z: '#9b1429', Q: '#d63a52', H: '#ff9aa8', V: '#140105', S: '#c69c7b', N: '#3c2a22' } };

const PIECES = [];
const piece = (bone, name, from, to, paint, extra = {}) => PIECES.push({ bone, part: 'torso', name, from, to, paint, ...extra });

/** Crystal in facets: cut on the diagonal (light on one side of each cut), black veins here and there. */
const facets = (f, u, v) => {
  if (!isSide(f)) return 'Y';
  const d = (u + v) % 5, e = (u - v + 50) % 7;
  if (e === 0) return 'V';
  if (d === 0) return 'Q';
  if (d === 1) return 'Z';
  return (u * 3 + v) % 4 === 0 ? 'y' : 'Y';
};
/** Veins of blood under the skin: sparse thin lines crossing, brighter where they meet; clear (the skin) elsewhere. */
const veins = (f, u, v) => {
  if (!isSide(f)) return null;
  const a = (u + 2 * v) % 13, b = (2 * u - v + 44) % 17;
  if (a === 0 && b === 0) return 'Q';
  if (a === 0 || b === 0) return (u + v) % 3 ? 'Z' : 'Y';
  return null;
};
/** A crust of crystal broken through the skin: facets, a ragged edge letting the skin show. */
const crust = (f, u, v, w, h) => {
  const edge = Math.min(u, v, w - 1 - u, h - 1 - v);
  if (edge === 0 && (u * 7 + v * 3) % 3 === 0) return null;
  return facets(f, u, v);
};
/** A shard: dark at its root, blood red, bright toward its point, a glint on one edge. */
const shardPaint = (f, u, v, w, h) => {
  if (f === 'up') return 'H';
  if (f === 'down') return 'X';
  const t = v / Math.max(1, h - 1);
  if (u === 0 && t < 0.5) return 'H';
  return t < 0.25 ? 'Q' : t < 0.6 ? 'Z' : t < 0.85 ? 'Y' : 'X';
};

/**
 * A shard growing out of the body: rooted at `at`, `length` long toward `dir`, `width` thick. Its box stands up from
 * its root and is turned (x, then z, Blockbench's order) so that up points along `dir`.
 */
function shard(bone, name, at, dir, length, width = 1.2) {
  const l = Math.hypot(...dir), [dx, dy, dz] = dir.map(c => c / l);
  const rx = Math.asin(dz) * 180 / Math.PI, rz = Math.atan2(-dx, dy) * 180 / Math.PI;
  const w = width / 2;
  piece(bone, name, [at[0] - w, at[1], at[2] - w], [at[0] + w, at[1] + length, at[2] + w], shardPaint, { pivot: at, rot: [rx, 0, rz] });
}

// ---- a plain body to see the change on (not part of it)
const skin = (f, u, v) => ((u + v) % 5 === 0 ? 's' : 'S');
piece('Head', 'body_head', [-4, 24, -4], [4, 32, 4], (f, u, v) => (f === 'up' || v < 2 ? 'N' : f === 'north' && v === 4 && (u === 2 || u === 5) ? 'X' : 'S'));
piece('Chest', 'body_chest', [-4, 18, -2], [4, 24, 2], skin);
piece('Torso', 'body_belly', [-4, 12, -2], [4, 18, 2], skin);
for (const [bone, name, from, to] of [['Arm_R', 'body_arm_r', [4, 17, -2], [8, 24, 2]], ['Hand_R', 'body_forearm_r', [4, 12, -2], [8, 17, 2]],
  ['Arm_L', 'body_arm_l', [-8, 17, -2], [-4, 24, 2]], ['Hand_L', 'body_forearm_l', [-8, 12, -2], [-4, 17, 2]]])
  piece(bone, name, from, to, skin);
for (const [bone, name, from, to] of [['Thigh_R', 'body_thigh_r', [0, 6, -2], [4, 12, 2]], ['Leg_R', 'body_shin_r', [0, 0, -2], [4, 6, 2]],
  ['Thigh_L', 'body_thigh_l', [-4, 6, -2], [0, 12, 2]], ['Leg_L', 'body_shin_l', [-4, 0, -2], [0, 6, 2]]])
  piece(bone, name, from, to, skin);

// ---- the changed skin: veins, the crystal heart, the crusts of the sternum and the back
piece('Chest', 'veins_chest', [-4.25, 18, -2.25], [4.25, 24.25, 2.25], veins, { hinge: { y: 18, bone: 'Torso' } });
piece('Torso', 'veins_belly', [-4.25, 12, -2.25], [4.25, 18, 2.25], veins, { hinge: { y: 18, bone: 'Chest' } });
piece('Arm_R', 'veins_arm_r', [3.75, 17, -2.25], [8.25, 24.25, 2.25], veins);
piece('Arm_L', 'veins_arm_l', [-8.25, 17, -2.25], [-3.75, 24.25, 2.25], veins);
piece('Chest', 'sternum_crust', [-2.4, 17.6, -2.7], [2.4, 23.8, -2.2], crust);
piece('Chest', 'heart', [-1.3, 19.4, -3.5], [1.3, 22.6, -2.7], (f, u, v) => (f === 'north' ? (u === 1 && v === 1 ? 'H' : 'Q') : 'Z'));
piece('Chest', 'heart_rim', [-1.9, 18.8, -3.1], [1.9, 23.2, -2.7], rim((f, u, v, w, h) => (u === 0 || u === w - 1 || v === 0 || v === h - 1 ? 'X' : 'Y')));
piece('Chest', 'back_crust', [-3.4, 18.4, 2.2], [3.4, 23.8, 2.7], crust);

// ---- the legs: veins, crusts on the knees and shins, shards from the hips, thighs and calves
for (const [side, sx] of [['r', 1], ['l', -1]]) {
  const x = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const thigh = sx > 0 ? 'Thigh_R' : 'Thigh_L', leg = sx > 0 ? 'Leg_R' : 'Leg_L';
  piece(thigh, 'veins_thigh_' + side, [...x(-0.25, 4.25).slice(0, 1), 6, -2.25], [...x(-0.25, 4.25).slice(1), 12, 2.25], veins, { hinge: { y: 6, bone: leg } });
  piece(leg, 'veins_shin_' + side, [...x(-0.25, 4.25).slice(0, 1), 0, -2.25], [...x(-0.25, 4.25).slice(1), 6, 2.25], veins, { hinge: { y: 6, bone: thigh } });
  piece(leg, 'knee_crust_' + side, [...x(0.2, 3.8).slice(0, 1), 4.4, -2.7], [...x(0.2, 3.8).slice(1), 7.4, -2.2], crust);
  piece(leg, 'shin_crust_' + side, [...x(-0.4, 4.4).slice(0, 1), 0.6, -2.6], [...x(-0.4, 4.4).slice(1), 3.8, 2.6], rim(crust));
  piece(thigh, 'hip_crust_' + side, [...x(2.4, 4.45).slice(0, 1), 8.6, -1.8], [...x(2.4, 4.45).slice(1), 11.8, 1.8], crust);
  shard(thigh, 'hip_shard_' + side, [sx * 4.45, 10.6, 0.2], [sx * 1, 0.55, 0.15], 3.4, 1.1);
  shard(thigh, 'thigh_shard_' + side, [sx * 4.45, 8.4, -0.8], [sx * 1, 0.2, -0.3], 2.4, 0.9);
  shard(leg, 'knee_shard_' + side, [sx * 2.0, 6.2, -2.7], [sx * 0.15, 0.5, -1], 2.2, 0.9);
  shard(leg, 'calf_shard_a_' + side, [sx * 2.4, 4.2, 2.25], [sx * 0.3, 0.35, 1], 2.8, 1.0);
  shard(leg, 'calf_shard_b_' + side, [sx * 4.4, 2.4, 0.8], [sx * 1, 0.4, 0.4], 2.4, 0.9);
}

// ---- shoulders and forearms: crusts and shards
for (const [side, sx] of [['r', 1], ['l', -1]]) {
  const x = (a, b) => (sx > 0 ? [a, b] : [-b, -a]);
  const [p0, p1] = x(3.6, 8.6), [b0, b1] = x(3.7, 8.4);
  const arm = sx > 0 ? 'Arm_R' : 'Arm_L', hand = sx > 0 ? 'Hand_R' : 'Hand_L';
  piece(arm, 'shoulder_crust_' + side, [p0, 21.4, -2.9], [p1, 24.8, 2.9], rim(crust));
  piece(hand, 'forearm_crust_' + side, [b0, 12.2, -2.45], [b1, 16.8, 2.45], rim(crust));
  // Out of the shoulder: up and out, one forward, one back, one sideways.
  shard(arm, 'shoulder_shard_a_' + side, [sx * 6.2, 24.8, -0.4], [sx * 0.45, 1, -0.1], 5.2, 1.4);
  shard(arm, 'shoulder_shard_b_' + side, [sx * 7.6, 24.6, 1.2], [sx * 0.9, 0.8, 0.35], 4.0, 1.2);
  shard(arm, 'shoulder_shard_c_' + side, [sx * 4.8, 24.8, -1.6], [sx * 0.15, 1, -0.5], 3.0, 1.0);
  shard(arm, 'shoulder_shard_d_' + side, [sx * 8.6, 22.8, 0.2], [sx * 1, 0.25, 0.1], 3.2, 1.0);
  // Spines along the outer forearm, leaning toward the hand (forward, the arm held out), and on its back.
  shard(hand, 'forearm_spine_a_' + side, [sx * 8.4, 16.2, -0.6], [sx * 1, -0.75, 0.1], 3.6, 1.1);
  shard(hand, 'forearm_spine_b_' + side, [sx * 8.4, 14.4, 0.6], [sx * 1, -0.6, 0.25], 3.0, 1.0);
  shard(hand, 'forearm_back_a_' + side, [sx * 6.2, 15.6, 2.45], [sx * 0.2, -0.55, 1], 3.2, 1.1);
  shard(hand, 'forearm_back_b_' + side, [sx * 5.0, 13.4, 2.45], [-sx * 0.15, -0.7, 1], 2.4, 0.9);
  // Over the knuckles, pointing on past the hand.
  shard(hand, 'knuckle_a_' + side, [sx * 6.8, 12.2, -1.2], [sx * 0.25, -1, -0.35], 2.6, 0.9);
  shard(hand, 'knuckle_b_' + side, [sx * 5.0, 12.2, -1.6], [-sx * 0.1, -1, -0.5], 2.2, 0.8);
}
// Great blades from the upper back, fanning up and back.
shard('Chest', 'back_blade_a', [-1.6, 22.4, 2.7], [-0.35, 1, 0.75], 7.0, 1.8);
shard('Chest', 'back_blade_b', [1.8, 22.0, 2.7], [0.4, 1, 0.7], 6.2, 1.6);
shard('Chest', 'back_blade_c', [-3.2, 19.8, 2.7], [-0.9, 0.6, 0.8], 4.6, 1.3);
shard('Chest', 'back_blade_d', [3.3, 19.4, 2.7], [0.9, 0.55, 0.8], 4.2, 1.3);
shard('Torso', 'back_shard_e', [0.2, 16.0, 2.25], [0.1, 0.4, 1], 3.0, 1.1);
// Small shards at the collarbones and breaking through the chest.
shard('Chest', 'collar_shard_r', [2.6, 24.25, -1.8], [0.3, 1, -0.4], 2.2, 0.9);
shard('Chest', 'collar_shard_l', [-2.8, 24.25, -1.4], [-0.35, 1, -0.3], 2.6, 0.9);
shard('Chest', 'chest_shard', [-3.3, 21.0, -2.25], [-0.4, 0.5, -1], 2.4, 0.9);
shard('Torso', 'belly_shard', [3.0, 15.2, -2.25], [0.45, 0.2, -1], 2.0, 0.8);
// Blood running down from the crust of the sternum.
for (const [x, len] of [[-1.8, 2.2], [-0.4, 1.4], [1.2, 2.8]])
  piece('Torso', 'drip_' + (x + 3).toFixed(1), [x, 17.6 - len, -2.55], [x + 0.6, 17.6, -2.25], (f, u, v, w, h) => (v === h - 1 ? 'Q' : 'Y'));

// ---- ten crystals of blood floating behind the back, in a fan round the shoulders (each its own group)
/** A cut crystal: a long core turned 45 degrees about its length (a diamond across), a point at each end. */
const CRYSTAL = [['core', [-0.75, -1.8, -0.75], [0.75, 1.8, 0.75]], ['tip', [-0.45, 1.8, -0.45], [0.45, 3.0, 0.45]], ['foot', [-0.45, -3.0, -0.45], [0.45, -1.8, 0.45]]];
const FLOATING = [];
for (let i = 0; i < 10; i++) {
  const a = -81 + 18 * i, r = 13 + (i % 2) * 1.8, rad = a * Math.PI / 180;
  const centre = [Math.sin(rad) * r, 19 + Math.cos(rad) * r * 0.8, 13 + (i % 3) * 1.0];
  for (const [part, from, to] of CRYSTAL)
    FLOATING.push({ bone: 'Root', part: 'torso', name: `float_crystal_${i + 1}_${part}`, crystal: i + 1, centre, from: from.map((v, k) => v + centre[k]), to: to.map((v, k) => v + centre[k]),
      paint: part === 'core' ? shardPaint : (f) => (f === 'up' || f === 'down' ? 'H' : part === 'tip' ? 'Q' : 'Y'), pivot: centre, rot: [0, 45, -a] });
}
PIECES.push(...FLOATING);

/** The project, each shard turned by its own rotation (any axes: a free model), the floating crystals in their groups. */
function project() {
  const faces = kit.pack(PIECES, ATLAS);
  const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, BLOOD, ATLAS));
  const bb = kit.bbmodel('blood_crystal_body', PIECES, [BLOOD], [atlas], ATLAS, 'models/blood_crystal');
  PIECES.forEach((p, i) => { if (p.rot) { bb.elements[i].rotation = p.rot.map(a => Math.round(a * 10) / 10); bb.elements[i].origin = p.pivot; } });
  // The floating crystals out of the body's Root, a group each at the top of the outliner.
  const floating = new Set(PIECES.map((p, i) => (p.crystal ? bb.elements[i].uuid : null)).filter(Boolean));
  const root = bb.outliner[0];
  root.children = root.children.filter(c => typeof c !== 'string' || !floating.has(c));
  const groups = [];
  for (let n = 1; n <= 10; n++) {
    const parts = PIECES.map((p, i) => (p.crystal === n ? i : -1)).filter(i => i >= 0);
    groups.push({ ...root, name: 'float_crystal_' + n, origin: PIECES[parts[0]].centre, children: parts.map(i => bb.elements[i].uuid),
      uuid: kit.stableId('blood_crystal_body/float/' + n) });
  }
  bb.outliner.push({ ...root, name: 'floating_crystals', origin: [0, 20, 8], children: groups, uuid: kit.stableId('blood_crystal_body/floating') });
  // The plain body: shown off, never exported.
  PIECES.forEach((p, i) => { if (p.name.startsWith('body_')) { bb.elements[i].visibility = false; bb.elements[i].export = false; } });
  return { bb, atlas, rows: Math.max(...faces.map(f => f.v + f.h)) };
}

/** Each overlay piece's mesh part, from its joint: the player's parts, so a renderer can hide an arm or a leg. */
const PART_OF = { Chest: 'torso', Torso: 'torso', Arm_R: 'rightArm', Hand_R: 'rightArm', Arm_L: 'leftArm', Hand_L: 'leftArm',
  Thigh_R: 'rightLeg', Leg_R: 'rightLeg', Thigh_L: 'leftLeg', Leg_L: 'leftLeg' };

/**
 * The overlay as an Epic Fight skinned mesh. A shard turned about any axes is a box Epic Fight cannot turn: its corners
 * are turned here (about its root, x then y then z as Blockbench does) and written as they stand.
 */
function overlayMesh() {
  const overlay = PIECES.filter(p => !p.name.startsWith('body_') && !p.crystal).map(p => ({ ...p, part: PART_OF[p.bone] }));
  const turn = (p, [x, y, z]) => {
    if (!p.rot) return [x, y, z];
    const [ax, ay, az] = p.rot.map(a => a * Math.PI / 180), o = p.pivot;
    let [a, b, c] = [x - o[0], y - o[1], z - o[2]];
    [b, c] = [b * Math.cos(ax) - c * Math.sin(ax), b * Math.sin(ax) + c * Math.cos(ax)];
    [a, c] = [a * Math.cos(ay) + c * Math.sin(ay), -a * Math.sin(ay) + c * Math.cos(ay)];
    [a, b] = [a * Math.cos(az) - b * Math.sin(az), a * Math.sin(az) + b * Math.cos(az)];
    return [a + o[0], b + o[1], c + o[2]];
  };
  const m = kit.mesh(overlay.map(p => (p.rot ? { ...p, turnCorner: c => turn(p, c) } : p)), ATLAS,
    ['torso', 'rightArm', 'leftArm', 'rightLeg', 'leftLeg']);
  return { ...m, render_properties: { texture_path: 'murimblock:textures/entity/blood_crystal.png', transparent: false } };
}

/** The floating crystals as a GeckoLib (Bedrock) model: Bedrock's x runs the other way, so x and the turns about y flip. */
function floatingGeo() {
  const flipFace = { east: 'west', west: 'east' };
  const bones = [{ name: 'floating_crystals', pivot: [0, 19, 13] }];
  for (let n = 1; n <= 10; n++) {
    const parts = PIECES.filter(p => p.crystal === n), c = parts[0].centre;
    bones.push({ name: 'float_crystal_' + n, parent: 'floating_crystals', pivot: [-c[0], c[1], c[2]],
      cubes: parts.map(p => ({
        origin: [-p.to[0], p.from[1], p.from[2]], size: p.to.map((v, k) => Math.round((v - p.from[k]) * 1000) / 1000),
        pivot: [-p.pivot[0], p.pivot[1], p.pivot[2]], rotation: [p.rot[0], -p.rot[1], -p.rot[2]],
        uv: Object.fromEntries(kit.FACES.map(face => { const [u0, v0, u1, v1] = p.uv[face]; return [flipFace[face] || face, { uv: [u0, v0], uv_size: [u1 - u0, v1 - v0] }]; })),
      })) });
  }
  return { format_version: '1.12.0', 'minecraft:geometry': [{ description: { identifier: 'geometry.blood_crystal_floating',
    texture_width: ATLAS, texture_height: ATLAS, visible_bounds_width: 4, visible_bounds_height: 4, visible_bounds_offset: [0, 1.5, 0] }, bones }] };
}

/** Their hover: each bobs and turns on its own beat (GeckoLib, Molang; degrees). */
function floatingAnimation() {
  const bones = {};
  for (let n = 1; n <= 10; n++)
    bones['float_crystal_' + n] = { position: [0, `math.sin((query.anim_time + ${(n * 0.37).toFixed(2)}) * 120) * 0.7`, 0],
      rotation: [0, `query.anim_time * ${n % 2 ? 90 : -70}`, 0] };
  return { format_version: '1.8.0', animations: { 'animation.blood_crystal_floating.hover': { loop: true, animation_length: 12, bones } } };
}

if (require.main === module) {
  const { bb, atlas, rows } = project();
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'blood_crystal_body'), { recursive: true });
  fs.writeFileSync(path.join(art, 'blood_crystal_body', 'blood_crystal.png'), atlas);
  fs.writeFileSync(path.join(art, 'blood_crystal_body.bbmodel'), JSON.stringify(bb, null, 1));
  console.log('wrote the blood-crystal body:', PIECES.length, 'pieces, atlas rows used', rows, 'of', ATLAS);
  if (process.argv.includes('--kit')) {
    const out = path.join(ROOT, 'build', 'blood-crystal-kit'), a = path.join(out, 'assets', 'murimblock');
    for (const d of ['blockbench', 'animmodels/overlay', 'textures/entity', 'geo', 'animations']) fs.mkdirSync(d === 'blockbench' ? path.join(out, d) : path.join(a, d), { recursive: true });
    fs.writeFileSync(path.join(out, 'blockbench', 'blood_crystal_body.bbmodel'), JSON.stringify(bb, null, 1));
    fs.writeFileSync(path.join(a, 'textures', 'entity', 'blood_crystal.png'), atlas);
    fs.writeFileSync(path.join(a, 'animmodels', 'overlay', 'blood_crystal_body.json'), JSON.stringify(overlayMesh()));
    fs.writeFileSync(path.join(a, 'geo', 'blood_crystal_floating.geo.json'), JSON.stringify(floatingGeo(), null, 1));
    fs.writeFileSync(path.join(a, 'animations', 'blood_crystal_floating.animation.json'), JSON.stringify(floatingAnimation(), null, 1));
    console.log('wrote the kit in', out);
  }
}

module.exports = { PIECES, BLOOD, project };
