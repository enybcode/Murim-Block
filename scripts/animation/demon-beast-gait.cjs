// The Devouring Demon Art's beast gait (docs/combat/styles/devouring-demon-art.md; Enzo, 18/10/2026: "start working on
// being on four legs"): in the demon form the player goes on all fours, the hands on the ground, the hips high, the head
// up. Three Epic Fight biped animations: demon_beast_idle (crouched, breathing, weight shifting), demon_beast_walk (a
// diagonal walk: right hand with left foot) and demon_beast_run (a bounding gallop: the hands together, then the feet).
//
//   node scripts/animation/demon-beast-gait.cjs [--check]
// Writes animmodels/animations/biped/murim/demon_beast_*.json. --check prints, at every key, how high the hands and the
// feet are off the ground (a hand or a foot on the ground must be at 0, give or take 3 cm; none may go under it).
const b = require('./biped.cjs');
const { arm, locals, writeSmooth } = require('./pose.cjs');

/**
 * The beast's pose. lean: the body's pitch forward from upright (deg); hip: the hips' height (m); the four limbs'
 * swings (deg, + forward) and bends; head up to look ahead.
 */
function beast({ lean = 90, hip = 0.45, sway = 0, breath = 0, armR = 0, armL = 0, bendR = 0, bendL = 0,
                 thighR = 0, thighL = 0, kneeR = 0, kneeL = 0, liftHandR = 0, liftHandL = 0, head = 0, look = 0, tilt = 0 } = {}) {
  return {
    Root: { pos: [0, 0, hip], rot: [['x', -lean], ['z', sway]] },
    // A hungry beast: the back hunched up between the shoulders, the shoulders low.
    Torso: [['x', 8 + breath]],
    Chest: [['x', 10 - breath]],
    // The head raised against the lean, looking ahead and a little up (Enzo: "the head must be raised"); it looks about.
    Head: [['x', lean - 4 - head], ['y', look], ['z', tilt]],
    // The arms reach down to the ground: forward against the lean, a little out.
    Arm_R: [['x', lean + 3 + armR], ['y', -8]], Hand_R: [['x', 38 + bendR + liftHandR]],
    Arm_L: [['x', lean + 3 + armL], ['y', 8]], Hand_L: [['x', 38 + bendL + liftHandL]],
    // The thighs come forward under the hips, the knees bent back: the legs of a crouched beast.
    Thigh_R: [['x', lean - 33 + thighR], ['y', -10]], Leg_R: [['x', -36 + kneeR]],
    Thigh_L: [['x', lean - 33 + thighL], ['y', 10]], Leg_L: [['x', -36 + kneeL]],
  };
}

/** How high (m) the two hands' tips and the two feet are, in a pose. */
function contacts(pose) {
  const space = b.modelSpace(arm, locals(pose));
  const at = (m, p) => [m[0] * p[0] + m[1] * p[1] + m[2] * p[2] + m[3], m[4] * p[0] + m[5] * p[1] + m[6] * p[2] + m[7], m[8] * p[0] + m[9] * p[1] + m[10] * p[2] + m[11]];
  const tip = j => at(space[j], [0, 0.1, 0])[2];
  const foot = j => at(space[j], [0, 0.375, 0])[2];
  return { handR: tip('Tool_R'), handL: tip('Tool_L'), footR: foot('Leg_R'), footL: foot('Leg_L') };
}

// The idle of a hungry animal: it pants (a quick breath), sniffs (the head twitching up), looks left and right, its weight
// shifting from paw to paw.
const IDLE = Array.from({ length: 25 }, (_, i) => {
  const t = i * 0.1, w = 2 * Math.PI * t / 2.4;
  const pant = Math.sin(2 * Math.PI * t / 0.4);
  const sniff = (i % 8 === 3 || i % 8 === 4) ? -6 : 0;
  return { t, pose: beast({ breath: 3 * pant, sway: 3 * Math.sin(w), head: sniff + 2 * pant, look: 28 * Math.sin(w + 0.6) * (Math.abs(Math.sin(w + 0.6)) > 0.3 ? 1 : 0.4),
    tilt: 8 * Math.sin(w * 2), armR: 3 * Math.sin(w), armL: -3 * Math.sin(w) }) };
});

// The walk (1.0 s): right hand and left foot forward together, then the other pair.
const W = 18;
const walk = p => beast({ hip: 0.45 + 0.02 * Math.cos(4 * Math.PI * p), sway: 3 * Math.sin(2 * Math.PI * p),
  armR: W * Math.cos(2 * Math.PI * p), armL: -W * Math.cos(2 * Math.PI * p),
  liftHandR: Math.max(0, 22 * Math.sin(2 * Math.PI * p)), liftHandL: Math.max(0, -22 * Math.sin(2 * Math.PI * p)),
  thighL: W * Math.cos(2 * Math.PI * p), thighR: -W * Math.cos(2 * Math.PI * p),
  kneeL: -Math.max(0, 24 * Math.sin(2 * Math.PI * p)), kneeR: -Math.max(0, -24 * Math.sin(2 * Math.PI * p)) });
const WALK = [0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1].map(p => ({ t: p * 1.0, pose: walk(p) }));

// The run (0.6 s), a gallop: the hands reach far ahead together, then the feet land under the chest and push.
const run = p => {
  const s = Math.sin(2 * Math.PI * p), c = Math.cos(2 * Math.PI * p);
  return beast({ lean: 78 - 6 * s, hip: 0.68 + 0.08 * Math.max(0, s), breath: 4 * s,
    armR: 34 * c, armL: 30 * c, liftHandR: Math.max(0, 30 * s), liftHandL: Math.max(0, 30 * s),
    thighR: -30 * c, thighL: -26 * c, kneeR: -Math.max(0, -40 * s), kneeL: -Math.max(0, -40 * s), head: -4 * s });
};
const RUN = [0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1].map(p => ({ t: p * 0.6, pose: run(p) }));

/** Sets a pose down so its lowest hand or foot is on the ground: none under it, and always one on it. */
function grounded(pose) {
  const c = contacts(pose), low = Math.min(c.handR, c.handL, c.footR, c.footL);
  return { ...pose, Root: { ...pose.Root, pos: [pose.Root.pos[0], pose.Root.pos[1], pose.Root.pos[2] - low] } };
}
const ground = keys => keys.map(k => ({ t: k.t, pose: grounded(k.pose) }));
const anims = { demon_beast_idle: ground(IDLE), demon_beast_walk: ground(WALK), demon_beast_run: ground(RUN) };
if (process.argv.includes('--check')) {
  for (const [name, keys] of Object.entries(anims)) for (const k of keys) {
    const c = contacts(k.pose);
    console.log(name.padEnd(18), k.t.toFixed(3), Object.entries(c).map(([j, z]) => `${j} ${z.toFixed(2)}`).join('  '));
  }
} else {
  for (const [name, keys] of Object.entries(anims)) writeSmooth(name, keys);
}
module.exports = { beast, contacts, anims };
