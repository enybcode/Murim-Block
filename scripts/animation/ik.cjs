// A small pose solver for the animation scripts: finds joint angles so points of the body (the hilt, the blade's point
// and edge, the hands, feet, knees, head) reach targets, in the character's axes (x right, y front, z up; blocks), by
// coordinate descent. Bind points of the sword: its point 1.04 ahead of the hilt; its edge faces down in the bind pose.
const b = require('./biped.cjs');
const { arm, locals } = require('./pose.cjs');
const bind = b.modelSpace(arm, locals({}));
const R = m => [[m[0], m[1], m[2]], [m[4], m[5], m[6]], [m[8], m[9], m[10]]];
const carried = (m, j, bindPoint) => {
  const o0 = b.origin(bind[j]), d = bindPoint.map((v, i) => v - o0[i]);
  const rb = R(bind[j]), rm = R(m[j]);
  const local = [0, 1, 2].map(c => rb[0][c] * d[0] + rb[1][c] * d[1] + rb[2][c] * d[2]);
  return b.origin(m[j]).map((v, i) => v + rm[i][0] * local[0] + rm[i][1] * local[1] + rm[i][2] * local[2]);
};
const POINTS = {
  hilt: m => b.origin(m.Tool_R),
  tip: m => carried(m, 'Tool_R', [0.37, 1.01, 0.73]),
  handL: m => b.origin(m.Tool_L),
  footR: m => carried(m, 'Leg_R', [0.12, 0, 0]),
  footL: m => carried(m, 'Leg_L', [-0.13, 0, 0]),
  kneeR: m => b.origin(m.Leg_R),
  kneeL: m => b.origin(m.Leg_L),
  head: m => b.origin(m.Head),
  mid: m => carried(m, 'Tool_R', [0.37, 0.5, 0.78]),
  // On a long haft, where the second hand holds: a fist (0.25) behind the right hand, on the blade's line.
  haft: m => carried(m, 'Tool_R', [0.375, -0.28, 0.84]),
  edge: m => carried(m, 'Tool_R', [0.37, 0.5, 0.68]),
  elbowR: m => b.origin(m.Hand_R),
};
function measure(pose) {
  const m = b.modelSpace(arm, locals(pose));
  const out = {};
  for (const [k, fn] of Object.entries(POINTS)) out[k] = fn(m);
  return out;
}
/**
 * pose: the base pose, its joint lists holding the axes to solve (values are starting guesses);
 * vars: [[joint, index, min, max]]; targets: {point: [x, y, z] (null to skip an axis), weight}; stay: a weight
 * keeping the angles near their starting values (per degree squared), for a motion that stays smooth from a last pose.
 */
function solve(pose, vars, targets, iters = 4000, stay = 0) {
  const p = JSON.parse(JSON.stringify(pose));
  const get = ([j, i]) => p[j][i][1], set = ([j, i], v) => { p[j][i][1] = v; };
  const start = vars.map(get);
  const cost = () => {
    const m = measure(p); let c = 0;
    vars.forEach((v, n) => { c += stay * (get(v) - start[n]) ** 2; });
    for (const [k, t] of Object.entries(targets)) {
      const w = t.w || 1;
      t.at.forEach((v, i) => { if (v !== null) c += w * (m[k][i] - v) ** 2; });
    }
    return c;
  };
  let best = cost(), step = 20;
  for (let it = 0; it < iters && step > 0.05; it++) {
    let improved = false;
    for (const v of vars) {
      for (const s of [step, -step]) {
        const old = get(v), nv = Math.max(v[2], Math.min(v[3], old + s));
        set(v, nv); const c = cost();
        if (c < best - 1e-9) { best = c; improved = true; } else set(v, old);
      }
    }
    if (!improved) step *= 0.6;
  }
  for (const v of vars) set(v, Math.round(get(v)));
  return { pose: p, cost: cost(), at: measure(p) };
}
const fmt = v => v.map(x => x.toFixed(2)).join(', ');
function report(name, r) {
  console.log(`--- ${name} cost ${r.cost.toFixed(4)}`);
  for (const [k, v] of Object.entries(r.at)) console.log('  ', k.padEnd(7), fmt(v));
  const j = Object.entries(r.pose).filter(([k]) => k !== 'Root').map(([k, v]) => `${k}: ${JSON.stringify(v).replace(/"/g, "'")}`).join(', ');
  console.log('  ', `Root: ${JSON.stringify(r.pose.Root)}, ${j}`);
}
module.exports = { solve, measure, report, fmt };
