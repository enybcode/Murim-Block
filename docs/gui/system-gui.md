# System GUI

The player's menu, styled after the System windows of Solo Leveling: dark translucent panel, glowing cyan
border, bright corner brackets, a title tag across the top border and `[!]` system messages.

Mockups validated by Enzo on 23 Oct 2026 (Design canvas "Murimblock System GUI" on claude.ai). The values in
them (Qi, realms, arts, quests, keys) are placeholders, not decisions.

## Pages

Opened with **K** (`key.murimblock.open_system`, rebindable); K or Escape closes it. The game is not paused.

| Tab | Title tag | Content |
| --- | --- | --- |
| Profile | STATUS | 3D player, identity, HP and Qi bars, Qi panel. No attributes: everything comes from Qi. |
| Techniques | MARTIAL ART | One active art with its passive, techniques and ultimate; other learned arts can be switched to outside combat. |
| Cultivation | CULTIVATION | Sub-tabs Realm (Murim rank holding a 9-stage xianxia realm, dantian, Qi sources, breakthrough trial) and Meridians (cubic body, real acupoints, zoom on the chosen meridian). |
| Info | INFO | Sub-tabs Quests (daily training, main quests), System log, Guide (controls, topics). |

## Code

- `client/gui/SystemLayout`: pure layout math (window 530 x 310 GUI px, shrinks to fit), unit tested.
- `client/gui/SystemTheme`: colours and drawing (window, boxes, title tag, glowing and letter-spaced text, bars).
- `client/gui/SystemButton`: every control, in the styles TAB, SUB_TAB, ACTION and ROW.
- `client/gui/SystemScreen` + `SystemPage`: the window and its four tabs; pages draw inside `SystemLayout.content`.
- `client/gui/page/*Page`: one `SystemPageView` per page, all four done. Sub-tab rows use `SubTabs`. Cultivation's Meridians view draws the body itself (`MeridianBody`) in screen pixels from a
  gliding camera (`MeridianCamera`), so squares stay sharp at any zoom.
- `CultivationData`: realms, the 12 meridians + Small Heavenly Circuit with their real acupoints (positions on a
  160 x 320 front view of the player), and placeholder progress (stage 3, 620 / 1000 Qi, 4 open meridians).
- `InfoData`: placeholder quests and log; in the guide only the System key is read from the real binding, the
  other keys (C, 1-4, R, G, V) are placeholders until those actions exist.
- `client/gui/data/*Data`: what each page shows. `ProfileData`: real name and health; Qi, realm and sect are
  placeholders until the Qi system exists. `MartialArtsData`: the mockup's three example arts, the active one
  kept for the client session only; `inCombat()` is always false until combat exists.
- `client/MurimKeys`: the key binding.

## Checks

- `gradlew build` compiles and runs the unit tests.
- `gradlew runGuiShots` (dev only, gated by the `murimblock.guiShots` property) opens (or creates) the creative
  test world `murim-gui-shots`, shows each page at 1920 x 1080, GUI scale 3, saves
  `run/screenshots/murim_<page>[_<sub-tab>].png` (7 shots), then quits.
