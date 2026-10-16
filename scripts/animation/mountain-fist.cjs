// The Mountain-Shattering Fist's moves for the player (docs/combat/styles/mountain-shattering-fist.md): the Bandit
// Warlord's own (bandit-warlord-3d.cjs: the same biped skeleton, so the same keys, at a man's size), and the art's own
// ultimate and finish. Written as Epic Fight animations biped/murim/msf_* (pose.cjs writeSmooth).
//   msf_crusher          Mountain-Crushing Punch: wound back, a step, the right fist driven through;
//   msf_earth_breaker    Earth Breaker: both fists raised high and brought down on the ground;
//   msf_bull_charge      Bull Charge: the shoulder lowered, a rush of some four blocks, the impact;
//   msf_sky_breaker      Sky Breaker (the secret): crouched low, an uppercut rising on the toes;
//   msf_falling_fist     any technique from the air: diving fist first (held while the body falls);
//   msf_land             the landing of a dive or of the ultimate: fist in the ground, one knee down, then up;
//   msf_ascend           Heaven-Falling Mountain (the ultimate): a deep crouch, then up into the sky, arms raised;
//   msf_burial           Mountain Burial (the finish): over the body, three blows of the fist driving it into the ground.
// Usage: node scripts/animation/mountain-fist.cjs
const { writeSmooth } = require('./pose.cjs');
const { ANIMS, REST, LANDED, DIVE, CROUCH, HIP } = require('./bandit-warlord-3d.cjs');

/** Up on the toes, the right fist raised high behind the head (the Collapse's wind-up). */
const RAISED = (z = 0) => ({ Root: { pos: [0, 0, HIP + 0.04] }, Torso: [['x', 14], ['z', -16 + z]], Chest: [['x', 6]], Head: [['x', -16], ['z', 14]],
  Arm_R: [['x', 176], ['y', -10]], Hand_R: [['x', 50]], Arm_L: [['x', 40], ['y', 30]], Hand_L: [['x', 60]],
  Thigh_R: [['y', -10]], Leg_R: [], Thigh_L: [['x', 20], ['y', 10]], Leg_L: [['x', -10]] });
/** Over the body, one knee down: the fist raised, or driven down into it. */
const KNEEL_UP = { ...LANDED(-20), Arm_R: [['x', 150], ['y', -10]], Hand_R: [['x', 80]], Torso: [['x', -24]], Head: [['x', 30]] };
const KNEEL_DOWN = (lean = 0) => ({ ...LANDED(-12 + lean), Arm_R: [['x', 30], ['y', -4]], Hand_R: [['x', 0]] });

const GAME = {
  msf_crusher: ANIMS.mountain_crusher,
  msf_earth_breaker: ANIMS.earth_breaker,
  msf_bull_charge: ANIMS.bull_charge,
  msf_sky_breaker: ANIMS.sky_breaker,
  msf_falling_fist: [{ t: 0, pose: DIVE(HIP) }, { t: 0.4, pose: { ...DIVE(HIP), Arm_L: [['x', 140], ['z', -40]] } }, { t: 0.8, pose: DIVE(HIP) }],
  msf_land: ANIMS.leap_land,
  msf_ascend: [
    { t: 0, pose: REST() },
    { t: 0.3, pose: { ...CROUCH, Arm_R: [['x', -40], ['y', -16]], Hand_R: [['x', 40]], Arm_L: [['x', -40], ['y', 16]], Hand_L: [['x', 40]] } },
    { t: 0.45, pose: { Root: { pos: [0, 0, HIP] }, Torso: [['x', 10]], Chest: [['x', 4]], Head: [['x', -30]],
      Arm_R: [['x', 170], ['z', 20]], Hand_R: [['x', 10]], Arm_L: [['x', 170], ['z', -20]], Hand_L: [['x', 10]],
      Thigh_R: [['x', -10]], Leg_R: [['x', -20]], Thigh_L: [['x', 20]], Leg_L: [['x', -40]] } },
    { t: 1.2, pose: { Root: { pos: [0, 0, HIP] }, Torso: [['x', 6]], Chest: [['x', 4]], Head: [['x', -20]],
      Arm_R: [['x', 165], ['z', 25]], Hand_R: [['x', 20]], Arm_L: [['x', 165], ['z', -25]], Hand_L: [['x', 20]],
      Thigh_R: [['x', 20]], Leg_R: [['x', -50]], Thigh_L: [['x', 40]], Leg_L: [['x', -70]] } },
  ],
  msf_burial: [
    { t: 0, pose: REST() },
    { t: 0.3, pose: RAISED() },
    { t: 0.45, pose: KNEEL_DOWN() },
    { t: 0.62, pose: KNEEL_UP },
    { t: 0.8, pose: KNEEL_DOWN(4) },
    { t: 1.05, pose: { ...KNEEL_UP, Arm_R: [['x', 170], ['y', -10]], Torso: [['x', -10]], Head: [['x', 10]] } },
    { t: 1.4, pose: KNEEL_DOWN(-6) },
    { t: 1.9, pose: KNEEL_DOWN(0) },
    { t: 2.4, pose: REST() },
  ],
};

if (require.main === module) for (const [name, keys] of Object.entries(GAME)) writeSmooth(name, keys);

module.exports = { GAME };
