# Families (proposal, 30 Oct 2026)

> Status: proposal to validate; nothing is in game. Enzo, 30 Oct 2026: start the family system from scratch (nothing
> taken from V1), a random draw with rerolls, design doc and mockup first. Mockup:
> `docs/gui/boards/families.png` (source `families.html`).

## The idea

A sect teaches whoever it picks; a family hands down by blood. Every player is born into one of the Murim's houses: on
first joining a world the System draws a family for them. The family gives a surname (shown on the Profile) and a
**heritage**, one small permanent bonus in the house's style. It colours the start of a character; it never replaces
what the player earns by playing (arts, realms, meridians).

## The draw

- **When:** the first time a player joins a world, before they move, the System opens the **Destiny** window. It also
  opens from the Profile page while the player has a reroll to spend.
- **How:** a seal spins and lands on a family; the window shows its name, zone, rarity, crest and heritage.
- **Rarities:**

  | Rarity | Chance | Families | Colour |
  |---|---|---|---|
  | Common | 60 % | 6 (10 % each) | cyan |
  | Rare | 32 % | 4 (8 % each) | violet |
  | Legendary | 8 % | 2 (4 % each) | gold |

- **Rerolls:**
  - one free reroll in the first Destiny window: **ACCEPT** keeps the family, **REROLL** draws again (and the second
    draw is final);
  - later, a **Destiny Talisman** (rare: boss drops, quest rewards; never crafted) gives one more reroll, spent from the
    Profile page. A reroll always draws among all families, the current one included;
  - **pity:** after 10 draws in a row without a legendary, the next draw is rare or better.
- **One family per player**, saved with their data under a stable id (like arts). Kept on death. In multiplayer each
  player draws their own.

## The twelve families

One or two per zone of the [Murim map](map.md), named for the land they hold. Names in game are in English; Chinese
characters are kept in the crest.

| Family | Zone | Rarity | Heritage | Kind | What it does |
|---|---|---|---|---|---|
| **Wang** 王 | Central plains | Common | *Rooted Stance* | Passive | −20 % knockback taken. |
| **Ma** 马 | Northern wastes | Common | *Born in the Saddle* | Passive + item | A saddle at start; a ridden horse runs 15 % faster. |
| **Zhou** 周 | Hubei waterlands | Common | *River Lungs* | Passive | Swims 30 % faster, holds breath twice as long. |
| **Hu** 胡 | Loess plateau | Common | *Loess Hands* | Passive | Digs earth, sand and loess 25 % faster; +1 bare-hand damage. |
| **Liang** 梁 | Honam rice terraces | Common | *Harvest Blessing* | Passive | Eating gives Regeneration I for 5 s. |
| **Kong** 孔 | Shandong hills | Common | *Scholar's Mind* | Passive | +10 % art mastery gained. |
| **Tang** 唐 | Sichuan (the Tang clan site) | Rare | *Tang Darts* | Item | 16 throwing darts at start, picked back up after a throw; the clan restocks them (a recipe). |
| **Duan** 段 | Unnam | Rare | *One Finger Qi* | Technique | A thin bolt of Qi from the finger, 12 blocks, 15 Qi, 8 s; its own seal beside the art's on the combat bar. |
| **Murong** 慕容 | Qinghai plateau | Rare | *Return to Sender* | Passive | 10 % of melee damage taken is returned to the striker. |
| **Shangguan** 上官 | Southeast coast | Rare | *Coastal Fortune* | Item | 30 taels and a fine sword at start. |
| **Zhuge** 诸葛 | Chang-An (loess) | Legendary | *Eight Gates* | Technique | A formation drawn on the ground (6 blocks) that slows foes inside for 6 s; 30 Qi, 30 s. |
| **Nangong** 南宫 | The sacred mountain | Legendary | *Heaven's Favour* | Passive | +10 % maximum Qi and +5 % breakthrough chance. |

Eight passives, two items, two techniques; the numbers are a first pass, to tune in game. The legendaries are the ones
worth a talisman; the commons are pleasant, never decisive.

## Where it shows

- **Profile:** a FAMILY row (crest, name, rarity colour) above TITLE, and the heritage in the Qi column; a REROLL
  button while the player holds a talisman.
- **Combat bar:** a family technique (Duan, Zhuge) gets a small seal of its own, left of the art's seals, on its own key
  (a seventh technique key, rebindable).
- **System log:** "[!] You were born into the Zhou family." and every reroll.

## Later (not in this first slice)

- Each family's seat on the map (beside the reserved faction sites), its elder and its family quest.
- Family reputation; marriages and adoption between players.
- A family art (a full martial art) taught only to its members, at a high realm.

## First slice (once validated)

Family data and save (`FamilyData`, stable ids), the draw with its free reroll and pity, the Destiny window in the
System's pixel art, the Profile row and the eight passive heritages. Items and the two techniques come after (the
techniques wait for the combat technique system).
