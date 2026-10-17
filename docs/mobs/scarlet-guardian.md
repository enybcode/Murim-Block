# Guardian of the Scarlet Threshold

First playable review, requested by Colleuhu95 on 17 October 2026. Integrated into Murimblock; no addon. Reuses Enzo's `scarlet_guardian` body, armour, idle and walk without redesigning them. The existing assets were not yet a registered combat entity.

The guardian stands about four blocks tall. It has 1000 health, 16 armour, 6 armour toughness, 0.95 knockback resistance and 20 Epic Fight stun armour. Base attack is 6, with slow heavy pursuit. It defends the cult against survival players and retaliates against attackers, but never targets fellow guardians, blood ninjas or blood soldiers. Its Epic Fight melee colliders also exclude those allies, even without scoreboard teams. Spawn egg and summon only; placement, rewards and capital access remain separate work. These defaults apply to newly spawned guardians; already saved entities retain their saved attributes. The health default stays within Minecraft's standard 1024 HP attribute cap.

Every player tracking a living guardian sees its red, ten-segment vanilla boss bar at the top of the screen, including before combat. The title shows only the boss name, with no HP numbers or percentage; the fill uses the exact health ratio. Damage, healing, custom names and maximum-health changes update server-side even with AI disabled. Leaving tracking range, death or removal clears the bar. This uses vanilla boss-event networking, not a client-only overlay or a globally broadcast bar.

Combat uses Epic Fight and the existing Sanguine Art animations, crystal/sword entities, textures and Photon impact effects. Boss-specific timings and AI do not change the player's art:

- Blood Crystal: a visible 1.6 s charge, then the existing throw; a large projectile that breaks on bodies and walls. Eight-second recovery between uses.
- Scarlet Tear: summons the existing blood sword, makes two telegraphed Epic Fight cuts, then throws it. Twelve-second cooldown. The sword is anchored to the animated hand.
- Blood Crystal Form: below 60% health, once per encounter; 30% damage reduction for 30 seconds. Six dorsal crystals grow during its transformation. Further successful blows replenish up to ten; they are spent by Crystal Barrage.
- Crystal Barrage: the crystals launch consecutively after a visible preparation. Eight-second cooldown, requires the form and available crystals.
- Blood Iceberg: once below 35% health, the existing temporary blood temple, only where there is enough headroom. Checks every column of its 45 × 45 bounding footprint, including standing space above the highest spires; refuses unloaded chunks and retries blocked checks after one second. Checks again before the temple rises, in case a roof appeared during the wind-up. Preserves solid terrain and collapses when its owner dies or leaves. No forced excavation of the cult's base.

At 1000 maximum HP the form threshold is 600 HP, and the temple threshold is 350 HP. This is an endurance tuning pass; player gear, cultivation level and party size still need human balance testing. The native capture opponent has 18 attack damage rather than ordinary zombie damage; it is a repeatable stress-test fixture.

The guardian's held crystals and sword are cleared if combat ends, its target is invalid, it dies or unloads. Projectiles retain the existing server collision and guard path. No custom manual melee damage loop. An empty arena ends the encounter after ten seconds, clears temporary effects, and rearms the form without healing the boss.

Implementation: export the validated meshes; register entity, attributes, egg, translations and Epic Fight patch; permit living mob owners in the shared Sanguine projectile with player-only progression retained; implement encounter state; reuse the temporary temple; verify server combat and player regressions; film native client GIFs. Test/capture harnesses belong exclusively to `src/gameTest`.

Review deliverables are real framebuffer captures of the registered entity, not the earlier mesh-only storyboard. Publication requested by Colleuhu95; integration into test remains Enzo's decision.

## Reproduce and review

Spawn with `/summon murimblock:scarlet_guardian` or the creative spawn egg.

- `gradlew.bat build compileGameTestJava`: normal release build and development harness compilation.
- `gradlew.bat -PgameTests -PgameTestFilter=scarlet_guardian runGameTestServer`: 18 guardian tests, including real Epic Fight sword contacts against cultist bystanders, roof checks, cleanup, endurance and vanilla boss packets delivered to two independent mock player connections.
- `gradlew.bat -PgameTests runGameTestServer`: full suite, required before every push; the optional Dawn Day survey is reported separately if it fails.
- `gradlew.bat -PgameTests runScarletGuardianTour`: native client review of charge/impact, Scarlet Tear, crystal form/barrage and Blood Iceberg.
- `gradlew.bat -PgameTests runScarletGuardianHealthTour`: live combat with the native name-only HUD; a high-health test zombie deals 18 base melee damage.

Client tours create development worlds and framebuffer captures under `build`; these are never packaged or committed. The release JAR contains the guardian class, animations and mesh, but no tour or GameTest classes. Mock-player packet tests and scripted client tours do not certify two graphical clients or final player-gear/party balance.

To regenerate the unchanged authored body/armour atlas and game mesh, run `node scripts/animation/scarlet-guardian-game.cjs`. The shared biped loader expects the pinned Epic Fight JAR in the default Gradle cache; for a custom cache, set `EPICFIGHT_BIPED` to its extracted `assets/epicfight/animmodels/entity/biped.json` first. Existing Sanguine animation JSON is copied to guardian-specific animation accessors; the player's animation timing remains unchanged.
