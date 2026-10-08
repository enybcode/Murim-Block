# Epic Fight Integration

## Pinned Environment

- Minecraft 1.21.1, NeoForge 21.1.248, Java 21.
- Epic Fight mod version **21.17.3.1**, Modrinth artifact **8HHhJt6i**.
- Release `21.17.3.1-mc1.21.1-neoforge`, published 2026-05-31.
- Exact required dependency on both client and server; Gradle installs it for IntelliJ.
- The examined release embeds its own jar-in-jar dependencies; it declares no additional
  mandatory mod IDs. Do not infer compatibility with Fabric or newer engine APIs.
- No extracted engine code, textures or animations are bundled in Murimblock's jar.

## Responsibilities

Epic Fight owns animation, player rendering, attack timing/colliders, damage, guard,
default weapon combos, patched entities and network synchronization. Murimblock
disables player stamina as a combat resource.
Its default entity patches also affect supported mobs and bosses. This is not a
sword-only replacement. Outside combat mode, the engine permits vanilla-mode combat.

Murimblock retains Qi, cultivation, rewards, manuscript screens and stable addon API.
`EpicFightBridge` delegates to the actual `PlayerPatch`. The unsaved `CombatData`
attachment is a HUD/event mirror, not a second combat authority. Epic Fight owns
actual mode/skill persistence; logout clears only the mirror. Switches respect the
engine gamerule/events. No new Qi costs were introduced.

Qi charging cannot start during an action or held skill. The server `CAST_SKILL`
hook rejects casts during Qi charging. Existing movement locks, regeneration and
charge effects remain. Test damage/stun interruption behavior in actual play.

## Interface And Controls

- **K** opens Murim Profile, **V** switches actual combat mode, **R** charges Qi.
- Attack, guard, dodge, innate skill, mover and four target-lock controls retain
  their actual engine input objects and useful user key assignments.
- One **Murimblock** category holds twelve useful bindings instead of four native
  engine blocks. Skill/config/emote/mode/debug shortcuts are disabled and hidden.
  Their reset defaults are Unbound, so Reset All cannot restore duplicate shortcuts.
  The native tooltip modifier still works but its redundant menu row is hidden.
- Skill editor and book screens route to **Techniques**. Configuration, sidebar,
  cosmetics, emote and developer-editor screens route to **Info / Combat Settings**.
  This does not provide replacement emote/cosmetic/datapack-editing functionality.
- Outside a world, native settings entry points open vanilla Options instead of
  a player-dependent Murim screen. Combat settings are available in a loaded world.
- Settings expose camera type, automatic perspective, first-person camera/body,
  lock-on snapping, blood effects and vanilla keybind settings. Advanced native
  settings are not all recreated in this slice.
- Only four native engine HUD layers are cancelled: stamina, skills, innate and
  charging. Native target indicator and mob health bars are suppressed. Murim draws
  compact skill/charge indicators outside the hotbar's health/armor/food columns
  and keeps the blue Qi replacement of XP in combat mode. The yellow stamina meter
  is removed entirely. XP data is unchanged.
- Three access-transformed fields allow presentation regrouping and row filtering.
  A client-only mixin suppresses the temporary engine version banner; credits/version
  remain in the mod list and documentation. No engine fork or global font override.
- The manuscript font is scoped to Murim screens; labels remain in `lang/en_us.json`.

## Stamina Disabled

`EpicFightStaminaMixin` applies to `PlayerPatch` on **both client and server**.
Stamina reads report the existing finite maximum, finite nonnegative stamina costs
are affordable even when larger than that maximum, and stamina writes cannot
deplete it. This is a compatibility shim, not a larger pool or faster regeneration.
It also neutralizes legacy depleted values and direct writes outside skill casts.
Upstream attributes, data IDs and serialization remain registered and unchanged;
no engine fork or save migration is necessary.

Guard and dodge still require the appropriate equipped skills, weapon, action
state and positioning. Front-facing and unblockable-damage rules remain native;
stamina exhaustion can no longer cause guard break. The Qi-charge cast exclusion,
skill cooldowns, weapon charge and explicit health costs remain unchanged. No
stamina cost is transferred to Qi, hunger or health, including Forbidden Strength's
insufficient-stamina fallback. Protocol **6** requires matching client/server builds
so an older client cannot retain its own stamina prediction.

Upstream stamina-only upgrades lose their resource-management purpose. Their saved
IDs are retained; balancing those native skills and their descriptions is later
work. Do not claim every native passive or addon interaction has been rebalanced.

## Technique Learning And Equipping

`TechniqueChangePayload` carries only a skill identifier, slot and held-book index.
The server checks player state, learnable category, passive limit, learned ownership,
prerequisite, cooldown and matching actual held book. Successful survival learning
consumes exactly one book; duplicate/rejected requests do not consume books or Qi.
Creative players can equip registered learnable skills without a book. Owner and
tracking players receive native skill packets; no optimistic client mutation.

Guard must be equipped and compatible with the weapon before right-click guard works.
For creative testing, select Guard in Techniques, choose the guard slot, then Equip.
In survival use a real Guard book. Library and descriptions scroll independently;
tooltips show full labels. A held book is offered even before its skill is learned.

This is **native skill selection**, not martial styles or four configurable M1 slots.
Those, custom animation authoring and physical blade clashes remain later work.
No automatic guard grant, new Qi costs or clash implementation is included.

## Release Compatibility Patch

The pinned release's wooden sword datapack uses unresolved sound/particle IDs.
Murim's independently authored `data/minecraft/capabilities/weapons/wooden_sword.json`
references registered `epicfight:` IDs, impact 0.5 and one strike. No extracted
engine assets are included. Recheck this small override before upgrading the engine
or adding weapon-balance datapacks.

## Verification Loop

```powershell
.\gradlew.bat build compileGameTestJava
.\gradlew.bat -PgameTests runGameTestServer
```

Unit checks cover Qi/cultivation/GUI resources, meter bounds, native HUD namespaces,
payload validation/roundtrip and screen routing. Server GameTests use only
`build/game-test-run`; they cover item use, mode/Qi invariants, charging/lifecycle,
serialization, invalid technique requests, book consumption and actual animated
sword collision and capabilities across all six vanilla sword tiers. The embedded
connection's real listener is advanced after equipment initialization; the world
drives the animation clock. This does not emulate a remote client or latency.
Applied damage is counted through `LivingDamageEvent.Post`; target helmets exclude
daylight burning.

Optional real-client capture run:

```powershell
# Create the parent first; copy only a disposable test world, never a user save.
New-Item -ItemType Directory -Path build/client-smoke-run/saves -Force
Copy-Item -LiteralPath build/game-test-run/world -Destination build/client-smoke-run/saves/murim-smoke -Recurse
.\gradlew.bat -PgameTests runVisualSmoke
```

Do not overwrite an active test world. Development-only `ClientVisualSmoke` opens
the four actual tabs, native skill/book routes, settings, keybinds and HUD; screenshots
go to `build/client-smoke-run/screenshots`. It checks hidden rows and grouping,
and writes `visual-smoke-passed.txt` only after success. A process exit alone is
not success. Neither the capture harness nor GameTests enter published mod jars.

### Recorded Checks (2026-10-08)

- 114 unit tests: zero failures or errors.
- 14 required server GameTests passed, including one damage application from an
  actual animated sword contact, book consumption and all six sword capabilities.
  Added repeated guard/dodge resource checks above the old maximum, Forbidden
  Strength without health fallback, retained health/charge/cooldown requirements,
  a grounded dodge cast and front-only guard with no stamina or Qi consumption.
- Real Minecraft client smoke run passed: four tabs, native screen redirection,
  combat settings, one controls category with twelve useful bindings and hidden
  redundant rows. Eight fresh captures were reviewed at 427x240 and 320x240 GUI
  sizes; the native version banner and yellow stamina bar are absent and remaining
  HUD indicators avoid the hotbar. The client reads full stamina and accepts costs
  above the old maximum, confirming the common-side shim is active locally too.
- Native book-screen routing was exercised, but this does not replace manual
  right-click checks with real books in both hands or two-client network tests.

Before merging to main, manually validate:

1. Selection, scrolling, label legibility, small-window layout, rebinding and Reset All.
2. Real books in both hands, learn/equip/unequip and relog.
3. Third-person sword combos, front/rear guard, dodge, lock-on, building, shield,
   bow/tool/item interactions and transitions outside combat mode.
4. Qi charging versus attacks/guard, damage/stun interruptions and movement restoration.
5. Death/respawn, dimension changes and equipment changes mid-action.
6. Two real clients on a dedicated server: owner/observer animation, single damage,
   late tracking, disconnect/reconnect and different skill/equipment choices.

Automated tests and static captures do not validate feel or guarantee no bugs.
Keep main unchanged until explicit user approval.

## Known Boundaries

- Native client-to-server packets still exist. The validated Murim technique path
  is not an anti-cheat audit or hardening of every upstream packet.
- Upgrade checks must cover skill/container APIs, screen namespaces, layer names,
  key categories, gamerules, the three access-transformed fields and the pinned
  `PlayerPatch.getStamina`, `hasStamina` and `setStamina` mixin targets.
- Upstream diagnostics can mention `epicfight:air_slash`, missing subtitles and
  optional WaveyCapes integration. Do not claim every native skill was tested or
  patch the whole engine to suppress unrelated logs.
- No two-client latency or exhaustive boss/addon compatibility claim is made.

## Official References And Terms

The exact published binary and sources jar were inspected, not just README claims
or an unreleased branch. Ignored reference files live in `.local/epicfight`.

- [Pinned release](https://modrinth.com/mod/epic-fight/version/21.17.3.1-mc1.21.1-neoforge)
- [Official source](https://github.com/Antikythera-Studios/epicfight/tree/1.21.1)
- [API setup](https://epicfight-docs.readthedocs.io/API/Starting/)
- [Asset terms](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/LICENSE-ASSETS)

Code and animation assets have separate terms: GPL engine code and reserved assets.
Using animations from an installed dependency is not permission to redistribute
extracted files. Murimblock does not do that.
