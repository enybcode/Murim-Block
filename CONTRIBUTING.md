# Contributing

## Workflow

1. Create feature branches from `develop`.
2. Keep changes focused and small enough to review.
3. Run `.\gradlew.bat build` before opening a pull request.
4. Open pull requests into `develop`.
5. Merge `develop` into `main` only for stable milestones.

## Code Style

- Use Java 21.
- Use 4 spaces for indentation.
- Keep packages under `com.enybcode.murimblock`.
- Use `MurimBlock.MOD_ID` instead of duplicating the mod id.
- Keep registry names lowercase with underscores only when needed.
- Prefer small classes grouped by feature area.

## Assets And Data

- Use the `murimblock` namespace for assets and data.
- Put textures, models, and lang files under `src/main/resources/assets/murimblock/`.
- Put recipes, tags, loot tables, and data maps under `src/main/resources/data/murimblock/`.
