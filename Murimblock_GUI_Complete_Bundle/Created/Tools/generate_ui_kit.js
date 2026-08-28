const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const ROOT = path.resolve('Murimblock_GUI_Kit');
const dirs = ['Mockups','Panels','Buttons','Tabs','Qi','Techniques','Tomes','Icons','Decorations','SVG'];
for (const d of dirs) fs.mkdirSync(path.join(ROOT, d), { recursive: true });

const C = {
  ink:'#15191c', ink2:'#252b2e', paper:'#f3efe5', paper2:'#ddd5c4', pale:'#fffaf0',
  line:'#464b4c', muted:'#8d918d', qi:'#59c9ed', qi2:'#207ea8', gold:'#d9ad5b',
  gold2:'#8c672e', disabled:'#616567', white:'#f6f0df'
};

function svg(w,h,body,defs='') {
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}"><defs>${defs}</defs>${body}</svg>`;
}
function paperDefs() {
  return `<linearGradient id="paper" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#faf7ef"/><stop offset="1" stop-color="#e9e2d4"/></linearGradient><linearGradient id="dark" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#202528"/><stop offset="1" stop-color="#111416"/></linearGradient><linearGradient id="blue" x1="0" y1="0" x2="1" y2="0"><stop stop-color="#297ea8"/><stop offset=".55" stop-color="#69d5f3"/><stop offset="1" stop-color="#b9f4ff"/></linearGradient><filter id="shadow" x="-30%" y="-30%" width="160%" height="160%"><feDropShadow dx="0" dy="6" stdDeviation="7" flood-opacity=".28"/></filter><filter id="glow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="5" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>`;
}
function corner(x,y,flipX=1,flipY=1,stroke=C.line) {
  return `<g transform="translate(${x} ${y}) scale(${flipX} ${flipY})" fill="none" stroke="${stroke}" stroke-width="2" stroke-linecap="square"><path d="M0 26V0h26M8 26V8h18"/><path d="M4 4l8 8" opacity=".42"/></g>`;
}
function frame(w,h,fill='url(#paper)',stroke=C.ink) {
  return `<rect x="10" y="10" width="${w-20}" height="${h-20}" rx="3" fill="${fill}" stroke="${stroke}" stroke-width="4"/><rect x="22" y="22" width="${w-44}" height="${h-44}" rx="1" fill="none" stroke="${stroke}" stroke-width="2" opacity=".78"/>${corner(22,22,1,1)}${corner(w-22,22,-1,1)}${corner(22,h-22,1,-1)}${corner(w-22,h-22,-1,-1)}`;
}
function separator(w, y=18, color=C.line) {
  const mid=w/2; return `<path d="M10 ${y}H${mid-12}M${mid+12} ${y}H${w-10}" stroke="${color}" stroke-width="2"/><path d="M${mid} ${y-7}l7 7-7 7-7-7z" fill="none" stroke="${color}" stroke-width="2"/>`;
}
function label(text,x,y,size=28,anchor='start',fill=C.ink,weight='400') { return `<text x="${x}" y="${y}" font-family="Georgia,serif" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}" fill="${fill}" letter-spacing="1">${text}</text>`; }
function sans(text,x,y,size=22,anchor='start',fill=C.ink,weight='400') { return `<text x="${x}" y="${y}" font-family="Segoe UI,Arial,sans-serif" font-size="${size}" font-weight="${weight}" text-anchor="${anchor}" fill="${fill}">${text}</text>`; }

async function emit(folder,name,w,h,body,defs=paperDefs()) {
  const source=svg(w,h,body,defs);
  const svgPath=path.join(ROOT,'SVG',`${name}.svg`);
  fs.writeFileSync(svgPath, source);
  await sharp(Buffer.from(source)).png().toFile(path.join(ROOT,folder,`${name}.png`));
}

function buttonBody(w,h,state) {
  const cfg={normal:[C.ink,C.paper2,C.line],hover:['#22282b',C.pale,C.gold],pressed:['#0d1012','#c7bfae',C.gold2],disabled:['#34383a','#777b7a','#515557'],selected:['#171a1c',C.pale,C.gold]}[state];
  const [fill,accent,stroke]=cfg;
  const glow=state==='hover'||state==='selected'?' filter="url(#glow)"':'';
  return `<path d="M18 4H${w-18}l14 14v${h-36}l-14 14H18L4 ${h-18}V18z" fill="${fill}" stroke="${stroke}" stroke-width="3"${glow}/><path d="M26 12H${w-26}M26 ${h-12}H${w-26}" stroke="${accent}" stroke-width="2" opacity=".9"/><path d="M11 22l11-11M${w-11} 22l-11-11M11 ${h-22}l11 11M${w-11} ${h-22}l-11 11" stroke="${accent}" stroke-width="2"/>`;
}
function slotBody(size,state) {
  const stroke={normal:C.line,hover:C.pale,selected:C.gold,locked:'#555b5e',unknown:'#777c7d'}[state];
  const fill=state==='locked'?'#1c2022':state==='unknown'?'#292d2f':'url(#dark)';
  const glow=(state==='hover'||state==='selected')?' filter="url(#glow)"':'';
  return `<path d="M18 6h${size-36}l12 12v${size-36}l-12 12H18L6 ${size-18}V18z" fill="${fill}" stroke="${stroke}" stroke-width="4"${glow}/><path d="M22 14h${size-44}M22 ${size-14}h${size-44}" stroke="${stroke}" stroke-width="2" opacity=".55"/>`;
}
function lockIcon(cx,cy,s=1,color=C.white) { return `<g transform="translate(${cx} ${cy}) scale(${s})" fill="none" stroke="${color}" stroke-width="4" stroke-linecap="round"><rect x="-18" y="-2" width="36" height="30" rx="4" fill="${C.ink2}"/><path d="M-11-2v-9a11 11 0 0122 0v9"/><path d="M0 8v8"/></g>`; }
function unknownIcon(cx,cy,s=1,color=C.white) { return `<g transform="translate(${cx} ${cy}) scale(${s})" fill="none" stroke="${color}" stroke-width="5" stroke-linecap="round"><path d="M-12-16c2-15 25-16 27-2 2 13-15 13-15 25"/><path d="M0 22v1"/></g>`; }
function bookIcon(open=false,color=C.white) {
  return open?`<g fill="none" stroke="${color}" stroke-width="4" stroke-linejoin="round"><path d="M12 20c16-7 31-5 40 5v49c-10-9-25-10-40-3zM92 20c-16-7-31-5-40 5v49c10-9 25-10 40-3z"/><path d="M52 25v49"/></g>`:`<g fill="none" stroke="${color}" stroke-width="4"><path d="M18 15h57l13 12v62H31L18 77z"/><path d="M31 27h57M31 27v62"/><path d="M42 42h34M42 54h29"/></g>`;
}
function lotusIcon(color=C.white) { return `<g transform="translate(4 10)" fill="none" stroke="${color}" stroke-width="3"><path d="M48 62C20 48 20 22 20 22c22 6 28 22 28 40zM48 62C76 48 76 22 76 22 54 28 48 44 48 62zM48 56C32 33 48 9 48 9c16 24 0 47 0 47zM48 65C25 70 10 56 10 56c22-2 32 4 38 9zM48 65c23 5 38-9 38-9-22-2-32 4-38 9z"/></g>`; }
function qiIcon(color=C.qi) { return `<g transform="translate(48 48)" fill="none" stroke="${color}" stroke-linecap="round"><circle r="33" stroke-width="4" stroke-dasharray="154 54" transform="rotate(-38)"/><circle r="25" stroke-width="2" opacity=".48"/><path d="M0-13L13 0 0 13-13 0z" stroke-width="3"/><circle r="4" fill="${color}" stroke="none"/></g>`; }

async function main(){
  // Panels and GUI foundations
  await emit('Panels','panel_base',640,420,frame(640,420));
  await emit('Panels','panel_details',420,500,frame(420,500,'url(#dark)',C.paper2));
  await emit('Panels','player_viewport',400,420,`${frame(400,420,'rgba(255,255,255,.06)',C.paper2)}<ellipse cx="200" cy="355" rx="110" ry="28" fill="none" stroke="${C.gold}" stroke-width="3" opacity=".7"/><ellipse cx="200" cy="355" rx="78" ry="16" fill="none" stroke="${C.paper2}" stroke-width="2" opacity=".5"/>`);
  await emit('Panels','gui_profile_background',1080,716,`${frame(1080,716)}<path d="M40 116H1040M40 616H1040" stroke="${C.line}" stroke-width="2"/>`);
  await emit('Panels','gui_techniques_background',1080,716,`${frame(1080,716)}<path d="M40 116H1040M310 140V590M742 140V590M40 616H1040" stroke="${C.line}" stroke-width="2"/>`);

  for(const s of ['normal','hover','pressed','disabled','selected']) await emit('Buttons',`button_${s}`,480,120,buttonBody(480,120,s));
  for(const s of ['normal','hover']) {
    const col=s==='hover'?C.gold:C.paper2;
    await emit('Buttons',`close_${s}`,96,96,`${buttonBody(96,96,s==='hover'?'hover':'normal')}<path d="M31 31l34 34M65 31L31 65" stroke="${col}" stroke-width="7" stroke-linecap="round"/>`);
    await emit('Buttons',`back_${s}`,96,96,`${buttonBody(96,96,s==='hover'?'hover':'normal')}<path d="M64 23L34 48l30 25M36 48h34" stroke="${col}" stroke-width="7" fill="none" stroke-linecap="round" stroke-linejoin="round"/>`);
  }
  for(const s of ['normal','hover','active','disabled']) {
    const state=s==='active'?'selected':s;
    await emit('Tabs',`tab_${s}`,360,104,buttonBody(360,104,state));
  }

  await emit('Qi','qi_icon',96,96,qiIcon());
  await emit('Qi','qi_bar_background',600,56,`<rect x="3" y="3" width="594" height="50" rx="8" fill="${C.ink}" stroke="${C.line}" stroke-width="4"/><rect x="14" y="14" width="572" height="28" rx="4" fill="#34393b"/><path d="M18 10H582" stroke="${C.paper2}" stroke-width="2" opacity=".35"/>`);
  await emit('Qi','qi_bar_fill',572,28,`<rect width="572" height="28" rx="4" fill="url(#blue)"/><path d="M10 6H562" stroke="#d4f9ff" stroke-width="3" opacity=".48"/>`);
  await emit('Qi','qi_bar_fill_gold',572,28,`<rect width="572" height="28" rx="4" fill="${C.gold}"/><path d="M10 6H562" stroke="#fff1bd" stroke-width="3" opacity=".48"/>`);

  for(const s of ['normal','hover','selected','locked','unknown']) {
    let extra=''; if(s==='locked') extra=lockIcon(80,80,1.2,'#9aa0a0'); if(s==='unknown') extra=unknownIcon(80,80,1.2,'#aeb2b0');
    await emit('Techniques',`technique_slot_${s}`,160,160,slotBody(160,s)+extra);
  }
  for(const s of ['normal','selected','locked','unknown']) {
    const state=s==='selected'?'selected':s==='normal'?'normal':'disabled';
    let extra=''; if(s==='locked') extra=lockIcon(62,60,.85,'#909697'); if(s==='unknown') extra=unknownIcon(62,60,.8,'#aeb2b0');
    await emit('Tomes',`tome_slot_${s}`,520,120,buttonBody(520,120,state)+extra);
  }

  await emit('Icons','icon_book_closed',104,104,bookIcon(false));
  await emit('Icons','icon_book_open',104,104,bookIcon(true));
  await emit('Icons','icon_cultivation',104,104,lotusIcon());
  await emit('Icons','icon_scroll',104,104,`<g fill="none" stroke="${C.white}" stroke-width="4"><path d="M28 18h48v67H28zM20 18c0-8 8-12 16-8v8M84 85c0 8-8 12-16 8v-8"/><path d="M39 36h26M39 49h26M39 62h18"/></g>`);
  await emit('Icons','icon_profile',104,104,`<circle cx="52" cy="32" r="16" fill="${C.white}"/><path d="M20 88c2-27 16-39 32-39s30 12 32 39z" fill="${C.white}"/>`);
  await emit('Icons','icon_info',104,104,`<circle cx="52" cy="52" r="37" fill="none" stroke="${C.white}" stroke-width="5"/><circle cx="52" cy="34" r="4" fill="${C.white}"/><path d="M52 47v27" stroke="${C.white}" stroke-width="6" stroke-linecap="round"/>`);
  await emit('Icons','icon_lock',96,96,lockIcon(48,48,1.35));
  await emit('Icons','icon_unknown',96,96,unknownIcon(48,48,1.35));
  await emit('Icons','icon_slash',128,128,`<path d="M19 105C55 86 80 52 107 17 91 70 57 104 19 105z" fill="${C.white}"/><path d="M27 110C62 94 91 61 113 32" fill="none" stroke="${C.gold}" stroke-width="4" opacity=".75"/>`);
  await emit('Icons','icon_crescent_slash',128,128,`<path d="M18 101C79 104 112 62 98 18c34 48 3 102-80 83z" fill="${C.white}"/><path d="M31 90c45-2 68-28 66-57" fill="none" stroke="${C.gold}" stroke-width="4" opacity=".75"/>`);

  await emit('Decorations','decor_separator',640,44,separator(640,22,C.line));
  await emit('Decorations','decor_diamond',48,48,`<path d="M24 5l19 19-19 19L5 24z" fill="none" stroke="${C.line}" stroke-width="3"/>`);
  await emit('Decorations','decor_corner',96,96,corner(12,12,2.4,2.4));
  await emit('Decorations','decor_brush_circle',240,240,`<path d="M178 47C119 10 47 54 42 119c-6 74 79 111 135 68 42-32 38-91 6-121" fill="none" stroke="${C.ink}" stroke-width="17" stroke-linecap="round" opacity=".9"/><path d="M45 158c12 31 42 49 68 52" fill="none" stroke="${C.ink}" stroke-width="7" opacity=".35"/>`);
  await emit('Decorations','decor_bamboo',240,500,`<g fill="none" stroke="${C.ink2}" stroke-linecap="round"><path d="M64 482C89 367 101 241 93 25M67 415c35-27 58-58 77-98M91 285c-29-22-49-47-64-79M96 184c31-23 49-51 61-84" stroke-width="9"/><path d="M86 350c-37-23-63-18-76-10 29 3 49 16 76 10zM116 351c38-7 58 5 68 17-29-9-49-5-68-17zM58 248c-28-25-45-21-56-15 22 5 37 18 56 15zM113 225c31-9 54 0 67 11-27-4-46 2-67-11zM119 146c22-26 43-29 59-26-22 9-37 21-59 26zM87 103c-24-21-42-18-54-12 20 4 34 14 54 12z" fill="${C.ink2}" stroke-width="2"/></g>`);
  await emit('Decorations','decor_mountains',640,220,`<path d="M8 205c69-29 105-95 164-118 25-10 52 13 70 29 35-53 75-92 117-91 45 2 78 63 103 96 27-24 60-35 89-17 31 19 53 66 81 101z" fill="${C.ink2}" opacity=".22"/><path d="M9 205c61-24 109-67 154-89 34-17 57 16 79 27 44-70 79-97 117-104 42 3 69 55 103 99 35-34 71-30 101-5 20 17 39 45 69 72" fill="none" stroke="${C.ink2}" stroke-width="5" opacity=".58"/><path d="M240 143l43-23 34 5 42-86 24 76 47 32M112 148l51-32 22 28" fill="none" stroke="${C.ink2}" stroke-width="3" opacity=".38"/>`);

  // Mockups: presentation text is intentionally confined to these files.
  const close=`<g transform="translate(982 28) scale(.72)">${buttonBody(96,96,'normal')}<path d="M31 31l34 34M65 31L31 65" stroke="${C.paper2}" stroke-width="7" stroke-linecap="round"/></g>`;
  const nav=(x,text,active=false)=>`<g transform="translate(${x} 631)"><path d="M10 0h219l10 10v45l-10 10H10L0 55V10z" fill="${active?C.ink:'#f2ede2'}" stroke="${active?C.gold:C.line}" stroke-width="2"/><path d="M18 8h203M18 57h203" stroke="${active?C.gold:C.line}" stroke-width="1" opacity=".55"/>${sans(text,119.5,41,20,'middle',active?C.pale:C.ink,active?'600':'400')}</g>`;
  const headerOrnaments=`<path d="M72 76H392M688 76H938" stroke="${C.line}" stroke-width="2"/><path d="M404 68l8 8-8 8-8-8zM676 68l8 8-8 8-8-8z" fill="none" stroke="${C.line}" stroke-width="2"/>`;
  const profile=`${frame(1080,716)}<path d="M40 116H1040M40 615H1040" stroke="${C.line}" stroke-width="2"/>${headerOrnaments}${label('MURIMBLOCK',540,82,30,'middle',C.ink,'600')}${close}<g transform="translate(375 136)"><rect x="0" y="0" width="330" height="282" rx="2" fill="rgba(255,255,255,.12)" stroke="${C.line}" stroke-width="3"/><rect x="10" y="10" width="310" height="262" fill="none" stroke="${C.line}" stroke-width="1.5" opacity=".72"/>${corner(10,10,1,1)}${corner(320,10,-1,1)}${corner(10,272,1,-1)}${corner(320,272,-1,-1)}<ellipse cx="165" cy="237" rx="82" ry="15" fill="none" stroke="${C.gold2}" stroke-width="2" opacity=".82"/><path d="M165 82v95M125 177h80" stroke="${C.muted}" stroke-width="1.5" stroke-dasharray="7 8"/>${sans('ZONE JOUEUR 3D',165,150,16,'middle',C.muted,'600')}</g>${label('EnzoBertrand',540,458,27,'middle')}${sans('ROYAUME',540,486,13,'middle',C.muted,'600')}${label('Qi Refining III',540,517,21,'middle')}<path d="M436 531H644" stroke="${C.line}" stroke-width="1" opacity=".45"/><path d="M540 526l5 5-5 5-5-5z" fill="${C.paper}" stroke="${C.line}" stroke-width="1.5"/><g transform="translate(246 536) scale(.56)">${qiIcon()}</g>${sans('QI',310,552,13,'start',C.muted,'700')}<rect x="310" y="561" width="460" height="30" rx="5" fill="${C.ink}" stroke="${C.line}" stroke-width="2"/><rect x="317" y="568" width="446" height="16" rx="3" fill="#34393b"/><rect x="317" y="568" width="323" height="16" rx="3" fill="url(#blue)"/><path d="M324 571H632" stroke="#d5f7ff" stroke-width="2" opacity=".45"/>${sans('725 / 1000',785,582,16,'start',C.ink,'500')}${nav(42,'Profil',true)}${nav(294,'Techniques')}${nav(546,'Cultivation')}${nav(798,'Infos')}`;
  await emit('Mockups','Profile_GUI',1080,716,profile);

  const tome=(y,text,state='normal')=>`<g transform="translate(55 ${y}) scale(.42)">${buttonBody(520,120,state==='selected'?'selected':'normal')}</g>${sans(text,122,y+34,18,'start',state==='locked'?C.muted:C.pale,state==='selected'?'600':'400')}${state==='locked'?`<g transform="translate(88 ${y+24}) scale(.42)">${lockIcon(0,0,.8,C.muted)}</g>`:''}`;
  const skill=(x,y,state,icon)=>`<g transform="translate(${x} ${y}) scale(.62)">${slotBody(160,state)}${icon==='slash'?`<g transform="scale(.8) translate(16 16)"><path d="M19 105C55 86 80 52 107 17 91 70 57 104 19 105z" fill="${C.white}"/></g>`:icon==='lock'?lockIcon(80,80,1.1,'#8b9091'):unknownIcon(80,80,1.1,'#aeb2b0')}</g>`;
  const techniques=`${frame(1080,716)}<path d="M40 116H1040M310 142V594M742 142V594M40 610H1040" stroke="${C.line}" stroke-width="2"/>${label('TECHNIQUES',540,78,31,'middle',C.ink,'600')}${label('Tomes',170,160,25,'middle')}<g transform="translate(55 170)">${separator(230,8)}</g>${tome(205,'Tome Épée I','selected')}${tome(275,'Tome Épée II')}${tome(345,'???','locked')}${tome(415,'???','locked')}${label('Techniques',526,160,25,'middle')}<g transform="translate(350 170)">${separator(350,8)}</g>${skill(355,220,'selected','slash')}${skill(480,220,'normal','slash')}${skill(605,220,'locked','lock')}${skill(418,355,'normal','slash')}${skill(543,355,'unknown','unknown')}${label('Détails',908,160,25,'middle')}<g transform="translate(778 170)">${separator(260,8)}</g>${label('Slash frontal',776,228,25)}${sans('Un arc de lame rapide et précis.',776,270,17)}${sans('Coût en Qi',776,330,15,'start',C.muted,'600')}${sans('25 Qi',1015,330,18,'end',C.ink,'600')}${sans('Cooldown',776,374,15,'start',C.muted,'600')}${sans('4 sec',1015,374,18,'end',C.ink,'600')}<g transform="translate(792 460) scale(.52)">${buttonBody(480,120,'selected')}</g>${sans('ÉQUIPER',917,502,18,'middle',C.pale,'600')}${nav(30,'Profil')}${nav(293,'Techniques',true)}${nav(556,'Cultivation')}${nav(819,'Infos')}${close}`;
  await emit('Mockups','Techniques_GUI',1080,716,techniques);

  const readme = [
    '# Murimblock GUI Kit', '',
    'Kit graphique autonome pour les futures interfaces de Murimblock. Aucun fichier du mod, code Java, comportement de gameplay ou système de Qi n’est inclus.', '',
    '## Direction visuelle', '',
    'Interface sobre inspirée du Murim / Wuxia : encre noire, blanc cassé, gris, accents Qi bleus et sélection dorée. Les éléments sont reconstruits proprement à partir des références artistiques, sans découpe directe.', '',
    '## Échelle et dimensions', '',
    '- Base Minecraft visée : **270 × 179 px**.',
    '- Mockups fournis : **1080 × 716 px** (×4).',
    '- Assets individuels : dessinés en SVG avec dimensions PNG ×4, pour permettre un downscale contrôlé.',
    '- Pour une version 1×, réduire exactement à 25 % avec un filtre nearest-neighbor pour un rendu plus pixelisé, ou Lanczos pour un rendu lissé.', '',
    '## Dossiers', '',
    '- Mockups/ : vues complètes Profil et Techniques avec texte de démonstration.',
    '- Panels/ : fonds et panneaux réutilisables, sans texte.',
    '- Buttons/ : boutons génériques, fermeture et retour.',
    '- Tabs/ : fonds d’onglets sans icône ni texte.',
    '- Qi/ : icône, fond de barre et remplissages séparés.',
    '- Techniques/ : slots de techniques.',
    '- Tomes/ : lignes / cartes de tomes.',
    '- Icons/ : pictogrammes autonomes.',
    '- Decorations/ : séparateurs, bambou, montagnes, cercle de pinceau et ornements.',
    '- SVG/ : sources vectorielles de tous les PNG.', '',
    '## États', '',
    'Boutons : normal, hover, pressed, disabled, selected. Onglets : normal, hover, active, disabled. Techniques : normal, hover, selected, locked, unknown. Tomes : normal, selected, locked, unknown.', '',
    '## Superposition conseillée', '',
    '- Barre de Qi : qi_bar_background.png → remplissage recadré horizontalement → qi_icon.png → texte dynamique du jeu.',
    '- Slot technique : état du slot → icône de technique → cadenas ou point d’interrogation si nécessaire.',
    '- Tome : état du tome → icône de livre → libellé dynamique.',
    '- Onglet : fond d’état → icône → libellé dynamique.', '',
    '## Règles d’intégration future', '',
    'Les PNG individuels ne contiennent pas de texte. Conserver le ratio des assets, éviter les redimensionnements fractionnaires et ne pas fusionner le remplissage de Qi avec son fond. Les mockups servent de référence de composition uniquement.', ''
  ].join('\n');
  fs.writeFileSync(path.join(ROOT,'README.md'),readme);

  // Exact 25% raster exports for the intended 270 × 179 Minecraft base scale.
  const oneXRoot=path.join(ROOT,'Minecraft_1x');
  for(const folder of dirs.filter(d=>d!=='SVG')) {
    const srcDir=path.join(ROOT,folder);
    const dstDir=path.join(oneXRoot,folder);
    fs.mkdirSync(dstDir,{recursive:true});
    for(const file of fs.readdirSync(srcDir).filter(f=>f.endsWith('.png'))) {
      const input=path.join(srcDir,file);
      const meta=await sharp(input).metadata();
      await sharp(input).resize(Math.max(1,Math.round(meta.width/4)),Math.max(1,Math.round(meta.height/4)),{kernel:'lanczos3'}).png().toFile(path.join(dstDir,file));
    }
  }
}

main().catch(e=>{console.error(e);process.exit(1)});
