# Arène de l'Alliance orthodoxe — maquettes (proposition à valider par Enzo)

> Demande d'Enzo du 15/10/2026 : « crée une structure pour l'Alliance, une sorte d'arène où il y aura le capitaine
> dedans, une arène dans le style Murim », puis « fais-moi une maquette avant de construire quoi que ce soit ».
>
> **Statut : maquettes voxel, rien n'est construit en jeu.** Même couleurs que l'[avant-poste](alliance-outpost.md) :
> tuiles azur, ornements or, colonnes rouges, murs blancs. Le capitaine est le boss de l'Alliance de la session
> « design » (`murimblock:peak_captain`, pas encore sur `test`), qui se bat avec la
> [Lance du Pic Céleste](../combat/styles/celestial-peak-spear.md).
>
> Maquettes : `node scripts/concepts/render-arena.cjs` (Blockbench isolé, `--port=` ; port 9232 par défaut), scène
> dans `scripts/concepts/arena.js`. 1 voxel = 1 bloc. Les PNG rendus ont été convertis en JPG pour la doc.

![Vue d'ensemble](images/alliance-arena/overview.jpg)

## L'idée

Un **leitai** (擂台), l'estrade de combat des romans Murim : une plateforme carrée surélevée où l'on règle les défis
devant tout le monde ; **qui tombe de l'estrade a perdu**. L'Alliance y éprouve ses disciples et y reçoit les
défis des inconnus. Le capitaine y attend, debout au centre : on monte les marches pour l'affronter.

- **Dimensions** : une cour fermée de **49 × 49 blocs** ; le leitai fait **17 × 17**, son plateau 3 blocs au-dessus
  de la cour. Avec la route et l'arche, le site tient dans 57 × 71 blocs.
- **Où** : comme l'avant-poste, dans n'importe quel biome Murim mais **seulement au centre de la carte ou près
  d'une frontière entre régions** (les terres de l'Alliance). Plus rare que l'avant-poste.

## Le plan

![Plan numéroté](images/alliance-arena/plan.jpg)

| N° | Lieu | Ce que c'est | Qui y est |
|---|---|---|---|
| 1 | Porte | Poteaux rouges, linteau sombre à bandeau or, toit azur, lanterne | Deux gardes à la lance |
| 2 | Arche (paifang) sur la route | Annonce l'arène à 10 blocs de la porte | — |
| 3 | **Le leitai** | 17 × 17, jupe de planches rouges entre poteaux rouges, plateau de pierre lisse, cercle de combat tracé en rouge, une lanterne sur chaque poteau d'angle, marches côté porte | **Le capitaine**, au centre, face à la porte |
| 4 | Tribune ouest | Trois gradins de pierre, une galerie derrière, colonnes rouges, grand toit azur | Disciples spectateurs |
| 5 | Tribune est | Pareille | Disciples spectateurs |
| 6 | Pavillon des juges | Plancher surélevé ouvert sur le leitai, murs blancs, siège du capitaine entre deux blocs d'or, bannières de l'Alliance, deux cloches, la cache du site | — |
| 7 | Mâts à bannières | Quatre mâts aux angles du leitai, bannières azur | — |
| 8 | Braseros | Deux feux sur socles de pierre près de la porte | — |
| 9 | Tambours de guerre | Deux tambours sur pieds, de part et d'autre de l'allée | — |
| 10 | Râteliers | Armes contre le mur sud | — |

## Vues

**Depuis la route** : l'arche, la porte, les gardes.

![Depuis la route](images/alliance-arena/gate.jpg)

**Le leitai** : le cercle rouge, le capitaine (coiffe dorée) face à un challenger, les tribunes, le pavillon des juges au fond.

![Le leitai](images/alliance-arena/leitai.jpg)

## En jeu, une fois validé

- Un site comme les autres (`murimblock:alliance_arena`), construit en code bloc par bloc, trouvé par `/locate`.
- **Matériaux prévus** : briques de pierre et calcite pour les murs, tuiles azur pour les toits, manglier écorcé
  (rouge) pour les colonnes et la jupe du leitai, pierre lisse pour le plateau, tapis rouge pour le cercle, bloc de
  rayon de miel pour l'or, chêne noir pour les poutres et les mâts, cloches, feux de camp, tonneaux.
- **Habitants** : le capitaine au centre du leitai ; deux gardes à la lance à la porte ; deux disciples à l'épée dans
  les tribunes. Tant que le capitaine n'est pas sur `test`, un sergent de l'Alliance tiendrait sa place.

## Questions pour Enzo

- Le capitaine attaque-t-il dès qu'on monte sur le leitai, ou faut-il le défier (clic droit, comme Sensei Yun) ?
- Tomber du leitai fait-il perdre le duel (le capitaine reprend sa place, le joueur doit remonter) ?
- Les disciples des tribunes se battent-ils aussi, ou regardent-ils seulement ?
- Combien d'arènes : une seule par monde (près du centre), ou quelques-unes comme les avant-postes ?
