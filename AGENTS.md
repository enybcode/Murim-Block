# Murimblock V2

Murim (wuxia) mod for Minecraft, restarted from zero on 23 Oct 2026. Owner: Enzo.

## Stack (keep these versions)

- Minecraft 1.21.1, NeoForge 21.1.248 (ModDevGradle), Java 21, Gradle wrapper.
- GeckoLib 4.9.3: models and animations of items, blocks, effects and non-combat models.
- Player Animation Library 1.1.6 (`player_animation_library`): non-combat player animations (lotus, trial).
- Shenfa (身法, "the art of the body"): our own skeletal animation engine; it draws players and our mob looks (zombie = Disciple déchu, `SkeletalMobs`)
  (`com.murimblock.shenfa`, glTF from Blender); combat (`com.murimblock.combat`) builds on it. To-do:
  `docs/combat/shenfa-todo.md`.
- Photon 2.2.8 (with LDLib2, KilaGraph): effects and dynamic lights.
- No Epic Fight, no TerraBlender: combat is our own code.
- Versions live in `gradle.properties` (Modrinth version ids).

## Rules

- V1 (branch `v1` and the old `Murimblock` folder) is frozen: never edit it, and copy nothing
  from it (code or assets) unless Enzo asks for that specific piece.
- Work on `main` (V2; renamed from `main-V2` on 28 Oct 2026); commit and push `origin/main` after each verified
  change. Never force-push.
- All player-facing text in English, through `assets/murimblock/lang/en_us.json`.
- Keep saved IDs and player data stable once released.
- Original assets only; never extract other mods' assets into our jar.
- Beauty first: every model gets relief and original detail, every visual gets care (`docs/art/visual-rules.md`).
- Before any model or animation, draw its storyboard and get it validated by Enzo (`docs/art/visual-rules.md`).

## Checks

- Build: `./gradlew build` (`gradlew.bat` on Windows).
- In game: `./gradlew runClient`. Report what was actually run; a build is not an in-game test.
- Never run `gradlew --stop`: daemons are shared with other checkouts.

## Docs

- `docs/README.md` indexes the design docs; read only the ones the task needs.
