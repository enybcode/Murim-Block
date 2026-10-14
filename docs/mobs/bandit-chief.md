# Bandit Chief (山大王)

Chef des bandits des montagnes, premier mini-boss. Pas encore d'apparition dans le monde : œuf (`Bandit Chief Spawn Egg`)
ou `/summon murimblock:bandit_chief`. Les camps de bandits qu'il commandera viendront plus tard.

## Allure

Une brute 1,3 fois plus grande qu'un homme (boîte 0,8 x 2,5), modèle 3D sur le squelette biped d'Epic Fight :

- tête rasée, chignon et longue mèche, barbe tressée à perle d'or, cicatrice sur l'œil gauche, anneaux d'or ;
- gilet de cuir ouvert sur le torse nu et le ventre, large ceinture cloutée à boucle de bronze en tête de tigre,
  tassettes lamellaires ;
- peau de tigre sur le dos et l'épaule gauche, tête du tigre sur l'épaule, patte pendante ; spalière de fer à pointe
  de bronze à droite ; bras nus tatoués, brassards de fer ; pantalon cramoisi bouffant sur des bottes à revers de fourrure.

Sa grande hache (`Chief's Great Axe`) : lame en croissant, pointe arrière, pointe de fer au sommet, manche gainé de
corde rouge, anneaux de bronze, houppe rouge. Tenue à deux mains (une grande épée pour Epic Fight).

## Combat

- 120 PV, armure 8 (robustesse 2), recul résisté à 80 %, armure de stun 10 : les coups ne l'interrompent pas tous.
  Pas d'attaque propre : la hache donne 8.
- Toutes ses attaques sont lentes à venir et **parables** (garde Murim) :
  - **Fendoir** (`chief_cleave`, ×1,3) : la hache derrière la tête, puis abattue jusqu'au sol devant (≤ 3,4 blocs) ;
  - **Fauchage** (`chief_sweep`, ×1,0, plusieurs cibles) : armé loin à droite, balayé à plat jusqu'à gauche (≤ 3,6 blocs) ;
  - **Saut écrasant** (`chief_slam`, ×1,5, met à terre) : de 3 à 7 blocs, il bondit de 1,8 bloc et plante la hache ;
    à 0,75 s, onde de choc de 3,2 blocs autour du point d'impact (0,6 × l'attaque, projette). Le saut est un seul coup :
    qui vient de prendre ou de parer la hache n'est pas touché par l'onde. Recharge 9 s.
- Touché, il appelle les bandits autour de lui. Barre de boss rouge pour les joueurs proches.
- Au repos, son idle à lui (`chief_idle`, `scripts/animation/mob-idles.cjs`) : la hache sur l'épaule droite, le poing gauche
  sur la boucle, grandes respirations, un regard à gauche, la nuque roulée, le menton levé. Son visage vit (`MobFaces`) :
  clignements, coups d'œil de son œil valide (l'œil balafré ne bouge pas), marmonnement ; en combat les sourcils se
  nouent, il montre les dents quand il frappe.
- Butin : 4 à 9 pépites d'or, cuir, viande cuite ; sa hache une fois sur dix. 40 XP, 20 Qi (+80 à la première victoire).

## Fichiers

- Modèle : `scripts/animation/bandit-chief-3d.cjs` (avec `mesh-kit.cjs`) → `animmodels/entity/bandit_chief.json`,
  `textures/entity/bandit_chief.png`, `art/blockbench/bandit_chief.bbmodel`.
- Hache : Crescent Moon (月牙斧), choisie parmi six styles (`scripts/concepts/chief-axes.cjs`, images
  `docs/mobs/images/chief-axe-*.png`) ; `scripts/animation/chief-axe-model.cjs` → `models/item/chief_great_axe(_3d).json`, ses textures,
  `art/blockbench/chief_great_axe.bbmodel`.
- Attaques : `scripts/animation/chief-axe.cjs` → `animations/biped/murim/chief_*.json` (rotations interpolées par
  quaternions entre les poses clés), `art/blockbench/bandit_chief_axe.bbmodel` pour voir les poses.
- Code : `BanditChiefEntity`, `BanditChiefPatch`, `BanditChiefRenderer`, `MurimAnimations.buildBanditChief`.
- Tests : `BanditChiefGameTests` (hache à deux mains, ses attaques, chacune parée, l'onde de choc) ; captures
  `32-chief.png`, `33-chief-front.png`, `chief-cleave/`, `chief-sweep/`, `chief-slam/` du smoke client.

## Limites connues

- Sans le rendu d'Epic Fight (entité filtrée dans sa config), le rendu vanilla de secours affiche son atlas sur un corps
  de joueur : il n'a pas de skin plat.
- Les attaques sont vérifiées en solo (GameTests serveur, client réel) ; à valider en vrai combat et à deux clients.
