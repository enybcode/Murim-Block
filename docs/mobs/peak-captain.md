# Captain of the Peak Guard

`murimblock:peak_captain` (`PeakCaptainEntity`, `PeakCaptainPatch`, `client/PeakCaptainRenderer`). The Orthodox
Alliance's spear boss, from the concept sheet Enzo chose on 15 October 2026 (the "guard" proposal).

## Model: three separate Blockbench projects

As for every Murim model, the body is cloth only; armour and weapon are items of their own that drop.

| What | Script | Blockbench | In game |
|---|---|---|---|
| Body (cloth) | `scripts/animation/peak-captain-3d.cjs` | `art/blockbench/peak_captain.bbmodel` | `animmodels/entity/peak_captain.json`, `textures/entity/peak_captain/` (10 faces) |
| Armour (4 pieces) | `scripts/animation/peak-guard-armor.cjs` | `art/blockbench/peak_guard_armor.bbmodel` | `animmodels/armor/peak_guard_*.json`, flat layers `peak_guard_layer_1/2` |
| Spear | `scripts/animation/crimson-dragon-spear-model.cjs` | `art/blockbench/crimson_dragon_spear.bbmodel` | `models/item/crimson_dragon_spear(_3d).json` |

- Body: a long azure coat over a white under-robe, gold lapels, cuffs and long panels hemmed with clouds, a belt with a
  gold beast-head buckle, a jade pendant, a white cape with the Alliance's emblem, and a topknot. HD face: scar, long
  moustache, goatee.
- Armour (`MurimArmorMaterials.PEAK_GUARD`, 3/8/6/3 = 20, toughness 2 a piece, durability ×30, mended with iron):
  - **Peak Guard Helm**: plumed iron helm, cheek guards, neck curtain, pheasant feathers;
  - **Peak Guard Lamellar**: gold-laced cuirass, mirror plate, lion pauldrons, gauntlets;
  - **Peak Guard Tassets**: tassets, apron, knee guards;
  - **Peak Guard Greaves**.
- **Crimson Dragon Spear**: gold-edged leaf blade, dragon-head socket, gem cross bar, red mane, red lacquered shaft.
  Epic Fight spear, 6 attack damage (the soldiers' Azure Peak Spear has 5).

## Cloth physics

The cape, the sash tail and the pendant's tassel are left out of the rigid mesh. The script describes them in
`assets/murimblock/cloth/peak_captain.json`, and `client/ClothLayer` draws them. `ClothLayer` is a custom Epic Fight
layer, reusable on any mob.

- Each strip hangs from its joint.
- A `ClothStrand` (Verlet chain, with a soft spring back to the modelled slant and gravity) simulates it in world space.
  Walking, turning, lunges and falls therefore move it.
- A plane keeps it out of the body.

Epic Fight's own cloth simulator is tied to the player's cape and its Blender export, so it is not used.

## Behaviour

- Neutral toward peaceful people and ordinary animals, like the Alliance's soldiers. Bandits and tigers are threats
  he attacks on sight. When struck, he turns on the attacker, calls the
  soldiers near him, and stays angry for 30 to 50 s.
- 180 health, plus his armour's 20 points. Hard to stagger (stun armour 8). Boss bar (blue).
- Three phases (`PeakCaptainEntity.Phase`), every move parryable:
  1. **SPEAR**: Epic Fight's spear moves.
  2. **RETREAT**, at half his health:
     - he leaps back, returns to his post and sounds the horn ("Peak Guard, to me!");
     - blows glance off him while he is out of the fight;
     - two waves of spear guards drop from the sky 6 blocks around his post, set on his foe: 3 men, then 4 once the
       first are down (a wave is waited 30 s at most);
     - the retreat lasts until both waves are down, 60 s at most.
  3. **ART**: he comes back with the Celestial Peak Spear:
     - Piercing Dragon at 4 to 8 blocks;
     - Plum Blossom Thrusts at 2 to 4 blocks;
     - Sweep of a Thousand Soldiers within 2.6 blocks;
     - the two-handed spear's thrusts between them.
- His men drop in their own pose (`scripts/animation/sky-drop.cjs`: `alliance_sky_fall`, then the crouching landing
  `alliance_sky_landing`, with dust and a thud). They take no fall damage. Summoned, they leave neither loot nor
  experience.
- Persistent, never spawned naturally: spawn egg, `/summon`, and the leitai of the Alliance arena
  (`AllianceArena.captain`), the only place he appears in the world. He stands at his post there (no wandering) and walks
  back to it after a fight.

## Loot (`loot_table/entities/peak_captain.json`)

His worn gear never drops the usual way (drop chance 0). Instead:

- one piece of the Peak Guard armour, every time;
- killed by a player:
  - the Celestial Peak Spear Manual, every time (the art's only source: no chest or barracks, Enzo, 15/10);
  - the spear, 15 % of the time (Looting raises it);
  - a Realm Breaking Pill, 30 %;
  - 10 to 16 silver taels.

## Checks

- `PeakCaptainGameTests`: gear, neutrality, anger and soldiers, boss bar, loot (manual every time), the retreat and
  its two waves then the art, a man landing unhurt.
- `ClothStrandTest`: hangs, flies when running, never passes through the body, keeps its length.
- `gradlew -PgameTests runPeakCaptainTour`: front, back and face shots, a film of him running, and a film of the
  retreat and the first wave dropping (`build/peak-captain-tour-run/screenshots`).
