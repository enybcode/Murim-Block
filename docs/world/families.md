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
| Common | 60 % | 6 ordinary surnames (10 % each) | grey | Nothing: a name, and the whole road to walk. |
| Rare | 25 % | 3 (about 8.3 % each) | blue | Pills and equipment of the house. |
| Epic | 11 % | 2 (5.5 % each) | violet | Extra stats and rare equipment. |
| Legendary | 4 % | 2 (2 % each) | gold | The house's martial art and extra stats. |

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
| **Moyong** | 慕容 | Epic | Qinghai plateau | refined swordsmen of the west, masters of returning a blow | Coming soon |
| **Jegal** | 諸葛 | Epic | Shandong hills | strategists, formations and mechanisms | Coming soon |
| **Dang** | 唐 | Rare | Sichuan (the Tang clan site) | poisons, antidotes and hidden weapons | Coming soon |
| **Paeng** | 彭 | Rare | Central plains | the saber family, raw strength | Coming soon |
| **Hwangbo** | 皇甫 | Rare | Loess plateau | the fist and the iron body | Coming soon |
| Wang, Li, Zhang, Liu, Chen, Zhao | 王 李 张 刘 陈 赵 | Common | anywhere | ordinary families of the Murim | Nothing |

(Chen and Jin share 陳 in Korean; if Jin is confirmed with it, the common Chen becomes another surname.)

## Where it shows

- **Profile:** a FAMILY row (crest, name, rarity colour) above TITLE, the inheritance in the Qi column, and a REROLL
  button while the player holds a talisman.
- **System log:** "[!] You were born into the Namgung family." and every reroll.
- A legendary house's art joins the player's known arts in K, Techniques.

## Later

- Each house's seat on the map, its elder and its family quest.
- Family reputation; marriages and adoption between players.

## First slice (once validated)

House data and save (`FamilyData`, stable ids), the draw with its free reroll and pity, the Destiny window in the
System's pixel art, the Profile row and the log lines. Inheritances stay "Coming soon" until each house is designed.
