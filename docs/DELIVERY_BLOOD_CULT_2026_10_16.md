# Blood Cult test delivery — 16 October 2026

Colleuhu95 requested publication of the tested standalone addon and the complete
underground base, rather than integration into the main Murimblock JAR.

## Published content

- [`addons/blood-sect-decor`](../addons/blood-sect-decor/README.md): Blood Sect Decor
  1.10.1 source, runtime assets, editable models and development-only tests.
  Includes Blood Ninja patrol/return behavior, animated blood dao M1, Blood Cloud
  Step Slash with a bounded 15%-of-maximum-health heal, and flowing blood.
- [`art/structures/blood-cult`](../art/structures/blood-cult/README.md): complete
  underground WorldEdit schematic, 72-block-diameter boss arena, floorplans,
  static checks and import instructions. No entities or boss implementation.

The main project's runtime implementation is unchanged by this delivery.
The branch was first updated with `git fetch origin` and `git merge origin/test`;
this was a fast-forward to `d67833f1547f93bc616be92f53c4e49e5cee7163`.
Minecraft 1.21.1, NeoForge 21.1.248, Epic Fight 21.17.3.1 and Java 21 remain pinned.
No worlds, caches, IDE state, credentials, development logs or compiled JARs are
published. Build the separate addon with the repository's Gradle wrapper.

## Checks run before publication

All commands below were run from the repository root with Java 21 on Windows.

| Command | Actual result |
| --- | --- |
| `gradlew.bat build compileGameTestJava` | `BUILD SUCCESSFUL`; 181 unit tests, zero failures/errors/skips |
| `gradlew.bat -PgameTests runGameTestServer` | `BUILD SUCCESSFUL`; server reports `All 288 required tests passed :)` and `1 optional tests failed` |
| `gradlew.bat -p addons/blood-sect-decor build compileGameTestJava` | `BUILD SUCCESSFUL`; runtime and GameTest compilation complete |
| `gradlew.bat -p addons/blood-sect-decor -PgameTests runGameTestServer` | `BUILD SUCCESSFUL`; `All 29 required tests passed :)` |
| Runtime archive inspection | All 184 file entries match the previously tested 1.10.1 JAR byte-for-byte; no GameTests, tours, fixtures or Blockbench models in the runtime archive |
| Source and asset comparison | All 173 runtime source/resource files and 8 art files match the standalone test project byte-for-byte |
| Schematic readback/review | Sponge v3, DataVersion 3955, 2,159,010 cells, 24 valid states; 14 empty chests, no entities; SHA-256 matches metadata |
| Staged diff and independent read-only review | No critical/important findings; corrected addon tour commands to use `-p addons/blood-sect-decor` |

Two development test sources had only a trailing blank line removed. No runtime
code, resource or build setting was changed while copying the tested addon.

### Optional Dawn Day failure (not hidden)

The main-project server run logged the following optional test failure. This
test belongs to the current upstream Dawn Day integration, outside the copied
standalone addon. Its code, required/optional classification and assertions were
not changed for this publication.

```text
(optional) dawndaysattackshitandcanbeparried failed
Dawn Day: halberd_auto1: no damage without guard (the case tests nothing)
| halberd_auto1: a held guard took 0.0 of 0.0
| saber_auto1: a held guard took 0.0 of 8.799988
| florett_auto1: no damage without guard (the case tests nothing)
| florett_auto1: a held guard took 0.0 of 0.0
| iron_fist_auto1: no damage without guard (the case tests nothing)
| iron_fist_auto1: a held guard took 0.0 of 0.0
| knife_onehand_auto1: no damage without guard (the case tests nothing)
| knife_onehand_auto1: a held guard took 0.0 of 0.0
```

The addon's first GameTest startup logged `NoSuchFileException: server.properties`
before generating its new development server configuration. The server then ran
all 29 tests successfully; this was not a failed test.

## In-game evidence and remaining validation

The user previously tested the ninja/body/dao and the 1.10.1 reaction/healing
behavior in the standalone CurseForge profile. On 16 October, WorldEdit 7.3.8
confirmed a real-client paste of this schematic: 2,159,010 blocks, no entities.
That confirms loading and pasting, not a complete architectural walkthrough or
fluid stability after reloading every chunk.

Still pending: complete base walkthrough, multiplayer and coexistence with the
full Murimblock modpack. Epic Fight/Murimblock combat integration, factions,
progression, natural generation, drops and the cult leader are not part of this
standalone delivery. Enzo decides whether and how to integrate it.

## Artifact fingerprints

- Schematic: `15e92590d6ced6421b65af84ca2c103c7468f0a146f9c42905589afbab6a3c16`
- Locally rebuilt addon JAR: `5853ac12650533f67425fa4b578b69b1e7ba15f4a0bd9f70f13e64dd2fb9375f`

The archive fingerprint may vary with ZIP metadata; the archive content comparison
above checked each individual file against the tested build.
