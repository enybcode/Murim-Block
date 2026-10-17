// The Blood Sect's light armour for its lowest ranks, worn over the Blood Cultists' cloth (blood-cultist-3d.cjs) and, later,
// dropped by them for a player to wear. Enzo (16/10/2026): "une légère armure en métal, des épaulettes", then "pas trop
// extravagant, c'est le bas de la chaîne". Four proposals, cheap and plain, each its own Blockbench project:
//   a_single_pad     one shoulder only (the left), a leather pad with an iron plate on it, a strap round the chest to hold
//                    it; leather bracers;
//   b_brigandine     a leather vest studded with iron, open down the middle on the robe's lapels, the back left bare for
//                    the sect's mark; short leather flaps on the shoulders; leather bracers;
//   c_iron_caps      a plain rounded iron cap on each shoulder, a chest strap with an iron ring; iron bracers;
//   d_lamellae       three small iron lames on each shoulder laced with red cord; leather bracers.
// Written here: art/blockbench/blood_cult_armor_<design>.bbmodel (+ blood_cult_armor/<design>.png), the pieces on the
// biped's joints. The game files (Epic Fight meshes, items, icons) come once Enzo picks one.
// Usage: node scripts/animation/blood-cult-armor.cjs
const fs = require('fs');
const path = require('path');
const kit = require('./mesh-kit.cjs');
const { isSide, rim } = kit;

const ROOT = path.join(__dirname, '..', '..');
const ATLAS = 128;

/** I iron, J its light edge (and the studs), N black leather straps, T dark leather, X red (the lacing). */
const ARMOUR = { key: 'blood_cult_armor', c: { I: '#565a62', J: '#8e949c', N: '#17131a', T: '#3b2a22', X: '#a3202a' } };
const DESIGNS = ['a_single_pad', 'b_brigandine', 'c_iron_caps', 'd_lamellae'];

const PIECES = [];
const piece = (design, bone, part, name, from, to, paint, extra = {}) => PIECES.push({ design, bone, part, name, from, to, paint, ...extra });
const both = fn => { for (const [side, sx] of [['R', 1], ['L', -1]]) fn(side, sx, (a, b) => (sx > 0 ? [a, b] : [-b, -a]), side.toLowerCase()); };

/** Leather: plain, a darker stitch along its edges. */
const leather = (f, u, v, w, h) => (isSide(f) && (v === 0 || v === h - 1) ? 't' : 'T');
/** Iron: its light top edge, plain below. */
const iron = (f, u, v, w, h) => (f === 'up' || v === 0 ? 'J' : f === 'down' ? 'i' : 'I');
/** A leather bracer over the wrist wraps, a strap at each end. */
const leatherBracer = (design, side, part, s, xs) => {
  const [b0, b1] = xs(3.75, 8.25);
  piece(design, 'Hand_' + side, part, 'bracer_' + s, [b0, 12.5, -2.42], [b1, 14.4, 2.42], rim((f, u, v, w, h) => (isSide(f) && (v === 1 || v === h - 2) ? 'N' : 'T')));
};

// ---- A: one pad on the left shoulder, held by a strap round the chest
{
  const D = 'a_single_pad';
  piece(D, 'Arm_L', 'leftArm', 'pad', [-8.6, 21.6, -2.75], [-3.6, 24.4, 2.75], rim(leather));
  piece(D, 'Arm_L', 'leftArm', 'pad_plate', [-8.2, 23.4, -2.3], [-4.2, 24.7, 2.3], rim(iron));
  piece(D, 'Chest', 'torso', 'strap', [-4.12, 21.2, -2.68], [4.12, 21.7, 2.68], rim(f => (f === 'south' ? null : 'N')));
  both((side, sx, xs, s) => leatherBracer(D, side, sx > 0 ? 'rightArm' : 'leftArm', s, xs));
}

// ---- B: a studded leather vest, open in front, bare at the back; leather flaps on the shoulders
{
  const D = 'b_brigandine';
  const studs = (f, u, v, w, h) => (f === 'north' && u % 3 === 1 && v % 3 === 1 ? 'J' : leather(f, u, v, w, h));
  for (const [n, x0, x1] of [['vest_r', 1.0, 4.3], ['vest_l', -4.3, -1.0]]) {
    piece(D, 'Chest', 'torso', n + '_hi', [x0, 18.0, -2.7], [x1, 23.2, 2.7], rim((f, u, v, w, h) => (f === 'south' ? null : studs(f, u, v, w, h))), { hinge: { y: 18, bone: 'Torso' } });
    piece(D, 'Torso', 'torso', n + '_lo', [x0, 16.0, -2.7], [x1, 18.0, 2.7], rim((f, u, v, w, h) => (f === 'south' ? null : studs(f, u, v, w, h))), { hinge: { y: 18, bone: 'Chest' } });
  }
  both((side, sx, xs, s) => {
    const part = sx > 0 ? 'rightArm' : 'leftArm', [p0, p1] = xs(3.7, 8.4);
    piece(D, 'Arm_' + side, part, 'flap_' + s, [p0, 22.4, -2.6], [p1, 24.4, 2.6], rim((f, u, v, w, h) => (f === 'north' && u % 3 === 1 && v === 1 ? 'J' : leather(f, u, v, w, h))));
    leatherBracer(D, side, part, s, xs);
  });
}

// ---- C: plain rounded iron caps, a chest strap with a ring; iron bracers
{
  const D = 'c_iron_caps';
  both((side, sx, xs, s) => {
    const part = sx > 0 ? 'rightArm' : 'leftArm', [c0, c1] = xs(3.7, 8.5), [t0, t1] = xs(4.3, 7.9), [b0, b1] = xs(3.75, 8.25);
    piece(D, 'Arm_' + side, part, 'cap_' + s, [c0, 22.6, -2.65], [c1, 24.5, 2.65], rim(iron));
    piece(D, 'Arm_' + side, part, 'cap_top_' + s, [t0, 24.5, -2.0], [t1, 24.9, 2.0], iron);
    piece(D, 'Hand_' + side, part, 'bracer_' + s, [b0, 12.6, -2.42], [b1, 14.2, 2.42], rim((f, u, v, w, h) => (isSide(f) && v === h - 1 ? 'N' : iron(f, u, v, w, h))));
  });
  piece(D, 'Chest', 'torso', 'strap', [-4.12, 21.4, -2.68], [4.12, 21.9, 2.68], rim(f => (f === 'south' ? null : 'N')));
  piece(D, 'Chest', 'torso', 'ring', [-0.7, 21.0, -2.95], [0.7, 22.3, -2.6], (f, u, v, w, h) => (f === 'north' && u > 0 && u < w - 1 && v > 0 && v < h - 1 ? null : 'J'), { density: 2 });
}

// ---- D: three small iron lames on each shoulder, laced with red cord
{
  const D = 'd_lamellae';
  both((side, sx, xs, s) => {
    const part = sx > 0 ? 'rightArm' : 'leftArm';
    [[4.4, 0], [3.8, 1], [3.2, 2]].forEach(([top, i]) => {
      const [l0, l1] = xs(3.7 - i * 0.05, 8.4 + i * 0.1);
      const y1 = 20.4 + top, y0 = y1 - 1.5;
      piece(D, 'Arm_' + side, part, 'lame_' + i + '_' + s, [l0, y0, -2.6 - i * 0.05], [l1, y1, 2.6 + i * 0.05],
        rim((f, u, v, w, h) => (isSide(f) && u % 3 === 1 && v === 0 ? 'X' : iron(f, u, v, w, h))));
    });
    leatherBracer(D, side, part, s, xs);
  });
}

module.exports = { PIECES, ARMOUR, DESIGNS };

if (require.main === module) {
  const art = path.join(ROOT, 'art', 'blockbench');
  fs.mkdirSync(path.join(art, 'blood_cult_armor'), { recursive: true });
  for (const design of DESIGNS) {
    const pieces = PIECES.filter(p => p.design === design);
    const faces = kit.pack(pieces, ATLAS);
    const atlas = kit.png(ATLAS, ATLAS, kit.paintAtlas(faces, ARMOUR, ATLAS));
    fs.writeFileSync(path.join(art, 'blood_cult_armor', design + '.png'), atlas);
    fs.writeFileSync(path.join(art, 'blood_cult_armor_' + design + '.bbmodel'),
      JSON.stringify(kit.bbmodel('blood_cult_armor_' + design, pieces, [{ ...ARMOUR, key: design }], [atlas], ATLAS, 'models/armor'), null, 1));
    console.log("wrote the Blood Sect's armour", design + ':', pieces.length, 'pieces');
  }
}
