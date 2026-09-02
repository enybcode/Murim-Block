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

## Developer documentation

- `docs/ARCHITECTURE.md`: project architecture and package responsibilities.
- `docs/ADDON_API.md`: public Java API currently available to addons.
- `docs/ADDON_GUIDE.md`: how addon developers should depend on Murimblock.
- `docs/DATA_DRIVEN.md`: planned direction for datapacks, data maps and config.
- `docs/QI_REWARDS.md`: current mob Qi reward balance table.

