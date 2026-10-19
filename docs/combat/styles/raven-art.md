# L'Art du Corbeau messager (鸦使术, *Raven Messenger Art*)

> **État : conception à valider par Enzo (19/10/2026), rien en jeu.** Demande d'Enzo : « l'art du corbeau, inspiré du
> château de l'infini et du Shiro Shiro no Mi : un familier qui se déplace avec nous, avec un monde intérieur », « on
> peut aussi s'en servir comme base ». Parmi trois directions, il a choisi **le Corbeau messager** (mobilité,
> éclaireur), et parmi trois modèles le **Corbeau d'ombre messager** (`scripts/concepts/raven-familiars.cjs`). Puis :
> « tout doit être basé autour du changement de dimension ». Inspiré de
> ces idées sans rien en copier (ni formes, ni noms, ni scènes). Noms en jeu en anglais.

## L'idée

Un corbeau noir aux reflets bleu nuit, yeux d'ambre, l'étui à message cerclé d'or sur le dos, qui ne te quitte pas : il
vole autour de toi, se pose sur ton épaule, part en éclaireur. Derrière lui s'ouvre **les Couloirs du Corbeau**, un monde
intérieur de couloirs et de salles d'ombre qui relient les **perchoirs** que tu poses dans le monde. On y voyage d'un
perchoir à l'autre, et on peut en faire **sa base** : y construire, y ranger ses coffres, s'y reposer.

- **Niveau proposé : intermédiaire** (Qi Condensation), comme la Lance du Conquérant. *(À confirmer.)*
- **Arme proposée : l'éventail de fer** (déjà en jeu) : on dirige le corbeau d'un geste, les plumes partent de
  l'éventail. *(À confirmer : ou mains nues, ou n'importe quelle arme.)*
- **Source proposée : un nouveau mob, le Maître aux corbeaux**, un ermite dans une tour isolée, entouré de corbeaux ; on
  réussit son épreuve (ou on le bat) et il confie un œuf de corbeau et le rouleau. *(À confirmer.)*

## Le principe : passer d'un monde à l'autre

Tout l'art repose sur **les Couloirs du Corbeau**, le monde intérieur. Le corbeau ouvre des **portes** entre le monde et
les couloirs : on y fait entrer et sortir **soi-même, les ennemis, les alliés et même les projectiles**. Une porte est
un cercle d'ombre bordé d'ambre, d'où sortent des plumes ; on la voit s'ouvrir (on peut l'éviter en s'écartant).

## Le familier

- Apparaît dès qu'on connaît l'art et que c'est le style principal ; il suit le joueur partout (même dimension), se pose
  sur son épaule à l'arrêt, vole autour en combat. Il ne meurt pas : frappé, il passe dans les couloirs et revient 10 s
  plus tard.
- C'est lui qui ouvre les portes : il vole jusqu'à l'endroit voulu (16 blocs au plus) et l'ouverture part de lui.

## Le passif : « Between Worlds » (Entre les mondes)

- **Ta roulade passe par les couloirs** : tu disparais au début de l'esquive et tu réapparais à son bout, 2 blocs plus
  loin que d'habitude, invulnérable pendant le passage.
- Chaque M1 qui touche ouvre une petite **faille** sur la cible (3 au plus) ; à 3, le M1 suivant la **fait basculer**
  dans les couloirs une demi-seconde et elle retombe derrière toi, désorientée (lenteur 1 s).

## Les 5 techniques

| # | Technique | Effet |
|---|---|---|
| 1 | **Raven Gate** (Porte du corbeau) | Une porte s'ouvre sous l'ennemi visé : il tombe dans les couloirs, disparaît 4 s, puis le corbeau le **relâche de haut** (10 blocs) au-dessus de l'endroit où il était : il retombe et prend sa chute. |
| 2 | **Halls Step** (Pas des couloirs) | Tu entres dans une porte et **ressors là où vole le corbeau** (16 blocs) ou là où tu regardes ; tenu, tu choisis l'endroit. |
| 3 | **Exchange** (Échange) | Deux portes s'ouvrent à la fois : **tu échanges ta place avec l'ennemi visé**, ou échange un ennemi avec un allié (accroupi). |
| 4 | **Swallowing Gate** (Porte qui avale) | Une porte devant toi pendant 3 s **avale tous les projectiles** (flèches, boules d'eau, rayons) ; relâchée, elle **les renvoie** vers celui qui les a tirés. |
| 5 | **Closed Room** (Salle close) | Toi et un ennemi êtes **aspirés dans une salle fermée** des couloirs : un duel de 20 s où personne d'autre ne peut entrer ; vous ressortez à vos places à la fin (ou quand l'un tombe à terre). **Accroupi : tu rentres chez toi**, dans ton monde personnel (ci-dessous) ; tu en ressors plus tard là où tu étais entré. |

- **Secret « Return to Sender »** (Retour à l'envoyeur) : après une parade parfaite, **l'attaquant est avalé** par une
  porte et ressort juste derrière toi, dos tourné, étourdi 1 s.
- **Ultime « The Halls Unfold »** (Les couloirs se déploient) : le monde intérieur **se superpose** au monde sur
  30 blocs : tous les ennemis sont aspirés et **dispersés chacun dans une salle** des couloirs ; tu vas de salle en
  salle par les portes pour les affronter un par un pendant 30 s ; à la fin, tous ressortent là où ils étaient.
- **Finish « Last Message »** : une porte s'ouvre sous l'adversaire à terre ; il tombe dans les couloirs et la porte se
  referme sur lui.

## Le monde intérieur : les Couloirs du Corbeau, ton monde personnel

> Enzo, 19/10/2026 : « si on sneak et qu'on envoie Closed Room, tu te téléportes dans ton monde ; tu peux emmener tes
> alliés avec un objet (choisi plus tard) ; et tu fais un monde perso. »

- **Y aller** : Closed Room lancé accroupi (hors combat ou en combat : c'est aussi une fuite). Tu ressors, quand tu le
  veux, par la porte de sortie du hall, là où tu étais entré (ou à un perchoir).
- **Tes alliés** : ils viennent avec toi s'ils tiennent un **objet de passage** (lequel : à choisir plus tard) au
  moment où tu entres, près de toi ; sans lui, personne d'autre n'entre jamais.
- **Un monde à toi** : tu le construis et l'agrandis comme tu veux (voir ci-dessous) ; il est sauvegardé avec ton
  personnage.

- Une dimension à part (`murimblock:raven_halls`), un espace par joueur, comme le monde intérieur de la méditation.
- Le **hall** : pierre noire, lanternes d'ambre, plumes au sol, le nid du corbeau ; c'est **ta base** : on y construit
  librement, on y pose ses coffres, son lit (on peut y dormir et y réapparaître), ses établis. Les monstres n'y
  apparaissent jamais.
- Des **portes** dans le hall mènent à tes **perchoirs** : on en pose dans le monde (un poteau noir surmonté d'un
  corbeau de pierre, 3 au plus) en tenant le rouleau-carte de l'art (clic droit), et on y voyage en passant par le hall
  (Halls Step tenu, ou la porte du hall).
- Les **salles** : des pièces vides et fermées, à part de ta base, où vont les ennemis avalés (Raven Gate, Closed Room,
  l'ultime) ; ils ne voient jamais ta base.

## Le château sans fin (19/10, demande d'Enzo : « vraiment un effet d'infini »)

La dimension est remplie partout, dans les trois directions, d'un même palais sans fin (`RavenCastleFeature`, une
pièce par chunk et par étage, tous les 14 blocs, de l'eau noire du fond jusqu'en haut, 384 blocs) : longues galeries
couvertes ou à balustrade qui traversent des rangées entières de chunks, grandes colonnes rouges cerclées d'or à travers
tous les étages, planchers, pavillons droits ou pendus à l'envers sous l'étage du dessus, escaliers d'un étage à l'autre,
planchers posés sur le côté comme des murs, lanternes au bout de longues chaînes, et des vides pour voir les étages
continuer en haut et en bas. Un brouillard sombre et proche (`client/RavenHallsClient`) avale le lointain : on ne
voit jamais de fin. Bois sombre, laque rouge, papier, tuiles, touches d'or. Ton hall est un pavillon du même style, sur
une terrasse suspendue dans un espace dégagé du palais, à mi-hauteur.

## Les effets

- Le corbeau : modèle 3D animé (vol, posé, cri, plongée), yeux d'ambre qui brillent dans le noir.
- Les portes : un cercle d'ombre (modèle GeckoLib) bordé d'une lueur d'ambre, qui s'ouvre en tournant, des plumes qui en
  sortent, un son grave ; une lumière Photon ambre à chaque passage.
- Le passage : le corps qui s'efface en plumes noires d'un côté et se reforme de l'autre.
- Les couloirs : brume noire, lueur ambre des lanternes, plumes qui tombent, les portes dans les murs.

## Chiffres de départ (à régler en jeu)

| Technique | Qi | Recharge |
|---|---|---|
| Raven Gate | 40 | 14 s |
| Halls Step | 30 | 8 s |
| Exchange | 35 | 12 s |
| Swallowing Gate | 30 | 15 s |
| Closed Room | 60 | 60 s |
| The Halls Unfold (ultime) | 180 | 5 min |

Les boss ne peuvent pas être avalés (Raven Gate, Closed Room, l'ultime les ralentissent seulement), ni les joueurs
d'un royaume plus haut que le tien.

## Décidé (19/10)

Intermédiaire (Qi Condensation), éventail de fer, source : le Maître des Corbeaux (ermite dans une tour), aucune quête
(le rouleau enseigne les cinq techniques et l'ultime ; le secret se découvre).

## Reste à décider

- L'objet qui permet à un allié d'entrer dans ton monde avec toi.

## État (19/10, étape 1 sur 5)

Fait : la dimension `murimblock:raven_halls` (vide, nuit fixe, lits utilisables), ton hall construit à ta première
venue (`RavenHallBuilder`, un hall par joueur à 4 096 blocs des autres), Closed Room accroupi pour y aller et en revenir
(sans éventail en main), les portes d'ombre (sud : retour ; ouest, est, nord : tes 3 perchoirs), le perchoir
(`RavenBlocks`, 3 au plus, le plus ancien tombe), les salles closes (`RavenHalls` : avalés et duels, gardées chargées
tant que quelqu'un y est). Techniques en version de base, sans animation ni effet propre : Raven Gate, Halls Step,
Exchange, Swallowing Gate, Closed Room (duel), The Halls Unfold, passif (failles, roulade) et secret. Boss : tag
`murimblock:raven_unswallowable`. Tests `RavenArtGameTests` (6), visite `runRavenTour`.

À venir : 2. le corbeau familier (modèle GeckoLib, animations, suivi) ; 3. portes et effets visuels, animations des
techniques, finish Last Message ; 4. bilan des perchoirs ; 5. Maître des Corbeaux, rouleau, guide, icônes.
