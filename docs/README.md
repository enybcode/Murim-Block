# Documentation Index

## Current Runtime

- [Architecture](ARCHITECTURE.md): packages, authority and addon boundaries.
- [Epic Fight](EPIC_FIGHT.md): pinned dependency and retained/disabled features.
- [Clear Manuscript](gui/TAB_LAYOUT.md): current GUI, not the V4 reference.
- [Qi rewards](QI_REWARDS.md): balance and temporary anti-farm history.
- [Qi and cultivation](cultivation/qi-cultivation.md): meditation, aura, breakthroughs, the inner world and the Heart Demon, Qi Sense, elixirs, realm bonuses (14 October, on test).
- [Murim map](biomes/murim-map.md): the 150 000-block Murim bubble with fixed regions, its ocean ring, the two northern biomes (14 October, on test).
- [Technique balance](combat/technique-balance.md): technique damage by weapon and by Qi Max, and no combo that kills at once (14 October, on test).
- [Armored crab](ARMORED_CRAB_DELIVERY.md): integrated assets, behavior and local verification.
- [Bandit Chief](mobs/bandit-chief.md): the bandits' mini-boss, his great axe and attacks.
- [Sensei Yun](mobs/azure-sensei.md): the Azure Cloud Sect's master in the Misty Bamboo Temple, his sword and his manual.
- [Alliance Merchant](mobs/alliance-merchant.md): the Orthodox Alliance's merchant, his trades and the silver taels.
- [Addon API](ADDON_API.md) and [addon guide](ADDON_GUIDE.md): supported contracts.
- [Murim biomes](biomes/README.md): the seven biomes, their blocks and textures (redrawn 14 October).
- [Murim structures](structures/README.md): the Cloud Cliff Sanctuary and the six sites with their technical sheets.
- [Murim lore](lore/README.md): the world bible: the Murim, its history, regions, powers and mysteries; the player has no lore of his own (15 October, proposal awaiting Enzo).
- [Murim factions](factions/README.md): proposal for the beta: Orthodox Alliance (centre) and Heavenly Demon Cult (desert), 12 arts, bosses, mobs (15 October, awaiting Enzo).

## Development And Delivery

- [Point de reprise et installation sur un autre PC](../l%C3%A0%20o%C3%B9%20je%20me%20suis%20arr%C3%AAter/README.md): état courant, derniers échanges Claude, dépendances et suite.
- [Local publication, 13 October](DELIVERY_2026_10_13.md): combat/techniques changes, current checks and remaining validation.
- [Git workflow](GIT_WORKFLOW.md): `test` for pending validation, `main` stable.
- [Codex and Claude](AI_COLLABORATION.md): shared rules, local setup and safe handoffs.
- [Claude handoff](CLAUDE_HANDOFF.md): compact project state, decisions and next validation steps.
- [Cloud import](CLOUD_IMPORT.md): setup, networking, headless verification and handoff.
- [Audit report](CODE_AUDIT_2026_10_08.md): fixes, checks and remaining limitations.
- [GUI/mob delivery](DELIVERY_CLEAR_MANUSCRIPT_MOBS.md): earlier implementation report.

## Creating Content

- [Mob creation](MOB_CREATION_GUIDE.md): French handoff guide and validation stages.
- [Animation pipeline](ANIMATION_PIPELINE.md): Epic Fight rigs and GeckoLib boundaries.
- [Mob request form](examples/mobs/MOB_REQUEST_TEMPLATE.md): what the user provides.
- [Data-driven direction](DATA_DRIVEN.md): future design, not implemented configuration.

`examples/mobs` contains inactive examples. `gui/legacy-v4` contains historical
art only. `gui/tab-layout-demo.html` is a historical V4 demo, not a screenshot
of the current Minecraft GUI. Local AI references and captures remain in `.local`
or `build`, outside version control and outside the mod distribution.
