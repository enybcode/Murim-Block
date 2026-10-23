# Inner world (mockup slice)

Status: in the game since 23 Oct 2026. Mockup validated by Enzo the same day (64-block island, see below). Enzo looked
at V1's inner worlds on `main` for the idea only; nothing was copied.

## Decisions (Enzo, 23 Oct)

- One island per player, private: nobody else can enter it. The same island for everyone for now, whatever the
  technique (themes per martial art can come later).
- Big enough for a boss fight, but not too big: 64 blocks across, a flat 32-block arena in the middle.
- Nothing can be broken, placed or taken inside, in any game mode.
- An 8x8 zone to the north is kept for a future NPC: floor only, nothing implemented.
- Unlocked after the Mortal realm: from realm 1 (Third-rate Warrior, Qi Condensation) on, so after the first trial.
- While inside, the player's body (or an avatar) stays seated in meditation where they sat.

## How it works

- Enter: meditate, open the System (K) > Cultivation > **ENTER INNER WORLD**. Below realm 1 the button reads
  `INNER WORLD · THIRD-RATE` and is disabled; the server checks the realm and meditation again.
- The player arrives on the stone circle south of the arena, facing north. Inside, the same button reads
  **RETURN TO BODY**.
- The body (`MeditatingBody`) sits where the player sat, with their skin, legs forward, hands on the knees. Any blow
  or damage on it tears the player back. It removes itself once its owner is back, died, or left by other means.
- Back to the body: the button, falling off the island, or a lethal blow inside (nobody dies there). Logging in
  inside (server stop, disconnect) wakes the player in their body.
- Privacy: islands sit 4096 blocks apart in `murimblock:inner_world` (void, fixed noon). A player in the inner world
  without a trip in progress, or far from their own island, is sent back.
- Guard: block breaking (and cracking), placing, using items on blocks, buckets, trampling, explosions and creative
  pick-block are all cancelled there.

- Any added dimension makes vanilla mark the world "experimental": a warning when creating it and a backup question
  on its first opening (NeoForge then remembers it). Hidden at Enzo's request (24 Oct): `InnerWorldClient` answers
  both screens (yes, no backup), and logs `Skipped the experimental world warning`. It also hides that warning for
  other mods' experimental settings.

## Island layout (`InnerIsland`, version 1)

Center at Y 100 on grass. Upside-down cone of dirt and stone below. Arena of polished andesite with a stone-brick rim
and a chiseled center; dirt paths arrival → arena → NPC zone; pond to the south-east; seven pines; three floating
mossy stones out of reach; grass and flowers. Raise `InnerIsland.VERSION` after changing the layout: older islands are
cleared and rebuilt on their owner's next visit.

## Code

`innerworld/`: `InnerWorld` (enter, leave, checks), `InnerWorldEvents` (travel payload, guards, login, damage),
`InnerWorldState` (attachment `murimblock:inner_world`: trip in progress, return point, body id), `InnerIslands`
(saved data `murimblock_inner_islands`: slot and built version per player), `InnerIsland` (the blocks),
`MeditatingBody` (entity `murimblock:meditating_body`). Client: `client/innerworld/MeditatingBodyRenderer`,
`InnerWorldClient`. Dimension: `data/murimblock/dimension[_type]/inner_world.json`.

## Check

`gradlew runInnerWorldTour` (own folder `run/inner-world-tour`, so it runs beside an open client): a mortal stays
out, a Third-rate enters on their own island with the body seated, an arena block cannot be broken, no block can be
placed, falling returns to the body, a blow on the body ends the trip. PASS/FAIL in the log, shots
`murim_inner_arrival`, `murim_inner_page`, `murim_inner_aerial`, `murim_inner_body` in its `screenshots/`.
