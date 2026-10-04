# Murimblock

Technical foundation for the Murimblock Minecraft mod.

## Development environment

- Minecraft 1.21.1
- NeoForge 21.1.248
- Java 21
- Gradle Wrapper 9.2.1

## Common tasks

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat -PcombatGameTests runGameTestServer
```

## Current systems

- Qi and Qi Max player data.
- Passive and active Qi regeneration.
- Configurable keybind for charging Qi.
- Qi charging movement lock, FOV and particle effects.
- Temporary Qi debug HUD.
- Cultivation realms, stages and breakthrough checks.
- Server-side Qi rewards for mob kills with anti-farm and boss first victories.
- Server-authoritative Combat Mode foundation with configurable keybind and addon API.
- English in-game text and a dedicated bitmap font for the Murim profile GUI.
- Four distinct GUI pages with the 3D player shown only on Profile.
- Sword-only directional guard in Combat Mode, with Qi cost and guard break.
- Weapon-category and martial-art profiles; only the basic sword profile is enabled.
- Shared blade-trajectory/contact core, not yet connected to live attacks or animation.

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
- `docs/SWORD_COMBAT.md`: live sword guard controls, profiles, restrictions and validation.
- `docs/COMBAT_REWORK.md`: delivered contact core and the staged combat replacement plan.
- `docs/GIT_WORKFLOW.md`: publication on `test` and user-approved promotion to `main`.

