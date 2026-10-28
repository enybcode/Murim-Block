// The zombie family's looks: the Disciple déchu (the zombie) and its variants, which keep its skeleton and clips and
// only change the look. Figures and texts for sheet.js (see its header for the shape format).
const iron = '#8e98a2', bg = '#f8f2e6';

const FIG = {};

FIG.disciple = {
  front: [
    [40, 0, 16, 10, '#2b2b2b'], [44, -6, 8, 8, '#2b2b2b'], [32, 8, 64, 64, '#8a9a86'], [32, 8, 64, 14, '#2b2b2b'],
    [42, 40, 10, 7, '#7fffd4'], [76, 40, 10, 7, '#7fffd4'], [54, 58, 20, 5, '#4a5446'],
    [32, 72, 64, 96, '#d8d2c2'], {d: 'M32 72 L64 112 L96 72 Z', fill: '#a8a294'}, [40, 88, 12, 22, '#7a2020'],
    [32, 146, 64, 9, '#5b3a29'], [58, 146, 8, 26, '#5b3a29'],
    {d: 'M32 168 L40 160 L48 168 L56 160 L64 168 L72 160 L80 168 L88 160 L96 168 Z', fill: '#d8d2c2'},
    [0, 72, 32, 96, '#d8d2c2'], [96, 72, 32, 96, '#d8d2c2'], [0, 136, 32, 10, '#efe9da'], [96, 128, 32, 10, '#efe9da'],
    [0, 168, 32, 16, '#8a9a86'], [96, 168, 32, 16, '#8a9a86'],
    [32, 168, 31, 88, '#cfc8b6'], [65, 168, 31, 88, '#cfc8b6'], [32, 232, 31, 10, '#efe9da'],
    [32, 256, 31, 14, '#3b2a20'], [65, 256, 31, 14, '#3b2a20'],
  ],
};

// Husk: a desert sect's disciple, dried by the sun.
{
  const skin = '#a88c62', skinD = '#8a7048', robe = '#c9a25a', robeD = '#a5823f', wrap = '#ddcc9e', wrapD = '#b8a57a',
    straw = '#d2ae66', strawD = '#9a7838', trou = '#a07a42', eye = '#ffc94a', rope = '#6b4a2a', hair = '#3a2e22',
    sand = '#d8b878', gourd = '#b88a4a';
  FIG.sables = {
    front: [
      [32, 168, 31, 88, trou], [65, 168, 31, 88, trou], [88, 168, 8, 88, '#8a6838'],
      [32, 222, 31, 6, wrap], [32, 234, 31, 6, wrap], [65, 228, 31, 6, wrap], [65, 240, 31, 6, wrap],
      [32, 256, 31, 14, straw], [65, 256, 31, 14, straw], [44, 256, 6, 14, strawD], [77, 256, 6, 14, strawD],
      [32, 72, 64, 96, robe], [86, 72, 10, 96, robeD], {d: 'M32 72 L64 108 L96 72 L84 72 L64 96 L44 72 Z', fill: robeD},
      [32, 144, 64, 9, rope], [52, 144, 10, 9, '#4a321c'], [56, 153, 5, 22, rope],
      [84, 148, 14, 22, gourd], [88, 142, 6, 7, strawD], [90, 154, 2, 10, '#6b4a28'],
      {d: 'M32 168 L40 178 L48 168 L56 180 L64 168 L72 178 L80 168 L88 180 L96 168 Z', fill: robe},
      [0, 72, 32, 48, robe], [96, 72, 32, 48, robe], [0, 116, 32, 4, robeD], [96, 116, 32, 4, robeD],
      [0, 120, 32, 48, wrap], [96, 120, 32, 48, wrap],
      [0, 128, 32, 3, wrapD], [0, 142, 32, 3, wrapD], [0, 156, 32, 3, wrapD], [96, 134, 32, 3, wrapD], [96, 150, 32, 3, wrapD],
      [0, 168, 32, 16, skin], [96, 168, 32, 16, skin], [0, 180, 32, 4, skinD], [96, 180, 32, 4, skinD],
      [32, 20, 64, 52, skin], [32, 20, 64, 6, skinD], [32, 22, 8, 26, hair], [88, 22, 8, 22, hair],
      [42, 38, 10, 6, eye], [76, 38, 10, 6, eye], [40, 46, 6, 6, skinD], [82, 46, 6, 6, skinD],
      [32, 52, 64, 20, wrap], [32, 60, 64, 2, wrapD], [32, 66, 64, 2, wrapD], [90, 52, 8, 30, wrap],
      {d: 'M6 22 L122 22 L104 12 L82 -6 L46 -6 L24 12 Z', fill: straw}, {d: 'M40 2 L88 2 L92 8 L36 8 Z', fill: strawD},
      [6, 20, 116, 2, strawD], {d: 'M92 22 L104 22 L97 9 Z', fill: bg},
      [8, 192, 4, 4, sand], [16, 206, 3, 3, sand], [112, 196, 4, 4, sand], [104, 214, 3, 3, sand], [100, 232, 3, 3, sand],
    ],
    side: [
      [16, 168, 32, 88, trou], [16, 226, 32, 6, wrap], [16, 238, 32, 6, wrap], [16, 256, 40, 14, straw],
      [16, 72, 32, 96, robe], [16, 72, 6, 96, robeD], [16, 144, 32, 9, rope], [4, 148, 12, 22, gourd], [7, 142, 6, 7, strawD],
      {d: 'M16 168 L24 178 L32 168 L40 180 L48 168 Z', fill: robe},
      {t: 'rotate(-38 32 80)', items: [[16, 72, 32, 48, robe], [16, 120, 32, 48, wrap], [16, 130, 32, 3, wrapD],
        [16, 146, 32, 3, wrapD], [16, 168, 32, 16, skin]]},
      [0, 20, 64, 52, skin], [0, 22, 12, 28, hair], [50, 38, 10, 6, eye], [36, 52, 28, 20, wrap], [36, 60, 28, 2, wrapD],
      {d: 'M-16 22 L80 22 L62 10 L44 -6 L18 -6 L2 10 Z', fill: straw}, {d: 'M14 2 L50 2 L54 8 L10 8 Z', fill: strawD},
      [60, 190, 3, 3, sand], [68, 206, 4, 4, sand],
    ],
    palette: [[skin, 'Peau'], [robe, 'Robe'], [wrap, 'Bandes'], [straw, 'Chapeau'], [trou, 'Pantalon'], [rope, 'Corde'],
      [eye, 'Yeux'], [sand, 'Sable']],
  };
}

// Drowned: a disciple who drowned in a lake during a trial.
{
  const skin = '#6f9a96', skinD = '#557c79', robe = '#3f5f7e', robeD = '#2f4a63', wet = '#34506b', hair = '#1f2e2b',
    kelp = '#4f7a3a', kelpD = '#3a5e2a', eye = '#7fe8ff', shell = '#d8d0c0', drop = '#8fd8f0', haft = '#7a5a3a';
  FIG.noye = {
    front: [
      [32, 168, 31, 88, robeD], [65, 168, 31, 82, robeD], [32, 200, 31, 4, '#26394d'], [65, 214, 31, 4, '#26394d'],
      [32, 256, 31, 14, '#2a2a30'], [65, 250, 31, 20, skin], [65, 266, 31, 4, skinD],
      [32, 72, 64, 96, robe], [32, 118, 64, 50, wet], [86, 72, 10, 96, robeD],
      {d: 'M32 72 L64 110 L96 72 L84 72 L64 98 L44 72 Z', fill: robeD},
      [32, 144, 64, 8, kelp], [40, 152, 6, 18, kelp], [70, 152, 5, 24, kelpD],
      {d: 'M32 168 L38 180 L46 170 L54 184 L62 170 L72 182 L80 168 L90 178 L96 168 Z', fill: wet},
      [0, 72, 32, 96, robe], [96, 72, 32, 96, robe], [0, 118, 32, 50, wet], [96, 118, 32, 50, wet],
      [0, 160, 32, 24, skin], [96, 168, 32, 16, skin],
      [6, 98, 7, 7, shell], [18, 110, 5, 5, '#b8b0a0'], [100, 124, 6, 6, shell],
      [80, 68, 10, 36, kelp], [88, 96, 6, 28, kelpD], [96, 68, 10, 20, kelp],
      [32, 8, 64, 64, skin], [76, 50, 8, 6, '#82aca8'], [32, 8, 64, 10, hair], [32, 18, 10, 48, hair], [86, 18, 10, 56, hair],
      [46, 18, 10, 32, hair], [70, 18, 8, 14, hair],
      [56, 42, 4, 6, eye], [74, 40, 10, 7, eye], [58, 58, 16, 6, '#2f4544'],
      [20, 192, 3, 6, drop], [42, 186, 3, 6, drop], [90, 192, 3, 6, drop], [108, 204, 3, 5, drop], [60, 196, 3, 5, drop],
    ],
    side: [
      [16, 168, 32, 82, robeD], [16, 250, 32, 20, skin],
      [16, 72, 32, 96, robe], [16, 118, 32, 50, wet], [16, 144, 32, 8, kelp], [8, 152, 6, 20, kelp],
      {d: 'M16 168 L22 180 L30 170 L38 182 L48 168 Z', fill: wet},
      {t: 'rotate(-38 32 80)', items: [[16, 72, 32, 96, robe], [16, 118, 32, 50, wet], [16, 160, 32, 24, skin]]},
      [90, 44, 5, 224, haft], [80, 40, 25, 5, iron], [80, 20, 5, 24, iron], [90, 12, 5, 30, iron], [100, 20, 5, 24, iron],
      [24, 66, 10, 32, kelp],
      [0, 8, 64, 64, skin], [0, 8, 64, 10, hair], [0, 18, 14, 60, hair], [40, 18, 10, 30, hair], [54, 40, 8, 7, eye],
      [54, 58, 10, 5, '#2f4544'],
      [70, 210, 3, 6, drop],
    ],
    palette: [[skin, 'Peau'], [robe, 'Robe'], [wet, 'Trempée'], [hair, 'Cheveux'], [kelp, 'Algues'], [shell, 'Coquille'],
      [eye, 'Yeux'], [haft, 'Fourche']],
  };
}

// Zombie villager: a peasant who never learned to fight, with a sealing talisman.
{
  const skin = '#8fa27c', skinD = '#728560', tunic = '#9c8a6a', tunicD = '#7f6e52', apron = '#6a8a4a', apronD = '#55703a',
    trou = '#6e6252', roll = '#8a7c68', band = '#5a4a3a', paper = '#e8cf55', ink = '#b32020', eye = '#b8d8c8',
    straw = '#c8a868', nose = '#7f9270', cuff = '#b8a888';
  FIG.paysan = {
    front: [
      [32, 168, 31, 58, trou], [65, 168, 31, 58, trou], [32, 222, 31, 8, roll], [65, 222, 31, 8, roll],
      [32, 230, 31, 26, skin], [65, 230, 31, 26, skin], [88, 230, 8, 26, skinD],
      [32, 256, 31, 14, straw], [65, 256, 31, 14, straw],
      [32, 72, 64, 96, tunic], [86, 72, 10, 96, tunicD], {d: 'M32 72 L72 120 L80 120 L44 72 Z', fill: tunicD},
      [42, 92, 14, 12, '#b3a07c'], [42, 92, 14, 2, band], [32, 100, 64, 4, '#4a3a2a'],
      [40, 104, 48, 62, apron], [80, 104, 8, 62, apronD], [52, 130, 24, 14, apronD],
      [0, 72, 32, 64, tunic], [96, 72, 32, 64, tunic], [0, 132, 32, 8, cuff], [96, 132, 32, 8, cuff],
      [0, 140, 32, 44, skin], [96, 140, 32, 44, skin], [0, 176, 32, 8, skinD], [120, 140, 8, 44, skinD],
      [32, 8, 64, 64, skin], [32, 8, 64, 8, '#2b2b2b'], [32, 16, 64, 8, band], [96, 16, 10, 6, band], [100, 22, 6, 12, band],
      [40, 40, 12, 2, skinD], [76, 40, 12, 2, skinD], [40, 42, 12, 6, eye], [76, 42, 12, 6, eye],
      [58, 44, 12, 20, nose], [58, 62, 12, 2, skinD], [52, 66, 24, 4, '#4a5446'],
      [54, 20, 20, 30, paper], [62, 24, 4, 22, ink], [57, 28, 14, 3, ink], [57, 38, 14, 3, ink],
    ],
    side: [
      [16, 168, 32, 58, trou], [16, 222, 32, 8, roll], [16, 230, 32, 26, skin], [16, 256, 40, 14, straw],
      [16, 72, 32, 96, tunic], [16, 72, 6, 96, tunicD], [44, 104, 8, 62, apron], [16, 100, 36, 4, '#4a3a2a'],
      {t: 'rotate(-38 32 80)', items: [[16, 72, 32, 64, tunic], [16, 132, 32, 8, cuff], [16, 140, 32, 44, skin]]},
      [0, 8, 64, 64, skin], [0, 8, 64, 8, '#2b2b2b'], [0, 16, 64, 8, band], [-10, 16, 10, 6, band], [-8, 22, 6, 12, band],
      [50, 42, 10, 6, eye], [64, 40, 10, 22, nose], [50, 64, 12, 4, '#4a5446'],
      {t: 'rotate(-14 64 20)', items: [[64, 20, 5, 32, paper], [65, 24, 3, 22, ink]]},
    ],
    palette: [[skin, 'Peau'], [tunic, 'Tunique'], [apron, 'Tablier*'], [trou, 'Pantalon'], [band, 'Bandeau'], [paper, 'Talisman'],
      [ink, 'Encre'], [straw, 'Sandales']],
  };
}

// Zombified piglin: a boar-clan warrior of the demonic cult, fallen in the Nether.
{
  const skin = '#c98a86', skinD = '#a86c68', rot = '#8fae7a', bone = '#e8e0cc', snout = '#e0a49e', lac = '#4a1f22',
    lacD = '#2f1215', gold = '#d6a83a', red = '#a32020', eye = '#ff6a3a', dark = '#3a181a', boot = '#2a1414',
    blade = '#e8c25a', edge = '#fff0a8', guard = '#a8822a', nost = '#6a3a38', mohawk = '#2a1a1a';
  FIG.sanglier = {
    front: [
      [32, 168, 31, 88, dark], [65, 168, 31, 88, dark], [34, 198, 12, 10, rot],
      [32, 224, 31, 32, lac], [65, 224, 31, 32, lac], [32, 224, 31, 3, gold], [65, 224, 31, 3, gold],
      [32, 256, 31, 14, boot], [65, 256, 31, 14, boot],
      [32, 148, 64, 22, lacD], [34, 150, 14, 20, lac], [50, 150, 14, 20, lac], [66, 150, 14, 20, lac], [82, 150, 14, 20, lac],
      [32, 72, 64, 76, lac], [32, 72, 64, 4, gold], [32, 92, 64, 2, lacD], [32, 108, 64, 2, lacD], [32, 124, 64, 2, lacD],
      [88, 72, 8, 76, lacD], [34, 90, 26, 30, rot], [36, 96, 22, 3, bone], [36, 104, 22, 3, bone], [36, 112, 22, 3, bone],
      [32, 140, 64, 10, red], [72, 150, 10, 26, red],
      [0, 72, 32, 20, lac], [0, 90, 32, 3, gold], [0, 93, 32, 47, skin], [8, 104, 14, 12, rot], [0, 140, 32, 22, dark],
      [0, 162, 32, 22, skin],
      [96, 72, 32, 20, lac], [96, 90, 32, 3, gold], [96, 93, 32, 47, skin], [96, 140, 32, 22, dark], [96, 162, 32, 22, skin],
      [98, 178, 28, 6, guard], {d: 'M104 184 L118 184 L122 250 L112 266 L104 254 Z', fill: blade},
      {d: 'M118 184 L122 250 L116 260 L114 186 Z', fill: edge},
      [26, 8, 76, 64, skin], [90, 8, 12, 64, skinD], [26, 18, 18, 24, rot], [30, 46, 8, 10, bone],
      [12, 12, 16, 14, skinD], [14, 26, 8, 8, skinD], [100, 10, 12, 8, skinD], [52, -2, 24, 12, mohawk],
      [36, 28, 16, 4, skinD], [76, 28, 16, 4, skinD], [38, 32, 12, 7, eye], [78, 32, 12, 7, eye],
      [46, 44, 36, 22, snout], [52, 52, 6, 6, nost], [70, 52, 6, 6, nost], [60, 64, 8, 6, gold],
      [42, 58, 6, 14, bone], [80, 58, 6, 14, bone],
    ],
    side: [
      [16, 168, 32, 88, dark], [16, 224, 32, 32, lac], [16, 224, 32, 3, gold], [16, 256, 40, 14, boot],
      [16, 148, 32, 22, lacD], [16, 72, 32, 76, lac], [16, 92, 32, 2, lacD], [16, 108, 32, 2, lacD], [16, 124, 32, 2, lacD],
      [16, 140, 32, 10, red], [4, 146, 12, 30, red],
      {t: 'rotate(-38 32 80)', items: [[16, 72, 32, 20, lac], [16, 90, 32, 3, gold], [16, 93, 32, 47, skin],
        [16, 140, 32, 22, dark], [16, 162, 32, 22, skin], [12, 180, 40, 6, guard],
        {d: 'M24 186 L38 186 L41 226 L32 240 L24 230 Z', fill: blade}]},
      [0, 8, 64, 64, skin], [0, 40, 16, 20, rot], [-10, 12, 14, 16, skinD], [10, -2, 34, 12, mohawk],
      [44, 28, 16, 4, skinD], [46, 32, 12, 7, eye], [64, 44, 18, 22, snout], [76, 50, 6, 6, nost], [64, 58, 6, 14, bone],
      [80, 62, 4, 8, gold],
    ],
    palette: [[skin, 'Peau'], [rot, 'Pourri'], [bone, 'Os'], [lac, 'Laque'], [gold, 'Or'], [red, 'Écharpe'],
      [eye, 'Yeux'], [dark, 'Dessous']],
  };
}

const T = {
  g: '<span class="tag ok">Garde</span>', pf: '<span class="tag ok">Parade facile</span>',
  p: '<span class="tag warn">Parade</span>', e: '<span class="tag ok">Esquive</span>',
  gw: '<span class="tag warn">Garde</span>', clip: '<span class="tag new">clip à faire</span>',
};
const BASE_ANIMS = 'Tous les clips du Disciple déchu, sans changement : idle, walk, run, stance, réactions, death';
const FOOT = 'Squelette du Disciple · hitbox vanilla · yeux émissifs';

const SHEETS = {
  sables: {
    ...FIG.sables, title: 'DISCIPLE DES SABLES', seal: '黃沙<br>弟子', height: '1,95 bloc (32 px)',
    sub: 'Remplace le husk · <code>minecraft:husk</code> (même identifiant, même apparition, mêmes drops) · reprend les animations du Disciple déchu',
    parts: ['Chapeau fendu', 'Yeux d\'ambre', 'Voile (tissu)', 'Robe ocre', 'Bras en bandes', 'Gourde fendue', 'Sandales', 'Sable qui coule'],
    footnote: FOOT,
    lore: 'Disciple d\'une secte du désert, mort de soif en traversant les sables. Le soleil a séché sa chair : il ne brûle plus le jour. Son Qi mort a gardé la soif, et chaque coup qu\'il porte vide l\'eau du corps de sa proie.',
    stats: [['Vie', '20 ♥', 40], ['Dégâts (coup simple)', '3 + Soif', 30], ['Vitesse', '0,23', 46],
      ['Résistance au vacillement', 'Faible', 20], ['Rythme d\'attaque', '≈ 2 s', 35]],
    statsNote: 'Base vanilla, à régler en jeu. Soif = l\'effet Faim du husk vanilla.',
    behavior: ['Même combat que le Disciple déchu : garde à <b>3 blocs</b>, double coup, ruée',
      'Ne brûle pas au soleil (vanilla)', 'Chaque coup qui touche donne <b>Soif</b> (Faim vanilla)',
      'Ses coups et sa mort soulèvent du <b>sable</b> au lieu du Qi'],
    attacks: [['Coup maladroit', 'Poing large, + Soif', '2 blocs', '3', T.g + T.pf],
      ['Double coup', 'Le 2ᵉ est plus rapide', '2 blocs', '2 + 3', T.g + T.p],
      ['Ruée', 'Plonge, bras en avant', '4 blocs', '4', T.e + T.gw]],
    anims: [['clips', BASE_ANIMS], ['effets', 'Filet de sable des manches en marchant, nuage de sable à chaque coup'],
      ['death', 'Même clip ; le corps s\'effondre en sable au lieu de laisser le Qi s\'échapper']],
    animNote: '<b>Aucun nouveau clip.</b> Le chapeau, le voile et les bandes sont du modèle ; les bouts du voile bougent comme le tissu de la corde.',
  },
  noye: {
    ...FIG.noye, title: 'DISCIPLE NOYÉ', seal: '溺水<br>弟子', height: '1,95 bloc (32 px)', sideLabel: 'PROFIL (avec fourche)',
    sub: 'Remplace le noyé · <code>minecraft:drowned</code> (même identifiant, même apparition, mêmes drops) · reprend les animations du Disciple déchu',
    parts: ['Mèches défaites', 'Yeux cyan', 'Algues (tissu)', 'Robe détrempée', 'Coquillages', 'Ceinture d\'algues', 'Un pied nu', 'Gouttes'],
    footnote: FOOT,
    lore: 'Disciple tombé dans un lac glacé pendant une épreuve. Le Qi de l\'eau froide l\'a gardé : la nuit, il remonte des rivières et des lacs. Certains serrent encore la fourche de pêcheur avec laquelle ils ont coulé.',
    stats: [['Vie', '20 ♥', 40], ['Dégâts (coup simple)', '3', 30], ['Fourche lancée', '8', 60],
      ['Vitesse', '0,23', 46], ['Résistance au vacillement', 'Faible', 20]],
    statsNote: 'Base vanilla, plus rapide dans l\'eau, à régler en jeu.',
    behavior: ['Hors de l\'eau : même combat que le Disciple déchu',
      'Avec une fourche : les coups de bâton déjà validés (balayage, deux estocs, charge)',
      'Lance sa fourche, nage et attaque sous l\'eau (vanilla)',
      'Laisse tomber des <b>gouttes</b> ; ses coups éclaboussent'],
    attacks: [['Coup maladroit', 'Poing large et lent', '2 blocs', '3', T.g + T.pf],
      ['Double coup', 'Le 2ᵉ est plus rapide', '2 blocs', '2 + 3', T.g + T.p],
      ['Ruée', 'Plonge, bras en avant', '4 blocs', '4', T.e + T.gw],
      ['Lancer de fourche', 'Seulement s\'il en porte une', 'distance', '8', T.e + T.clip]],
    anims: [['clips', BASE_ANIMS], ['armé', 'Fourche : clips « outil tenu comme un bâton », déjà validés'],
      ['à faire', '<b>lancer de fourche</b> et <b>nage</b> : deux clips, storyboard avant']],
    animNote: 'Le reste vient du Disciple déchu. Les algues et les mèches bougent comme du tissu.',
  },
  paysan: {
    ...FIG.paysan, title: 'PAYSAN POSSÉDÉ', seal: '附身<br>農夫', height: '1,95 bloc (32 px)',
    sub: 'Remplace le zombie villageois · <code>minecraft:zombie_villager</code> (même identifiant, même apparition, mêmes drops) · reprend les animations du Disciple déchu',
    parts: ['Talisman (papier)', 'Bandeau noué', 'Grand nez', 'Yeux ternes', 'Tunique rapiécée', 'Tablier du métier*', 'Pantalon retroussé', 'Sandales'],
    footnote: '* couleur du tablier selon le métier (vanilla)',
    lore: 'Villageois sans technique, tué par les morts qui rôdent la nuit. Un moine de passage lui a collé un talisman pour sceller son corps, mais le sceau est trop faible. Un médecin peut encore le ramener : le talisman brille, puis tombe.',
    stats: [['Vie', '20 ♥', 40], ['Dégâts (coup simple)', '3', 30], ['Vitesse', '0,23', 46],
      ['Résistance au vacillement', 'Faible', 20], ['Rythme d\'attaque', '≈ 2,5 s', 30]],
    statsNote: 'Base vanilla, à régler en jeu.',
    behavior: ['Même approche et même garde que le Disciple déchu',
      'Proposition : <b>pas de double coup</b>, il n\'a jamais appris à se battre',
      'Se soigne comme en vanilla (Faiblesse + pomme d\'or)',
      'Soigné : le talisman brille, puis tombe'],
    attacks: [['Coup maladroit', 'Poing large et lent', '2 blocs', '3', T.g + T.pf],
      ['Ruée', 'Plonge, bras en avant', '4 blocs', '4', T.e + T.gw]],
    anims: [['clips', BASE_ANIMS], ['talisman', 'Le papier bat comme du tissu à chaque pas et à chaque coup'],
      ['soins', 'Effet seulement : le talisman brille, le corps tremble (vanilla), puis il tombe']],
    animNote: '<b>Aucun nouveau clip.</b> Le grand nez se lit de loin, comme le villageois vanilla.',
  },
  sanglier: {
    ...FIG.sanglier, title: 'SANGLIER DU CULTE', seal: '魔豬<br>武士', height: '1,95 bloc (32 px)', sideLabel: 'PROFIL (en garde)',
    sub: 'Remplace le piglin zombifié · <code>minecraft:zombified_piglin</code> (même identifiant, même apparition, mêmes drops) · reprend les animations du Disciple déchu',
    parts: ['Groin et défenses', 'Oreille déchirée', 'Yeux de braise', 'Côtes à nu', 'Armure laquée', 'Écharpe (tissu)', 'Sabre d\'or (dao)', 'Crête noire'],
    footnote: FOOT + ' · tête élargie',
    lore: 'Guerrier du clan du Sanglier, au service du culte démoniaque. Tombé dans les Enfers, il en est revenu pourri, mais fidèle à sa meute : qui en frappe un réveille tous ceux qui l\'entourent.',
    stats: [['Vie', '20 ♥', 40], ['Dégâts (sabre)', '8', 65], ['Vitesse', '0,23', 46],
      ['Résistance au vacillement', 'Moyenne', 45], ['Rythme d\'attaque', '≈ 2 s', 35]],
    statsNote: 'Base vanilla (normal), à régler en jeu.',
    behavior: ['<b>Neutre</b> : il ne frappe que si on l\'attaque (vanilla)',
      'Frappé, il appelle toute la meute autour (vanilla)',
      'Toujours armé : les coups au sabre déjà validés pour le Disciple',
      'Armure laquée : vacille moins ; braises aux coups'],
    attacks: [['Coupe', 'Sabre en diagonale', '2,5 blocs', '8', T.g + T.p],
      ['Coupe et revers', 'Deux coups liés', '2,5 blocs', '4 + 8', T.g + T.p],
      ['Estoc en avant', 'Se fend vers l\'avant', '4,5 blocs', '8', T.e + T.gw]],
    anims: [['clips', BASE_ANIMS], ['armé', 'Clips à l\'épée du Disciple, déjà validés (coupe, revers, estoc)'],
      ['effets', 'Braises aux yeux, étincelles dorées sur le sabre']],
    animNote: '<b>Aucun nouveau clip.</b> À vérifier : la tête plus large ne doit pas gêner la garde.',
  },
};

const LINEUP = {
  title: 'LA FAMILLE DU DISCIPLE DÉCHU', seal: '屍<br>族',
  sub: 'Toutes les variantes de zombie gardent le squelette et les animations du Disciple déchu ; seul le look change · fiches de conception à valider',
  mobs: [
    {front: FIG.disciple.front, name: 'Disciple déchu', id: 'zombie', base: true, line: 'La base, déjà en jeu (validé).'},
    {front: FIG.sables.front, name: 'Disciple des sables', id: 'husk', line: 'Desséché par le désert ; ses coups donnent Soif.'},
    {front: FIG.noye.front, name: 'Disciple noyé', id: 'drowned', line: 'Remonté du lac, fourche de pêcheur.'},
    {front: FIG.paysan.front, name: 'Paysan possédé', id: 'zombie_villager', line: 'Sans technique, scellé d\'un talisman ; soignable.'},
    {front: FIG.sanglier.front, name: 'Sanglier du culte', id: 'zombified_piglin', line: 'Guerrier démoniaque, sabre d\'or ; neutre, en meute.'},
  ],
  note: 'Les bébés de chaque variante gardent la règle déjà en jeu : moitié de taille, grosse tête d\'enfant. Hors de la famille, car ce ne sont pas des bipèdes : zombie cheval et zoglin, à faire avec les quadrupèdes.',
};
