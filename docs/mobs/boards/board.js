// Storyboard sheets for the Disciple déchu's armed moves (docs/mobs/boards/*.html, printed to PNG with Chrome).
// A pose is drawn in profile, facing right, as blocky limbs. Angles are in degrees on screen: limbs from straight down
// (0) toward the front (+); the torso from straight up (0) leaning forward (+); the weapon from straight down (0),
// forward at 90, up at 180, back at 270. Near limbs (right arm, right leg) are light, far ones dark.
const C = {
  robe: '#cfc5ad', robeDark: '#9d927b', skin: '#7f907a', skinDark: '#5d6b5a', hair: '#26222a', belt: '#5b3a29',
  blade: '#b9c2cc', bladeEdge: '#eef3f7', haft: '#6b4a2f', iron: '#8e98a2', arc: '#c23b2f', ghost: '#9aa3ad',
  ink: '#2a2420', soft: '#6b5a4c', target: '#c9cfd6',
};
const rad = d => d * Math.PI / 180;
const down = (a, l) => [Math.sin(rad(a)) * l, Math.cos(rad(a)) * l];
const up = (a, l) => [Math.sin(rad(a)) * l, -Math.cos(rad(a)) * l];
const add = (p, d) => [p[0] + d[0], p[1] + d[1]];
const L = {torso: 34, upper: 17, fore: 17, thigh: 17, shin: 17, head: 22};

/** Joint positions of a pose with its hips at (x, y). */
function body(x, y, p) {
  const hip = [x + (p.hx || 0), y + (p.hy || 0)];
  const neck = add(hip, up(p.lean || 0, L.torso));
  const shoulder = add(hip, up(p.lean || 0, L.torso - 4));
  const r = {hip, neck, shoulder};
  r.elbowR = add(shoulder, down(p.aR, L.upper));
  r.handR = add(r.elbowR, down(p.fR, L.fore));
  // A knee only bends forward: the shin never points further forward than its thigh.
  r.kneeF = add(hip, down(p.tF, L.thigh));
  r.footF = add(r.kneeF, down(Math.min(p.sF, p.tF - 4), L.shin));
  r.kneeB = add(hip, down(p.tB, L.thigh));
  r.footB = add(r.kneeB, down(Math.min(p.sB, p.tB - 4), L.shin));
  const w = WEAPONS[p.weapon] || WEAPONS.none;
  // The weapon from the right hand: its butt behind the hand, its length ahead.
  r.butt = add(r.handR, down(p.w + 180, w.butt));
  r.tip = add(r.handR, down(p.w, w.length));
  if (w.bow) {
    // A bow in the right hand: the arrow points along w, the stave across it; the string drawn back to the nock
    // by the left hand (draw 0 to 1), which reaches it by IK.
    const ahead = down(p.w, 1);
    r.tipA = add(r.handR, down(p.w + 90, 24));
    r.tipB = add(r.handR, down(p.w - 90, 24));
    r.nock = add(r.handR, down(p.w + 180, 5 + 20 * (p.draw || 0)));
    r.tip = add(r.nock, [ahead[0] * 34, ahead[1] * 34]);
    if (p.draw !== undefined) {
      r.elbowL = reach(shoulder, r.nock, L.upper, L.fore);
      r.handL = r.nock;
      return r;
    }
  }
  if (w.twoHands) {
    // The left hand on the haft, reached by the arm (two-bone IK, elbow down).
    r.grip = add(r.handR, down(p.w, w.second));
    const ik = reach(shoulder, r.grip, L.upper, L.fore);
    r.elbowL = ik;
    r.handL = r.grip;
  } else {
    r.elbowL = add(shoulder, down(p.aL, L.upper));
    r.handL = add(r.elbowL, down(p.fL, L.fore));
  }
  return r;
}

function reach(from, to, a, b) {
  const dx = to[0] - from[0], dy = to[1] - from[1];
  const d = Math.min(Math.hypot(dx, dy), a + b - 0.01);
  const angle = Math.atan2(dy, dx);
  const bend = Math.acos((a * a + d * d - b * b) / (2 * a * d));
  // The elbow below the line from shoulder to hand.
  const e = angle + bend;
  return [from[0] + Math.cos(e) * a, from[1] + Math.sin(e) * a];
}

const WEAPONS = {
  none: {length: 0, butt: 0},
  sword: {length: 44, butt: 7, draw: 'sword'},
  axe: {length: 46, butt: 6, second: 20, twoHands: true, draw: 'axe'},
  tool: {length: 52, butt: 6, second: 24, twoHands: true, draw: 'tool'},
  bow: {length: 0, butt: 0, bow: true, draw: 'bow'},
};

function line(a, b, color, width, extra = '') {
  return `<line x1="${a[0]}" y1="${a[1]}" x2="${b[0]}" y2="${b[1]}" stroke="${color}" stroke-width="${width}"
    stroke-linecap="square" ${extra}/>`;
}

/** The figure (and its weapon) as SVG. ghost: a faded earlier pose under it. */
function figure(x, y, p, ghost) {
  const r = body(x, y, p);
  const o = ghost ? ' opacity="0.28"' : '';
  const col = c => ghost ? C.ghost : c;
  let s = `<g${o}>`;
  const w = WEAPONS[p.weapon] || WEAPONS.none;
  // Far limbs first.
  s += line(r.hip, r.kneeB, col(C.robeDark), 8) + line(r.kneeB, r.footB, col(C.robeDark), 7);
  s += line(r.footB, add(r.footB, [6, 0]), col(C.ink), 4);
  if (!w.twoHands) {
    s += line(r.shoulder, r.elbowL, col(C.robeDark), 7) + line(r.elbowL, r.handL, col(C.skinDark), 6);
  }
  // Torso, belt, head.
  s += line(r.hip, r.neck, col(C.robe), 16);
  s += line(add(r.hip, up(p.lean || 0, 6)), add(r.hip, up(p.lean || 0, 9)), col(C.belt), 17);
  const head = add(r.neck, up((p.lean || 0) + (p.head || 0), 11));
  s += `<rect x="${head[0] - 11}" y="${head[1] - 11}" width="22" height="22" fill="${col(C.skin)}"
    transform="rotate(${(p.lean || 0) + (p.head || 0)} ${head[0]} ${head[1]})"/>`;
  s += `<rect x="${head[0] - 12}" y="${head[1] - 13}" width="24" height="9" fill="${col(C.hair)}"
    transform="rotate(${(p.lean || 0) + (p.head || 0)} ${head[0]} ${head[1]})"/>`;
  s += `<rect x="${head[0] + 5}" y="${head[1] - 3}" width="3" height="3" fill="${ghost ? C.ghost : (C.eye || '#9fffe0')}"
    transform="rotate(${(p.lean || 0) + (p.head || 0)} ${head[0]} ${head[1]})"/>`;
  // Near leg.
  s += line(r.hip, r.kneeF, col(C.robe), 8) + line(r.kneeF, r.footF, col(C.robe), 7);
  s += line(r.footF, add(r.footF, [6, 0]), col(C.ink), 4);
  // Weapon, then the arms holding it.
  if (w.draw) {
    s += weapon(w.draw, r, p, ghost);
  }
  if (w.twoHands) {
    s += line(r.shoulder, r.elbowL, col(C.robeDark), 7) + line(r.elbowL, r.handL, col(C.skinDark), 6);
  }
  s += line(r.shoulder, r.elbowR, col(C.robe), 7) + line(r.elbowR, r.handR, col(C.skin), 6);
  s += '</g>';
  return s;
}

function weapon(kind, r, p, ghost) {
  const col = c => ghost ? C.ghost : c;
  let s = '';
  if (kind === 'sword') {
    const guard = add(r.handR, down(p.w, 3));
    s += line(r.butt, r.handR, col(C.haft), 4);
    s += line(add(guard, down(p.w + 90, 6)), add(guard, down(p.w - 90, 6)), col(C.iron), 4);
    s += line(guard, r.tip, col(C.blade), 5);
    s += line(add(guard, down(p.w + 90, 1.5)), add(r.tip, down(p.w + 90, 1.5)), col(C.bladeEdge), 1.5);
  } else if (kind === 'axe') {
    s += line(r.butt, r.tip, col(C.haft), 5);
    const head = add(r.tip, down(p.w + 180, 6));
    s += line(head, add(head, down(p.w + 90, 13)), col(C.iron), 11);
    s += line(add(head, down(p.w + 90, 13)), add(head, down(p.w + 90, 15)), col(C.bladeEdge), 12);
  } else if (kind === 'tool') {
    s += line(r.butt, r.tip, col(C.haft), 4);
    s += line(add(r.tip, down(p.w + 180, 2)), add(r.tip, down(p.w, 11)), col(C.iron), 10);
  } else if (kind === 'bow') {
    const bulge = add(r.handR, down(p.w, 10 - 4 * (p.draw || 0)));
    const c = [2 * bulge[0] - (r.tipA[0] + r.tipB[0]) / 2, 2 * bulge[1] - (r.tipA[1] + r.tipB[1]) / 2];
    s += `<path d="M${r.tipA[0]},${r.tipA[1]} Q${c[0]},${c[1]} ${r.tipB[0]},${r.tipB[1]}" fill="none"
      stroke="${col(C.haft)}" stroke-width="5"/>`;
    s += line(r.tipA, r.nock, col('#d8d0b8'), 1.2) + line(r.nock, r.tipB, col('#d8d0b8'), 1.2);
    if (p.arrow) {
      s += line(r.nock, r.tip, col('#6b4a2f'), 2.5);
      s += line(r.nock, add(r.nock, down(p.w, 6)), col('#b33a35'), 4);
      s += line(add(r.tip, down(p.w + 180, 4)), r.tip, col(C.iron), 4);
    }
  }
  return s;
}

function arrowHead(p, dir, color) {
  const a = Math.atan2(dir[1], dir[0]);
  const b = [p[0] - Math.cos(a - 0.45) * 9, p[1] - Math.sin(a - 0.45) * 9];
  const c = [p[0] - Math.cos(a + 0.45) * 9, p[1] - Math.sin(a + 0.45) * 9];
  return `<path d="M${p[0]},${p[1]} L${b[0]},${b[1]} L${c[0]},${c[1]} Z" fill="${color}"/>`;
}

/** A motion arc from a to b bulging by bend (px, to the left of the travel), with its arrowhead. */
function arc(a, b, bend, color = C.arc, dash = '5 4', width = 2.5) {
  const m = [(a[0] + b[0]) / 2, (a[1] + b[1]) / 2];
  const d = [b[0] - a[0], b[1] - a[1]];
  const n = Math.hypot(d[0], d[1]) || 1;
  const c = [m[0] + -d[1] / n * bend, m[1] + d[0] / n * bend];
  const end = [b[0] - (b[0] - c[0]) * 0.02, b[1] - (b[1] - c[1]) * 0.02];
  return `<path d="M${a[0]},${a[1]} Q${c[0]},${c[1]} ${end[0]},${end[1]}" fill="none" stroke="${color}"
    stroke-width="${width}" stroke-dasharray="${dash}"/>` + arrowHead(b, [b[0] - c[0], b[1] - c[1]], color);
}

/** One key frame panel. */
function panel(frame, weaponKind) {
  const W = 190, H = 190, x = 78, y = 118;
  const pose = {...frame.pose, weapon: weaponKind};
  // The whole drawing, from the weapon raised high to a lunge far ahead, at 236 by 206.
  let s = `<svg viewBox="-6 -8 212 184" width="236" height="206">`;
  s += `<rect x="-6" y="-8" width="212" height="184" fill="#f8f3e8"/>`;
  s += `<line x1="0" y1="${y + 34}" x2="${W}" y2="${y + 34}" stroke="#d9cfbd" stroke-width="2"/>`;
  if (frame.target) {
    // The prey, faded, where the blow should meet it.
    s += `<rect x="${frame.target}" y="${y - 52}" width="14" height="86" fill="${C.target}"/>
      <rect x="${frame.target - 2}" y="${y - 74}" width="18" height="18" fill="${C.target}"/>`;
  }
  if (frame.from) {
    s += figure(x, y, {...frame.from, weapon: weaponKind}, true);
  }
  s += figure(x, y, pose, false);
  const now = body(x, y, pose);
  if (frame.from) {
    const before = body(x, y, {...frame.from, weapon: weaponKind});
    const what = frame.trace || 'tip';
    s += arc(before[what], now[what], frame.bend || 0);
  }
  for (const extra of frame.arrows || []) {
    const from = extra.at ? add(now[extra.at], extra.from) : extra.from;
    const to = extra.at ? add(now[extra.at], extra.to) : extra.to;
    s += arc(from, to, extra.bend || 0, extra.color || '#3a6fb0', extra.dash || '', 2.5);
  }
  if (frame.impact) {
    const p = now.tip;
    s += `<g stroke="#e8a20c" stroke-width="3">${[0, 60, 120, 180, 240, 300].map(a =>
      line(add(p, [Math.cos(rad(a)) * 6, Math.sin(rad(a)) * 6]), add(p, [Math.cos(rad(a)) * 15,
        Math.sin(rad(a)) * 15]), '#e8a20c', 3)).join('')}</g>`;
  }
  s += `<text x="0" y="6" font-size="11" font-weight="700" fill="${C.ink}">${frame.ticks}</text>`;
  s += `<text x="0" y="19" font-size="10" fill="${frame.phaseColor || C.soft}">${frame.phase}</text>`;
  s += '</svg>';
  return `<div class="frame">${s}<div class="note">${frame.note}</div></div>`;
}

function sheet(spec) {
  Object.assign(C, spec.colors || {});
  let h = `<h1>${spec.title}</h1><div class="sub">${spec.sub}</div>`;
  h += `<div class="top"><div class="panelbox wrong"><h2>Ce qui ne va pas dans la version actuelle</h2><ul>`;
  h += spec.wrong.map(w => `<li>${w}</li>`).join('') + '</ul></div>';
  h += `<div class="panelbox fix"><h2>Ce qui change pour toutes</h2><ul>`;
  h += spec.fix.map(w => `<li>${w}</li>`).join('') + '</ul></div></div>';
  for (const anim of spec.anims) {
    h += `<div class="anim"><div class="label"><div class="name">${anim.name}</div>
      <div class="meta">${anim.meta}</div><div class="feel"><b>Sensation :</b> ${anim.feel}</div>
      <div class="read"><b>Pour le joueur :</b> ${anim.read}</div></div><div class="frames">`;
    h += anim.frames.map(f => panel(f, spec.weapon)).join('') + '</div></div>';
  }
  h += `<div class="legend"><span><i style="background:${C.arc}"></i>trajectoire de l'arme (pointillés rouges)</span>
    <span><i style="background:#3a6fb0"></i>mouvement du corps / des pieds (bleu)</span>
    <span><i style="background:#e8a20c"></i>impact</span>
    <span><i style="background:${C.ghost}"></i>pose précédente (fantôme)</span>
    <span>ticks : 20 par seconde</span></div>`;
  document.body.innerHTML = h;
}
