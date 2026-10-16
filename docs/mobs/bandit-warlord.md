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

## Seconde phase : l'arène dans le ciel (maquette du 16/10, à valider)

> Demandes d'Enzo : « une arène dans le ciel, une espèce de météore creusé » ; « fais plus grand, le fight aura de gros
> dégâts de zone, il faut vraiment une grosse zone » ; « et même en profondeur ».
>
> `node scripts/concepts/render-warlord-sky-arena.cjs --port=<Blockbench isolé>` (voxels : `warlord-sky-arena.js`) rend
> les vues en PNG dans `images/warlord-sky-arena/` ; celles de cette fiche sont passées en JPEG.
>
> **Ces images sont la maquette.** Le modèle réel à construire, en vrais blocs, est
> [`art/structures/warlord-sky-arena/warlord_sky_arena.schem`](../../art/structures/warlord-sky-arena/README.md)
> (WorldEdit Sponge v3, blocs vanilla), généré par `node scripts/worldgen/warlord-sky-arena.cjs`.

![Le météore creusé au-dessus de la mer de nuages](images/warlord-sky-arena/overview.jpg)

- **Un météore flottant** d'environ **170 blocs de large** et 150 de haut, en basalte noir et roche brûlée, veiné de
  lave qui rougeoie ; dessous, un cône rugueux d'où pendent des coulées de roche en fusion. Des blocs arrachés par la
  chute dérivent autour. Très loin dessous, une mer de nuages.
- **Creusé en cuvette** : un **sol de combat plat de près de 100 blocs de large** (rayon 48), pour ses gros coups de
  zone, puis des **parois qui montent d'environ 40 blocs** (26 à 47 selon l'endroit) jusqu'au bord (rayon 72). On voit tout le combat depuis le fond, et
  personne ne peut se cacher derrière quoi que ce soit : seuls quelques rochers bas sont épars sur le sol.
- **Au centre, la cicatrice de son atterrissage** : obsidienne et lave, et **14 fissures rougeoyantes** qui en
  rayonnent sur le sol.
- **Autour du sol**, 12 braseros de bronze. **Sur le bord**, 34 pics de roche, certains penchés vers l'extérieur, et
  6 grandes bannières pourpre et or du Seigneur.

![Depuis le bord](images/warlord-sky-arena/rim.jpg)
![Sur le sol de combat, à hauteur de joueur](images/warlord-sky-arena/floor.jpg)

![Plan : 1 la cicatrice, 2 le sol de combat, 3 un brasero, 4 les parois de la cuvette, 5 les pics et les bannières du bord](images/warlord-sky-arena/plan.jpg)
![Dessous](images/warlord-sky-arena/below.jpg)

**Propositions pour le jeu (à décider)** :
- **Comment on y monte** : sous la moitié de sa vie, il rugit, frappe le fond du cratère d'un Brise-ciel, et la
  cinématique emporte tous les joueurs du combat avec lui dans le ciel ; ils retombent dans la cuvette du météore.
- **Où il est** : construit dans le ciel au-dessus du camp, vers y 200 à 300 (la limite est 320), le temps du combat. Il
  disparaît après, ou il reste comme une curiosité visible de loin.
- **Tomber du bord** : on retombe dans le cratère du camp, avec une chute amortie ou une vraie mort, à choisir.
- **Le sol de combat** pourrait se fissurer de plus en plus à chaque Brise-terre (les fissures s'élargissent), jusqu'à
  la fin du combat.

## Questions pour Enzo

- Son nom (Bandit Warlord ? un nom chinois ?) et ses couleurs (pourpre et or, ou plus sombre) ?
- Sa vie et ses dégâts : plus dur que Sensei Yun ?
