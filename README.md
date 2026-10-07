# Murimblock

Technical foundation for the Murimblock Minecraft mod.

## Development environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- Gradle Wrapper 9.2.1

Epic Fight is the selected combat engine. Its dependency and integration are not
installed in this cleanup build; attacks currently remain Minecraft's default.
See `docs/EPIC_FIGHT.md` for the integration boundary.

## Common tasks

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat -PgameTests runGameTestServer
```

## Current systems

- Qi and Qi Max player data.
- Passive and active Qi regeneration.
- Configurable keybind for charging Qi.
- Qi charging movement lock, FOV and particle effects.
- Compact blue Qi HUD, displayed through the retained mode toggle.
- Cultivation realms, stages and breakthrough checks.
- Server-side Qi rewards for mob kills with anti-farm and boss first victories.
- Temporary server-authoritative mode toggle for the HUD, GUI and addon API;
  this flag does not change attacks, damage, guard or animation.
- English in-game text and a dedicated bitmap font for the Murim profile GUI.
- Four distinct GUI pages with the 3D player shown only on Profile.

## Repository Layout

- `src/main`: shipped mod code and resources.
- `src/test`: unit and resource checks.
- `src/gameTest`: development-only foundation server tests.
- `docs`: maintained technical documentation and GUI demo.
- `.local/gui`: local GUI references and previous asset kits, excluded from Git.
- `build`, `.gradle`, `run`: generated output, caches and local development worlds.

Keep local references out of `src` and commits. Never delete development worlds
as part of source cleanup.

## GUI Preview

`docs/gui/tab-layout-demo.html` previews the four integrated tab layouts in English.
It shares the manuscript font with the mod and uses example values and an
illustrative player. The Minecraft GUI reads synchronized player data and actual
configured keybindings. The integrated appearance was approved by Enzo on
2026-10-03.

## Developer documentation

- `docs/ARCHITECTURE.md`: project architecture and package responsibilities.
- `docs/ADDON_API.md`: public Java API currently available to addons.
- `docs/ADDON_GUIDE.md`: how addon developers should depend on Murimblock.
- `docs/DATA_DRIVEN.md`: planned direction for datapacks, data maps and config.
- `docs/QI_REWARDS.md`: current mob Qi reward balance table.
- `docs/EPIC_FIGHT.md`: selected combat engine, current boundary and integration checks.
- `docs/GIT_WORKFLOW.md`: publication on `test` and user-approved promotion to `main`.

