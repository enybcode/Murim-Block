# Addon Guide

This guide explains how another NeoForge mod should depend on Murimblock conceptually.

## Dependency

In the addon metadata, declare Murimblock as a dependency:

```toml
[[dependencies.your_addon_id]]
modId="murimblock"
type="required"
versionRange="[0.1.0,)"
ordering="AFTER"
side="BOTH"
```

The exact Gradle dependency coordinates depend on how Murimblock is published. Until a Maven artifact exists, addon developers can use a local Maven publication or composite build during development.

## Allowed Packages

Use:

```text
com.murimblock.api
com.murimblock.api.qi
com.murimblock.api.cultivation
com.murimblock.api.combat
```

Avoid:

```text
com.murimblock.qi
com.murimblock.cultivation
com.murimblock.network
com.murimblock.command
com.murimblock.client
```

Those implementation packages can change while Murimblock is in the `0.x` series.

## Reading Qi

```java
double qi = MurimblockApi.qi().getQi(player);
double qiMax = MurimblockApi.qi().getQiMax(player);
```

## Awarding Qi From An Addon

Run this on the logical server:

```java
if (player instanceof ServerPlayer serverPlayer) {
    MurimblockApi.qi().addQi(serverPlayer, 25.0);
}
```

Do not modify Murimblock attachments directly.

## Reading Cultivation

```java
CultivationSnapshot cultivation = MurimblockApi.cultivation().getCultivation(player);

if ("qi_guiding".equals(cultivation.realmId())) {
    // Addon-specific behavior.
}
```

## Reading Combat Mode

```java
boolean inCombat = MurimblockApi.combat().isInCombatMode(player);
```

To change combat mode, run on the logical server:

```java
if (player instanceof ServerPlayer serverPlayer) {
    MurimblockApi.combat().toggleCombatMode(serverPlayer);
}
```

## Future Mob Reward Integration

Mob Qi rewards are currently implemented in Java. The planned direction is a NeoForge data-driven model so addons can define rewards for their own mobs without modifying Murimblock source code.

Until that system exists, addons should award extra Qi through `MurimblockApi.qi()` from their own server-side kill logic.
