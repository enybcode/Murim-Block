# Le Seigneur des bandits (Bandit Warlord)

> Demande d'Enzo du 16/10/2026 : le boss du grand camp de bandits ([bandit-camp.md](../structures/bandit-camp.md)),
> « inspiré de Nappa, une montagne de muscles qui se bat avec ses poings » ; un nouveau boss ; il lâche le coffre du
> chef, un art martial et du Qi max ; une maquette d'abord.
>
> **Statut : en jeu depuis le 16/10/2026** (voir « En jeu » ci-dessous). Un seigneur des bandits à nous, dans l'esprit Murim, pas une copie du
> personnage. Nom provisoire : **Bandit Warlord** (Seigneur des bandits), à choisir par Enzo.
>
> Fichiers : `node scripts/animation/bandit-warlord-3d.cjs` (Blockbench isolé, `--port=`) écrit le projet
> `art/blockbench/bandit_warlord.bbmodel` (modèle et mouvements, à ouvrir dans Blockbench, onglet Animer) et filme les
> images et GIF de cette fiche (`docs/mobs/images/warlord-*`). Il est fait sur le squelette humain d'Epic Fight, comme le
> Bandit Chief : la maquette passera telle quelle dans le jeu.

![Le Seigneur des bandits](images/warlord-three.png) ![De face](images/warlord-front.png)

## L'allure

- **Une montagne de muscles**, dessinée **deux fois la taille d'un homme** en jeu (environ 3,6 blocs de haut) : un
  bandit ordinaire lui arrive à la taille.
- **La tête** : crâne rasé et luisant, une épaisse **moustache noire en fer à cheval** qui tombe jusqu'au menton,
  des sourcils lourds sur de petits yeux durs, un anneau d'or à l'oreille. Un cou aussi large que la tête.
- **L'armure** : une **cuirasse de fer noir** bordée d'or, une **tête d'ours en bronze** sur la poitrine, un ventre de
  lamelles pourpres, d'immenses **épaulières évasées de laque pourpre** bordées d'or.
- **Les bras** : énormes et nus, des brassards de fer, **les poings bandés de tissu blanc**. Il n'a pas d'arme.
- Une **ceinture d'étoffe rouge** nouée devant, un pantalon sombre et ample, des bottes ferrées.

![Sa taille, à côté d'un bandit](images/warlord-size.png)

![Le visage](images/warlord-face.png) ![De dos](images/warlord-back.png)

## Ses mouvements

| Mouvement | Durée | Ce qu'il fait | Proposition en jeu |
|---|---|---|---|
| **Repos** | 3 s | Il respire, roule des épaules, tourne la tête | — |
| **Marche** | 1,2 s | Lourde, le tronc qui roule, les bras qui balancent | — |
| **La chute** (`sky_fall`) | 3,3 s | Il tombe du ciel en météore, genoux remontés, atterrit poing au sol un genou à terre, se relève et rugit | Son entrée, en cinématique, une fois les 30 bandits morts, et l'ultime de son art : l'impact creuse le camp en cratère |
| **Le rugissement** (`roar`) | 2,6 s | Accroupi, il se lève, frappe ses poings l'un contre l'autre et rugit, les bras levés | En seconde phase |
| **Poing écrase-montagne** (`mountain_crusher`) | 1,1 s | Un direct du droit : il arme, fait un pas et enfonce le poing | Attaque de base, gros recul |
| **Brise-terre** (`earth_breaker`) | 1,7 s | Les deux poings levés au-dessus de la tête, abattus au sol | Onde de choc autour de lui (comme le saut du Bandit Chief) |
| **Charge du taureau** (`bull_charge`) | 1,6 s | L'épaule baissée, une course de quatre blocs, l'impact | Pour rattraper un joueur qui recule ; renverse |
| **Brise-ciel** (`sky_breaker`) | 1,15 s | Accroupi bas, un uppercut qui monte en se dressant sur la pointe des pieds | Projette la cible en l'air |

![La chute](images/warlord-sky_fall.gif) ![Le rugissement](images/warlord-roar.gif) ![Poing écrase-montagne](images/warlord-mountain_crusher.gif)

![Brise-terre](images/warlord-earth_breaker.gif) ![Brise-ciel](images/warlord-sky_breaker.gif)

![Charge du taureau, de profil](images/warlord-bull_charge-side.gif) ![Repos](images/warlord-idle.gif) ![Marche](images/warlord-walk.gif)

Profils : [poing](images/warlord-mountain_crusher-side.gif), [brise-terre](images/warlord-earth_breaker-side.gif),
[brise-ciel](images/warlord-sky_breaker-side.gif).

## En jeu (16/10/2026, « reprends le boss »)

![Sa chute, filmée en jeu](images/warlord-entrance.gif)

![Il descend, vu d'en haut](images/warlord-game-fall.jpg) ![Il se relève et rugit](images/warlord-game-roar.jpg)

- **`murimblock:bandit_warlord`** (`BanditWarlordEntity`, `BanditWarlordPatch`, `BanditWarlordAnimations`,
  `client/BanditWarlordRenderer`) : son modèle 3D et ses mouvements sortent de `bandit-warlord-3d.cjs` (maillage
  `entity/bandit_warlord`, animations `biped/murim/warlord_*`), dessiné deux fois la taille d'un homme (3,9 blocs).
  Œuf « Bandit Warlord Spawn Egg » et `/summon` (pas de cratère alors).
- **Force** : 400 PV, armure 12 (ténacité 4), attaque 12, presque pas de recul, ne brûle pas, ne disparaît jamais.
  Barre de boss **pourpre**. Sous la moitié de sa vie, une fois : il rugit et va 25 % plus vite.
- **Ses coups**, tous parables, ceux du Poing Brise-Montagne : Poing écrase-montagne (×1,0, renvoie, jusqu'à 4 blocs),
  Brise-terre (×1,3 et une onde de choc de 5,5 blocs autour de lui, ×0,7, qui projette en l'air ; recharge 8 s), Charge du
  taureau (×1,1, renverse, de 4 à 10 blocs ; recharge 10 s), Brise-ciel (×1,4, la cible projetée à sept blocs de haut ;
  recharge 12 s).
- **Son entrée** (`WarlordEntrance`) : au 35e mort du camp, le tambour bat trois fois (0,5 s, 1,1 s, 1,7 s), puis il
  tombe de 70 blocs au-dessus de la place (2,5 s), en **météore** : du feu autour de lui, une traînée de flammes et de
  fumée ; il tombe environ 3 s. Intouchable pendant la chute et l'atterrissage. À l'impact : explosion, flammes, poussière,
  tonnerre ; tout ce qui est à 10 blocs est **projeté** (sans dégâts : on ne pouvait pas l'éviter) ; il atterrit poing au
  sol, se relève et rugit (2,8 s), puis le combat commence. Le **cratère** se creuse à l'impact.
- **La cinématique** (`client/WarlordCinematic`, protocole réseau 20), pour les joueurs du camp, environ 9 s, sans
  bouger ni être touchés (Résistance V et Chute lente) : le tambour de près, la caméra qui monte vers le ciel, un point
  rouge qui grossit, puis **la caméra descend avec lui, juste au-dessus de son épaule, le camp qui fonce vers lui**
  (Enzo : « un vrai gros plan, vu d'en haut, comme la météorite de One Punch Man »), le flash blanc et la secousse de
  l'impact vus du bord, puis lui en contre-plongée qui se relève et rugit, son nom en grand : « Bandit Warlord — Lord of
  the Mountain Bandits ».
- **Butin** : 2 à 4 yuanbao d'or, 12 à 20 taels d'argent, une pilule de percée (tué par un joueur), de la viande ; le
  coffre du chef reste dans la grande salle (le cratère ne casse aucun coffre). **+40 Qi Max** à la première victoire
  (récompense de Qi de boss, 150). Le rouleau du Poing Brise-Montagne viendra avec l'art (pas encore fait).
- **Vérifié** : GameTests `BanditWarlordGameTests` (boss à lui, chute intouchable et impact qui projette, onde de choc,
  cratère qui garde les coffres, chute d'un camp notée une fois) et `BanditCampCountGameTests` ; visite en vrai client
  `gradlew -PgameTests runWarlordTour` (camp construit sur un plateau, entrée filmée, cratère, chaque mouvement :
  `build/warlord-tour-run/screenshots`). **Pas encore vu** : un vrai combat à la main, ni à deux joueurs.

## Proposition pour le jeu (la maquette)

- **Apparition** (16/10, demandes d'Enzo) : le camp compte ses 30 bandits ; au dernier mort, le tambour de guerre
  résonne trois fois et **il tombe du ciel comme un météore de feu**, **en cinématique** pour les joueurs du camp :
  l'impact **change le camp en cratère** pour de bon, une arène de PvP ensuite
  ([la chute et le cratère](../structures/bandit-camp.md#la-chute-et-le-cratère-maquette-du-1610-à-valider)).
  C'est l'ultime de son art. Il ne revient pas : un camp libéré le reste.

![Sa chute sur le camp](../structures/images/bandit-camp/fall.jpg)
- **Force** : un boss au-dessus du Bandit Chief et de Sensei Yun, par exemple **400 PV**, des coups à 10-16 dégâts,
  **tous parables** ; une seconde phase sous la moitié de sa vie (plus rapide, la charge plus souvent). Barre de boss
  pourpre et or.
- **Butin** (choix d'Enzo) : **le coffre du chef** de la grande salle s'ouvre à sa mort ; **un art martial**, le [Poing Brise-Montagne](../combat/styles/mountain-shattering-fist.md) (zone et destruction, ses propres mouvements ; lâché à coup sûr comme le manuel de Sensei Yun) ;
  **du Qi max** à la première victoire (par exemple +40, plus que Sensei Yun).

## Questions pour Enzo

- Son nom (Bandit Warlord ? un nom chinois ?) et ses couleurs (pourpre et or, ou plus sombre) ?
- Sa vie et ses dégâts : plus dur que Sensei Yun ?
