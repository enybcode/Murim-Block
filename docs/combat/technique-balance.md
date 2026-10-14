# Équilibrage des techniques

> Demande d'Enzo du 14/10/2026 : équilibrer les dégâts des techniques selon l'arme, puis selon le Qi, sans qu'un combo puisse tuer d'un coup. Sur `test`, en attente de validation en jeu.

## La formule

Dégâts d'un coup de technique = **attaque du joueur × part du coup × facteur de Qi × (poings entraînés)**, puis la baisse du combo (−8 % par coup déjà porté, au plus bas 40 %), puis côté victime le bouclier des faibles, l'armure, et la limite anti-one-shot.

- **L'arme** : l'attaque du joueur est celle de l'arme en main (épée en bois 4, pierre 5, fer 6, diamant 7, netherite 8 ; enchantements en plus), multipliée par le bonus de dégâts du royaume. Les arts d'épée exigent une épée.
- **Les poings (Poing Long)** : une main nue vaut 1 plus le bonus « mains nues » du royaume. Les techniques du Poing Long ajoutent **+3 de main entraînée** (`TechniquePower.FIST_TRAINING`) : un débutant frappe comme avec une épée en pierre (4), un peu moins qu'une épée en fer, mais ses techniques coûtent moins de Qi et rechargent plus vite. Plus le royaume monte, plus le bonus « mains nues » rattrape l'épée.
- **Le Qi** : **+12 % par doublement du Qi Max au-dessus de 100** (`TechniquePower.qiFactor`).

| Qi Max | 100 | 250 | 650 | 1 250 | 2 500 | 5 000 | 10 000 | 45 000 |
|---|---|---|---|---|---|---|---|---|
| Facteur des techniques | ×1,00 | ×1,16 | ×1,32 | ×1,44 | ×1,56 | ×1,68 | ×1,80 | ×2,06 |

Seules les techniques en profitent, ainsi que ce qu'elles envoient (lames de Qi, coupes à retardement de Thousand Drizzle, chute du Croissant). Les attaques normales (M1) restent celles de l'arme.

## Les techniques (en multiples de l'attaque)

Exemple pour un débutant (Qi Sensing, 100 Qi Max) : épée en fer = 6, poings entraînés = 4.

| Technique | Qi | Recharge | Coups | Total (avant combo) | Débutant |
|---|---|---|---|---|---|
| Cloud Step Slash | 12 | 6 s | 1 | ×1,4 | 8,4 |
| Thousand Drizzle | 18 | 9 s | 6 × 0,3 + 0,8, puis coupes ×0,6 | ×3,2 (×2,5 après la baisse du combo) | ≈ 15 |
| Heaven-Splitting Crescent | 25 | 14 s | 1,2, frappe 1,0, chute 0,5 | ×2,7 | ≈ 16 |
| Tiger Step Punch | 10 | 5 s | 1 | ×1,3 | 5,2 |
| Rapid Fists | 15 | 8 s | 5 × 0,3 + 0,8 | ×2,3 (×1,7 après la baisse) | ≈ 7 |
| Whirlwind Kick | 20 | 12 s | 1, tout autour | ×1,0 par cible | 4 |
| Inch Force (secret) | 0 | — | 1 | ×2,0 | 8 |

Changement au passage : les estocs de Thousand Drizzle passent de 0,35 à 0,3 et le dernier de 0,9 à 0,8 (c'était la technique la plus forte pour son coût).

## Pas de one-shot

`combat/BurstGuard` : en **3 secondes**, un même joueur ne peut pas retirer à un joueur plus de **60 % de sa vie maximale**, quoi qu'il utilise (techniques, M1, lames de Qi, flèches). Ce qui dépasse ne fait pas de dégâts (le coup touche quand même : recul, étourdissement). Un joueur en pleine vie survit donc à n'importe quel combo ; il en faut un second. Le démon du cœur, double du joueur, suit la même règle. Les créatures non : une technique peut tuer un zombie d'un coup.

Exceptions : l'exécution d'un joueur à terre (le finish), le vide et /kill.

Conséquence voulue : un coup « mortel » d'un joueur ne met K.O. qu'un adversaire déjà entamé (moins de 60 % de sa vie retirable d'un coup).

## Tests

`TechniquePowerTest` (facteurs et limite), `MartialGameTests` (épée en fer et Qi, poings entraînés, trois coups de 8 sur 20 PV : 8, 4, puis rien ; trois secondes après, le combo suivant porte).
