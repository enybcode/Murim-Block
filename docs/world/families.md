# Families (proposal, 30 Oct 2026)

> Status: proposal; nothing is in game. Enzo, 30 Oct 2026:
> - start the family system from scratch (nothing taken from V1), a random draw with rerolls, design doc and mockup
>   first;
> - legendary families give a martial art and extra stats, epic ones stats and rare equipment, rare ones pills and
>   equipment, common ones nothing;
> - the great families are the ones of the genre around *The Legend of the Northern Blade*; every family is "coming
>   soon" for now, and each is designed with Enzo one by one.
>
> Mockup: `docs/gui/boards/families.png` (source `families.html`).

## The idea

A sect teaches whoever it picks; a family hands down by blood. Every player is born into a house of the Murim: on first
joining a world the System draws it. The family gives a surname (shown on the Profile) and, for the great houses, an
**inheritance** whose size follows the house's rarity.

## Rarities and what each gives

| Rarity | Chance | Houses | Colour | Inheritance |
|---|---|---|---|---|
| Common | 60 % | 8 ordinary surnames (7.5 % each) | grey | Nothing: a name, and the whole road to walk. |
| Rare | 25 % | 5 (5 % each) | blue | Pills and equipment of the house. |
| Epic | 11 % | 4 (2.75 % each) | violet | Extra stats and rare equipment. |
| Legendary | 4 % | 3 (about 1.33 % each) | gold | The house's martial art and extra stats. |

The exact content of each house (which pills, which stats, which art) is designed with Enzo, one house at a time; until
then each shows **Coming soon**.

## The draw

- **When:** the first time a player joins a world, before they move, the System opens the **Destiny** window. It also
  opens from the Profile page while the player has a reroll to spend.
- **How:** a seal spins and lands on a house; the window shows its name, rarity, crest and inheritance.
- **Rerolls:**
  - one free reroll in the first Destiny window: **ACCEPT** keeps the house, **REROLL** draws again (the second draw is
    final);
  - a **Destiny Talisman** (Enzo, 30 Oct: an item rather than a command, crafted from every ore but netherite) gives
    one more reroll: used (right click), it is broken and draws again among all houses, the current one included;
  - **pity:** after 10 draws in a row without an epic or legendary house, the next one is epic or better.
- **One house per player**, saved with their data under a stable id (like arts). Kept on death. In multiplayer each
  player draws their own.

## The houses

The great houses are the ones of the Korean murim genre that *The Legend of the Northern Blade* moves among (the
"great families" trope): their surnames are the genre's, written in the Korean romanisation the manhwa uses. Nothing
else is taken from the work: no characters, story or events. Seats are proposals on the [Murim map](map.md).

| House | Characters | Rarity | Seat (proposal) | Known for, in the genre | Inheritance |
|---|---|---|---|---|---|
| **Jin** | 陳 (to confirm) | Legendary | North Sky (the reserved site, north) | the fallen house of the north, its blade art hidden for years | Coming soon |
| **Namgung** | 南宮 | Legendary | Hubei waterlands | the great sword family of the plains, proud and old | Coming soon |
| **Dokgo** | 獨孤 | Legendary | Western snow peaks | the lone swordsmen, one heir a generation | Coming soon |
| **Moyong** | 慕容 | Epic | Qinghai plateau | refined swordsmen of the west, masters of returning a blow | Coming soon |
| **Jegal** | 諸葛 | Epic | Shandong hills | strategists, formations and mechanisms | Coming soon |
| **Sama** | 司馬 | Epic | Central plains | old officials' blood, scholars and generals | Coming soon |
| **Seomun** | 西門 | Epic | Southeast coast | wealth of the coast, swift swords | Coming soon |
| **Dang** | 唐 | Rare | Sichuan (the Tang clan site) | poisons, antidotes and hidden weapons | Coming soon |
| **Paeng** | 彭 | Rare | Central plains | the saber family, raw strength | Coming soon |
| **Hwangbo** | 皇甫 | Rare | Loess plateau | the fist and the iron body | Coming soon |
| **Yeon** | 燕 | Rare | Frozen steppe | riders and archers of the north | Coming soon |
| **Ha** | 夏 | Rare | Rice terraces | healers and herb growers | Coming soon |
| Wang, Li, Zhang, Liu, Zhao, Sun, Wu, Zhou | 王 李 张 刘 赵 孙 吴 周 | Common | anywhere | ordinary families of the Murim | Nothing |

## Inheritances

Enzo, 30 Oct 2026: every house stays **Coming soon** for now; the houses will be designed later, but the system that
grants an inheritance must be ready (see Built). A first draft of all twelve was written that day and set aside (git
history, commit 12d09291); nothing of it is decided.

## Where it shows

- **Profile:** a FAMILY row (crest, name, rarity colour) above TITLE, the inheritance in the Qi column.
- **System log:** "[!] You were born into the Namgung family." and every reroll.
- A legendary house's art joins the player's known arts in K, Techniques.

## Later

- Each house's seat on the map, its elder and its family quest.
- Family reputation; marriages and adoption between players.

## Built (30 Oct 2026)

Enzo asked for twenty houses (three legendary, then four, five, eight) and for the draw, its assignment and its display
to be built with every inheritance "coming soon": nothing is granted but the house's name.

- `family/Family` (the houses, rarities, stable ids), `FamilyDraw` (shares and pity, unit tested in
  `FamilyDrawTest`), `FamilyState` (the `murimblock:family` attachment: house, free reroll, pity; kept on death).
- `FamilyEvents`: on first login the server draws a house and opens the Destiny window; ACCEPT keeps it, REROLL
  spends the free reroll (that draw is final); the System says "[!] You were born into the X family." Commands (op):
  `/family draw`, `/family set <house>`, `/family reset` (a new draw with its free reroll).
- `client/family/DestinyScreen`: the seal spins through the houses and lands; the house's name, rarity, inheritance
  ("Coming soon", or "No inheritance" for a common house), ACCEPT and REROLL (1) / FINAL. Escape leaves it for the
  next login.
- Profile: a FAMILY row at the top (a diamond and the name in the rarity's colour); hovering it shows the house, its
  rarity, its seat and what its rarity hands down, "Coming soon".
- Inheritance system (Enzo, 30 Oct: houses later, system ready): `family/Inheritance` is what a house hands down
  (attribute bonuses, items, an art id, or COMING_SOON / NONE); `Inheritance.of(house)` is the one place to fill in
  when a house is designed. `InheritanceGrants.apply` runs when the house is settled (ACCEPT, the final reroll,
  `/family set`, `/family draw`), at login and respawn: the old house's bonuses come off (permanent modifiers
  `murimblock:family`, their attributes saved as `stats`), the new ones go on, items and art are given once per house
  (`gifts_for`). The art goes to `InheritanceGrants.teachArt`, which only logs until the server has an art system.
  Destiny and the Profile tooltip list each bonus, item and art ("+2 Armor", "3 × Bread", "Art: ..."), or the rarity's
  promise and "Coming soon". `runGuiShots` gives every house a test inheritance (+2 armour, 3 bread, an art), checks
  it is granted after ACCEPT and taken back once the real ones return, and logs PASS or FAIL.
- Destiny Talisman (`murimblock:destiny_talisman`, `DestinyTalismanItem`, a white jade bi disc with a gold ring and a red cord,
  board `docs/items/boards/destiny-talisman.png` (B, chosen 31 Oct), texture by `tools/textures/destiny-talisman.cjs`): shapeless recipe of coal, copper, iron, gold, redstone, lapis, diamond,
  emerald and quartz; right click breaks it (totem sound), draws a final new house (`FamilyEvents.redraw`: old bonuses
  off, new inheritance granted, the Destiny window shows it with FINAL); refused while the first window's free reroll
  waits. The `/family` commands stay for ops and the dev checks; `runGuiShots` burns one and logs PASS or FAIL.
- Not built yet: each house's inheritance, the System log page (still placeholders).
