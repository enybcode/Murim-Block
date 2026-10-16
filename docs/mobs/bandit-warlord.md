# Le Seigneur des bandits — maquette (proposition à valider par Enzo)

> Demande d'Enzo du 16/10/2026 : le boss du grand camp de bandits ([bandit-camp.md](../structures/bandit-camp.md)),
> « inspiré de Nappa, une montagne de muscles qui se bat avec ses poings » ; un nouveau boss ; il lâche le coffre du
> chef, un art martial et du Qi max ; une maquette d'abord.
>
> **Statut : maquette seulement, rien en jeu.** Un seigneur des bandits à nous, dans l'esprit Murim, pas une copie du
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

## Proposition pour le jeu

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
