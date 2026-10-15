# Avant-poste de l'Alliance orthodoxe — maquettes (proposition à valider par Enzo)

> Demande d'Enzo du 15/10/2026 : « fais-moi une maquette d'un avant-poste pour l'Alliance ».
>
> **Statut : maquettes voxel, rien n'est construit en jeu.** Les noms restent à choisir par Enzo. Ce document ne
> reprend rien de la bible du monde au-delà de l'introduction. Mêmes couleurs que la [capitale](alliance-capital.md) :
> tuiles azur et ornements or, colonnes rouges, murs blancs.
>
> Maquettes : `node scripts/concepts/render-outpost.cjs` (Blockbench isolé, port 9232 par défaut, `--port=` pour un
> autre), scène dans `scripts/concepts/outpost.js`. 1 voxel = 1 bloc.

![Vue d'ensemble](images/alliance-outpost/overview.jpg)

## L'idée

Un **fort de garnison** sur une butte au bord d'une route : ce que l'Alliance pose là où elle veut tenir un passage,
une frontière ou une région loin de la capitale. Il sert de relais. On peut s'y reposer, y prendre les missions de
l'Alliance, et sa tour de guet allume un feu de signal qui transmet les nouvelles de poste en poste jusqu'à la
capitale.

- **Dimensions** : environ **53 × 47 blocs** à l'intérieur des murs, sur une butte nivelée à 6 blocs au-dessus de la route ;
  la tour de guet monte à **26 blocs** au-dessus de la cour, plus sa fumée. Tout le site tient dans 110 × 110 blocs avec la
  route et ses abords.
- **Réutilisable** : un même plan peut se poser en plusieurs endroits de la carte (passes de montagne, gués, frontière du
  désert face au Culte), avec quelques variantes : sable et grès au Hexi, neige au nord.

## Le plan

![Plan numéroté](images/alliance-outpost/plan.jpg)

| N° | Lieu | Ce qu'on y fait | Qui y est |
|---|---|---|---|
| 1 | Porte et corps de garde | Entrée ; on y est contrôlé | Deux gardes de l'Alliance |
| 2 | Arche (paifang) au pied de la butte | Marque l'entrée du territoire de l'Alliance sur la route | — |
| 3 | Tour de guet et feu de signal | Le feu relaie les nouvelles vers la capitale ; on voit la région de là-haut | Un guetteur |
| 4 | Salle du commandement | **Les missions de l'avant-poste**, les rapports, le rang | Le commandant |
| 5 | Cour d'entraînement | Mannequins, râteliers ; duels d'entraînement | Disciples, un instructeur |
| 6 | Caserne | Lits, coffres ; point de réapparition pour les membres | Disciples au repos |
| 7 | Écuries | Chevaux de l'Alliance (dans la maquette, des chameaux font office de chevaux) | Palefrenier |
| 8 | Tente d'infirmerie | Soins, élixirs simples | Un médecin |
| 9 | Tableau des avis | **Avis de recherche et petites quêtes** pour tous, membres ou non | Voyageurs |
| 10 | Puits et réserves | Eau, vivres, une charrette | — |
| 11 | Murs, chemin de ronde et tours d'angle | Défense ; gardes sur les murs | Gardes |
| 12 | Salon de thé hors les murs | Halte des voyageurs ; rumeurs de la route | Tenancier, voyageurs |

## Vues

**Depuis la route** : l'arche au pied de la butte, le salon de thé, la porte, les tours d'angle, la fumée du signal.

![Depuis la route](images/alliance-outpost/road.jpg)

**La cour** : la salle du commandement au fond, les mannequins et les disciples, la caserne à gauche, les écuries à
droite, la tour de guet.

![La cour](images/alliance-outpost/yard.jpg)

## Questions pour Enzo

- Le nom de ces avant-postes, et combien en poser sur la carte (un par région ? sur la route vers le Culte ?).
- Peut-on les attaquer ? Une idée : le Culte en tient un aussi, et les deux peuvent se prendre.
- Les membres de l'Alliance y réapparaissent-ils (lit de caserne) ?
