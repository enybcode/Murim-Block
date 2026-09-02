# Data-Driven Direction

Murimblock should gradually move gameplay balance values out of Java when those values are meant to be tuned by designers, datapacks, servers, or addons.

## Already Implemented

- Player Qi and Qi Max are stored through NeoForge attachments.
- Player cultivation state is stored through NeoForge attachments.
- Mob kill Qi reward calculation is centralized in `QiRewardManager`.
- Anti-farm state is centralized in `QiKillTracker`.

## Not Yet Data-Driven

- Mob Qi rewards are currently a Java table in `QiRewardManager`.
- Cultivation Qi Max requirements are currently a Java table in `CultivationProgression`.
- Qi charging VFX thresholds are currently Java constants in `QiChargeVisuals`.

These values are centralized, but they are not yet reloadable from datapacks or config files.

## Recommended Direction For Mob Qi Rewards

NeoForge 1.21.1 supports Data Maps, which attach data-driven reloadable values to registry objects. This is a good fit for entity-type rewards because the target key is naturally an `EntityType`.

Recommended future shape:

```text
data/murimblock/data_maps/entity_type/qi_rewards.json
```

Conceptual content:

```json
{
  "values": {
    "minecraft:zombie": {
      "qi_reward": 4
    },
    "minecraft:warden": {
      "qi_reward": 400,
      "first_victory_bonus": 600
    }
  }
}
```

An addon could later provide:

```text
data/myaddon/data_maps/entity_type/qi_rewards.json
```

```json
{
  "values": {
    "myaddon:demon_king": {
      "qi_reward": 500,
      "first_victory_bonus": 1500
    }
  }
}
```

## Configuration Versus Gameplay Data

Server configuration should be used for broad switches and multipliers:

- enable or disable mob Qi rewards;
- global Qi reward multiplier;
- anti-farm window duration;
- anti-farm minimum multiplier.

Gameplay data should be used for content-specific balance:

- `minecraft:zombie` reward;
- `minecraft:warden` reward;
- future technique definitions;
- future progression balancing.

## Future Progression Data

Cultivation progression can eventually move from `CultivationProgression` into datapack data once the realm/stage model stabilizes. Until then, the Java table is acceptable because the system is still early and heavily tested.

## Source

NeoForge Data Maps documentation: https://docs.neoforged.net/docs/1.21.1/resources/server/datamaps/
