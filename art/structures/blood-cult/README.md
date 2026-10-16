# Culte du Sang — base souterraine

`culte_sang_souterrain_boss_72.schem` est la version de test complète exportée
depuis la maquette approuvée par Colleuhu95, au format WorldEdit Sponge v3 pour
Minecraft Java 1.21.1 (DataVersion 3955).

- Volume : **161 × 90 × 149** blocs (largeur × hauteur × profondeur).
- Quatre niveaux : quartiers du culte, sanctuaire, crypte, arène du chef.
- Salle du chef : **104 × 112** blocs ; disque de combat **Ø 72**, hauteur libre **30**.
- Trône, appartements privés, escaliers intérieurs, 7 bassins et 4 cascades.
- Construction et mobilier seulement : aucun boss, aucun mob, 14 coffres vides.

Le [mod Blood Sect Decor](../../../addons/blood-sect-decor/README.md) fournit
`bloodsect:blood`, utilisé dans les bassins et cascades. Ce fichier n'est donc
pas une structure vanilla. Il ne se génère pas naturellement et `/locate`
ne peut pas le trouver.

## Importation

1. Installer Blood Sect Decor 1.10.1 et WorldEdit 7.3.8 dans un profil NeoForge 1.21.1.
2. Copier le `.schem` dans `config/worldedit/schematics` du profil.
3. Ouvrir un monde de test avec commandes autorisées. Le collage remplace la
   roche et l'air de son volume ; utiliser un emplacement libre.
4. Exécuter ces commandes dans l'ordre, sans déplacer le personnage entre le
   `/tp` et `//paste`. Le mode spectateur évite de tomber avant le collage.

```text
/gamemode spectator
/tp @s 0 80 0
//schem load culte_sang_souterrain_boss_72
//paste
```

Attendre la fin de l'opération WorldEdit (**2 159 010 cellules**).
Utiliser `//paste` **sans `-a`** : l'air creuse les salles dans le terrain.
L'entrée est au point de collage, et la base s'étend vers les Z négatifs.
Le volume occupe Y=0 à Y=89 pour un collage à Y=80.

Après le collage ci-dessus, visiter l'arène :

```text
/tp @s 0 4 -77
/gamemode creative
```

Autres positions : sanctuaire `(0, 56, -44)`, crypte `(0, 40, -44)`,
appartements du chef `(68, 4, -77)`. Si le point de collage diffère,
le centre de l'arène est toujours **(X, Y − 76, Z − 77)** relativement au
point annoncé par WorldEdit. `//undo` annule le collage s'il est la dernière
opération WorldEdit.

## Sources et vérification

Le `.schem` est l'asset de construction modifiable dans WorldEdit. Les coupes
de `plans-de-la-base.png` sont calculées depuis ses blocs.
`export-stats.json` contient dimensions, palette, coordonnées et empreinte SHA-256.
`verification.json` décrit les contrôles statiques : format, palette 1.21.1,
28 accès reliés à l'entrée, 4 060 cases de sol dégagées dans l'arène,
30 blocs de hauteur libre, bassins bordés et cascades continues.
Le parcours statique utilise deux blocs de dégagement et une marche maximale
d'un bloc ; il ne remplace pas la physique de Minecraft.

Le 16 octobre 2026, WorldEdit 7.3.8 a confirmé en vrai client le collage
des 2 159 010 cellules, sans entité, dans le profil de test de Colleuhu95.
Une visite complète de chaque pièce, la stabilité des fluides après
rechargement et le multijoueur restent à valider.

Référence : [presse-papiers WorldEdit](https://worldedit.enginehub.org/en/latest/usage/clipboard/).
