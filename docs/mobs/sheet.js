// Design sheets of mob looks (docs/mobs/<name>.html, printed to PNG with Chrome at 1600 x 800), in the layout of
// disciple-dechu.html. A page sets window.SHEET (one sheet) or window.LINEUP (several fronts side by side) and loads
// this script. Figures are lists of shapes on the biped's front (128 x 270 units, 8 units = 1 px of the skin) and
// profile: [x, y, w, h, fill], {d: path, fill}, or {t: transform, items: [...]}.
const CSS = `
*{box-sizing:border-box;margin:0;padding:0}
body{width:1600px;height:800px;background:#efe7d6;font-family:"Segoe UI",Arial,sans-serif;color:#2a2420;padding:28px 36px;position:relative;overflow:hidden}
body:before{content:"";position:absolute;inset:12px;border:3px double #7a2020;pointer-events:none}
h1{font-family:Georgia,serif;font-size:44px;font-weight:600;color:#7a2020;letter-spacing:1px}
.sub{font-size:17px;color:#5b4a3e;margin-top:2px}
.seal{position:absolute;right:48px;top:24px;width:84px;height:84px;border:4px solid #a32020;color:#a32020;font-family:"KaiTi","SimSun",serif;font-size:30px;display:flex;align-items:center;justify-content:center;text-align:center;line-height:1.05;transform:rotate(-6deg)}
.grid{display:grid;grid-template-columns:520px 1fr 1fr;gap:22px;margin-top:26px}
.col{display:flex;flex-direction:column;gap:18px}
.panel{background:#f8f2e6;border:1.5px solid #c9b89a;border-radius:6px;padding:14px 16px}
h2{font-size:17px;text-transform:uppercase;letter-spacing:1.5px;color:#7a2020;border-bottom:1.5px solid #c9b89a;padding-bottom:5px;margin-bottom:9px}
table{width:100%;border-collapse:collapse;font-size:14.5px}
td,th{padding:4px 5px;text-align:left;vertical-align:top}
th{font-size:12.5px;color:#7d6a5a;text-transform:uppercase;letter-spacing:.5px;border-bottom:1px solid #d8cab0}
tr+tr td{border-top:1px solid #e6dcc8}
td.v{font-weight:600;white-space:nowrap}
.tag{display:inline-block;font-size:12px;padding:1px 7px;border-radius:10px;background:#e4d9c3;margin:0 4px 2px 0}
.ok{background:#cfe3c0}.warn{background:#f3d9a8}.no{background:#f0c4c0}.new{background:#d6d0f0}
.bar{height:9px;background:#e1d6c0;border-radius:5px;overflow:hidden;margin-top:3px}.bar i{display:block;height:100%;background:#7a2020}
ul{list-style:none;font-size:14.5px}li{padding:3px 0 3px 16px;position:relative}li:before{content:"◆";position:absolute;left:0;color:#a32020;font-size:10px;top:7px}
p{font-size:14.5px;line-height:1.5}
.small{font-size:13px;color:#6b5a4c;line-height:1.4}
.lineup{display:grid;gap:14px;margin-top:22px}
.card{background:#f8f2e6;border:1.5px solid #c9b89a;border-radius:6px;padding:10px 12px;display:flex;flex-direction:column;align-items:center;gap:6px}
.card h3{font-family:Georgia,serif;color:#7a2020;font-size:21px;font-weight:600;text-align:center}
.card .mc{font-family:Consolas,monospace;font-size:12.5px;color:#6b5a4c}
.card p{font-size:13.5px;text-align:center;line-height:1.4}
.card.base{border-color:#7a2020;border-width:2px}
`;

const NS = 'http://www.w3.org/2000/svg';
function shapes(items) {
  return items.map(s => {
    if (Array.isArray(s)) return `<rect x="${s[0]}" y="${s[1]}" width="${s[2]}" height="${s[3]}" fill="${s[4]}"/>`;
    if (s.d) return `<path d="${s.d}" fill="${s.fill}"/>`;
    return `<g transform="${s.t}">${shapes(s.items)}</g>`;
  }).join('');
}

function appearance(S) {
  const parts = S.parts.map((p, i) =>
    `<text x="352" y="${40 + i * 40}"><tspan font-weight="600">${i + 1}</tspan> ${p}</text>`).join('');
  const pal = S.palette.map(([c, n], i) =>
    `<rect x="${20 + i * 58}" y="398" width="36" height="22" fill="${c}" stroke="#b9ad96" stroke-width="${/^#(e|f)/i.test(c) ? 1 : 0}"/><text x="${20 + i * 58}" y="436">${n}</text>`).join('');
  return `<div class="panel" style="padding-bottom:8px"><h2>Apparence</h2>
<svg xmlns="${NS}" viewBox="0 0 490 470" width="490" height="470">
<g transform="translate(30,40) scale(1.15)">${shapes(S.front)}</g>
<g transform="translate(190,40) scale(1.15)">${shapes(S.side)}</g>
<text x="104" y="372" font-size="13" text-anchor="middle" fill="#6b5a4c">FACE</text>
<text x="250" y="372" font-size="13" text-anchor="middle" fill="#6b5a4c">${S.sideLabel || 'PROFIL'}</text>
<line x1="18" y1="33" x2="18" y2="351" stroke="#7d6a5a"/><line x1="12" y1="33" x2="24" y2="33" stroke="#7d6a5a"/><line x1="12" y1="351" x2="24" y2="351" stroke="#7d6a5a"/>
<text x="10" y="200" font-size="12" fill="#6b5a4c" transform="rotate(-90 10 200)" text-anchor="middle">${S.height}</text>
<g font-size="13" fill="#2a2420">${parts}</g>
<g font-size="11.5" fill="#4a3d32">${pal}</g>
<text x="20" y="462" font-size="12.5" fill="#6b5a4c">${S.footnote}</text>
</svg></div>`;
}

function sheet(S) {
  const stats = S.stats.map(([k, v, w]) =>
    `<tr><td>${k}</td><td class="v">${v}</td><td style="width:45%"><div class="bar"><i style="width:${w}%"></i></div></td></tr>`).join('');
  const attacks = S.attacks.map(([n, d, r, dmg, tags]) =>
    `<tr><td><b>${n}</b><br><span class="small">${d}</span></td><td class="v">${r}</td><td class="v">${dmg}</td><td>${tags}</td></tr>`).join('');
  const anims = S.anims.map(([k, v]) => `<tr><td class="v">${k}</td><td>${v}</td></tr>`).join('');
  return `<h1>${S.title}</h1><div class="sub">${S.sub}</div><div class="seal">${S.seal}</div>
<div class="grid">${appearance(S)}
<div class="col">
<div class="panel"><h2>Lore</h2><p>${S.lore}</p></div>
<div class="panel"><h2>Statistiques</h2><table>${stats}</table><p class="small" style="margin-top:6px">${S.statsNote}</p></div>
<div class="panel" style="flex:1"><h2>Ce qui change</h2><ul>${S.behavior.map(b => `<li>${b}</li>`).join('')}</ul></div>
</div>
<div class="col">
<div class="panel"><h2>Attaques</h2><table><tr><th>Attaque</th><th>Portée</th><th>Dégâts</th><th>Réponse</th></tr>${attacks}</table></div>
<div class="panel" style="flex:1"><h2>Animations</h2><table>${anims}</table><p class="small" style="margin-top:8px">${S.animNote}</p></div>
</div></div>`;
}

function lineup(L) {
  const cards = L.mobs.map(m => `<div class="card${m.base ? ' base' : ''}">
<svg xmlns="${NS}" viewBox="-24 -24 176 300" width="250" height="426">${shapes(m.front)}</svg>
<h3>${m.name}</h3><div class="mc">${m.id}</div><p>${m.line}</p></div>`).join('');
  return `<h1>${L.title}</h1><div class="sub">${L.sub}</div><div class="seal">${L.seal}</div>
<div class="lineup" style="grid-template-columns:repeat(${L.mobs.length},1fr)">${cards}</div>
<p class="small" style="margin-top:14px">${L.note}</p>`;
}

document.head.insertAdjacentHTML('beforeend', `<style>${CSS}</style>`);
document.body.innerHTML = window.LINEUP ? lineup(window.LINEUP) : sheet(window.SHEET);
