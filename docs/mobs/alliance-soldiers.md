# Soldats de l'Alliance orthodoxe

Les soldats de base de l'Alliance, demandés par Enzo le 15/10/2026 d'après la planche de concepts qu'il a choisie : cinq
sortes, chacune son type d'entité, **toutes en tissu**. Leur armure viendra en items à part ; leurs armes sont celles
de l'Alliance déjà en jeu.

| Soldat (nom en jeu) | Arme | PV | Armure | Visage |
|---|---|---|---|---|
| Alliance Spear Guard | Azure Peak Spear | 30 | 4 | vétéran balafré, moustache et bouc |
| Alliance Sword Disciple | Snow Plum Jian | 30 | 4 | jeune, chignon à couronne d'or, bandeau azur |
| Alliance Archer | arc (vanilla, en attendant un arc de l'Alliance) | 24 | 2 | mèche de côté, yeux plissés |
| Alliance Warrior Monk | ses poings | 36 | 6 | crâne rasé, serein |
| Alliance Sergeant | Snow Plum Jian + Rattan Shield | 40 | 6 | barbe pleine, gros sourcils |

## Allure

Un seul modèle 3D sur le squelette biped d'Epic Fight (`scripts/animation/alliance-soldier-3d.cjs`), une tenue par
sorte (les pièces qu'une sorte ne porte pas sont transparentes dans son atlas) :

- robe azure croisée, col blanc bordé d'or, bandes de tissu aux avant-bras, ceinture de cuir cloutée d'or, jupe de robe
  sur chaque cuisse, pantalon indigo, bottes noires ;
- garde, archer, sergent : veste matelassée ; ceinture blanche (garde) ou or (archer, sergent) ;
- disciple : chignon à couronne d'or, bandeau azur à pans, ceinture blanche à pan ;
- moine : robe ocre, kasaya azur en écharpe, chapelet ;
- visages en 16 x 16 (`hd-face.cjs`), dix expressions chacun (`MobFaces`) : clignements, regards ; en combat les
  sourcils se froncent.

## Comportement

- **Neutres, comme les piglins** (demande d'Enzo, 15/10/2026) : ils n'attaquent personne en premier, ni joueur ni
  monstre. Frappé, un soldat se retourne contre celui qui l'a frappé et appelle tous les soldats autour de lui (puis de
  nouveau toutes les 4 à 6 s tant qu'il le voit). Ils restent en colère 20 à 39 s après le dernier coup, puis oublient.
  Un monstre qui frappe l'un d'eux est combattu de la même façon.
- Ils restent autour de leur poste (10 blocs) et ne disparaissent jamais.
- **Combat (Epic Fight, tout est parable)** :
  - jian (disciple, sergent avec ou sans bouclier) : les coupes de base du Nuage Azur, Drifting Cut, Returning Cut,
    Piercing Mist, et de temps en temps Rising Cloud puis Falling Rain ;
  - lance (garde) : les coups de lance d'Epic Fight ;
  - poings (moine) : Rapid Fists, Whirlwind Kick, et Tiger Step Punch sur qui se tient à 2,5 à 5 blocs ;
  - arc (archer) : il s'approche jusqu'à voir son adversaire à 15 blocs, bande l'arc 1 s, tire, attend 1 s. Ses flèches ne
    se ramassent pas.
- **Butin** (tué par un joueur) : 1 à 3 taëls (+ Butin) ; son arme ou son bouclier une fois sur cent.

## Où les trouver

- **Avant-poste de l'Alliance** (`AllianceOutpost`, `docs/structures`) : deux gardes à la lance juste derrière la
  porte, deux disciples entre les mannequins d'entraînement, l'archer sur la vigie de la tour de guet, le sergent dans
  la grande salle près du commandant, le moine à l'infirmerie.
- Œufs (onglet Murimblock) et `/summon murimblock:alliance_spear_guard` (`_sword_disciple`, `_archer`,
  `_warrior_monk`, `_sergeant`).

## Vérifications

`AllianceSoldierGameTests` : l'arme de chacun (Epic Fight y voit une lance, une épée), aucun ne disparaît ; un disciple
n'attaque ni le bandit ni le joueur à côté de lui ; l'archer frappé par un zombie lui tire dessus ; un garde frappé et le sergent à côté se retournent contre le
joueur et restent en colère comme des piglins ; butin en taëls. `SiteGameTests.allianceOutpostIsWholeAndCarried` compte la garnison. Portraits en vrai client :
`gradlew -PgameTests runMugshotTour`.
