# Les mondes intérieurs des arts martiaux

> **État : en jeu sur `test` le 19/10/2026.** Demande d'Enzo : « chaque technique en a un, en fonction du raccord à la
> technique et en lien avec son élément » ; les techniques sont les **arts martiaux** ; on voit le monde de **l'art
> pratiqué** (le style principal). Propositions validées par Enzo (« bah fais-le »).

L'île du monde intérieur ([qi-cultivation.md](qi-cultivation.md)) prend l'allure du **style principal** de son
cultivateur et de son élément : son sol et sa roche, ses arbres (dès Qi Condensation), son eau, ses pierres flottantes
(dès Qi Liquefaction), ses lieux propres, la couleur de son ciel (vers laquelle tourne le vide quand la réserve est
pleine) et son air (particules, sons). Ce qui suit le royaume reste commun : la taille de l'île, l'étang, les pierres
flottantes, le noyau doré au Core Formation, une stèle par art appris, une fissure par grande percée ratée. Changer de
style principal reconstruit l'île à la visite suivante. Sans style principal : l'île simple d'herbe et de pins.

| Art | Élément | Le monde |
|---|---|---|
| Azure Cloud Sword | vent, nuages | mer de nuages sous l'île, pavillon aux tuiles vertes, épées plantées dans la roche, ciel azur, nuages qui passent |
| Long Fist | terre, corps | cour d'entraînement dallée, mannequins, pierres de force, bannières rouges, jujubiers, ciel de couchant orange, poussière |
| Golden Bell | métal | cour de temple veinée d'or, tour de la cloche (une vraie cloche qui sonne de temps en temps), osmanthes, étang aux lotus, ciel doré |
| Celestial Peak Spear | montagne | trois pics enneigés au nord, plaques de neige, épicéas du Tianshan, râteliers de lances, bannières bleues de l'Alliance, ciel d'altitude, neige |
| Traveller's Staff | bois, la route | prairie traversée d'un chemin sinueux bordé de lanternes, vieux camphrier, autel du bord de la route, saules, soir mauve, pétales |
| Purple Heaven Thunder Saber | tonnerre | plateau de pierre noire et de basalte, tiges de fer, tambours de guerre, cristaux d'améthyste, arbres morts, ciel d'orage violet, étincelles et éclairs |
| Seven Stars Sword | étoiles | nuit éternelle étoilée, sept piliers de lumière en forme de Grande Ourse, bassin qui reflète le ciel, arbres aux colombes |
| Mountain Shattering Fist | terre brisée | montagne en morceaux : gros blocs flottants, anneau de sol brisé autour de la plateforme, veines de magma, souches de roche, poussière |
| Overlord Spear | guerre, feu | champ de bataille au couchant rouge sang, lances brisées, étendards tombés, tente du conquérant et son feu, arbres calcinés, braises |
| Sanguine Art | sang | lac de sang, flèches de cristal de sang, arbres pourpres, ciel cramoisi, brume rouge |
| Devouring Demon | démon, vide | île rongée sur ses bords, débris qui tombent dans le vide, grandes arches de côtes, sculk, œil rouge qui regarde depuis le nord |
| Majin Fist | fureur | arène de pierre noire fendue de rouge, chaînes suspendues dans le vide, braseros aux flammes sombres |

**On ne mine rien dans le monde intérieur** (Enzo, 19/10 ; `InnerWorldGuard`), quel que soit le mode de jeu : aucun bloc ne
se casse ni ne commence à se fendre. Vérifié par `runInnerWorldsTour` (un bloc cassé en créatif reste en place).

Les lieux évitent la plateforme, ses stèles, la place du démon du cœur (au nord) et celle de l'âme du vieux maître.

## Code et vérifications

`cultivation/inner/InnerTheme` (le monde de chaque art, la couleur de son ciel), `InnerWorlds` (sol, roche, arbres,
pierres flottantes, lieux), `InnerIsland` (l'île, commune), `client/cultivation/InnerWorldClient` (ciel et air de chaque
monde ; aucun paquet réseau : le client connaît déjà le style principal par l'aura). Visite en vrai client :
`gradlew -PgameTests runInnerWorldsTour` (les douze îles au Core Formation, `build/inner-worlds-tour-run/screenshots`).
Le serveur de GameTests n'a pas la dimension du monde intérieur : les tests de cultivation n'y entrent pas.
