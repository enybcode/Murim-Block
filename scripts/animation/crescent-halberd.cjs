// The Crescent Moon Halberd Art's moves (docs/combat/styles/crescent-moon-halberd.md), the halberd held in both hands,
// built on the Celestial Peak Spear's poses (celestial-spear.cjs: the same hold, both arms solved for where it points).
// An art of circles: wide turns, the crescent drawn through the air, nothing jerked.
//   halberd_moonlit_circle   Moonlit Circle: drawn back, then a whole turn low, the crescent skimming the ground;
//   halberd_tide_hook        Tide Hook: a long reaching thrust, the hook caught, then the halberd drawn back hard;
//   halberd_falling_crescent Falling Crescent: raised high, then a great slash down and across, three crescents flying off;
//   halberd_mirror           Mirror Lake Stance: the halberd brought upright before the body and held there, still;
//   halberd_mirror_end       its end: back to guard;
//   halberd_full_moon        Night of the Full Moon: the halberd raised to the sky, held while the moon rises, then
//                            swept down toward the enemies as its beams fall.
// Usage: node scripts/animation/crescent-halberd.cjs
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { writeSmooth, sample, arm } = require('./pose.cjs');
const b = require('./biped.cjs');
const c = require('./celestial-spear.cjs');
const { hold, begin, GUARD, WIND, THRUST, DRAW, SWEEP, STANCE, LUNGE, CROUCH, HIP, yawed, pitched, look, through, SHAFT } = c;

const r3 = t => Math.round(t * 1000) / 1000;

// ---------------------------------------------------------------- 1. Moonlit Circle
begin();
const halberd_moonlit_circle = (() => {
  const keys = [{ t: 0, pose: GUARD() }, { t: 0.16, pose: WIND(-0.08, 52) }];
  const start = 0.24, per = 0.42;
  for (let q = 0; q <= 4; q++) keys.push({ t: r3(start + per * q / 4), pose: SWEEP(q * 90, 0.05) });
  const back = GUARD(0.05); back.Root.rot = [['z', 360]];
  keys.push({ t: r3(start + per + 0.3), pose: back });
  return keys;
})();

// ---------------------------------------------------------------- 2. Tide Hook
begin();
const halberd_tide_hook = [
  { t: 0, pose: GUARD() },
  { t: 0.1, pose: DRAW(0, 6, -4) },
  { t: 0.2, pose: THRUST(0.35, 0, -2, -0.16, LUNGE) },
  { t: 0.34, pose: THRUST(0.4, 0, -2, -0.18, LUNGE) },
  // The hook caught: the halberd drawn back hard, the weight thrown onto the back foot.
  { t: 0.48, pose: WIND(0.15, 40) },
  { t: 0.62, pose: WIND(0.1, 44) },
  { t: 0.86, pose: GUARD(0.1) },
];

// ---------------------------------------------------------------- 3. Falling Crescent
begin();
/** Raised high over the right shoulder, the crescent toward the sky. */
const HIGH = hold({ Root: { pos: [0, 0, HIP + 0.04] }, Torso: [['x', 14], ['z', 30]], Chest: [['x', 8], ['z', 10]], ...look(20, -18), ...STANCE },
  [0.3, -0.1, 0.86], yawed(pitched([0, 1, 0], 62), 24));
/** Slashed down and across to the left, low, the body turned after it. */
const ACROSS = hold({ Root: { pos: [0, 0.25, HIP - 0.22] }, Torso: [['x', -26], ['z', -34]], Chest: [['x', -10], ['z', -12]], ...look(-24, 18), ...LUNGE },
  [-0.32, 0.52, 0.32], yawed(pitched([0, 1, 0], -30), -40));
const halberd_falling_crescent = [
  { t: 0, pose: GUARD() },
  { t: 0.2, pose: HIGH },
  { t: 0.28, pose: HIGH },
  { t: 0.38, pose: ACROSS },
  { t: 0.56, pose: ACROSS },
  { t: 0.84, pose: GUARD(0.25) },
];

// ---------------------------------------------------------------- 4. Mirror Lake Stance
begin();
/** Upright before the body, the shaft held in both hands at the chest, the crescent level with the face. */
const UPRIGHT = (y = 0) => hold({ Root: { pos: [0, y, HIP - 0.12] }, Torso: [['x', -2], ['z', 6]], Chest: [['z', 2]], ...look(4, 0), ...STANCE },
  [0.04, 0.42, 0.52], pitched([0, 1, 0], 82));
const halberd_mirror = [
  { t: 0, pose: GUARD() },
  { t: 0.16, pose: UPRIGHT(-0.02) },
  { t: 3.4, pose: UPRIGHT(-0.04) },
];
begin();
const halberd_mirror_end = [
  { t: 0, pose: UPRIGHT(-0.04) },
  { t: 0.26, pose: GUARD() },
];

// ---------------------------------------------------------------- the ultimate: Night of the Full Moon
begin();
const TO_SKY = (z = 0) => hold({ Root: { pos: [0, -0.05, HIP + z] }, Torso: [['x', 22]], Chest: [['x', 12]], ...look(0, -40), ...STANCE },
  [0.08, -0.02, 0.98], pitched([0, 1, 0], 86));
const halberd_full_moon = [
  { t: 0, pose: GUARD() },
  { t: 0.3, pose: TO_SKY(0.02) },
  { t: 1.6, pose: TO_SKY(0.04) },
  { t: 1.85, pose: ACROSS },
  { t: 2.1, pose: ACROSS },
  { t: 2.5, pose: GUARD(0.25) },
];

const anims = { halberd_moonlit_circle, halberd_tide_hook, halberd_falling_crescent, halberd_mirror, halberd_mirror_end, halberd_full_moon };
module.exports = { anims };

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) {
    const { times, frames } = sample(keys);
    let worst = { depth: 0 };
    times.forEach((t, n) => { const r = through(b.modelSpace(arm, frames[n])); if (r.depth > worst.depth) worst = { ...r, t }; });
    if (worst.depth > 2 * SHAFT) console.warn(`  ${name}: halberd through the ${worst.part} at ${worst.t}s (${worst.depth.toFixed(2)})`);
    writeSmooth(name, keys);
  }
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'crescent_moon_halberd_moves.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('crescent_moon_halberd', anims), null, 1));
  console.log('wrote', out);
}
