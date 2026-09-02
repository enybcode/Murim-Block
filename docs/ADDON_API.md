# Murimblock Addon API

Murimblock exposes a small Java API under `com.murimblock.api`.

The API is intentionally small because Murimblock is still in the `0.x` series. Breaking changes are possible until the project declares a stable API version.

## Entry Point

```java
import com.murimblock.api.MurimblockApi;
```

Use:

```java
MurimblockApi.qi()
MurimblockApi.cultivation()
```

Avoid depending on:

- `com.murimblock.qi.*`
- `com.murimblock.cultivation.*`
- `com.murimblock.network.*`
- `com.murimblock.client.*`
- attachments and packet classes

## Qi API

```java
double qi = MurimblockApi.qi().getQi(player);
double qiMax = MurimblockApi.qi().getQiMax(player);
```

Server-authoritative mutations require `ServerPlayer`:

```java
MurimblockApi.qi().addQi(serverPlayer, 25.0);
MurimblockApi.qi().removeQi(serverPlayer, 10.0);
MurimblockApi.qi().addQiMax(serverPlayer, 50.0);
MurimblockApi.qi().refillQi(serverPlayer);
```

Qi is clamped by the current Qi Max rules. Addons should not bypass this behavior by touching attachments directly.

## Cultivation API

```java
CultivationSnapshot cultivation = MurimblockApi.cultivation().getCultivation(player);

String realmId = cultivation.realmId();
String stageId = cultivation.stageId();
String displayName = cultivation.displayName();
```

Progression checks:

```java
boolean ready = MurimblockApi.cultivation().canAttemptBreakthrough(player);
Optional<CultivationSnapshot> next = MurimblockApi.cultivation().getNextCultivation(player);
```

Progression mutation currently exposed:

```java
boolean advanced = MurimblockApi.cultivation().advanceAfterSuccessfulBreakthrough(serverPlayer);
```

## Public Events

No custom Murimblock API events are currently exposed.

Current extension guidance:

- use NeoForge events in your addon for your own mechanics;
- use `MurimblockApi.qi()` to read or mutate Qi;
- use `MurimblockApi.cultivation()` to read cultivation state;
- avoid tick-based polling unless your mechanic truly needs it.

Potential future events:

- Qi changed.
- Qi Max changed.
- Cultivation changed.
- Qi charge started or stopped.

These are not implemented yet because the current codebase does not need them and exposing events too early would create avoidable compatibility pressure.
