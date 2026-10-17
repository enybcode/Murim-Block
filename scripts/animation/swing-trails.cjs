// The swing trails of the Murim attack animations (Enzo, 16/10/2026: trails on every weapon). Epic Fight draws a
// weapon's trail from its item skin (assets/murimblock/item_skins/<item>.json: colour, length along the blade) during
// the windows an animation names in its data file (animmodels/animations/<path>/data/<name>.json, "trail_effects"),
// as its own sword_auto1 does. This writes those files for our attacks: one window per phase, from just before the
// swing starts to just after the blow lands, on the hand that strikes.
// The phases come from the dump of the GameTest AnimationPhaseDump (gradlew -PgameTests runGameTestServer):
//   node scripts/animation/swing-trails.cjs build/game-test-run/murim-attack-phases.json
// Bare-handed arts (no blade to trail), the thrown needles and the Crab King's claws get none.
const fs = require('node:fs'), path = require('node:path');
const phases = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const root = path.join(__dirname, '../../src/main/resources/assets/murimblock/animmodels/animations');
const SKIP = /\/(inch_force|rapid_fists|sanguine_|tiger_step|whirlwind_|needles_|crab_king_)/;
const round = (t) => Math.round(t * 1000) / 1000;
let written = 0;
for (const [id, list] of Object.entries(phases)) {
  if (SKIP.test('/' + id.split('/').pop())) continue;
  const trail_effects = list.map((p) => ({
    start_time: round(Math.max(p.start, p.antic - 0.04)),
    end_time: round(p.contact + 0.04),
    joint: p.hand === 'OFF_HAND' ? 'Tool_L' : 'Tool_R',
    item_skin_hand: p.hand === 'OFF_HAND' ? 'off_hand' : 'main_hand',
  }));
  const dir = path.join(root, path.dirname(id), 'data');
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, path.basename(id) + '.json'), JSON.stringify({ trail_effects }, null, 2) + '\n');
  written++;
}
console.log('trail windows for', written, 'attack animations');
