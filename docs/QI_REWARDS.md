# Qi Rewards

Mob kill Qi rewards are currently implemented in `QiRewardManager`.

This document mirrors the current balancing table for designers and future addon authors. It is documentation only; editing this file does not change gameplay yet.

## Current Values

| Entity | Qi reward |
| --- | ---: |
| Passive vanilla mobs | 1 |
| `minecraft:silverfish` | 2 |
| `minecraft:endermite` | 2 |
| `minecraft:bee` | 2 |
| `minecraft:llama` | 2 |
| `minecraft:trader_llama` | 2 |
| `minecraft:pufferfish` | 2 |
| `minecraft:spider` | 3 |
| `minecraft:zombie` | 4 |
| `minecraft:zombie_villager` | 4 |
| `minecraft:wolf` | 4 |
| `minecraft:goat` | 4 |
| `minecraft:drowned` | 5 |
| Drowned holding trident | 20 |
| Small slime | 3 |
| Medium slime | 4 |
| Large slime | 6 |
| Small magma cube | 4 |
| Medium magma cube | 7 |
| Large magma cube | 10 |
| `minecraft:husk` | 6 |
| `minecraft:skeleton` | 6 |
| `minecraft:phantom` | 7 |
| `minecraft:pillager` | 7 |
| `minecraft:stray` | 8 |
| `minecraft:bogged` | 8 |
| `minecraft:piglin` | 8 |
| `minecraft:breeze` | 10 |
| `minecraft:polar_bear` | 10 |
| `minecraft:cave_spider` | 12 |
| `minecraft:zombified_piglin` | 12 |
| `minecraft:vex` | 12 |
| `minecraft:enderman` | 15 |
| `minecraft:guardian` | 15 |
| `minecraft:blaze` | 15 |
| `minecraft:ghast` | 15 |
| `minecraft:witch` | 18 |
| `minecraft:creeper` | 18 |
| `minecraft:shulker` | 18 |
| `minecraft:wither_skeleton` | 25 |
| `minecraft:hoglin` | 25 |
| `minecraft:vindicator` | 30 |
| `minecraft:zoglin` | 30 |
| `minecraft:piglin_brute` | 35 |
| `minecraft:evoker` | 40 |
| `minecraft:iron_golem` | 45 |
| `minecraft:ravager` | 50 |
| `minecraft:elder_guardian` | 150 |
| `minecraft:warden` | 400 |
| `minecraft:wither` | 300 after first victory |
| `minecraft:ender_dragon` | 500 after first victory |

## First Victory Rewards

| Entity | First total | Later reward |
| --- | ---: | ---: |
| `minecraft:elder_guardian` | 400 | 150 |
| `minecraft:warden` | 1000 | 400 |
| `minecraft:wither` | 1500 | 300 |
| `minecraft:ender_dragon` | 2500 | 500 |

## Anti-Farm

For each player and entity type, Murimblock tracks kills over an approximate 15 minute window:

| Recent kills | Multiplier |
| ---: | ---: |
| 1-10 | 100% |
| 11-20 | 75% |
| 21+ | 50% |

The final multiplier is clamped between 50% and 100%. Rewards use ceiling rounding, so `15 * 0.5` becomes `8`.

## Planned Data Format

Future versions should move this table to NeoForge Data Maps or another datapack-friendly system. A future file may look like:

```json
{
  "values": {
    "minecraft:zombie": {
      "qi_reward": 4
    }
  }
}
```
