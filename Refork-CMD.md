# Refonte Architecturale du Système de Commandes (Refork-CMD)

## Contexte et Objectifs

Le système de commandes actuel du projet manque de cohérence architecturale, mélange des styles de déclaration, et n'est pas optimisé pour la compatibilité inter-versions de Bukkit/Spigot (NMS). Cette refonte vise à moderniser, unifier et fiabiliser la gestion des commandes pour les objectifs suivants :

*   **Respect des Conventions** : Adhérer plus profondément aux conventions Java et Bukkit/Spigot, en adoptant des patterns d'architecture standardisés.
*   **Compatibilité NMS (1.8-1.21)** : Introduire des mécanismes d'abstraction pour gérer les spécificités de chaque version de Minecraft, permettant une portabilité des commandes sur un large éventail de versions.
*   **Maintenabilité Améliorée** : Simplifier l'ajout, la modification et la suppression de commandes grâce à une structure claire et des principes de conception solides.
*   **Extensibilité** : Faciliter l'intégration de nouvelles fonctionnalités et modules de commande sans perturber l'existant.
*   **Professionnalisme** : Élever la qualité du code du système de commandes pour une meilleure lisibilité, testabilité et robustesse.

## Nouvelle Architecture des Packages de Commandes

La nouvelle structure sera organisée sous `src/main/java/fr/lampalon/lifemod/platform/bukkit/commands/` comme suit :

```
src/main/java/fr/lampalon/lifemod/platform/bukkit/commands/
│
├── 📦 api
│   ├── LifeCommand.java          <- Interface ou classe abstraite parente
│   └── CommandContext.java       <- Wrapper (Sender, Args, NMS Player, Services, etc.)
│
├── 📦 engine
│   ├── CommandRegistry.java      <- Enregistre les commandes via Reflection (NMS Friendly)
│   └── BukkitCommandWrapper.java <- Fait le pont avec org.bukkit.command.Command
│
├── 📦 impl
│   ├── 📂 admin                  <- Ex: VanishCommand.java, ReloadCommand.java
│   ├── 📂 moderation             <- Ex: BanCommand.java, MuteCommand.java
│   ├── 📂 player                 <- Ex: FeedCommand.java, HealCommand.java
│   ├── 📂 utility                <- Ex: TimeCommand.java, WeatherCommand.java
│   └── ... (autres catégories)
│
└── 📦 utils
    └── TabCompleterUtils.java    <- Aide pour les suggestions automatiques (filtre, etc.)
```

## Plan d'Action Détaillé

### **Étape 1 : Préparation de la Nouvelle Structure de Packages**

1.  Créer les nouveaux packages `api`, `engine`, `impl`, `utils` sous `src/main/java/fr/lampalon/lifemod/platform/bukkit/commands`.
2.  Créer les sous-packages `admin`, `moderation`, `player`, `utility`, `world`, etc., sous `src/main/java/fr/lampalon/lifemod/platform/bukkit/commands/impl`.

### **Étape 2 : Définition des Interfaces et Classes Clés (api)**

1.  **Refactoriser `LifeCommand.java` :**
    *   Déplacer `fr.lampalon.lifemod.common.commands.framework.LifeCommand` (l'ancienne version) vers `fr.lampalon.lifemod.platform.bukkit.commands.api.LifeCommand`.
    *   La transformer en **interface** ou en **classe abstraite** légère définissant :
        *   Méthodes `getName()`, `getAliases()`, `getPermission()`, `isPlayerOnly()`, `getDescription()`, `getUsage()`.
        *   Une méthode `execute(CommandContext context)`.
        *   Une méthode `onTabComplete(CommandContext context)`.
    *   L'ancienne classe `LifeCommand` sera renommée ou remplacée par cette nouvelle structure.

2.  **Créer `CommandContext.java` :**
    *   Créer cette classe dans `fr.lampalon.lifemod.platform.bukkit.commands.api`.
    *   Elle encapsulera :
        *   `CommandSender` Bukkit (pour l'accès direct si nécessaire).
        *   `String[] args`.
        *   Une référence à `LifeMod` (le plugin principal).
        *   Des méthodes pour accéder aux services (ex: `getLang()`, `getDebug()`).
        *   **Point clé NMS :** Une méthode `getNMSPlayer()` ou `getAbstractPlayer()` qui retournera une abstraction compatible NMS du joueur, permettant des interactions sans dépendre directement des classes `net.minecraft.server` spécifiques à la version. Ceci pourrait impliquer une interface `NMSPlayerAdapter` et des implémentations versionnées.

### **Étape 3 : Implémentation du Moteur d'Enregistrement (engine)**

1.  **Déplacer et Adapter `CommandRegistry.java` :**
    *   Déplacer `fr.lampalon.lifemod.platform.bukkit.managers.CommandRegistry` vers `fr.lampalon.lifemod.platform.bukkit.commands.engine.CommandRegistry`.
    *   Mettre à jour sa méthode `scanAndRegisterCommands` pour rechercher les implémentations de la *nouvelle interface/classe abstraite* `LifeCommand` et les instancier dynamiquement.
    *   Assurer que la logique d'enregistrement via le `CommandMap` (par réflexion) est correcte et robuste pour gérer les alias.

2.  **Déplacer et Adapter `BukkitCommandWrapper.java` :**
    *   Déplacer `fr.lampalon.lifemod.platform.bukkit.commands.adapter.BukkitCommandWrapper` vers `fr.lampalon.lifemod.platform.bukkit.commands.engine.BukkitCommandWrapper`.
    *   Mettre à jour son `execute` pour créer et passer un `CommandContext` à la nouvelle `LifeCommand.execute()`, et de même pour `tabComplete`.

### **Étape 4 : Utilitaire de Complétion (utils)**

1.  **Créer `TabCompleterUtils.java` :**
    *   Créer cette classe dans `fr.lampalon.lifemod.platform.bukkit.commands.utils`.
    *   Contiendra des méthodes utilitaires pour la complétion de tabulation (filtrage des noms de joueurs en ligne, suggestions d'énumérations, etc.).

### **Étape 5 : Refonte des Implémentations de Commandes (impl)**

1.  **Déplacer et Réécrire toutes les commandes existantes :**
    *   Chaque commande (`UnbanCmd`, `VanishCmd`, `GmCmd`, etc.) sera déplacée vers son sous-package `impl` approprié.
    *   Chaque commande sera réécrite pour implémenter la nouvelle `LifeCommand` et utiliser le `CommandContext` dans ses méthodes `execute` et `onTabComplete`.
    *   **Abstraction NMS :** Pour les commandes qui nécessitent des interactions spécifiques à la version du serveur, les appels directs aux API Bukkit/Spigot sensibles au NMS devront être remplacés par des appels à des méthodes abstraites dans `CommandContext` ou à des services d'abstraction NMS dédiés si la complexité le justifie.

### **Étape 6 : Mise à jour de `LifeMod.java`**

1.  Mettre à jour les imports et l'initialisation de `CommandRegistry` pour pointer vers le nouveau package (`fr.lampalon.lifemod.platform.bukkit.commands.engine`).
2.  Supprimer l'ancienne structure `fr.lampalon.lifemod.common.commands.framework`.

---

Ce document servira de guide tout au long du processus. Je vais maintenant commencer par l'**Étape 1 : Préparation de la Nouvelle Structure de Packages**.