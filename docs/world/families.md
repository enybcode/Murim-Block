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
  - later, a **Destiny Talisman** (rare: boss drops, quest rewards; never crafted) gives one more reroll, spent from the
    Profile page; a reroll draws among all houses, the current one included;
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

## Inheritances (proposal, 30 Oct 2026)

> Enzo: "fais-les toutes". Proposal for every great house, to validate before any is granted. Names in game are in
> English and our own (no art or item name from the genre's works). Numbers are a first pass.

Given once, at the draw's ACCEPT (items in the inventory; stats are permanent while the player belongs to the house; a
legendary art joins the arts known in K, Techniques).

### Legendary: the house's martial art and extra stats

| House | Art (our own) | What the art is | Stats |
|---|---|---|---|
| **Jin** | *Frostfall Saber* | A saber of the cold north: wide arcs that slow, a charge that drags a trail of frost, an ultimate that brings a storm of falling blades around the user. | +15 % max Qi, +10 % damage |
| **Namgung** | *Azure Heaven Sword* | The proud sword of the plains: straight, fast thrusts, a guard that answers each blocked blow with one of its own, an ultimate of one blinding cut. | +10 % damage, +10 % attack speed |
| **Dokgo** | *Solitary Edge* | A sword that waits: its techniques are counters, stronger the closer the parry; the ultimate cuts every foe who struck the user in the last seconds. | +25 % parry window, +10 % max health |

The three arts need the combat technique system (techniques still cast nothing): until then the art is given and
shown, its techniques wait.

### Epic: extra stats and rare equipment

| House | Stats | Equipment (rare) |
|---|---|---|
| **Moyong** | +10 % max Qi, 5 % of melee damage taken returned | *Moyong Star Jian*: a jian a little better than iron, faster |
| **Jegal** | +15 % Qi regeneration, +5 % breakthrough chance | *Crane Feather Fan*: a war fan, light hits that push back |
| **Sama** | +2 armour, −15 % knockback taken | *Officer's Lamellar*: a chestplate between iron and diamond |
| **Seomun** | +8 % movement speed, +5 % damage | *Willow Saber*: a curved saber, quick and long-reaching |

### Rare: pills and equipment

| House | Pills | Equipment |
|---|---|---|
| **Dang** | 3 *Antidote Pills* (cure poison and nausea), 2 *Minor Qi Pills* (+30 Qi) | 16 *Dang Needles*: throwing needles, picked back up |
| **Paeng** | 2 *Iron Bone Pills* (Resistance I, 60 s) | *Paeng Broad Saber*: a heavy saber, as strong as iron, slower, more knockback |
| **Hwangbo** | 2 *Tiger Strength Pills* (Strength I, 60 s) | *Iron Fist Wraps*: worn in the off hand, +2 bare-hand damage |
| **Yeon** | 2 *Wind Step Pills* (Speed I, 60 s) | *Yeon Horn Bow* and 32 arrows |
| **Ha** | 4 *Healing Pills* (heal 4 hearts) | *Herbalist's Sickle*: cuts plants and herbs twice as fast |

### Common: nothing

A name, and the whole road to walk.

### To build them

- Stats: attribute modifiers held by the house (max Qi, Qi regen and breakthrough through the cultivation code), safe
  to redo if the house changes on a reroll.
- Items: our own textures (pills as small coloured pills in a paper twist; weapons and the chestplate after a sheet
  validated by Enzo, as for mobs).
- The three arts: after the combat technique system, each with its own design sheet.

## Where it shows

- **Profile:** a FAMILY row (crest, name, rarity colour) above TITLE, the inheritance in the Qi column, and a REROLL
  button while the player holds a talisman.
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
- Not built yet: the Destiny Talisman, the inheritances themselves, the System log page (still placeholders).
