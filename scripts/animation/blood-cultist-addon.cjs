// The Blood Cultists (blood-cultist-3d.cjs) and their armour (blood-cult-armor.cjs) for Colleuhu95's Blood Sect Decor addon
// (addons/blood-sect-decor), where they replace the Blood Ninja's look (Enzo, 16/10/2026). The addon draws its ninja with
// Minecraft's own model parts (ReferenceNinjaModel: head, body, the arms and their forearms, the legs and their shins), whose
// poses its attacks animate; our pieces have a free uv on every face, which a vanilla cube cannot hold, so the model draws
// our faces as quads on its own parts instead. Written for it:
//   assets/bloodsect/models/entity/blood_cultist.json     the quads, by part, in each part's own space (vanilla pixels:
//                                                          x to the model's left, y down from the neck, z back), and the
//                                                          armour's, by design; each quad is its four corners (x, y, z,
//                                                          u, v), then its normal;
//   assets/bloodsect/textures/entity/blood_cultist/<outfit>.png (+ <outfit>_blink.png)   the five outfits' atlases;
//   assets/bloodsect/textures/entity/blood_cultist/armour_<design>.png                   the four armours' atlases.
// Usage: node scripts/animation/blood-cultist-addon.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const cult = require('./blood-cultist-3d.cjs');
const armour = require('./blood-cult-armor.cjs');

const ROOT = path.join(__dirname, '..', '..');
const ASSETS = path.join(ROOT, 'addons', 'blood-sect-decor', 'src', 'main', 'resources', 'assets', 'bloodsect');
const ATLAS = 128;

/** Our joints to the addon's parts, and each part's pivot at rest in vanilla pixels (ReferenceNinjaGeometry). */
const PART = {
  Head: 'head', Chest: 'body', Torso: 'body',
  Arm_R: 'right_arm', Hand_R: 'right_forearm', Arm_L: 'left_arm', Hand_L: 'left_forearm',
  Thigh_R: 'right_leg', Leg_R: 'right_shin', Thigh_L: 'left_leg', Leg_L: 'left_shin',
};
const PIVOT = {
  head: [0, 0, 0], body: [0, 0, 0],
  right_arm: [-5.5, 2, 0], right_forearm: [-5.5, 5.8, 0], left_arm: [5.5, 2, 0], left_forearm: [5.5, 5.8, 0],
  right_leg: [-1.9, 12, 0], right_shin: [-1.9, 18, 0], left_leg: [1.9, 12, 0], left_shin: [1.9, 18, 0],
};
/** Ours (Blockbench pixels: x to the model's right, y up from the ground, z back) to vanilla model space. */
const toVanilla = ([x, y, z]) => [-x, 24 - y, z];
const r4 = v => Math.round(v * 1e4) / 1e4;

/** Every face of `pieces` (packed) as a quad in its part's space. */
function quads(pieces) {
  const out = {};
  for (const p of pieces) {
    const part = PART[p.bone];
    if (!part) throw new Error('no part for ' + p.bone);
    const [px, py, pz] = PIVOT[part];
    for (const f of kit.FACES) {
      const cs = kit.corners(p, f).map(c => toVanilla(kit.tilt(p, c)));
      const [u0, v0, u1, v1] = p.uv[f];
      const uv = [[u0, v0], [u1, v0], [u1, v1], [u0, v1]];
      // The outward normal, as the Epic Fight mesh's (two flips of axes keep the turn of the corners).
      const [a, b, c] = [cs[0], cs[3], cs[2]];
      const e1 = b.map((x, i) => x - a[i]), e2 = c.map((x, i) => x - a[i]);
      const n = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]];
      const len = Math.hypot(...n);
      if (len < 1e-9) continue;
      const q = [];
      cs.forEach(([x, y, z], i) => q.push(r4(x - px), r4(y - py), r4(z - pz), r4(uv[i][0] / ATLAS), r4(uv[i][1] / ATLAS)));
      q.push(...n.map(x => r4(x / len)));
      (out[part] = out[part] || []).push(q);
    }
  }
  return out;
}

if (require.main === module) {
  const texDir = path.join(ASSETS, 'textures', 'entity', 'blood_cultist');
  fs.mkdirSync(texDir, { recursive: true });
  fs.mkdirSync(path.join(ASSETS, 'models', 'entity'), { recursive: true });
  // The cloth: every piece, the tails included (the addon has no cloth physics: they hang still).
  const pieces = cult.PIECES.map(p => ({ ...p }));
  const faces = kit.pack(pieces, ATLAS);
  for (const o of cult.OUTFITS) {
    fs.writeFileSync(path.join(texDir, o.key + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, o, ATLAS)));
    fs.writeFileSync(path.join(texDir, o.key + '_blink.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, { ...o, face: 'blink' }, ATLAS)));
  }
  const model = { texture_size: ATLAS, outfits: cult.OUTFITS.map(o => o.key), parts: quads(pieces), armour: {} };
  for (const design of armour.DESIGNS) {
    const ap = armour.PIECES.filter(p => p.design === design).map(p => ({ ...p }));
    const af = kit.pack(ap, ATLAS);
    fs.writeFileSync(path.join(texDir, 'armour_' + design + '.png'), kit.png(ATLAS, ATLAS, kit.paintAtlas(af, armour.ARMOUR, ATLAS)));
    model.armour[design] = quads(ap);
  }
  fs.writeFileSync(path.join(ASSETS, 'models', 'entity', 'blood_cultist.json'), JSON.stringify(model));
  const count = parts => Object.values(parts).reduce((s, a) => s + a.length, 0);
  console.log('wrote the Blood Cultists for the addon:', count(model.parts), 'quads,', cult.OUTFITS.length, 'outfits,',
    armour.DESIGNS.map(d => d + ' ' + count(model.armour[d])).join(', '));
}

module.exports = { quads, PART, PIVOT };
