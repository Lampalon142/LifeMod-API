# Système de Scan de Map & Inventaires (Anti-Duplication)

Ce document détaille le plan d'implémentation du système de scan pour LifeMod.

## 1. Objectifs
- Détecter les items dupliqués ou en circulation excessive.
- Scanner les inventaires des joueurs (en ligne).
- Scanner les Enderchests des joueurs (en ligne).
- Scanner tous les conteneurs du monde (Coffres, Shulker Boxes, Tonneaux, etc.).
- Supporter les items personnalisés (ItemsAdder).
- Fonctionnement asynchrone pour ne pas ralentir le serveur.

## 2. Commande
La commande principale sera `/scan`.

**Syntaxe :**
`/scan <map|inventories|enderchest|all> <player|all> <item|hand>`

**Arguments :**
1. **Type de scan :**
   - `map` : Scanne tous les conteneurs chargés dans le(s) monde(s).
   - `inventories` : Scanne les inventaires des joueurs.
   - `enderchest` : Scanne les Enderchests des joueurs.
   - `all` : Scanne tout ce qui est listé ci-dessus.
2. **Cible :**
   - `player` : Un joueur spécifique (doit être en ligne).
   - `all` : Tous les joueurs en ligne (ou tout le monde pour le scan de map).
3. **Item à rechercher :**
   - `hand` : Utilise l'item que l'administrateur tient en main.
   - `item_id` : Un ID de matériel Bukkit (ex: `DIAMOND`) ou un ID ItemsAdder (ex: `ia:ruby`).

## 3. Architecture Technique

### A. ScanManager
Une classe centrale `ScanManager` gérera la logique de scan.
- Utilisation de `CompletableFuture` pour l'asynchronisme.
- Méthodes pour scanner un `ItemStack` spécifique.
- Logique de comparaison d'items (Material, NBT, ItemsAdder ID).

### B. Scanner d'Inventaires
- Parcourt les joueurs cibles.
- Vérifie l'inventaire principal, l'armure et la main secondaire.
- Détection récursive des items dans les conteneurs transportables (Shulker Boxes).

### C. Scanner de Map (Conteneurs)
- Parcourt les chunks chargés dans les mondes.
- Identifie les `TileEntities` qui sont des conteneurs (`Chest`, `Barrel`, `ShulkerBox`, `Furnace`, `Hopper`, etc.).
- Pour les chunks non chargés : Le scan se limitera initialement aux chunks chargés pour des raisons de performance, avec une option de "Deep Scan" qui charge les chunks de manière asynchrone par petits lots si nécessaire.

### D. Support ItemsAdder & Généricité
- Intégration de l'API ItemsAdder pour identifier les items via leur `CustomStack`.
- **Note importante** : L'intégration doit être conçue de manière générique (ex: via un service dédié `ItemsAdderService`) afin d'être réutilisable par d'autres modules du plugin (Anti-Cheat, Logs, GUI, etc.). Cela facilitera l'ajout de futures fonctionnalités dépendantes d'ItemsAdder.

## 4. Retours et Affichage
- Progression du scan affichée en temps réel dans le chat pour l'administrateur.
- Rapport final détaillé :
  - Nombre total d'items trouvés.
  - Liste des positions (coordonnées) pour les conteneurs.
  - Liste des joueurs possédant l'item.
- Si le nombre de résultats est trop élevé (> 50), un résumé sera affiché avec les zones de forte concentration.

## 5. Étapes d'implémentation

1. **Étape 1 :** Création du `ScanManager` et de la structure de données des résultats.
2. **Étape 2 :** Implémentation du scanner d'inventaires et d'Enderchests.
3. **Étape 3 :** Implémentation du scanner de map (chunks chargés).
4. **Étape 4 :** Création de la commande `/scan` avec auto-complétion.
5. **Étape 5 :** Mise en place du service générique `ItemsAdderService` pour l'ensemble du plugin.
6. **Étape 6 :** Ajout des options de configuration dans `config.yml`.
7. **Étape 7 :** Tests et optimisations (gestion des threads).
