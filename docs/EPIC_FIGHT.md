# Epic Fight Integration

## Decision And Current State

Epic Fight is the selected combat engine for Murimblock. Use its installed mod
and API; do not copy its engine or animation assets into this repository.

This build is a source cleanup, not a completed Epic Fight integration. It has
no Epic Fight dependency yet, so attacks and shield use currently stay vanilla.
Qi, cultivation, rewards, profile screens and saved player data remain in
Murimblock.

The existing `MurimblockApi.combat()` flag remains for the compact Qi HUD and
profile screens. Its key and `/combat` mode commands do not change attacks and
are not connected to Epic Fight's battle mode. Preserve the public API when
introducing a bridge, and avoid exposing two independent battle-mode controls.

## Next Integration Slice

1. Pin an official Epic Fight release for Minecraft 1.21.1 / NeoForge / Java 21,
   including the dependencies required by that exact release.
2. Verify Gradle, metadata, IntelliJ runs and dedicated-server startup together.
3. Delegate player animation, attacks, damage, guard and collision to Epic Fight.
   Do not add a second attack handler or another player animation backend.
4. Bridge the existing HUD/GUI flag to the authoritative Epic Fight state, then
   verify key conflicts, Qi charging and the 3D profile renderer.
5. Test sword attacks, shield/item interactions, armor, death, respawn, equipment
   changes and dimensions, including two actual clients on a dedicated server.

Keep Qi costs outside this integration slice. Martial styles, technique loadouts
and four configurable M1 moves require a later, explicit API design and tests.

## Checks For This Build

- `./gradlew build`: remaining unit tests, GUI resource checks and packaging.
- `./gradlew -PgameTests runGameTestServer`: default attack/damage behavior,
  item use, mode lifecycle, Qi charging locks and player-data serialization.
- Manual: open all four profile tabs; toggle the compact Qi HUD; charge/release
  Qi; attack and use an offhand shield; relog and respawn in a disposable world.

The server tests are not a visual or two-client multiplayer validation. Keep
`main` unchanged until the user validates this build in game.

## Official References

- [Epic Fight 1.21.1 source and setup](https://github.com/Antikythera-Studios/epicfight/tree/1.21.1)
- [Epic Fight API entry points](https://github.com/Antikythera-Studios/epic-fight.github.io/blob/main/docs/API/Starting.en.md)
- [Epic Fight asset terms](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/LICENSE-ASSETS)

Referencing animations from an installed dependency does not transfer ownership
of those assets. Do not redistribute their extracted files with Murimblock.
