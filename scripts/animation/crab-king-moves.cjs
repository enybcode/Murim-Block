// The Crab King's moves (docs/mobs/crab-king.md), on the biped skeleton his mesh is made for (crab-king-3d.cjs: the outer
// finger of each pincer on the forearm, the inner one on the hand, so turning the hand about the front opens the pincer):
//   crab_king_idle   five seconds that loop: hunched, feet wide, the pincers held out low; slow breaths, a look about,
//                    the pincers clacking twice;
//   crab_king_sweep  the great right pincer wound back on the right, open, and swept level across the front, snapping;
//   crab_king_snap   both pincers lifted open, then a lunge, both driven forward and snapped shut;
//   crab_king_slam   both pincers raised high over the head and slammed to the ground ahead (the shockwave is the
//                    server's, CrabKingEntity, at CrabKingAnimations.SLAM_IMPACT);
//   crab_king_roar   under half his health: the pincers flung wide and open, the head thrown back;
//   crab_king_walk   his own walk, heavy (1.6 s, two steps, looped): each foot planted with the body sinking on it,
//                    the weight swaying from side to side, the shoulders twisting against the hips, the pincers hanging
//                    low and swinging a little;
//   crab_king_grab   the grab tried: the great right pincer drawn back wide open, then a lunge and the pincer shut on
//                    a body at GRAB_CATCH; if it caught nothing he draws back;
//   crab_king_grab_hold  what he caught (Enzo, 16/10: really between the jaws, squeezed, then thrown far): lifted before
//                    his face (0 to 0.5 s), drawn close to it while the jaws loosen a little (to 0.8 s), squeezed three
//                    times (SQUEEZES), the arm wound back over his shoulder (from 1.85 s) and flung forward, the pincer
//                    opening at GRAB_RELEASE: the victim flies (CrabKingEntity);
//   crab_king_victim played once on whoever he holds, on the same clock: struggling while lifted, frozen as he draws
//                    it to his face, crushed at each squeeze (back arched, arms pinned, head thrown back), limp between,
//                    whipped back by the wind-up;
//   crab_king_thrown looped while the victim flies: flailing, until it lands (knocked down);
//   crab_king_burrow "Pincer from the deep" (Enzo, 16/10): both pincers raised high, plunged into the ground before him
//                    at BURROW_PLUNGE and held there, trembling, while the giant pincer he calls fights (CrabKingEntity,
//                    CrabPincerEntity: 4.6 s), then pulled out.
// The victim is held between the jaws: GRIP, the point halfway between the middles of the pincer's two fingers
// (crab-king-3d.cjs), is worked out for every frame of the hold from these poses and written as
// integration/epicfight/CrabKingGrip.java, which the server follows (blocks, at his size).
// Written as game files with quaternion turns between keys (pose.cjs writeSmooth), and as a Blockbench project to look
// at the keys (art/blockbench/crab_king_moves.bbmodel).
// Usage: node scripts/animation/crab-king-moves.cjs
// Pose notation: see azure-m1.cjs (character axes: x right, y front, z up; the root's position in blocks). Arm y spreads
// the arm out to its side (the right negative); Head x pitch (+ down), z yaw (+ left).
const fs = require('fs');
const path = require('path');
const { project } = require('./blockbench-rig.cjs');
const { writeSmooth } = require('./pose.cjs');

const HIP = 0.764;
/** The pincers opened by `open` degrees (the hand turned about the front, away from the outer finger). */
const pincers = (openR, openL = openR) => ({ Tool_R: [['y', -openR]], Tool_L: [['y', openL]] });
const breath = (t, period, depth) => -depth * Math.sin((2 * Math.PI * t) / period);
const WIDE = { Thigh_R: [['x', 10], ['y', -8]], Leg_R: [['x', -16]], Thigh_L: [['x', -6], ['y', 8]], Leg_L: [['x', -14]] };

/** The stance: hunched, feet wide, the pincers out low and a little forward. */
const STANCE = (t = 0, { yaw = 0, open = 0 } = {}) => ({
  Root: { pos: [0, 0, HIP - 0.04] },
  Torso: [['x', -12]], Chest: [['x', -8 + breath(t, 2.5, 3)]], Head: [['x', -14], ['z', yaw]],
  Arm_R: [['x', 18], ['y', -20]], Hand_R: [['x', 26]], Arm_L: [['x', 14], ['y', 18]], Hand_L: [['x', 22]],
  ...pincers(open, open * 0.8), ...WIDE,
});

const anims = {};

// The idle: breathing, a look left then right, two clacks of the pincers.
{
  const clack = t => Math.max(0, Math.sin((t - 1.1) * Math.PI / 0.3)) * (t > 1.1 && t < 1.7 ? 24 : 0) + (t > 3.2 && t < 3.5 ? 24 * Math.sin((t - 3.2) * Math.PI / 0.3) : 0);
  const yaw = t => 18 * Math.sin((2 * Math.PI * t) / 5);
  const keys = [];
  for (let t = 0; t <= 5.0001; t += 0.1) keys.push({ t: Math.round(t * 10) / 10, pose: STANCE(t, { yaw: yaw(t), open: clack(t) }) });
  anims.crab_king_idle = keys;
}

// The sweep: the great right pincer, open, wound back on the right and swept level across the front.
anims.crab_king_sweep = [
  { t: 0, pose: STANCE() },
  { t: 0.35, pose: { ...STANCE(), Torso: [['x', -8], ['z', -38]], Chest: [['x', -4], ['z', -14]], Head: [['x', -10], ['z', 40]],
      Arm_R: [['x', 84], ['z', -74]], Hand_R: [['x', 14]], ...pincers(34, 6), Arm_L: [['x', 30], ['y', 24]] } },
  { t: 0.5, pose: { ...STANCE(), Torso: [['x', -10], ['z', 0]], Chest: [['x', -6]], Head: [['x', -10]],
      Arm_R: [['x', 88], ['z', 0]], Hand_R: [['x', 10]], ...pincers(20, 6), Arm_L: [['x', 22], ['y', 30]],
      Thigh_R: [['x', 20], ['y', -8]], Leg_R: [['x', -22]], Thigh_L: [['x', -14], ['y', 8]], Leg_L: [['x', -10]] } },
  { t: 0.64, pose: { ...STANCE(), Torso: [['x', -10], ['z', 36]], Chest: [['x', -6], ['z', 12]], Head: [['x', -10], ['z', -30]],
      Arm_R: [['x', 80], ['z', 72]], Hand_R: [['x', 22]], ...pincers(0, 6), Arm_L: [['x', 14], ['y', 34]],
      Thigh_R: [['x', 20], ['y', -8]], Leg_R: [['x', -22]], Thigh_L: [['x', -14], ['y', 8]], Leg_L: [['x', -10]] } },
  { t: 1.0, pose: STANCE() },
];

// The snap: both pincers lifted wide open, then a lunge, both driven forward and snapped shut.
anims.crab_king_snap = [
  { t: 0, pose: STANCE() },
  { t: 0.4, pose: { ...STANCE(), Torso: [['x', -2]], Chest: [['x', 8]], Head: [['x', -6]],
      Arm_R: [['x', 72], ['y', -28]], Hand_R: [['x', 34]], Arm_L: [['x', 72], ['y', 28]], Hand_L: [['x', 34]], ...pincers(38) } },
  { t: 0.56, pose: { ...STANCE(), Root: { pos: [0, 0.35, HIP - 0.1] }, Torso: [['x', -20]], Chest: [['x', -8]], Head: [['x', -12]],
      Arm_R: [['x', 92], ['y', -6]], Hand_R: [['x', 6]], Arm_L: [['x', 92], ['y', 6]], Hand_L: [['x', 6]], ...pincers(26),
      Thigh_R: [['x', 38], ['y', -6]], Leg_R: [['x', -40]], Thigh_L: [['x', -22], ['y', 8]], Leg_L: [['x', -12]] } },
  { t: 0.64, pose: { ...STANCE(), Root: { pos: [0, 0.42, HIP - 0.1] }, Torso: [['x', -22]], Chest: [['x', -8]], Head: [['x', -12]],
      Arm_R: [['x', 92], ['y', -4]], Hand_R: [['x', 4]], Arm_L: [['x', 92], ['y', 4]], Hand_L: [['x', 4]], ...pincers(0),
      Thigh_R: [['x', 40], ['y', -6]], Leg_R: [['x', -42]], Thigh_L: [['x', -24], ['y', 8]], Leg_L: [['x', -12]] } },
  { t: 0.9, pose: { ...STANCE(), Root: { pos: [0, 0.42, HIP - 0.08] }, Torso: [['x', -18]],
      Arm_R: [['x', 70], ['y', -12]], Hand_R: [['x', 20]], Arm_L: [['x', 70], ['y', 12]], Hand_L: [['x', 20]], ...pincers(0) } },
  { t: 1.25, pose: { ...STANCE(), Root: { pos: [0, 0.42, HIP - 0.04] } } },
];

// The slam: both pincers raised high over the head, the back arched, then slammed to the ground ahead, crouching.
const SLAM_IMPACT = 0.74;
anims.crab_king_slam = [
  { t: 0, pose: STANCE() },
  { t: 0.5, pose: { ...STANCE(), Root: { pos: [0, -0.05, HIP + 0.02] }, Torso: [['x', 6]], Chest: [['x', 12]], Head: [['x', -18]],
      Arm_R: [['x', 168], ['y', -16]], Hand_R: [['x', 30]], Arm_L: [['x', 168], ['y', 16]], Hand_L: [['x', 30]], ...pincers(30),
      Thigh_R: [['x', 4], ['y', -8]], Leg_R: [['x', -8]], Thigh_L: [['x', -4], ['y', 8]], Leg_L: [['x', -6]] } },
  { t: SLAM_IMPACT, pose: { ...STANCE(), Root: { pos: [0, 0.15, HIP - 0.24] }, Torso: [['x', -36]], Chest: [['x', -18]], Head: [['x', 10]],
      Arm_R: [['x', 62], ['y', -12]], Hand_R: [['x', 4]], Arm_L: [['x', 62], ['y', 12]], Hand_L: [['x', 4]], ...pincers(4),
      Thigh_R: [['x', 56], ['y', -8]], Leg_R: [['x', -76]], Thigh_L: [['x', 40], ['y', 8]], Leg_L: [['x', -62]] } },
  { t: 1.05, pose: { ...STANCE(), Root: { pos: [0, 0.15, HIP - 0.2] }, Torso: [['x', -32]], Chest: [['x', -16]], Head: [['x', 4]],
      Arm_R: [['x', 60], ['y', -14]], Hand_R: [['x', 8]], Arm_L: [['x', 60], ['y', 14]], Hand_L: [['x', 8]], ...pincers(10),
      Thigh_R: [['x', 52], ['y', -8]], Leg_R: [['x', -70]], Thigh_L: [['x', 36], ['y', 8]], Leg_L: [['x', -58]] } },
  { t: 1.45, pose: { ...STANCE(), Root: { pos: [0, 0.15, HIP - 0.04] } } },
];

// The roar: the pincers flung wide and open, the chest thrown out, the head back.
anims.crab_king_roar = [
  { t: 0, pose: STANCE() },
  { t: 0.45, pose: { ...STANCE(), Torso: [['x', 4]], Chest: [['x', 16]], Head: [['x', -30]],
      Arm_R: [['x', 46], ['y', -62]], Hand_R: [['x', 24]], Arm_L: [['x', 46], ['y', 62]], Hand_L: [['x', 24]], ...pincers(42) } },
  { t: 1.5, pose: { ...STANCE(), Torso: [['x', 6]], Chest: [['x', 18]], Head: [['x', -34], ['z', 6]],
      Arm_R: [['x', 50], ['y', -66]], Hand_R: [['x', 20]], Arm_L: [['x', 50], ['y', 66]], Hand_L: [['x', 20]], ...pincers(40) } },
  { t: 2.0, pose: STANCE() },
];

// The walk: heavy, two steps in 1.6 s, the body sinking onto each planted foot.
{
  const keys = [];
  for (let t = 0; t <= 1.6001; t += 0.1) {
    const p = (2 * Math.PI * t) / 1.6, sn = Math.sin(p), cs = Math.cos(p);
    keys.push({ t: Math.round(t * 10) / 10, pose: {
      Root: { pos: [0.035 * sn, 0, HIP - 0.05 - 0.045 * sn * sn] },
      Torso: [['x', -12], ['z', -7 * sn], ['y', 4 * sn]], Chest: [['x', -8 + breath(t, 1.6, 1.5)], ['z', -3 * sn]],
      Head: [['x', -14], ['z', 6 * sn]],
      Thigh_R: [['x', 24 * sn], ['y', -8]], Leg_R: [['x', -8 - 26 * Math.max(0, cs)]],
      Thigh_L: [['x', -24 * sn], ['y', 8]], Leg_L: [['x', -8 - 26 * Math.max(0, -cs)]],
      Arm_R: [['x', 16 - 10 * sn], ['y', -20 - 3 * sn]], Hand_R: [['x', 24]],
      Arm_L: [['x', 12 + 10 * sn], ['y', 18 - 3 * sn]], Hand_L: [['x', 20]],
      ...pincers(6 + 4 * Math.max(0, sn), 6 + 4 * Math.max(0, -sn)),
    } });
  }
  anims.crab_king_walk = keys;
}

// The grab: the great right pincer drawn back wide open, a lunge, shut on a body before him (the jaws closed on a man's
// width, not on themselves: the fingers bow out, a man fits between their middles).
const GRAB_CATCH = 0.55, GRAB_RELEASE = 2.45, SQUEEZES = [0.85, 1.25, 1.6];
/** The jaws on a body: a little open; squeezed, shut hard. */
const JAWS = 12, CRUSH = -3;
const CAUGHT = { ...STANCE(), Root: { pos: [0, 0.3, HIP - 0.1] }, Torso: [['x', -18], ['z', 0]], Chest: [['x', -6]], Head: [['x', -12]],
  Arm_R: [['x', 88], ['y', -6], ['z', 0]], Hand_R: [['x', 6]], Arm_L: [['x', 24], ['y', 30]], Hand_L: [['x', 24]], ...pincers(JAWS, 10),
  Thigh_R: [['x', 34], ['y', -6]], Leg_R: [['x', -36]], Thigh_L: [['x', -20], ['y', 8]], Leg_L: [['x', -12]] };
anims.crab_king_grab = [
  { t: 0, pose: STANCE() },
  { t: 0.4, pose: { ...STANCE(), Torso: [['x', -6], ['z', -22]], Chest: [['x', 6], ['z', -8]], Head: [['x', -10], ['z', 22]],
      Arm_R: [['x', 62], ['y', -30], ['z', -40]], Hand_R: [['x', 40]], ...pincers(46, 8), Arm_L: [['x', 20], ['y', 26]] } },
  { t: GRAB_CATCH, pose: CAUGHT },
  { t: 0.75, pose: { ...CAUGHT, Arm_R: [['x', 78], ['y', -10]], Hand_R: [['x', 16]] } },
  { t: 1.1, pose: { ...STANCE(), Root: { pos: [0, 0.3, HIP - 0.04] } } },
];
// Held: lifted before his face; drawn close to it; squeezed; wound back over his shoulder; flung forward.
/** Lifted: the arm up before him (the victim at his eyes), the head tilted to look at it; jaws open by `jaws`, the
 *  elbow drawn in by `close` (0 to 1: the victim brought to his face), the chest leaning in by `lean`. */
const LIFTED = (jaws = JAWS, close = 0, lean = 0) => ({ ...STANCE(), Root: { pos: [0, 0.3, HIP - 0.04] },
  Torso: [['x', -2 - lean * 3]], Chest: [['x', 6 - lean * 5]], Head: [['x', -10 + close * 6], ['z', 8 * close]],
  Arm_R: [['x', 112 - close * 8], ['y', -14 + close * 4]], Hand_R: [['x', 18 + close * 24]], Arm_L: [['x', 34], ['y', 40]], Hand_L: [['x', 30]],
  ...pincers(jaws, 30 - close * 10),
  Thigh_R: [['x', 12], ['y', -8]], Leg_R: [['x', -16]], Thigh_L: [['x', -8], ['y', 8]], Leg_L: [['x', -14]] });
const hold = [
  { t: 0, pose: CAUGHT },
  { t: 0.5, pose: LIFTED(JAWS) },
  { t: 0.8, pose: LIFTED(JAWS + 7, 1, 0.3) },
];
for (const [i, t] of SQUEEZES.entries()) {
  // Shut hard, held, loosened: the last the longest.
  hold.push({ t: t, pose: LIFTED(CRUSH, 1, 1) });
  hold.push({ t: t + (i === 2 ? 0.2 : 0.12), pose: LIFTED(CRUSH - 1, 1, 1.1) });
  if (i < 2) hold.push({ t: t + 0.25, pose: LIFTED(JAWS, 1, 0.4) });
}
hold.push(
  { t: 2.0, pose: { ...LIFTED(JAWS - 4, 0.5), Torso: [['x', 4], ['z', -34]], Chest: [['x', 10], ['z', -12]], Head: [['x', -8], ['z', 30]],
      Arm_R: [['x', 150], ['y', -36], ['z', -30]], Hand_R: [['x', 50]] } },
  { t: 2.25, pose: { ...LIFTED(JAWS - 4, 0.3), Torso: [['x', 6], ['z', -40]], Chest: [['x', 12], ['z', -14]], Head: [['x', -6], ['z', 34]],
      Arm_R: [['x', 165], ['y', -40], ['z', -40]], Hand_R: [['x', 60]] } },
  { t: GRAB_RELEASE, pose: { ...STANCE(), Root: { pos: [0, 0.45, HIP - 0.12] }, Torso: [['x', -24], ['z', 30]], Chest: [['x', -12], ['z', 12]],
      Head: [['x', -4], ['z', -20]], Arm_R: [['x', 96], ['y', -8], ['z', 28]], Hand_R: [['x', 6]], ...pincers(52, 10),
      Thigh_R: [['x', 40], ['y', -8]], Leg_R: [['x', -44]], Thigh_L: [['x', -20], ['y', 8]], Leg_L: [['x', -16]] } },
  { t: 2.65, pose: { ...STANCE(), Root: { pos: [0, 0.45, HIP - 0.08] }, Torso: [['x', -20], ['z', 34]], Arm_R: [['x', 70], ['z', 40]], ...pincers(30, 10) } },
  { t: 3.1, pose: { ...STANCE(), Root: { pos: [0, 0.45, HIP - 0.04] } } },
);
anims.crab_king_grab_hold = hold;

// The victim, on the hold's clock: struggling while lifted, frozen as he draws it close, crushed at each squeeze, limp
// between, whipped back by the wind-up (Head x: + down; it looks up at him).
{
  const STRUGGLE = (t, k = 1) => {
    const p = 2 * Math.PI * t / 0.5, sn = Math.sin(p);
    return {
      Root: { pos: [0, 0, HIP] }, Torso: [['x', 2 + 4 * k * Math.sin(2 * p)], ['z', 8 * k * sn]], Chest: [['x', 4]],
      Head: [['x', -26], ['z', 12 * k * sn]],
      // The forearms pushing out against the jaws.
      Arm_R: [['x', 10], ['y', -10 * k]], Hand_R: [['x', 60 + 16 * k * Math.max(0, sn)]],
      Arm_L: [['x', 10], ['y', 10 * k]], Hand_L: [['x', 60 + 16 * k * Math.max(0, -sn)]],
      Thigh_R: [['x', 30 * k * sn]], Leg_R: [['x', -20 - 26 * k * Math.max(0, Math.cos(p))]],
      Thigh_L: [['x', -30 * k * sn]], Leg_L: [['x', -20 - 26 * k * Math.max(0, -Math.cos(p))]],
    };
  };
  const FROZEN = { Root: { pos: [0, 0, HIP] }, Torso: [['x', -4]], Chest: [['x', -6]], Head: [['x', -34]],
    Arm_R: [['x', 6], ['y', 4]], Hand_R: [['x', 70]], Arm_L: [['x', 6], ['y', -4]], Hand_L: [['x', 70]],
    Thigh_R: [['x', 6]], Leg_R: [['x', -8]], Thigh_L: [['x', 2]], Leg_L: [['x', -6]] };
  const CRUSHED = { Root: { pos: [0, 0, HIP] }, Torso: [['x', -16]], Chest: [['x', -14]], Head: [['x', -48], ['y', 8]],
    Arm_R: [['x', 0], ['y', 2]], Hand_R: [['x', 4]], Arm_L: [['x', 0], ['y', -2]], Hand_L: [['x', 4]],
    Thigh_R: [['x', -12]], Leg_R: [['x', -4]], Thigh_L: [['x', -12]], Leg_L: [['x', -4]] };
  const LIMP = { Root: { pos: [0, 0, HIP] }, Torso: [['x', 10]], Chest: [['x', 8]], Head: [['x', 24], ['z', 10]],
    Arm_R: [['x', 4], ['y', 2]], Hand_R: [['x', 20]], Arm_L: [['x', 4], ['y', -2]], Hand_L: [['x', 24]],
    Thigh_R: [['x', 4]], Leg_R: [['x', -10]], Thigh_L: [['x', 10]], Leg_L: [['x', -16]] };
  const WHIPPED = { Root: { pos: [0, 0, HIP] }, Torso: [['x', 20]], Chest: [['x', 14]], Head: [['x', 30]],
    Arm_R: [['x', 30], ['y', -30]], Hand_R: [['x', 30]], Arm_L: [['x', 30], ['y', 30]], Hand_L: [['x', 30]],
    Thigh_R: [['x', 30]], Leg_R: [['x', -30]], Thigh_L: [['x', 40]], Leg_L: [['x', -40]] };
  const keys = [];
  for (let t = 0; t < 0.5; t += 0.05) keys.push({ t: Math.round(t * 100) / 100, pose: STRUGGLE(t, 1 - t) });
  keys.push({ t: 0.6, pose: STRUGGLE(0.6, 0.3) }, { t: 0.8, pose: FROZEN });
  for (const [i, t] of SQUEEZES.entries()) {
    keys.push({ t: t, pose: CRUSHED }, { t: t + (i === 2 ? 0.2 : 0.12), pose: CRUSHED });
    if (i < 2) keys.push({ t: t + 0.25, pose: LIMP });
  }
  keys.push({ t: 2.0, pose: LIMP }, { t: 2.25, pose: WHIPPED }, { t: GRAB_RELEASE, pose: WHIPPED });
  anims.crab_king_victim = keys;

  // Flying: flailing, the arms windmilling, the legs kicking (0.6 s, looped).
  const fly = [];
  for (let t = 0; t <= 0.6001; t += 0.05) {
    const p = 2 * Math.PI * t / 0.6, sn = Math.sin(p), cs = Math.cos(p);
    fly.push({ t: Math.round(t * 100) / 100, pose: { Root: { pos: [0, 0, HIP] }, Torso: [['x', -10]], Chest: [['x', -10]], Head: [['x', -20], ['z', 10 * sn]],
      Arm_R: [['x', 90 + 70 * sn], ['y', -50]], Hand_R: [['x', 30]], Arm_L: [['x', 90 - 70 * sn], ['y', 50]], Hand_L: [['x', 30]],
      Thigh_R: [['x', 30 * cs]], Leg_R: [['x', -30 - 20 * Math.max(0, sn)]], Thigh_L: [['x', -30 * cs]], Leg_L: [['x', -30 - 20 * Math.max(0, -sn)]] } });
  }
  anims.crab_king_thrown = fly;
}

// Held in the pincer: the arms pinned, the legs kicking, the head back and shaking (0.8 s, looped).
{
  const keys = [];
  for (let t = 0; t <= 0.8001; t += 0.05) {
    const p = (2 * Math.PI * t) / 0.8, sn = Math.sin(p), s2 = Math.sin(2 * p);
    keys.push({ t: Math.round(t * 100) / 100, pose: {
      Root: { pos: [0, 0, HIP] },
      Torso: [['x', 4 + 4 * s2], ['z', 6 * sn]], Chest: [['x', 6]], Head: [['x', -22 + 6 * s2], ['z', 14 * sn]],
      Arm_R: [['x', 6], ['y', 6]], Hand_R: [['x', 14 + 8 * Math.max(0, sn)]], Arm_L: [['x', 6], ['y', -6]], Hand_L: [['x', 14 + 8 * Math.max(0, -sn)]],
      Thigh_R: [['x', 26 * sn]], Leg_R: [['x', -18 - 22 * Math.max(0, Math.cos(p))]],
      Thigh_L: [['x', -26 * sn]], Leg_L: [['x', -18 - 22 * Math.max(0, -Math.cos(p))]],
    } });
  }
  anims.crab_king_held = keys;
}

// Pincers from the deep: both raised, plunged into the ground ahead and held, trembling, then pulled out.
const BURROW_PLUNGE = 0.62;
{
  const PLUNGED = (shake = 0) => ({ ...STANCE(), Root: { pos: [0, 0.12, HIP - 0.3] }, Torso: [['x', -40], ['z', shake * 2]], Chest: [['x', -18 + shake]],
    Head: [['x', 16], ['z', -shake * 3]],
    Arm_R: [['x', 52], ['y', -14]], Hand_R: [['x', 0]], Arm_L: [['x', 52], ['y', 14]], Hand_L: [['x', 0]], ...pincers(30),
    Thigh_R: [['x', 60], ['y', -10]], Leg_R: [['x', -84]], Thigh_L: [['x', 48], ['y', 10]], Leg_L: [['x', -70]] });
  const keys = [
    { t: 0, pose: STANCE() },
    { t: 0.42, pose: { ...STANCE(), Root: { pos: [0, -0.05, HIP + 0.02] }, Torso: [['x', 8]], Chest: [['x', 14]], Head: [['x', -22]],
        Arm_R: [['x', 172], ['y', -18]], Hand_R: [['x', 26]], Arm_L: [['x', 172], ['y', 18]], Hand_L: [['x', 26]], ...pincers(36) } },
    { t: BURROW_PLUNGE, pose: PLUNGED() },
  ];
  // Held plunged, trembling, while the giant pincer fights (its life: 4.6 s from the plunge).
  for (let t = 0.75, i = 0; t < 5.2; t += 0.1, i++) keys.push({ t: Math.round(t * 100) / 100, pose: PLUNGED(i % 2 ? 1.5 : -1.5) });
  keys.push({ t: 5.5, pose: { ...PLUNGED(), Torso: [['x', -20]], Arm_R: [['x', 80], ['y', -16]], Hand_R: [['x', 20]], Arm_L: [['x', 80], ['y', 16]], Hand_L: [['x', 20]], ...pincers(12) } },
    { t: 5.9, pose: STANCE() });
  anims.crab_king_burrow = keys;
}

module.exports = { anims, SLAM_IMPACT, GRAB_CATCH, GRAB_RELEASE, SQUEEZES, BURROW_PLUNGE };

/**
 * GRIP: between the jaws, for each frame of the hold (and the catch), in the character's axes at his size (blocks:
 * right, front, up from his feet), the root's own forward step left out (the game moves the body for it).
 */
function grips() {
  const { model } = require('./ik.cjs');
  const { sample } = require('./pose.cjs');
  const b = require('./biped.cjs');
  const { arm, locals } = require('./pose.cjs');
  const bind = b.modelSpace(arm, locals({}));
  const R = m => [[m[0], m[1], m[2]], [m[4], m[5], m[6]], [m[8], m[9], m[10]]];
  const carried = (m, j, p) => {
    const o0 = b.origin(bind[j]), d = p.map((v, i) => v - o0[i]), rb = R(bind[j]), rm = R(m[j]);
    const local = [0, 1, 2].map(c => rb[0][c] * d[0] + rb[1][c] * d[1] + rb[2][c] * d[2]);
    return b.origin(m[j]).map((v, i) => v + rm[i][0] * local[0] + rm[i][1] * local[1] + rm[i][2] * local[2]);
  };
  // The middles of the two fingers (crab-king-3d.cjs: Blockbench (x, y, z) = model (x, -z, y) times 16).
  const OUTER = [8.55 / 16, 0.9 / 16, 5.44 / 16], INNER = [3.75 / 16, 0.7 / 16, 5.94 / 16];
  const SCALE = 2;
  const at = (keys, t) => {
    // The pose at t, keys interpolated as the game does (its frames are sampled from them).
    let i = 0;
    while (i < keys.length - 2 && t > keys[i + 1].t) i++;
    const a = keys[i], c = keys[i + 1], k = Math.max(0, Math.min(1, (t - a.t) / (c.t - a.t)));
    const lerp = (x, y) => x + (y - x) * k;
    const pose = {};
    for (const j of new Set([...Object.keys(a.pose), ...Object.keys(c.pose)])) {
      if (j === 'Root') { const p0 = a.pose.Root.pos, p1 = c.pose.Root.pos; pose.Root = { pos: [0, 0, lerp(p0[2], p1[2])] }; continue; }
      const r0 = a.pose[j] || [], r1 = c.pose[j] || [];
      const axes = new Set([...r0.map(r => r[0]), ...r1.map(r => r[0])]);
      pose[j] = [...axes].map(ax => [ax, lerp((r0.find(r => r[0] === ax) || [ax, 0])[1], (r1.find(r => r[0] === ax) || [ax, 0])[1])]);
      // Keep each joint's axes in the order the key gave them.
      const order = (r0.length ? r0 : r1).map(r => r[0]);
      pose[j].sort((x, y) => order.indexOf(x[0]) - order.indexOf(y[0]));
    }
    const m = model(pose), o = carried(m, 'Hand_R', OUTER), n = carried(m, 'Tool_R', INNER);
    return o.map((v, i) => Math.round((v + n[i]) / 2 * SCALE * 1000) / 1000);
  };
  const hold = [];
  for (let t = 0; t <= GRAB_RELEASE + 1e-9; t += 0.05) hold.push(at(anims.crab_king_grab_hold, Math.round(t * 100) / 100));
  return { hold, catchAt: at(anims.crab_king_grab, GRAB_CATCH) };
}

if (require.main === module) {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
  const g = grips();
  const java = `package com.murimblock.integration.epicfight;

/**
 * Written by scripts/animation/crab-king-moves.cjs (do not edit): where the Crab King's great pincer holds its catch,
 * the point halfway between the middles of its two fingers, at his size, in blocks from his feet along his right,
 * his front and up (the root's own forward step left out: the game moves his body for it).
 */
public final class CrabKingGrip {
    /** One point every {@link #STEP} seconds of crab_king_grab_hold, from 0 to its release. */
    public static final float STEP = 0.05F;
    public static final float[][] HOLD = {
${g.hold.map(p => '            {' + p.map(v => v.toFixed(3) + 'F').join(', ') + '}').join(',\n')}
    };
    /** Where the jaws shut in crab_king_grab, at its catch. */
    public static final float[] CATCH = {${g.catchAt.map(v => v.toFixed(3) + 'F').join(', ')}};

    private CrabKingGrip() { }

    /** The grip at t seconds into the hold, between the two nearest points. */
    public static float[] at(float t) {
        float x = Math.max(0, t / STEP);
        int i = Math.min(HOLD.length - 1, (int) x), j = Math.min(HOLD.length - 1, i + 1);
        float k = Math.min(1, x - i);
        return new float[] {HOLD[i][0] + (HOLD[j][0] - HOLD[i][0]) * k, HOLD[i][1] + (HOLD[j][1] - HOLD[i][1]) * k, HOLD[i][2] + (HOLD[j][2] - HOLD[i][2]) * k};
    }
}
`;
  fs.writeFileSync(path.join(__dirname, '..', '..', 'src', 'main', 'java', 'com', 'murimblock', 'integration', 'epicfight', 'CrabKingGrip.java'), java);
  console.log('wrote CrabKingGrip: catch', g.catchAt, 'lifted', g.hold[10], 'squeezed', g.hold[17], 'release', g.hold[g.hold.length - 1]);
  const out = path.join(__dirname, '..', '..', 'art', 'blockbench', 'crab_king_moves.bbmodel');
  fs.writeFileSync(out, JSON.stringify(project('crab_king_moves', anims), null, 1));
  console.log('wrote', out);
}
