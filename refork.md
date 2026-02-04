# Plan de Refonte (Refork) v3 - LifeMod (Monorepo Simplifié)

Ce document présente une version simplifiée du plan de refonte, utilisant un **projet unique (Single Module)** tout en conservant les objectifs de compatibilité multi-plateforme, haute performance et extensibilité.

---

## 1. Structure du Projet (Package-based)

Au lieu de modules séparés, nous utiliserons une organisation par packages pour isoler la logique métier du code spécifique aux plateformes.

```text
src/main/java/fr/lampalon/lifemod
├── common              // Code partagé (Modèles, Services, Logique Redis, Config)
│   ├── model           // POJOs (User, Report, Sanction)
│   ├── service         // Logique métier (ModerationService, ChatService)
│   ├── storage         // Data Access (SQL, Redis)
│   └── messaging       // Sync inter-serveur (Redis Pub/Sub)
├── platform            // Code spécifique aux APIs
│   ├── bukkit          // Implémentation Bukkit (Listeners, InvUI, Commandes)
│   └── velocity        // (Optionnel) Implémentation Velocity
└── integration         // Bibliothèques tierces
    ├── nms             // Wrapper PacketEvents
    └── gui             // Wrapper InvUI
```

---

## 2. Piliers Techniques Intégrés

*   **Java Fondamental** :
    *   **Abstraction** : Utilisation intensive d'interfaces dans `common` pour que la logique métier ne dépende jamais directement de Bukkit.
    *   **Héritage** : Un système de commandes "Base" étendu par des implémentations spécifiques.
*   **PacketEvents** : Intégré pour gérer le NMS de manière universelle sans imports de versions spécifiques (v1_16_R3, etc.).
*   **InvUI** : Utilisé pour tous les menus dans le package `platform.bukkit.gui`.
*   **Redis** : Utilisé pour la synchronisation des données entre serveurs et le cache rapide.

---

## 3. Plan d'Actions (Simplifié)

### Étape 1 : Réorganisation des Packages
1.  Créer la structure `common`, `platform`, `integration`.
2.  Déplacer les utilitaires globaux dans `common.utils`.
3.  Mettre à jour le `pom.xml` avec toutes les dépendances (PacketEvents, InvUI, Jedis, HikariCP, Bukkit API, Velocity API).

### Étape 2 : Couche d'Abstraction (Le "Cœur")
1.  **Services** : Créer des interfaces pour chaque fonctionnalité (ex: `IModerationService`).
2.  **Storage** : Créer un `DatabaseManager` unique gérant à la fois Redis (pour le cache) et SQL (pour la persistance).
3.  **Config** : Centraliser le chargement des fichiers YAML dans `common.config`.

### Étape 3 : Implémentation Bukkit (NMS & GUI)
1.  **PacketEvents** : Initialiser PacketEvents dans le `onEnable` de la classe principale.
2.  **InvUI** : Créer une classe de base pour les menus utilisant InvUI afin de simplifier la création de fenêtres.
3.  **Commandes** : Créer un `BukkitCommandWrapper` qui fait le pont entre les commandes `common` et le système de Bukkit.

### Étape 4 : Synchronisation Redis
1.  Mettre en place un système de messages simple (Action + Données).
2.  Exemple : Quand un joueur est freeze sur un serveur, envoyer l'info via Redis pour que les autres serveurs soient au courant (utile pour le staff global).

---

## 4. Conventions de Développement

1.  **Zéro Statique** : Tout passe par les constructeurs. La classe `LifeMod` (Main) instancie les services et les distribue.
2.  **DRY (Don't Repeat Yourself)** : Si un code est utilisé par Bukkit et potentiellement Velocity, il DOIT être dans `common`.
3.  **Naming** :
    *   Interfaces : Commencer par `I` (ex: `IUserRepository`) ou utiliser le nom simple et suffixer l'implémentation (ex: `UserService` -> `BukkitUserService`).
4.  **Performance** :
    *   Utiliser des `Map` concurrentes pour les données en mémoire.
    *   Toujours utiliser l'asynchronisme pour Redis/SQL.

---

## 5. Pourquoi cette structure ?

*   **Simplicité** : Un seul `pom.xml`, un seul build, un seul JAR.
*   **Compatibilité** : En utilisant des interfaces, on peut ajouter Velocity plus tard simplement en ajoutant des classes dans `platform.velocity`.
*   **Maintenance** : Le code NMS (PacketEvents) et GUI (InvUI) est isolé, ce qui facilite les mises à jour de ces librairies.
