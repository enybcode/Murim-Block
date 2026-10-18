# Le Qi et la cultivation

> **État : implémenté sur `test` le 14/10/2026, en attente de validation en jeu par Enzo.** Voir « Ce qui est en jeu » à la fin : la méditation refaite (touche C), l'aura, les percées animées, le monde intérieur avec le démon du cœur et les missions, la perception du Qi (touche G) ont été ajoutés à la conception d'origine ci-dessous ; le démon du cœur remplace les pourcentages de la grande percée.
>
> Conception d'origine : Choix d'Enzo : environ **2 heures de jeu** pour atteindre le 2ᵉ royaume, des **bonus de stats** par royaume, une grande percée qui **peut échouer**, et **trois sources** de Qi Max (méditation, combat, élixirs). Les chiffres sont un premier jet, réglés ensuite par une simulation (voir « Équilibrage »).
>
> Les noms en jeu sont en anglais (règle du projet). Le nom anglais est donné en premier, la traduction entre parenthèses.

## Ce qui existe aujourd'hui

- **Deux nombres** (`QiData`) : le **Qi**, la réserve que les techniques dépensent, et le **Qi Max**, la capacité. Le Qi Max sert aussi de jauge de cultivation.
- **9 royaumes de 4 étapes** (`CultivationRealm`, `CultivationStage`), avec un seuil de Qi Max pour chaque étape (`CultivationProgression`) : de 100 à 90 000.
- **La charge de Qi** (`QiChargeService`) : touche tenue, joueur immobile, régénération plus rapide.
- **Les récompenses de kills** (`QiRewardManager`, [QI_REWARDS.md](../QI_REWARDS.md)) avec anti-farm et premières victoires sur les boss.
- **La mort** coûte 10 % du Qi Max pour de bon (plancher 100).

**Le problème :**
- Rien ne fait monter le Qi Max en jeu : les kills ne remplissent que la réserve, et seules les commandes changent le Qi Max.
- Il n'y a aucune action de percée, et un royaume n'apporte rien.
- La régénération (1 % du max par minute) est trop lente face au coût des techniques (10 à 20 Qi).

La cultivation existe donc sur le papier mais n'est pas jouable.

## L'idée

- **Le Qi est le carburant** du combat : il se vide et revient vite.
- **Le Qi Max est la cultivation** : il monte lentement, par trois sources, et chaque étape franchie compte.
- **Une percée par étape** : les petites se font toutes seules, les grandes (changer de royaume) se tentent, avec un risque.
- **Chaque royaume rend plus fort** : santé, dégâts, vitesse, poings nus.

## Le Qi (la réserve)

| Situation | Régénération |
|---|---|
| Hors mode combat | 5 % du Qi Max par minute |
| En mode combat | 2 % du Qi Max par minute |
| En méditation | 40 % du Qi Max par minute (plein en 2,5 min) |

Le combat continue de rendre du Qi comme aujourd'hui :
- +1 par coup du passif qui touche ;
- +5 ou +6 par parade parfaite ;
- la récompense des kills.

## La méditation : « Meditate »

C'est la charge de Qi actuelle, renommée à l'écran « Meditate » (l'identifiant de la touche ne change pas).

- **Posture** : le joueur s'assoit en tailleur, avec une nouvelle animation Blockbench (la posture du lotus), immobile, les mains jointes.
- **Ce qu'elle fait** :
  - tant que la réserve n'est pas pleine, elle la remplit (40 % par minute) ;
  - une fois pleine, elle **condense** le Qi en Qi Max.
- **Interruption** : un coup reçu, un mouvement, une attaque ou une mise à terre arrêtent la méditation.

### La condensation

- **Le gain** : 1,2 % du Qi Max par minute, multiplié par le lieu et par l'efficacité du royaume.
- **Exemple** : à 150 de Qi Max, dans un lieu ordinaire et au 1ᵉʳ royaume, on gagne environ 1,8 Qi Max par minute.

| Lieu | Multiplicateur |
|---|---|
| Ordinaire | ×1,0 |
| Un biome Murim (les 7) | ×1,25 |
| En altitude (au-dessus de y = 140) | ×1,5 |
| **Lieu de pouvoir** : le Sanctuaire de la Falaise des Nuages, plus tard d'autres sites | ×2,0 |

On garde le plus haut multiplicateur ; ils ne s'additionnent pas. L'onglet Cultivation affiche la qualité du lieu où l'on médite.

**Contre le farm en AFK** (surtout en multijoueur) :
- chaque jour de jeu (20 minutes réelles), les **10 premières minutes** de condensation comptent en entier ;
- au-delà, la condensation ne compte qu'au quart.

L'onglet affiche ce qui reste pour la journée.

## Le combat

Deux règles simples, toujours après l'anti-farm existant :

- **Les méridiens s'élargissent** : **5 %** du Qi dépensé en techniques va au Qi Max. Une technique à 10 Qi rapporte 0,5.
- **L'expérience du combat** : **5 %** de la récompense de chaque kill va au Qi Max, en plus de la réserve. Exemple : un zombie (4 Qi) rapporte 0,2 Qi Max.

**Première victoire sur un boss** : son bonus de première victoire va **au Qi Max**, une seule fois par joueur. En jeu : Bandit Chief +30, **Sensei Yun +30** (14/10), Elder Guardian +100, Warden +250, Wither +400, Ender Dragon +600, plus les 5 % habituels du Qi du kill.
- Bandit Chief : +30 Qi Max.
- Gardien ancien : +100 Qi Max.
- Warden : +250 Qi Max.
- Wither : +400 Qi Max.
- Ender Dragon : +600 Qi Max.

Ces chiffres sont à revoir dans l'équilibrage.

## Les élixirs

Deux pilules au départ. On les fabrique à l'établi avec la flore des biomes Murim, ce qui oblige à voyager. Un four d'alchimie viendra plus tard.

| Pilule | Recette (sans forme) | Effet |
|---|---|---|
| **Qi Gathering Pill** (Pilule de rassemblement du Qi), ×2 | Lotus Blossom (Jiangnan) + Chrysanthemum (vallée des pêchers) + Sea Buckthorn (plateau de loess) | +10 Qi Max, et la réserve remplie |
| **Realm-Breaking Pill** (Pilule de rupture du royaume), ×1 | Cistanche (désert du Hexi) + Huangshan Rhododendron (pics de la mer de nuages) + Lotus Blossom + lingot d'or | +30 % de chances à la prochaine grande percée, si elle est tentée dans les 10 minutes |

La Realm-Breaking Pill peut aussi tomber du Bandit Chief (1 chance sur 4).

### La toxicité

- **Chaque pilule ajoute 30 de toxicité.** Elle baisse de 1 par minute.
- **L'effet est réduit d'autant** : à 60 de toxicité, une pilule ne donne plus que 40 % de son effet.
- **Au-delà de 100**, la pilule empoisonne (Nausée et Poison pendant 10 s) et ne donne rien.
- La toxicité s'affiche dans l'onglet Cultivation. Au-dessus de 50, elle réduit aussi les chances de grande percée.

## Les percées

### La petite percée (entre deux étapes)

- Elle se fait **toute seule** dès que le Qi Max atteint le seuil, quelle que soit la source.
- On voit une spirale de particules montante, on entend un son, et un message s'affiche : « You broke through to Middle Qi Sensing ».
- La toute première arrive dès la première méditation : le seuil Early → Middle (100) est déjà atteint au départ. Elle sert de tutoriel.

### La grande percée (entre deux royaumes)

- **Le goulot d'étranglement** : à l'étape Peak, le Qi Max se bloque au seuil du royaume suivant. Le gain en trop est perdu tant que la percée n'a pas réussi.
- **Pour la tenter** :
  - Qi Max au seuil, réserve pleine ;
  - en méditation, pas en mode combat, santé au moins à moitié ;
  - puis bouton **« Attempt Breakthrough »** dans l'onglet Cultivation.
- **Le déroulé** : 10 secondes de concentration, où le Qi tourbillonne autour du joueur. Un coup reçu pendant ces 10 secondes compte comme un échec.

**Les chances de réussite**, affichées avant de tenter :

| Facteur | Effet |
|---|---|
| Base | 50 % |
| Lieu de pouvoir (sanctuaire) ; altitude ou biome Murim | +25 % ; +10 % |
| Realm-Breaking Pill prise dans les 10 dernières minutes | +30 % |
| Chaque échec précédent à ce royaume | +10 % (au plus +30 %) |
| Toxicité au-dessus de 50 | −20 % |
| Plafond | 95 % |

**Réussite :**
- royaume suivant, étape Early ;
- les bonus du royaume s'appliquent, la réserve est remplie ;
- grande colonne de lumière et explosion de particules, son de gong, message à tout le serveur : « [joueur] broke through to Qi Guiding ».

**Échec : la déviation du Qi** (走火入魔, *zǒuhuǒ rùmó*) :
- on perd **15 % du Qi Max**, ce qui fait repasser sous le seuil : il faut le regagner avant de retenter ;
- la réserve est vidée ;
- l'effet **« Qi Deviation »** dure 5 minutes : Lenteur I, Faiblesse I, aucune régénération ;
- particules rouge sombre, le joueur tousse ;
- on **ne perd jamais de royaume**.

**Plus tard** : à partir de Foundation Establishment (6ᵉ royaume), la **tribulation céleste**. Des éclairs frappent le joueur pendant sa percée, et il doit y survivre.

## Ce que donne chaque royaume

Des modificateurs d'attributs aux identifiants stables, recalculés à chaque changement de royaume **et d'étape** (`RealmBonuses`) :
- **Dégâts** : multiplient le total, arme comprise ;
- **Mains nues** : dégâts ajoutés quand la main principale est vide, ce qui règle aussi la question des dégâts du Poing Long ;
- **Armure et ténacité** (demande d'Enzo du 14/10), ajoutées à celles de l'équipement ;
- **Les étapes** (demande d'Enzo du 14/10 : « plus on a de Qi ») : chaque étape apporte un quart du chemin vers les bonus du royaume suivant (santé en cœurs entiers). Exemple : Qi Guiding Late = +6 santé (3 ♥), +3 armure, +7,5 % de dégâts.

Valeurs à l'étape Early de chaque royaume :

| Royaume | Qi Max pour y entrer | Santé | Dégâts | Vitesse | Mains nues | Armure | Ténacité |
|---|---|---|---|---|---|---|---|
| 1. Qi Sensing | 100 (départ) | — | — | — | — | — | — |
| 2. Qi Guiding | 250 | +4 (2 ♥) | +5 % | +2 % | +1 | +2 | — |
| 3. Qi Condensation | 650 | +8 | +10 % | +4 % | +2 | +4 | +1 |
| 4. Qi Liquefaction | 1 250 | +12 | +15 % | +6 % | +3 | +6 | +1 |
| 5. Qi Crystallization | 2 500 | +16 | +20 % | +8 % | +4 | +8 | +2 |
| 6. Foundation Establishment | 5 000 | +24 | +30 % | +10 % | +6 | +11 | +3 |
| 7. Core Formation | 10 000 | +32 | +40 % | +12 % | +8 | +14 | +4 |
| 8. Nascent Soul | 20 000 | +40 | +50 % | +14 % | +10 | +17 | +5 |
| 9. Void Enlightenment | 45 000 | +50 | +60 % | +15 % | +12 | +20 | +6 |

**Le faible contre le fort** (demande d'Enzo du 14/10) : un coup porté à un joueur par plus faible que lui perd **5 % par étape d'écart, 20 % par royaume, au plus 75 %** (`CultivationRules.shield`, appliqué avant l'armure). L'attaquant est un joueur (son royaume et son étape), un mob Murim ou un boss (le royaume que lit la perception du Qi), ou n'importe quelle autre créature, comptée comme Qi Sensing Early. Exemple : en Qi Condensation Early, un zombie fait 40 % de dégâts en moins, le Bandit Chief (Qi Guiding Peak) 5 %. Rien contre un égal ou un plus fort, ni pour les chutes, le feu ou le vide (pas d'attaquant). Le démon du cœur et l'ombre ont le royaume de leur maître : aucun effet.

Débloquages prévus plus tard, hors première tranche :
- le **qinggong** (saut léger) au 2ᵉ royaume ;
- des arts martiaux qui demandent un royaume minimum ;
- la pression d'aura d'un royaume supérieur.

## La mort

- Elle coûte toujours **10 % du Qi Max**, mais **jamais sous le seuil de l'étape actuelle** : on ne perd ni étape ni royaume.
- La réserve est vidée.

## Équilibrage

**Objectifs de temps de jeu** pour un joueur type, avec une partie normale (combat, exploration, un peu de méditation) :

| Passage | Temps visé (cumulé) |
|---|---|
| Royaume 1 → 2 | ~2 h |
| 2 → 3 | ~4 h (6 h) |
| 3 → 4 | ~7 h (13 h) |
| 4 → 5 | ~12 h (25 h) |
| au-delà | 20 h et plus par royaume (fin de jeu) |

**Répartition prévue pour les 2 premières heures** (+150 Qi Max, de 100 à 250) :
- méditation (~20 min de condensation) : ~45 ;
- techniques (~600 Qi dépensés) : ~30 ;
- kills (~120) : ~30 ;
- élixirs (3 pilules) : ~30 ;
- première victoire sur le Bandit Chief : +30.

**Comment on tient les objectifs :**
- le gain de méditation est un pourcentage du Qi Max ;
- l'**efficacité du royaume** baisse à chaque royaume (×0,8 par royaume, à régler), pour que les royaumes suivants prennent plus de temps ;
- un **test de simulation** (`CultivationBalanceTest`) joue un « joueur type » minute par minute et vérifie chaque objectif à ±20 %. On règle les chiffres dans un seul fichier (`CultivationProgression`, `QiConstants`) jusqu'à ce qu'il passe.

## Interface

**Onglet Cultivation :**
- royaume et étape ;
- barre de Qi Max vers le prochain seuil ;
- qualité du lieu ;
- condensation restante aujourd'hui ;
- toxicité ;
- bonus du royaume actuel et du suivant ;
- à l'étape Peak, bouton « Attempt Breakthrough » avec les chances affichées.

**HUD :** la petite barre bleue reste la même. En méditation, réserve pleine, un mince liseré doré montre la progression vers la prochaine étape.

**Profile et Info :** pas de changement (Stage ??? reste la règle).

## Données sauvegardées

- **Dans `QiData`**, de nouveaux champs optionnels :
  - toxicité ;
  - minutes de condensation du jour, et quel jour ;
  - échecs au royaume actuel ;
  - moment de la dernière Realm-Breaking Pill.
- Les anciennes sauvegardes se chargent sans eux.
- Les identifiants des royaumes et des étapes ne changent pas.

## Tests prévus

- **Unitaires** :
  - la simulation d'équilibrage ;
  - les seuils, le goulot d'étranglement et les chances de percée ;
  - la toxicité ;
  - le plancher de la mort ;
  - les anciennes sauvegardes.
- **GameTests** :
  - la méditation qui condense seulement une fois la réserve pleine ;
  - le plafond du jour ;
  - le Qi Max venu des techniques et des kills ;
  - les petites percées automatiques ;
  - la grande percée réussie et ratée (hasard fixé) ;
  - les bonus de stats appliqués et retirés ;
  - les pilules et leur toxicité ;
  - la mort.
- **Vrai client** : une visite (méditation assise, percée réussie, déviation, onglet Cultivation).

## Découpage proposé

1. **Le cœur** :
   - régénération rééquilibrée, méditation qui condense (lieux, plafond du jour) ;
   - Qi Max venu des techniques, des kills et des premières victoires sur les boss ;
   - petites et grandes percées, bonus de royaume, plancher de la mort ;
   - onglet Cultivation, simulation d'équilibrage.
2. **Les élixirs** : les deux pilules, leurs recettes, la toxicité, la pilule lâchée par le Bandit Chief.
3. **Plus tard** :
   - la posture du lotus en animation (en attendant, la pose de charge actuelle) ;
   - la tribulation céleste ;
   - le qinggong ;
   - les méthodes de cultivation (心法, des manuels qui multiplient la méditation) ;
   - le four d'alchimie ;
   - un mini-jeu de respiration.

## Questions ouvertes pour Enzo

1. **Message à tout le serveur** à chaque grande percée : oui, ou seulement au joueur ?
2. **Les chiffres de stats** du tableau : trop forts ou trop faibles ? (Au 9ᵉ royaume : +25 cœurs et +60 % de dégâts.)
3. **La posture du lotus** dès la première tranche, ou plus tard ?
4. **D'autres lieux de pouvoir** à ajouter : le Temple de bambou dans la brume, le jardin de la lune de lotus ?

## Ce qui est en jeu (14/10/2026)

Ce qui suit l'emporte sur la conception ci-dessus quand elles diffèrent.

- **La méditation (touche C)** : un appui s'assoit en tailleur (animations Blockbench `scripts/animation/meditation.cjs` : s'asseoir, lotus qui respire, se relever), un autre relève ; bouger relève aussi, un coup reçu arrête. En s'asseyant, une petite cinématique de 3 s : la vue tourne autour du corps ; puis on reste **en première personne** (Enzo, 17/10/2026), libre de regarder autour de soi, sans zoom (le blocage de la vitesse ne resserre plus la vue). La réserve remonte à 40 % par minute ; pleine, elle se condense en Qi Max (`MeditationService`). La barre de Qi s'affiche en méditant, avec un liseré doré vers l'étape suivante.
- **Les orbes de Qi** (17/10/2026, demande d'Enzo, « comme les orbes d'XP », `MeditationOrbs`) : après 3 s assis, le Qi du lieu se condense en orbes autour du corps (une toutes les 3,5 s, 5 au plus, une dorée sur 12), qui tournent lentement et s'éteignent au bout de 16 s. On les regarde (viseur, un quart de seconde) et elles viennent se fondre dans le corps, avec le son des orbes d'XP : chacune remplit 4 % de la réserve, ou, réserve pleine, condense comme 3,5 s de méditation de plus ; une dorée vaut cinq fois plus. Tout attraper fait méditer environ deux fois plus vite ; ne rien faire reste possible. Le serveur vérifie que l'orbe est la sienne, assise, à moins de 9 blocs (`CultivationPayloads.Absorb`, protocole 28). Tests `MeditationOrbsGameTests`, film `runMeditationOrbsTour`.
- **Le qinggong** (轻功, 18/10/2026, demande d'Enzo, `Qinggong`, `QinggongClient`) : dès Qi Guiding, on saute plus haut et l'on tombe plus loin sans dégâts à chaque royaume (force de saut +0,06 à +0,20, chute sans dégâts +3 à +15 blocs, `RealmBonuses`) ; et, en l'air, la touche de saut une seconde fois relance un saut sur son Qi (4 Qi, une fois jusqu'à toucher le sol) : en haut et dans le sens de la course, une bouffée d'air sous les pieds, la chute oubliée. Le client saute aussitôt ; le serveur vérifie le royaume et le Qi, prend le Qi et montre la bouffée à tous (`CultivationPayloads.Leap`, protocole 29). Un vide de 7 blocs se franchit en courant. Tests `QinggongGameTests`, film `runQinggongTour`.
- **L'aura**, vue de tous (`AuraState`, synchronisé à tous les clients, sans aucun royaume), refaite le 17/10/2026 d'après les auras de manhwa montrées par Enzo (il a choisi, parmi quatre maquettes, le « volume de flammes ») : des dizaines de flammes douces tout autour du corps, qui montent en ondulant et se chevauchent (`AuraClient.volume`).
  - **Sa taille suit le Qi tenu** (`AuraClient.qiScale`) : ×0,6 à 100 Qi, ×0,9 à 1 000, ×1,2 à 10 000, ×1,5 à 90 000 : plus large, plus épaisse, plus haute.
  - **Sa couleur et sa petite touche sont celles de l'art le mieux maîtrisé** (`MeditationService.auraArt`, la maîtrise du style ; à égalité, le style principal) : Sanguine Art rouge-magenta et gouttes de sang ; Nuage Azur azur et petits nuages ; Poing Long ambre et onde au sol ; Lance du Pic Céleste doré pâle et traits qui fusent ; Bâton du Voyageur vert et feuilles de lumière ; Poing Brise-Montagne orange et éclats de pierre ; Sept Étoiles bleu nuit et la Grande Ourse qui tourne au-dessus de la tête ; Sabre du Tonnerre violet et étincelles électriques ; Poing Majin rouge sombre et fumée noire ; Cloche d'Or or et anneaux en cloche. Sans art : blanche.
  - Sa propre aura ne se voit pas en première personne (seulement quelques braises loin du corps) : les autres la voient. Film `runAuraTour`.
- **Le Qi Max** monte par la méditation (biome Murim ×1,25, altitude ×1,5, sanctuaire ×2, monde intérieur ×1,5 ; 10 minutes pleines par jour de jeu), les techniques (5 % du Qi dépensé), les kills (5 % de la récompense), les premières victoires sur les boss (le bonus du Bandit Chief n'était jamais versé : corrigé), les élixirs et les missions. **Le rythme** (demande d'Enzo du 17/10/2026, un départ plus rapide puis une montée qui ralentit) : tout gain de Qi Max par l'effort (méditation et orbes, techniques, kills ; pas les pilules, missions ni boss) est multiplié selon le royaume (`CultivationRules.GROWTH` : ×6, ×2,2, ×1,4, puis ×1,5). Simulation d'un joueur type qui attrape les orbes (`CultivationRulesTest`) : Qi Guiding à 31 min, Qi Condensation à 2 h, Qi Liquidation à 5 h, Qi Crystallization à 9 h, Foundation Establishment à 15 h ; sans attraper les orbes, 38 min, 2 h 40, 6 h, 11 h, 18 h.
- **Les percées** : les petites sont automatiques (spirale de Qi, anneau, cloche, souffle du corps). À l'étape Peak, le Qi Max se bloque au seuil et un message dit de méditer réserve pleine, de tenir C sept secondes, et que le vieux maître proposera l'épreuve.
- **Le monde intérieur** (`murimblock:inner_world`, `InnerWorldService`) : en méditation, réserve pleine, tenir C **sept secondes** (une barre se remplit, les secondes restantes affichées ; le serveur refuse si le joueur n'est pas assis depuis au moins ce temps). Pour revenir, tenir C une seconde et demie. Le corps reste assis avec son aura ; un coup sur lui arrache le joueur avec une déviation. On ne meurt pas dans le monde intérieur ; tomber de l'île ou tenir C ramène au corps. Chaque île est construite à l'image du joueur (`InnerIsland`) : taille selon le royaume, étang, pins, pierres flottantes, noyau doré, cour et mannequins pour le Poing Long, nuages et pavillon pour l'épée, une stèle par art appris, une fissure par échec ; le vide prend la couleur du Qi (gris orageux bas, bleu profond plein, voile rouge après des échecs).
- **L'épreuve et le démon du cœur** (`HeartDemonEntity`, demande d'Enzo du 14/10) : au seuil d'un grand royaume, plus de démon à l'arrivée ; c'est le vieux maître qui propose de s'affronter soi-même (dialogue ci-dessous). Accepté, le démon se forme en trois secondes dans une fumée noire : une copie sombre du joueur (son skin, yeux rouges, aura violette) avec son art (épée et coupes du Nuage Azur, ou poings et techniques du Poing Long) et **exactement ses stats** : même vie, même armure et ténacité, mêmes dégâts (bonus du royaume compris), et la même épée s'il en tient une. Refuser ou repartir avant d'accepter ne coûte rien. La préparation ne l'affaiblit plus : elle règle le poids de ses coups sur le joueur (« Demon's blows » dans l'onglet) : pilule de rupture −30 %, sanctuaire −20 %, altitude ou biome Murim −10 %, chaque échec −10 % (au plus −30 %), toxicité au-dessus de 50 : +20 %, plancher 40 %. Vaincu : retour au corps, élévation en tailleur, colonne de lumière, onde de choc, message à tout le serveur, nouveau royaume. Perdu ou fui : déviation (−15 % de Qi Max sans descendre sous l'étape, réserve vidée, effet « Qi Deviation » 5 min).
- **Le dialogue avec le vieux maître** (`SoulDialogue`, écran `DialogueScreen`, service générique `dialogue/DialogueService` réutilisable pour d'autres personnages, protocole 17) : clic droit sur lui ; un panneau manuscrit en bas de l'écran, son nom sur un sceau rouge, ses mots qui s'écrivent, puis les réponses numérotées (clic ou touches 1-9 ; le serveur n'accepte qu'une réponse proposée, à 8 blocs au plus). Première rencontre (qui il est, où l'on est), puis : « Teach me. » (missions du jour), « How do I grow stronger? » (le conseil le plus utile : pilules, lieu du corps, royaume proche, les trois voies), « Who are you? » (un maître vaincu par son propre démon, qui s'est réfugié dans le dantian du joueur ; « call me Sensei »). Au seuil : ce qu'il y a au-delà, « Myself? », comment se préparer (avec le poids des coups du jour), l'avertissement (fuir ou tomber, c'est la déviation), « Summon it. » ou « Not yet. ». Pendant une mission : reprendre ou abandonner (sans perte).
- **L'esprit et les missions** : l'âme d'un vieux maître donne la mission du jour : « rassembler le Qi dispersé » (10 orbes en une minute), puis « affronter son ombre » (un double à moitié force, sans risque). Récompense : 3 % et 5 % de l'étendue du royaume en Qi Max.
  Le vieux maître est un fantôme en 3D (GeckoLib) : chignon et épingle de jade, longs sourcils tombants, moustache et barbe en gradins, robe à col croisé et ceinture rouge, manches larges jointes devant lui ; sans jambes, la robe s'effile en une queue de brume qui ondule. Translucide, il luit dans le noir, flotte, et sa lueur respire. Modèle, texture et animation générés par `scripts/animation/remnant-soul.cjs` (source Blockbench : `art/blockbench/remnant_soul.geo.json`).
- **Les élixirs** : Qi Gathering Pill (lotus, chrysanthème, argousier → 2 ; +10 Qi Max et réserve pleine) et Realm-Breaking Pill (cistanche, rhododendron, lotus, lingot d'or ; démon affaibli 10 min ; une chance sur quatre sur le Bandit Chief). Toxicité +30 par pilule, −1 par minute ; au-delà de 100, empoisonne.
- **Les royaumes et les étapes** donnent leurs bonus (santé, dégâts, vitesse, mains nues, armure, ténacité), et les coups des plus faibles pèsent moins (voir « Ce que donne chaque royaume ») ; la mort coûte 10 % du Qi Max sans descendre sous l'étape.
- **La perception du Qi (touche G, tenue)**, dès Qi Condensation, 1 Qi par seconde, décidée par le serveur pour chaque lecteur ; refaite le 18/10/2026 en **vision du Qi** (`QiVisionClient`, filtre `shaders/post/qi_vision`), après plusieurs maquettes avec Enzo :
  - **comme un aveugle** : le monde en nuances de gris sombres, sans textures (l'image floutée), les contours tracés en gris clair là où la profondeur se rompt ; tout s'éteint au-delà de la portée ;
  - **seul le Qi garde sa couleur** : chaque être vivant proche a l'aura en flammes de la méditation, à sa couleur (un joueur, l'art le mieux maîtrisé et la taille de son Qi ; une créature, rouge si hostile, verte si animal, bleu pâle sinon, de la taille de son corps ou de son royaume lu) ;
  - **la portée suit le royaume** (`CultivationRules.senseRange`) : auras lues de près et à vue à 16, 24, 32, 48, 64, 96, 128 blocs de Qi Condensation à Void Enlightenment ; les **puissants** (aussi forts que soi ou plus) sont sentis à travers tout jusqu'à trois fois plus loin (160 au plus) ;
  - **la pression spirituelle** (d'après Bleach) : une aura assez grande s'élève en **colonne de lumière** large (environ 6 blocs de rayon un royaume au-dessus, 10 bien au-dessus) jusqu'à 40 à 70 blocs, aux bords vacillants comme des flammes, dessinée par le filtre lui-même (aucune particule), visible à travers la forêt ; au pied, un petit tourbillon et une onde au sol ;
  - **l'écran tremble** devant une aura au-dessus de la sienne : un peu pour un royaume au-dessus, fort bien au-dessus, de plus en plus en s'approchant (`QiSenseClient.pressure`) ;
  - les **sceaux de royaume** (一 à 九) au-dessus des têtes sont retirés en attendant leur refonte (textures gardées) ;
  - les **courants de Qi** vers les veines (dès Qi Liquidation) sont prêts mais attendent les veines (tranche 3).
  Tests `CultivationGameTests` (lecture par royaume, les puissants de loin) ; film `runQiVisionTour`.
- **L'onglet Cultivation** : Qi Max vers le seuil, lieu, condensation du jour, état de la percée, poids des coups du démon, toxicité, échecs, bonus (cœurs, armure, dégâts, mains nues).
- **Protocole réseau 17** (16 pour la cultivation, 17 pour les dialogues). L'ancienne « charge de Qi » (touche R) est retirée.
- **Tests** : `CultivationRulesTest`, `CultivationJournalTest`, `CultivationGameTests` (le serveur de GameTests ne charge pas les dimensions des datapacks : l'End, très loin, y tient lieu de monde intérieur), et en vrai client `gradlew -PgameTests runCultivationTour` (26 vérifications, films de chaque étape, captures du dialogue). Pas encore validé par Enzo ni à deux vrais clients.
- **Plus tard** : la tribulation céleste, le qinggong, la dissimulation de l'aura, les méthodes de cultivation, le four d'alchimie, le mini-jeu de respiration, d'autres lieux de pouvoir.
