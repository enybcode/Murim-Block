# Bandits and protected buildings

The common Mountain Bandit takes the surface zombie's role in the Overworld. This change lives in Murimblock itself.
It uses the existing five outfits, dao and Epic Fight attacks. The Bandit Chief and Warlord keep their authored boss
behaviour; common bandits regard them as allies.

## Spawning

- Mountain Bandits can spawn in all Overworld biomes, in groups of 2–4, with the existing weight of 95.
- Night (13000–22999): all otherwise-valid attempts pass the time check. Day and twilight: one in four passes.
  This is an attempt multiplier, not a promise of exactly four times as many live entities; caps and available terrain still apply.
- Bandits ignore the darkness requirement and do not burn in daylight. They spawn at the surface, not in caves.
- Random vanilla **monsters** spawn only at least five blocks below the solid surface. Normal surface animals remain.
  Phantoms and surface zombie sieges cannot bypass the rule. Nether and End ecology is unchanged.
- Random spawns of any mob are blocked inside registered structure bounding boxes, including their courtyards.
  The separately built Cloud Cliff Sanctuary and Crab King's lair are also protected.
- Commands, spawn eggs, spawners and authored structure residents retain their explicit behaviour. Random wandering
  traders/llamas are protected against even though vanilla labels their arrival `EVENT`.
- Previously existing entities are not removed. During chunk generation, new random mobs receive a saved one-shot
  check and are rejected before their first AI tick if inside a protected site. Generation workers never load
  ServerLevel chunks or SavedData to perform the check.

## Player builds and WorldEdit

Minecraft does not record an arbitrary player-built house or `.schem` as a generated structure. Register its bounds
once with an operator command (permission level 2); the protection is saved per world dimension and survives restart.
It prevents random arrivals, not an enemy walking through the entrance.

```text
/murimspawn protect culte x1 y1 z1 x2 y2 z2
/murimspawn protecthere culte 160
/murimspawn list
/murimspawn remove culte
```

`protect` takes any two opposite corners, including relative coordinates. `protecthere` uses a square of the given
radius around the command source, over the full build height. The example radius must be adjusted to the actual base.
Using an existing name replaces its bounds. These commands do not alter blocks or load chunks.

## Relations and drops

- Common bandits pursue survival/adventure players, human NPCs and tigers. They spare normal animals and fellow bandits.
- The five Alliance soldier types, Peak Captain, Azure Sensei, Blood Ninja and Blood Soldier proactively hunt bandits.
  Peaceful merchants remain civilians; the invulnerable Golden Dragon Sage retains his story role.
- Target lists are entity type tags: `bandits`, `bandit_human_targets`, `bandit_enemies`. Other factions can extend them.
- Provisional balance: a player-attributed bandit kill has a **10% combined chance** to drop one art scroll,
  equally split between **Long Fist** and **Traveller's Staff Art** (5% each). Never both from this pool.
  Faction-only kills do not generate these scrolls. The existing dao, provisions and currency drops are retained.

## Verification

`BanditEcologyGameTests` exercises real spawn events, daylight/night attempts, generated/custom protected regions,
save/reload, target acquisition, animal exclusion, special spawners and repeated rolls of the actual loot table.
`BanditEcologyTour` records native Minecraft frames of automatic fights against the Alliance, Blood Soldier and tiger,
and checks that a nearby cow remains unharmed. It does not force targets or damage. All harness code stays in `src/gameTest`.

```text
gradlew.bat build compileGameTestJava
gradlew.bat -PgameTests runGameTestServer
gradlew.bat -PgameTests runBanditEcologyTour
```

Live spawn density in a long survival session and two-client multiplayer behaviour still need playtesting.
