# Murim Block

Murim Block is a Minecraft mod built with NeoForge for Minecraft 1.21.1.

## Prerequisites

- Java 21 JDK
- Git
- IntelliJ IDEA Community or Ultimate is recommended

## Clone

```powershell
git clone https://github.com/enybcode/Murim-Block.git
cd Murim-Block
git switch develop
```

## Setup

The Gradle wrapper downloads the required Gradle version and NeoForge userdev dependencies.

```powershell
.\gradlew.bat neoForgeIdeSync
.\gradlew.bat build
```

On Linux or macOS:

```bash
./gradlew neoForgeIdeSync
./gradlew build
```

`setupDecompWorkspace` and `genIntellijRuns` are not used by current NeoForge/ModDevGradle projects. `neoForgeIdeSync` prepares the files IntelliJ needs after Gradle import.

## Run In Development

From the command line:

```powershell
.\gradlew.bat runClient
```

From IntelliJ IDEA:

1. Open the project folder.
2. Let Gradle import the project.
3. Run `neoForgeIdeSync` from the Gradle tool window if the run configs are missing.
4. Start the generated Minecraft Client run configuration.

## Project Structure

- `src/main/java/com/enybcode/murimblock/` - Java source code
- `src/main/resources/assets/murimblock/` - client assets such as lang files, textures, and models
- `src/main/resources/data/murimblock/` - data packs, recipes, tags, and generated data
- `src/main/templates/META-INF/neoforge.mods.toml` - generated mod metadata template
- `gradle.properties` - Minecraft, NeoForge, and mod identity versions

## Branches

- `main` contains stable project state.
- `develop` is the shared integration branch for active development.
