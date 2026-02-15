# Refonte du Système de Commandes (Refork Command Framework)

Objectif : Optimiser, standardiser et rendre compatible (1.8 -> 1.21) le système de commande de LifeMod.

## 1. Nouvelle Architecture `LifeCommand` (FAIT)

La classe abstraite `LifeCommand` sera enrichie pour inclure :
- **Alias** : Support natif d'une liste d'alias.
- **Sous-commandes** : Système hiérarchique pour gérer `/lifemod reload`, `/lifemod help` proprement.
- **TabCompletion Intelligent** : Une méthode de base qui filtre automatiquement les résultats selon ce que le joueur a déjà tapé (startsWith ignoreCase).
- **CompletionProviders** : Des utilitaires statiques pour récupérer rapidement :
  - Liste des joueurs en ligne.
  - Liste des mondes.
  - Matériaux, Enchantements, Potions.
  - Gamemodes, etc.

## 2. Optimisations (EN COURS)

- **Lazy Loading** : Les sous-commandes ne sont instanciées que si nécessaire.
- **Filtrage TabComplete** : Déporté dans la classe mère pour éviter la duplication de code (FAIT).
- **Adapter** : Le `BukkitCommandAdapter` restera léger mais gérera mieux les retours (valeurs booléennes) (FAIT).

## 3. Compatibilité NMS & Versions

- Utilisation de l'API Bukkit standard pour la déclaration (`CommandMap` via réflexion si besoin pour contourner le `plugin.yml`).
- Pas de dépendance à Brigadier (1.13+) pour garantir le fonctionnement en 1.8, mais le système de TabComplete imitera ce comportement pour l'utilisateur.

## 4. Plan d'Action

1.  **Update Framework** : Modifier `LifeCommand.java` et `BukkitCommandAdapter.java` (FAIT).
2.  **Create Utils** : Créer `CompletionUtil.java` (FAIT).
3.  **Refactor Commandes** : Réécrire les commandes actuelles pour utiliser le nouveau système.
    - `GamemodeCmd` (FAIT).
    - `TeleportCmd` (FAIT).
    - À faire : `BanCmd`, `MuteCmd`, `KickCmd`, `WarnCmd`...
