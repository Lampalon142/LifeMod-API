# Audit PostHog — LifeMod Analytics

> Collecte de données privée pour améliorer LifeMod  
> **Public :** moi (le fondateur) uniquement — pas un module public

---

## Sommaire

1. [Vision](#1-vision)
2. [Configuration](#2-configuration)
3. [Architecture technique](#3-architecture-technique)
4. [Événements trackés](#4-événements-trackés)
5. [Propriétés globales](#5-propriétés-globales)
6. [Funnels — Analyse des parcours](#6-funnels--analyse-des-parcours)
7. [Dashboards — Tableaux de bord](#7-dashboards--tableaux-de-bord)
8. [Indicateurs clés](#8-indicateurs-clés)

---

## 1. Vision

LifeMod est un plugin de modération publique. PostHog est là pour **moi, le créateur** :

- Comprendre comment le plugin est utilisé sur les serveurs
- Savoir ce qui marche, ce qui est ignoré, ce qui bug
- Prioriser les améliorations basées sur des données réelles
- Détexter les tendances : baisse d'activité staff, pics de triche, etc.
- Mesurer l'efficacité des modérateurs (taux de réponse, actions, sanctions)
- Optimiser les parcours joueurs (rétention, récidive)

Ce n'est **pas** un module vendu aux serveurs — c'est mon outil de mesure interne.

---

## 2. Configuration

Ajout unique dans `config.yml` :

```yaml
posthog-enabled: false
```

Un seul boolean. API key **jamais en clair** dans le code — stockée sous forme XOR obfusquée.
Host hardcodé normalement :

```java
private static final String HOST = "https://app.posthog.com";
// API_KEY désobfusquée au runtime via XOR — jamais en string clair dans le bytecode
```

---

## 3. Architecture technique

### Sécurité de l'API key

**Problème :** Si la clé PostHog est en clair dans le code (`private static final String API_KEY = "phc_..."`), un adminserveur qui décompile LifeMod.jar peut voler la clé et envoyer de fausses données.

**Solution : XOR obfuscation**

```
API key originale  : "phc_nxLZeW3kEXznAb4gzakcC43aWQdCsU5CV97qdczR5Ybm"
                   ⊕ (XOR)
Mask (répété)      : "LifeMod-PostHog-2024"
                   =
Bytes stockés      : [0x3C, 0x01, 0x05, 0x3A, 0x23, ...]  (illisible dans le bytecode)
```

- Au runtime, `PostHogService` décode la clé via un XOR inverse
- `strings LifeMod.jar` ne montre **aucune** trace de la clé PostHog
- Le bytecode décompilé montre une boucle XOR + un tableau de bytes — pas la clé en clair
- Un reverse engineer déterminé POURRAIT extraire la clé (c'est pas du chiffrement), mais ça bloque 99% des gens

**Alternative (non retenue) :** Variable d'environnement `LIFEMOD_POSTHOG_KEY`. Plus sûr, mais nécessite de configurer chaque serveur manuellement. Le fondateur veut que ça marche out-of-the-box.

### Stack

```
Minecraft Server (Bukkit)
  → PostHogService
    → ConcurrentLinkedQueue<JsonObject>
      → ScheduledExecutorService (flush toutes les 5s)
        → POST /batch/ (HTTPS via HttpURLConnection)
          → PostHog Cloud
            → Dashboards + Funnels + Insights
```

### Fichier unique : `PostHogService.java`

| Point | Détail |
|---|---|
| Package | `fr.lampalon.lifemod.common.service` |
| Pattern | Classe simple, pas d'interface, pas de modèle séparé |
| Queue | `ConcurrentLinkedQueue<String>` (JSON strings prêts à envoyer) |
| Timer | `ScheduledExecutorService` avec 1 thread — flush toutes les 5s |
| HTTP | `HttpURLConnection` natif Java — zéro dépendance externe |
| Batching | Jusqu'à 100 events par requête POST /batch/ |
| Lifecycle | Créé dans `onEnable()` si activé, shutdown dans `onDisable()` |
| Résilience | Try/catch partout — si PostHog est down, le plugin continue normalement |
| Sécurité API key | XOR obfuscation avec mask dérivé du package — `strings LifeMod.jar` ne montre pas la clé |

---

## 4. Événements trackés

Tous les événements utilisent le préfixe `lifemod_` (ex: `lifemod_player_join`).

### 4.1 Sessions joueurs

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_player_join` | `PlayerJoin` (Bukkit) | `uuid` (hashé), `is_new`, `server_name` |
| `lifemod_player_first_join` | `PlayerJoin` (première visite) | `uuid` (hashé), `server_name` |
| `lifemod_player_quit` | `PlayerQuit` | `uuid` (hashé), `playtime_seconds`, `server_name` |

**Manquant (non tracké) :** `lifemod_player_login_blocked` (banni), `lifemod_player_join` côté Bungee

### 4.2 Sanctions

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_sanction` | `SanctionService.applySanction()` | `sanction_type`, `silent`, `duration_ms`, `auto_punish`, `has_reason` |
| `lifemod_sanction_pardon` | `SanctionService.revokeSanction()` | `sanction_type`, `has_reason` |
| `lifemod_auto_punish` | `SanctionService.checkAutoPunish()` | `sanction_type`, `duration_ms`, `reason_category`, `warning_count`, `threshold_triggered` |

**Manquant :** `lifemod_sanction_expired`, `lifemod_sanction` ne tracke pas `category`, `is_permanent`, ni `server_name`

### 4.3 Reports

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_report` | `ReportCommand` | `target`, `reason`, `server_name` |

**Manquant :** `lifemod_report_assigned`, `lifemod_report_status_changed`, `lifemod_report_note_added`

### 4.4 Staff Mode & Actions

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_staff_mode` | `StaffModeManager.enable/disableStaffMode()` | `action` (enable/disable) |
| `lifemod_freeze` | `FreezeManager.freeze/unfreezePlayer()` | `action` (freeze/unfreeze), `target` |
| `lifemod_staff_chat` | `StaffchatCommand` | `message_length`, `server_name` |
| `lifemod_vanish` | `VanishCommand` | `state`, `server_name` |
| `lifemod_spectate` | `SpectateManager` | `action` (start/stop), `target`, `server_name` |
| `lifemod_noclip` | `NoClipManager` | `action` (enable/disable), `server_name` |

**Manquant :** `lifemod_staff_action` (StaffActionManager — chaque action individuelle), `lifemod_follow`

### 4.5 Commandes

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_command` | `BukkitCommandWrapper` / `BukkitCommandAdapter` | `command_name`, `sender_type` (player/console), `has_permission`, `server_name` |
| `lifemod_command_failed` | `BukkitCommandWrapper` / `BukkitCommandAdapter` | `command_name`, `reason` (no_perm/player_only/error), `server_name` |

Événements **individuels** trackés en plus du générique (redondance assumée) :

| Événement | Commande | Propriétés |
|---|---|---|
| `lifemod_fly` | FlyCommand | `state`, `target`, `server_name` |
| `lifemod_gamemode` | GamemodeCommand | `gamemode`, `target`, `server_name` |
| `lifemod_heal` | HealCommand | `target`, `server_name` |
| `lifemod_feed` | FeedCommand | `target`, `server_name` |
| `lifemod_god` | GodModCommand | `state`, `target`, `server_name` |
| `lifemod_teleport` | TeleportCommand | `target`, `server_name` |
| `lifemod_speed` | SpeedCommand | `speed`, `type` (fly/walk), `server_name` |
| `lifemod_hearts` | HeartsCommand | `action` (set/add), `amount`, `target`, `server_name` |
| `lifemod_follow` | FollowCommand | `action` (start/stop), `target`, `server_name` |
| `lifemod_weather_set` | WeatherCommand | `weather`, `server_name` |
| `lifemod_time_set` | TimeCommand | `time`, `server_name` |
| `lifemod_difficulty_set` | DifficultyCommand | `difficulty`, `server_name` |
| `lifemod_clear_inv` | ClearinvCommand | `target`, `server_name` |
| `lifemod_broadcast` | BroadcastCommand | `message_length`, `server_name` |
| `lifemod_chat_clear` | ChatclearCommand | _(aucune propriété)_ |
| `lifemod_chat_toggle` | ToggleChatCommand | `state` (enabled/disabled), `server_name` |
| `lifemod_reload` | LifemodCommand | _(aucune propriété)_ |

**Manquant :** `lifemod_command_blocked` (ModeratorAuthListener)

### 4.6 Anti-Alt & Anti-VPN

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_antialt` | AntiAltManager (Bukkit) / BungeeAntiAltManager | `total_score`, `threshold_exceeded`, `rules_triggered`, `server_name` |
| `lifemod_antialt_pass` | AntiAltManager (connexion ignorée) | _(aucune propriété)_ |
| `lifemod_antivpn_block` | AntiVPNService | `reason`, `server_name` |
| `lifemod_antivpn_pass` | AntiVPNService (connexion autorisée) | _(aucune propriété)_ |
| `lifemod_antivpn_api_error` | AntiVPNService | `api_name`, `server_name` |

**Manquant :** `lifemod_antialt_ban_suggested` (score >= 85)

### 4.7 Chat

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_chat_filter` | `ChatManager.isBlocked()` | `reason` (blacklist), `word`, `server_name` |

**Manquant :** `lifemod_chat_message_sent`, `lifemod_chat_message_muted`, `lifemod_chat_blocked` (chat désactivé)

### 4.8 Outils de modération avancés

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_invsee` | `InvseeCommand` | `target`, `server_name` |
| `lifemod_history_view` | `HistoryCommand` | `target`, `server_name` |
| `lifemod_case_view` | `CaseCommand` | _(aucune propriété)_ |
| `lifemod_scan` | `ScanCommand` | `scan_type`, `target`, `results`, `server_name` |
| `lifemod_cps_high` | `CPSListener` (CPS > seuil) | `cps`, `target`, `server_name` |
| `lifemod_mod_auth` | `ModeratorAuthService` | `action`, `success`, `server_name` |
| `lifemod_mod_auth_fail` | `ModeratorAuthService` (échec) | `action`, `attempt`, `server_name` |

**Manquant :** `lifemod_invsee_modified` (InvseeListener), `lifemod_alts_checked` (AltsCommand/AltCommand), `lifemod_staff_history_viewed` (StaffHistoryCommand), `lifemod_cps_tested` (CPS normal)

### 4.9 Replay

**Aucun événement Replay tracké actuellement.**

**Manquant :** `lifemod_replay_recording_started`, `lifemod_replay_recording_stopped`, `lifemod_replay_played`

### 4.10 Performance & Technique

| Événement | Déclencheur | Propriétés |
|---|---|---|
| `lifemod_startup` | `PostHogService` (constructeur) | `plugin_version`, `platform` (bukkit/bungee), `server_version`, `java_version`, `database_type`, `redis_enabled`, `player_max` |
| `lifemod_environment` | `PostHogService.sendEnvironmentEvent()` | `os_name`, `os_arch`, `os_version`, `cpu_cores`, `max_memory_mb`, `allocated_memory_mb`, `plugin_version`, `platform` |
| `lifemod_config_snapshot` | `PostHogService.sendConfigSnapshot()` | `module_*` (booléens), `database_type`, `redis_enabled`, `commands_enabled_count`, `plugin_version`, `platform` |
| `lifemod_shutdown` | `LifeMod.onDisable()` / `BungeeLifeMod.onDisable()` | `plugin_version`, `platform`, `uptime_seconds` |

### Vue d'ensemble

| Catégorie | Nb events réels | Nb events audit (cible) | Écart |
|---|---|---|---|
| Sessions joueurs | 3 | 3 | ✅ |
| Sanctions | 3 | 4 | ❌ `sanction_expired` |
| Reports | 1 | 4 | ❌ assigned, status_changed, note_added |
| Staff mode | 6 | 5 | +1 (noclip) mais ❌ `staff_action` |
| Commandes | 18 | 2 | Beaucoup + de détails (redondant avec le générique) |
| Anti-Alt/VPN | 5 | 3 | +2 (pass, api_error) mais ❌ `ban_suggested` |
| Chat | 1 | 3 | ❌ message_sent, message_muted |
| Outils avancés | 7 | 8 | ❌ invsee_modified, alts_checked, staff_history |
| Replay | 0 | 3 | ❌ Aucun |
| Technique | 4 | 2 | +2 (environment, config_snapshot) |
| **Total** | **~49** | **~37** | **48 réels vs 37 documentés** |

---

## 5. Propriétés globales

Propriétés définies au niveau du service (`PostHogService`) et incluses via `capture()` :

| Propriété | Source | Description |
|---|---|---|
| `distinct_id` | `serverName` (config) | Nom du serveur configuré (pas de hash — le nom réel est envoyé) |
| `plugin_version` | `getDescription().getVersion()` | Version du plugin |
| `platform` | `"bukkit"` ou `"bungee"` | Plateforme |

Les autres propriétés (`server_version`, `java_version`, `database_type`, `redis_enabled`) ne sont **pas** globales — elles sont envoyées uniquement dans `lifemod_startup` et `lifemod_environment`.

**Amélioration possible :** Ajouter `server_name`, `server_version`, `platform` comme propriétés globales sur chaque appel `capture()`.

---

## 6. Funnels — Analyse des parcours

### 6.1 Report → Résolution

```
report_created
  → report_assigned
    → report_note_added
      → report_status_changed (CLOSED | REJECTED)
```

**Objectif :** Mesurer l'efficacité du système de reports.

**Métriques :**
- Taux de conversion : % de reports assignés, % résolus
- Temps moyen par étape (création → assignation → résolution)
- Taux d'abandon : reports jamais assignés, jamais résolus
- Temps de résolution moyen (médiane, p90)

### 6.2 Sanction → Récidive

```
sanction_applied (WARN)
  → sanction_applied (BAN/MUTE plus tard)
```

**Objectif :** Savoir si les avertissements sont efficaces et à quel seuil les joueurs récidivent.

**Métriques :**
- Taux de récidive : % de joueurs re-sanctionnés dans les 7/14/30 jours
- Temps moyen entre première sanction et récidive
- Escalade : WARN → BAN en combien d'étapes ?
- Corrélation entre la durée d'un ban et la récidive au retour

### 6.3 Staff — Activation modération

```
staff_mode_toggle (enabled)
  → staff_action (n'importe quelle)
    → staff_action (types différents)
      → staff_mode_toggle (disabled)
```

**Objectif :** Mesurer l'engagement et l'efficacité du staff.

**Métriques :**
- Durée moyenne d'une session de modération
- Nombre moyen d'actions par session
- Actions les plus utilisées (top 5)
- Staff inactif : en staff mode mais 0 action pendant 15 min
- Taux de staff qui utilisent toute la palette d'outils (> 5 types d'actions)

### 6.4 Anti-Alt → Action staff

```
antialt_analysis (score >= 85)
  → antialt_ban_suggested
    → sanction_applied (BAN)
```

**Objectif :** Mesurer la pertinence de l'anti-alt et la réactivité du staff.

**Métriques :**
- Taux de suggestions de ban suivies d'action
- Taux de faux positifs : score élevé mais pas de ban
- Délai entre l'alerte et la sanction
- Quels flags (règles) sont les plus prédictifs d'une infraction réelle ?
- Distribution des scores (moyen, médian, p90)

### 6.5 Nouveau joueur → Rétention

```
player_join (première session)
  → player_join (session J+1)
    → player_join (session J+7)
      → player_join (session J+30)
```

**Objectif :** Comprendre la rétention des nouveaux joueurs pour améliorer le plugin.

**Métriques :**
- Rétention J1, J7, J30
- Temps de jeu médian avant abandon
- Temps de jeu moyen par session (évolution dans le temps)
- Effet de l'activité staff sur la rétention

### 6.6 Commande → Utilisation

```
command_executed (première utilisation)
  → command_executed (utilisation régulière J+7)
```

**Objectif :** Identifier les fonctionnalités adoptées vs ignorées pour prioriser le développement.

**Métriques :**
- Taux d'adoption par commande
- Commandes les plus utilisées / les moins utilisées
- Ratio commandes staff vs commandes joueurs

---

## 7. Dashboards — Tableaux de bord

Les dashboards ci-dessous sont ceux créés automatiquement par `PostHogSetup.java`.  
**Légende :** `✅` = présent dans PostHogSetup · `❌` = pas encore créé (à ajouter)

### 7.1 Dashboard : Activité Staff (PostHogSetup: "LifeMod — Staff Tools")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Staff Mode toggles / jour | Barres quotidiennes | `lifemod_staff_mode` | ✅ |
| Freezes / jour (7j) | Line chart | `lifemod_freeze` | ✅ |
| Staff Chat messages / jour | Line chart | `lifemod_staff_chat` | ✅ |
| Vanish toggles / jour | Barres | `lifemod_vanish` | ✅ |
| Spectates / jour | Barres | `lifemod_spectate` | ✅ |
| NoClip toggles / jour | Barres | `lifemod_noclip` | ✅ |
| Actions staff par type | Pie chart | `lifemod_staff_action` | ❌ (event inexistant) |
| Durée moyenne session staff | Line chart | `lifemod_staff_mode` | ❌ |
| Rapports créés / jour (7j) | Barres | `lifemod_report` | ✅ |

### 7.2 Dashboard : Sanctions & Auto-Punish (PostHogSetup: "LifeMod — Sanctions & Modération")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Sanctions / jour (7j) | Line chart | `lifemod_sanction` | ✅ |
| Répartition par type | Pie chart | `lifemod_sanction.sanction_type` | ✅ |
| Bans silencieux vs publics | Barres | `lifemod_sanction.silent` | ✅ |
| Durées des sanctions | Barres | `lifemod_sanction.duration_ms` | ✅ |
| % sanctions automatiques | Pie chart | `lifemod_sanction.auto_punish` | ✅ |
| Auto-punish par catégorie | Barres | `lifemod_auto_punish.reason_category` | ✅ |
| Révocations / jour | Line chart | `lifemod_sanction_pardon` | ✅ |
| Délai avant révocation | Histogramme | `lifemod_sanction_pardon` | ❌ |

### 7.3 Dashboard : Reports (pas encore de dashboard dédié dans PostHogSetup)

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Reports créés par jour | Barres quotidiennes | `lifemod_report` | ✅ (dans Staff Tools) |
| Statut des reports | Pie chart | manquant | ❌ |
| Temps de résolution moyen | Line chart | manquant | ❌ |
| Notes ajoutées par report | Histogramme | manquant | ❌ |

### 7.4 Dashboard : Joueurs & Rétention (PostHogSetup: "LifeMod — Joueurs & Rétention")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Joueurs uniques / jour | Line chart | `lifemod_player_join` (unique_user) | ✅ |
| Nouveaux joueurs / jour | Barres | `lifemod_player_first_join` | ✅ |
| Temps de jeu moyen / jour | Line chart | `lifemod_player_quit` | ✅ |
| Distribution du temps de jeu | Histogramme | `lifemod_player_quit.playtime_seconds` | ❌ |

### 7.5 Dashboard : Anti-Alt & Sécurité (PostHogSetup: "LifeMod — Sécurité")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Blocages VPN / jour (7j) | Line chart | `lifemod_antivpn_block` | ✅ |
| Raisons de blocage VPN | Pie chart | `lifemod_antivpn_block.reason` | ✅ |
| Détections Anti-Alt / jour (7j) | Barres | `lifemod_antialt` | ✅ |
| Sévérité des détections | Pie chart | `lifemod_antialt.threshold_exceeded` | ✅ |
| Actions prises | Barres | `lifemod_antialt.rules_triggered` | ✅ |

### 7.6 Dashboard : Commandes & Utilisation (PostHogSetup: "LifeMod — Commandes & Usage")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Commandes / jour (7j) | Line chart | `lifemod_command` | ✅ |
| Commandes / jour (28j, hebdo) | Line chart | `lifemod_command` | ✅ |
| Top 10 commandes | Barres | `lifemod_command.command_name` | ✅ |
| Commandes échouées / jour | Barres | `lifemod_command_failed` | ✅ |
| Raisons d'échec | Pie chart | `lifemod_command_failed.reason` | ✅ |
| Commandes individuelles (fly, gm, etc.) | Barres groupées | événements individuels | ❌ |

### 7.7 Dashboard : Chat (PostHogSetup: "LifeMod — Chat")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Mots bloqués par jour | Barres | `lifemod_chat_filter` | ✅ |
| Top mots bloqués | Barres | `lifemod_chat_filter.word` | ✅ |
| Chat toggles / jour | Barres | `lifemod_chat_toggle` | ✅ |
| Chat clears / jour | Barres | `lifemod_chat_clear` | ✅ |

### 7.8 Dashboard : Outils avancés (PostHogSetup: "LifeMod — Outils Avancés")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Invsee / jour | Barres | `lifemod_invsee` | ✅ |
| History views / jour | Barres | `lifemod_history_view` | ✅ |
| Scans / jour par type | Barres | `lifemod_scan.scan_type` | ✅ |
| CPS élevés / jour | Barres | `lifemod_cps_high` | ✅ |
| Auth PIN (succès/échec) | Barres stackées | `lifemod_mod_auth` / `lifemod_mod_auth_fail` | ✅ |
| Case views / jour | Barres | `lifemod_case_view` | ✅ |

### 7.9 Dashboard : Replay (pas encore de dashboard dans PostHogSetup)

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Enregistrements / jour | Line chart | manquant | ❌ |
| Replays visionnés / jour | Barres | manquant | ❌ |

### 7.10 Dashboard : Technique (PostHogSetup: "LifeMod — Performance & Santé" + "LifeMod — Vue Générale" + "LifeMod — Configuration")

| Graphique | Type | Événement | Status |
|---|---|---|---|
| Serveurs actifs (7j) | Barres | `lifemod_startup` (unique_user) | ✅ |
| Instances actives (30j) | Line chart | `lifemod_startup` (unique_user) | ✅ |
| Par version LifeMod | Barres | `lifemod_startup.plugin_version` | ✅ |
| Par plateforme | Pie chart | `lifemod_startup.platform` | ✅ |
| Par type de DB | Pie chart | `lifemod_startup.database_type` | ✅ |
| Redis activé ? | Pie chart | `lifemod_startup.redis_enabled` | ✅ |
| OS des serveurs | Barres | `lifemod_environment.os_name` | ✅ |
| Mémoire allouée | Barres | `lifemod_environment.allocated_memory_mb` | ✅ |
| Modules activés (table) | Table | `lifemod_config_snapshot` | ✅ |
| Commandes activées (nb) | Barres | `lifemod_config_snapshot.commands_enabled_count` | ✅ |
| Uptime moyen (heures) | Barres (avg) | `lifemod_shutdown.uptime_seconds` | ✅ |
| Shutdowns / jour (7j) | Line chart | `lifemod_shutdown` | ✅ |

---

## 8. Indicateurs clés

### 8.1 KPIs Staff

| Métrique | Calcul | Objectif indicatif |
|---|---|---|
| Actions staff / jour / serveur | `COUNT(staff_action)` par jour | > 20 |
| Taux d'actions variées | Types distincts / total actions | > 40% |
| Sanctions / jour / serveur | `COUNT(sanction_applied)` par jour | > 5 |
| Temps de réponse aux reports | Temps médian création → assignation | < 30 min |
| Taux de résolution reports | Reports clos / reports créés (rolling 7j) | > 80% |
| Taux de révocation | Sanctions révoquées / sanctions appliquées | < 15% |
| Durée session staff | Temps moyen entre enable et disable | > 30 min |
| Taux d'activité en staff mode | Temps actions / temps total staff mode | > 60% |

### 8.2 KPIs Joueurs

| Métrique | Calcul |
|---|---|
| Rétention J1 | % de nouveaux joueurs revus le lendemain |
| Rétention J7 | % de nouveaux joueurs revus 7 jours après |
| Temps de jeu moyen par session | `AVG(player_quit.playtime_seconds)` |
| Taux de récidive | Joueurs sanctionnés → re-sanctionnés dans les 14 jours |
| Taux de faux positifs anti-alt | Alertes priority/ban sans sanction dans l'heure |

### 8.3 KPIs Plugin

| Métrique | Calcul | Utilité |
|---|---|---|
| Serveurs actifs | `COUNT(DISTINCT distinct_id)` sur 24h | Savoir combien de serveurs utilisent LifeMod |
| Version la plus utilisée | Mode de `server_startup.plugin_version` | Prioriser le support |
| Top 3 modules désactivés | `modules_enabled` = false | Améliorer ou supprimer ces modules |
| Commandes jamais utilisées | `command_executed.command_name` absent en 30j | Envisager de les retirer ou les améliorer |
| Taux de crash / erreur | `server_shutdown` sans `server_startup` propre | Stabilité |

### 8.4 Alertes PostHog

- **« Plus de 10 reports non assignés depuis 24h »** → le staff ne suit pas
- **« Aucune action staff depuis 2h sur [serveur] »** → staff inactif
- **« Score anti-alt moyen > 60 aujourd'hui »** → pic de triche potentiel
- **« -50% actions staff vs hier »** → baisse d'activité inhabituelle
- **« 500% de connexions vs moyenne »** → possible attaque ou événement
- **« Rétention J7 en baisse de 20% »** → problème d'expérience joueur