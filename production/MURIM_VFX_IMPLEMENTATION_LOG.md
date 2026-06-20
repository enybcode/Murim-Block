# Murim VFX Implementation Log

## Added Now

The mod now has a reusable Java VFX helper:

- `src/main/java/net/mcreator/murimblock/util/MurimVFX.java`

It uses vanilla/NeoForge-safe particles only, so there are no new external dependencies yet.

## Effects Wired

- Aura activation:
  - ignition ring;
  - small flash;
  - inner bright particles.
- Aura deactivation:
  - collapsing smoke/soul pulse.
- Aura active tick:
  - low-cost rising spiral around the player;
  - timed soul drift.
- First breakthrough:
  - vertical ascension column;
  - expanding rings;
  - flash and shockwave-style particles.
- Qi gained from killing:
  - visible energy stream from victim to cultivator;
  - stronger effect for aggressive mobs.
- Pill refinement:
  - soft recovery sparkle;
  - small qi ring at the feet.

## Reusable Presets Not Yet Wired

- `swordArc`: forward crescent slash for sword techniques.
- `impactBurst`: compact hit flash for technique damage.
- `demonicGuardPulse`: shield/parry pulse.

These are ready to call from future procedures or Java event hooks.

## Why This Path

This keeps the current mod stable:

- no new items;
- no new mobs;
- no new custom particle registry yet;
- no GeckoLib dependency yet;
- no dynamic-light dependency yet.

It still gives us a real VFX foundation that can be reused from MCreator procedures.

## Next Quality Steps

1. Add custom particle textures for qi, demonic qi, sword air cuts, dust, and sparks.
2. Register custom particle types if vanilla particles are not enough.
3. Wire `swordArc` to First Sword Art when the technique logic is ready.
4. Add sound events for ignition, slash, breakthrough, and impact.
5. Add camera shake only for major techniques and only client-side.
6. Test in the MCreator client and tune density/performance.
