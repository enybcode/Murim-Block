# Murim Block Development State

## Vision

Murim Block should feel as impressive as a top anime Minecraft mod showcase, but with its own Murim identity: cultivation, qi, sword arts, breakthroughs, sects, manuals, pills, beasts, realms, and clean progression.

Reference quality target from the Sorcery Age showcase:

- Techniques must be instantly readable: charge-up, impact, aftermath, cooldown.
- Keybinds and menus must make the player feel powerful without being confusing.
- HUD/overlays should show energy, state, unlocks, and combat readiness clearly.
- Mobs should have recognizable silhouettes, custom textures, roles, sounds, drops, and test notes.
- Visual effects should sell scale: particles, camera rhythm, projectiles, area marks, screen/UI feedback.
- The world context matters: a good arena/city/sect environment makes abilities look stronger.

## Production Pipeline

1. Idea
   - Define fantasy, gameplay role, unlock condition, and visual signature.
2. Blockbench / texture work
   - Create model, texture, optional animation notes, export names.
3. MCreator
   - Create or update element, procedures, GUI, keybind, overlay, recipes, advancements.
4. Code/assets review
   - Run `tools\murim.ps1 status`, inspect modified files, check missing assets.
5. In-game test
   - Confirm visuals, hit detection, cooldown, unlock, HUD, and no console spam.
6. Git
   - Commit only a coherent milestone, then push to GitHub.

## Current Priority Tracks

- Technique GUI: fix locked/unlocked image behavior and make skill state obvious.
- Sword art progression: make the first sword art feel like a real unlock moment.
- Qi system: keep the HUD readable and clamp/regen rules predictable.
- Mobs: upgrade from simple entity to memorable Murim creatures with drops and purpose.
- Tooling: keep MCreator, Blockbench assets, Gradle and Git synchronized.

## Local Commands

```powershell
cd "C:\Users\enzob\MCreatorWorkspaces\murim_BlockV2"
tools\murim.ps1 status
tools\murim.ps1 elements
tools\murim.ps1 assets
tools\murim.ps1 blockbench
tools\murim.ps1 report
```

## Quality Checklist For Every New Technique

- Has a fantasy name and short gameplay description.
- Has unlock condition.
- Has keybind/menu access.
- Has resource cost or cooldown.
- Has visible cast feedback.
- Has visible hit/activation feedback.
- Has failure feedback when locked or insufficient qi.
- Has an in-game test note.
- Has commit pushed after it works.
