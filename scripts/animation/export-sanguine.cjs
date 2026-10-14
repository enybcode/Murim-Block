// Export only the approved character movements; the showcase is never a combat animation.
const fs = require('fs');
const path = require('path');
const { convert, sample } = require('./blockbench-to-epicfight.cjs');
const { writeFrames, MURIM_BIPED } = require('./pose.cjs');
const model = JSON.parse(fs.readFileSync(path.join(__dirname, '../../art/blockbench/sanguine_art.bbmodel'), 'utf8'));
function slice(source, name, from, to) {
  const a = structuredClone(source); a.name = name; a.length = to - from;
  for (const track of Object.values(a.animators)) {
    const keys = track.keyframes;
    track.keyframes = [];
    for (const channel of ['position', 'rotation', 'scale']) {
      const ks = keys.filter(k => k.channel === channel);
      if (!ks.length) continue;
      const endpoint = t => {
        const [x, y, z] = sample(ks, t);
        return {channel, time: t - from, interpolation:'linear', data_points:[{x,y,z}]};
      };
      track.keyframes.push(endpoint(from), ...ks.filter(k => k.time > from && k.time < to)
        .map(k => ({...k, time:k.time - from})), endpoint(to));
    }
  }
  return a;
}
const named = name => model.animations.find(a => a.name === name);
const combo = named('sanguine_combo');
const clips = ['sanguine_guard','sanguine_crystal_charge','sanguine_crystal_throw','sanguine_scarlet_tear','sanguine_crystal_form','sanguine_crystal_barrage'].map(named);
clips.push(slice(named('sanguine_guard'),'sanguine_guard_hit',0,0.3),
  slice(combo,'sanguine_straight',0,0.36), slice(combo,'sanguine_cross',0.32,0.70),
  slice(combo,'sanguine_rise',0.66,1.52));
for (const clip of clips) {
  const { times, frames } = convert(model, clip);
  writeFrames(MURIM_BIPED, clip.name, times, frames);
  console.log(clip.name, times.length, 'frames', clip.length.toFixed(2), 'seconds');
}
