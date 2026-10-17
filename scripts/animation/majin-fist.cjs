// The Majin Fist's moves for the player (docs/combat/styles/majin-fist.md): the Son of the Demon's own
// (colossus-epicfight.cjs: his armature has the biped's joints, axes and hip height, so his keys read on the player at a
// man's size). Written as Epic Fight animations biped/murim/mjf_* (pose.cjs writeSmooth).
//   mjf_sunfire     Sunfire Fist: wound back, a step, the right fist driven through (its burst at 0.48 s);
//   mjf_flare       Pride Flare: drawn back, the palm thrust out (the wave at 0.7 s);
//   mjf_conjure     Cruel Sun: the palm raised, the sun grown over it, hurled (released at 1.6 s);
//   mjf_judgment    Noon Judgment: pointing at the sky, then the fist brought down (the pillars at 1.6 s);
//   mjf_leap        Midday Meteor: a deep crouch and the spring (off the ground at 0.7 s);
//   mjf_dive        held while the body falls onto its mark;
//   mjf_the_one     The One: gathered in, then flung open, rising;
//   mjf_finish      Noon Sentence (the finish): a sun raised over the body and brought down on it.
// Usage: node scripts/animation/majin-fist.cjs
const { writeSmooth } = require('./pose.cjs');
const { GAME: COLOSSUS } = require('./colossus-epicfight.cjs');

const GAME = {
  mjf_sunfire: COLOSSUS.punch,
  mjf_flare: COLOSSUS.flare,
  mjf_conjure: COLOSSUS.conjure,
  mjf_judgment: COLOSSUS.judgment,
  mjf_leap: COLOSSUS.leap,
  mjf_dive: COLOSSUS.dive,
  mjf_the_one: COLOSSUS.the_one,
  mjf_finish: COLOSSUS.conjure,
};

if (require.main === module) for (const [name, keys] of Object.entries(GAME)) writeSmooth(name, keys);

module.exports = { GAME };
