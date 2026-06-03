# LifeMod — Analyse & Audit PostHog

> Stratégie complète de tracking produit pour LifeMod (Bukkit + Bungee)
> Ce document définit quoi tracke, pourquoi, comment, et quoi en faire.

---

## Sommaire

1. [Philosophie & Objectifs](#1-philosophie--objectifs)
2. [Taxonomie des événements](#2-taxonomie-des-événements)
3. [Schéma des propriétés](#3-schéma-des-propriétés)
4. [Liste exhaustive des événements](#4-liste-exhaustive-des-événements)
5. [Group Analytics & Personnes](#5-group-analytics--personnes)
6. [Dashboards & Visualisations](#6-dashboards--visualisations)
7. [Funnels & Parcours](#7-funnels--parcours)
8. [Comparaisons & Segmentation](#8-comparaisons--segmentation)
9. [HogQL & Requêtes avancées](#9-hogql--requêtes-avancées)
10. [Alerts & Monitoring](#10-alerts--monitoring)
11. [Volume de données estimé](#11-volume-de-données-estimé)
12. [Plan d'implémentation](#12-plan-dimplémentation)

---

## 1. Philosophie & Objectifs

### Pourquoi on tracke ?

| Objectif | Comment | Métrique clé |
|----------|---------|-------------|
| **Améliorer le produit** | Savoir quelles features sont utilisées ou ignorées | Taux d'activation, utilisation par serveur |
| **Détecter les régressions** | Surveiller les erreurs par version | Erreurs/version, taux de crash |
| **Prioriser le développement** | Data > intuition | Top 10 commandes, modules les plus activés |
| **Fidéliser les serveurs** | Comprendre pourquoi les serveurs restent ou partent | Rétention J7/J30/J90 |
| **Optimiser les performances** | Identifier les goulots d'étranglement | Temps de requête DB, mémoire |

### Principes

1. **Tout événement doit répondre à une question** — si on ne sait pas quoi en faire, on ne le tracke pas
2. **Sanitized** — jamais de pseudo, UUID, IP, raison exacte, message
3. **Propriétés consistantes** — mêmes noms, mêmes types partout
4. **Distinct ID = serveur** — on analyse les serveurs, pas les joueurs

---

## 2. Taxonomie des événements

### Convention de nommage

```
lifemod_<domaine>_<action>
```

| Domaine | Exemples |
|---------|----------|
| `startup` | Cycle de vie du plugin |
| `config` | Configuration |
| `player` | Événements joueurs |
| `sanction` | Sanctions |
| `staff` | Outils staff |
| `command` | Commandes |
| `antivpn` | Anti-VPN |
| `antialt` | Anti-Alt |
| `chat` | Chat |
| `error` | Erreurs |
| `performance` | Performance |

### Niveaux de sévérité

| Niveau | Usage |
|--------|-------|
| **info** | Action normale (commande exécutée, joueur joint) |
| **success** | Action réussie (pardon, guérison) |
| **warning** | Action suspecte (CPS haut, tentative d'évitement) |
| **error** | Échec / exception |
| **critical** | Crash, boucle infinie, pool épuisé |

---

## 3. Schéma des propriétés

### Propriétés globales (sur TOUS les événements)

Ces propriétés doivent être ajoutées automatiquement à chaque `capture()` :

```yaml
plugin_version: string    # "2.0.0-SNAPSHOT-DEV"
platform: string           # "bukkit" | "bungee"
server_name: string        # distinct_id (déjà utilisé)
hour_of_day: integer       # 0-23
day_of_week: integer       # 0=Dim, 6=Sam
timestamp_iso: string      # ISO 8601
```

### Propriétés contextuelles (selon l'événement)

```yaml
# === COMMUNES MODÉRATION ===
sanction_type: string        # "BAN" | "KICK" | "MUTE" | "WARN"
duration_ms: integer         # durée en ms (0 = permanent)
duration_human: string       # "7d" | "permanent" | "30m"
silent: boolean              # sanction silencieuse ?
auto_punish: boolean         # automatique ?
has_reason: boolean          # raison fournie ?
issued_by_console: boolean   # console ou joueur ?

# === COMMUNES STAFF ===
action: string               # "enable" | "disable" | "start" | "stop"
target_self: boolean         # appliqué à soi-même ?
duration_seconds: integer    # durée de session

# === COMMUNES ANTI ===
reason: string               # motif du blocage
severity: string             # "alert" | "priority" | "suggest_ban"
score_range: string          # "30-49" | "50-69" | "70-84" | "85-100"
```

### Types de propriétés standardisés

| Type PostHog | Type Java | Exemple |
|-------------|-----------|---------|
| `String` | `String` | `"BAN"` |
| `Boolean` | `Boolean` | `true` |
| `Integer` | `Integer` / `Long` | `42` |
| `Float` | `Double` | `2.5` |
| `Array` | `List<String>` | `["ip_exact", "vpn"]` |

---

## 4. Liste exhaustive des événements

### 4.1 Cycle de vie

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 1 | `lifemod_startup` | `plugin_version`, `platform`, `server_version`, `java_version`, `database_type`, `redis_enabled`, `player_max` | 1/démarrage | Quelles versions tournent ? |
| 2 | `lifemod_shutdown` | `uptime_seconds`, `plugin_version`, `platform` | 1/arrêt | Les serveurs crash ou s'arrêtent proprement ? |
| 3 | `lifemod_config_snapshot` | `module_*`, `commands_enabled_count`, `database_type`, `redis_enabled` | 1/démarrage | Quels modules sont activés ? |
| 4 | `lifemod_environment` | `os_name`, `os_arch`, `cpu_cores`, `max_memory_mb`, `allocated_memory_mb` | 1/démarrage | Sur quel hardware tourne le plugin ? |

### 4.2 Joueurs

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 5 | `lifemod_player_join` | `player_count`, `is_new_player` | 1/join | Combien de joueurs uniques ? |
| 6 | `lifemod_player_quit` | `player_count`, `session_seconds` | 1/quit | Sessions courtes ou longues ? |
| 7 | `lifemod_player_first_join` | `player_count` | 1/joueur | Combien de nouveaux joueurs ? |
| 8 | `lifemod_player_kick` | `has_reason` | 1/kick | Volume de kicks non-sanction |
| 9 | `lifemod_player_chat` | `word_count`, `has_link`, `has_mention` | ~1/msg | Activité chat (samplé à 1/10) |
| 10 | `lifemod_player_command_blocked` | `command_category` | 1/blocage | Quelles commandes sont bloquées ? |

### 4.3 Commandes

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 11 | `lifemod_command` | `command_name`, `has_arguments` | 1/cmd | Top/flop commandes ? |
| 12 | `lifemod_command_failed` | `command_name`, `fail_reason` (no_perm/not_found/error) | 1/échec | Commandes qui échouent souvent ? |

### 4.4 Sanctions & Modération

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 13 | `lifemod_sanction` | `sanction_type`, `duration_ms`, `silent`, `auto_punish`, `has_reason`, `issued_by_console` | 1/sanction | Combien de bans/kicks/mutes ? |
| 14 | `lifemod_sanction_expire` | `sanction_type`, `duration_human` | 1/expiration | Les temporaires sont-ils assez longs ? |
| 15 | `lifemod_sanction_pardon` | `sanction_type`, `has_reason` | 1/pardon | Combien de débannissements ? |
| 16 | `lifemod_auto_punish` | `sanction_type`, `reason_category`, `warning_count`, `threshold_triggered` | 1/auto | L'auto-punish est-il efficace ? |
| 17 | `lifemod_warn` | `duration_ms`, `threshold_after` | 1/warn | À partir de combien de warns on ban ? |
| 18 | `lifemod_report` | `has_reason` | 1/report | Volume de signalements |
| 19 | `lifemod_note` | `action` (create/delete), `has_content` | 1/note | Les staff prennent des notes ? |
| 20 | `lifemod_history_view` | `target_type` (player/alt), `result_count` | 1/consultation | Histoires consultées |
| 21 | `lifemod_alts_check` | `alts_found_count` | 1/check | Combien d'alts en moyenne ? |
| 22 | `lifemod_case_view` | — | 1/consultation | Cas consultés |

### 4.5 Staff Tools

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 23 | `lifemod_staff_mode` | `action`, `duration_seconds` | 1/toggle | Durée session staff ? |
| 24 | `lifemod_vanish` | `action` | 1/toggle | Staff utilisent vanish ? |
| 25 | `lifemod_staff_chat` | `word_count` | 1/msg | Activité staffchat |
| 26 | `lifemod_mod_auth` | `success` | 1/tentative | Échecs de auth ? |
| 27 | `lifemod_mod_auth_fail` | `current_attempt` | 1/échec | Tentatives avant succès ? |
| 28 | `lifemod_freeeze` | `action`, `duration_seconds` | 1/toggle | Durée des freezes |
| 29 | `lifemod_invsee` | `interaction` (view/edit), `inventory_type` (player/ender) | 1/ouverture | Invsee utilisé ? |
| 30 | `lifemod_spectate` | `action` | 1/toggle | Spectate utilisé ? |
| 31 | `lifemod_follow` | `action` | 1/toggle | Follow utilisé ? |
| 32 | `lifemod_noclip` | `action` | 1/toggle | Noclip utilisé ? |
| 33 | `lifemod_gamemode` | `gamemode` (creative/survival/adventure/spectator) | 1/changement | Gamemodes les plus utilisés |
| 34 | `lifemod_fly` | `action` | 1/toggle | Fly utilisé ? |
| 35 | `lifemod_speed` | `speed_type` (fly/walk), `speed_value` | 1/changement | Speed moyenne ? |
| 36 | `lifemod_heal` | `target_self` | 1/soin | Auto-soin vs soin autre |
| 37 | `lifemod_feed` | `target_self` | 1/feed | Auto-feed vs feed autre |
| 38 | `lifemod_god` | `action` | 1/toggle | God mode utilisé ? |
| 39 | `lifemod_scan` | `results_count` | 1/scan | Scans faits ? |
| 40 | `lifemod_debug` | `action` | 1/toggle | Debug utilisé ? |

### 4.6 Anti-Cheat & Sécurité

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 41 | `lifemod_antivpn_block` | `reason` (vpn/proxy/geo/isp/bot_limit/bot_cooldown) | 1/blocage | Quelles menaces sont bloquées ? |
| 42 | `lifemod_antivpn_pass` | `checks_count` | 1/pass | Ratio blocage/pass |
| 43 | `lifemod_antivpn_api_error` | `api_name` | 1/erreur | L'API AntiVPN est fiable ? |
| 44 | `lifemod_antialt` | `score_range`, `severity`, `signals_count`, `action_taken` | 1/détection | Détections par sévérité |
| 45 | `lifemod_antialt_pass` | — | 1/analyse | Ratio détection/pass |
| 46 | `lifemod_cps_high` | `cps_value` | 1/détection | CPS suspects |
| 47 | `lifemod_chat_filter` | `action` (blocked/replaced), `word_category` | 1/blocage | Filtre chat efficace ? |

### 4.7 Utilitaires

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 48 | `lifemod_broadcast` | `has_message` | 1/broadcast | Broadcasts faits |
| 49 | `lifemod_chat_clear` | — | 1/clear | Chat clear |
| 50 | `lifemod_chat_toggle` | `action` (lock/unlock) | 1/toggle | Chat lock |
| 51 | `lifemod_time_set` | `time_value` | 1/changement | Heure définie |
| 52 | `lifemod_weather_set` | `weather_type` (clear/rain/storm) | 1/changement | Météo changée |
| 53 | `lifemod_difficulty_set` | `difficulty` (peaceful/easy/normal/hard) | 1/changement | Difficulté changée |
| 54 | `lifemod_clear_inv` | `target_self` | 1/clear | Inventaires clear |
| 55 | `lifemod_hearts` | `target_self` | 1/affichage | Coeurs affichés |
| 56 | `lifemod_teleport` | `teleport_type` (tp/tphere/follow/otp), `distance_category` (same_world/cross_world) | 1/tp | Tps effectués |

### 4.8 Système & Admin

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 57 | `lifemod_reload` | — | 1/reload | Recharges fréquentes ? |
| 58 | `lifemod_replay` | `action` (start/stop), `ticks_count` | 1/usage | Replay utilisé ? |
| 59 | `lifemod_reaction` | `reaction_type` | 1/réaction | Réactions envoyées |
| 60 | `lifemod_stafflist` | — | 1/consultation | Stafflist consultée |
| 61 | `lifemod_discord` | `webhook_type` (sanction/chat/join/staff), `success` | 1/envoi | Discord webhook OK ? |

### 4.9 Performance & Erreurs

| # | Event | Propriétés | Volume | Question |
|---|-------|-----------|--------|----------|
| 62 | `lifemod_error` | `exception_class`, `exception_message`, `origin`, `plugin_version`, `platform` | 1/erreur | Quels bugs ? |
| 63 | `lifemod_error_rate` | `error_count_5m`, `origin` | toutes les 5min | Détection précoce de crash |
| 64 | `lifemod_database_perf` | `db_type`, `query_count`, `avg_query_time_ms`, `slow_queries`, `pool_active`, `pool_idle` | toutes les 6h | Performance DB |
| 65 | `lifemod_memory_warning` | `allocated_memory_mb`, `max_memory_mb`, `usage_percent` | 1/quand >90% | Mémoire sous-dimensionnée |

---

## 5. Group Analytics & Personnes

### Server = Person (distinct_id)

Chaque serveur est une "personne" dans PostHog. Propriétés de la personne :

| Propriété | Type | Set quand | Exemple |
|-----------|------|-----------|---------|
| `server_name` | String | startup | `"Survival"` |
| `server_version` | String | startup | `"1.21.4"` |
| `plugin_version` | String | startup | `"2.0.0"` |
| `platform` | String | startup | `"bukkit"` |
| `database_type` | String | startup | `"sqlite"` |
| `os_name` | String | startup | `"Linux"` |
| `max_memory_mb` | Integer | startup | `4096` |
| `modules_enabled` | Array | startup | `["antialt", "chat_manager"]` |
| `is_bungee` | Boolean | startup | `false` |
| `first_seen` | DateTime | 1er startup | `"2026-01-15"` |
| `last_seen` | DateTime | chaque startup | `"2026-06-03"` |

Ces propriétés de personne permettent de :
- Filtrer les serveurs par version, OS, mémoire
- Comparer la rétention par taille de serveur
- Segmenter les serveurs qui utilisent MySQL vs SQLite

### Groupes (Group Analytics)

| Group Type | Key | Propriétés | Utilité |
|------------|-----|-----------|---------|
| `plugin_version` | version string | `release_date` | Suivi adoption version |
| `platform` | bukkit/bungee | — | Comparaison plateforme |

---

## 6. Dashboards & Visualisations

### Dashboard A — Vue Générale (Operational Overview)

**But :** En un coup d'œil, l'état de santé du réseau de serveurs.

| Widget | Type | Événement | Configuration |
|--------|------|-----------|---------------|
| **Serveurs actifs (7j)** | Carte (big number) | `lifemod_startup` | Uniq distinct_id, 7d |
| **Serveurs actifs (30j)** | Série temporelle | `lifemod_startup` | daily uniq, 30d |
| **Nouveaux serveurs (7j)** | Carte (big number) | `lifemod_startup` | uniq, 7d, first_seen dans la période |
| **Cumul lifetime** | Série temporelle (cumulative) | `lifemod_startup` | uniq cumulé, all time |
| **Par version** | Barres horizontales | `lifemod_startup` | breakdown `plugin_version`, 30d |
| **Par plateforme** | Camembert | `lifemod_startup` | breakdown `platform`, 30d |
| **Par type DB** | Camembert | `lifemod_startup` | breakdown `database_type`, 30d |
| **Par OS** | Barres | `lifemod_environment` | breakdown `os_name`, 30d |
| **Mémoire allouée** | Box plot | `lifemod_environment` | valeur `allocated_memory_mb` |
| **Rétention hebdo** | Table de rétention | `lifemod_startup` | Cohorte hebdomadaire |

### Dashboard B — Configuration & Adoption

**But :** Quels modules sont activés, quels réglages sont utilisés.

| Widget | Type | Événement | Configuration |
|--------|------|-----------|---------------|
| **Modules activés (%)** | Tableau | `lifemod_config_snapshot` | % true pour chaque `module_*` |
| **Évolution des modules** | Série temporelle | `lifemod_config_snapshot` | breakdown `module_*` |
| **Mode Geo AntiVPN** | Camembert | `lifemod_config_snapshot` | breakdown `antivpn_geo_mode` |
| **Mode Auto-Punish** | Camembert | `lifemod_config_snapshot` | breakdown `auto_punish_mode` |
| **Commandes activées** | Box plot | `lifemod_config_snapshot` | valeur `commands_enabled_count` |
| **Adoption version → modules** | Heatmap | `lifemod_config_snapshot` | X=version, Y=module_X |
| **Redis activé vs version** | Barres groupées | `lifemod_startup` | breakdown `redis_enabled` + `plugin_version` |

### Dashboard C — Commandes & Usage

| Widget | Type | Événement | Configuration |
|--------|------|-----------|---------------|
| **Commandes / jour** | Série temporelle | `lifemod_command` | daily total, 90j |
| **Commandes / serveur / jour** | Série temporelle (formule) | `lifemod_command` / `lifemod_startup` | A/B ratio |
| **Top 15 commandes** | Barres horizontales | `lifemod_command` | breakdown `command_name`, 30d |
| **Flop 5 commandes** | Barres horizontales | `lifemod_command` | breakdown `command_name`, tri asc, 30d |
| **Heatmap commandes** | Heatmap | `lifemod_command` | X=hour_of_day, Y=day_of_week |
| **Évolution top 5** | Série temporelle | `lifemod_command` | breakdown top 5 `command_name`, 90j |
| **Commandes avec/sans args** | Camembert | `lifemod_command` | breakdown `has_arguments` |
| **Taux d'échec des commandes** | Série temporelle | `lifemod_command_failed` | daily total |
| **Commandes par version** | Barres | `lifemod_command` | breakdown `plugin_version` |

### Dashboard D — Sanctions & Modération

| Widget | Type | Événement | Configuration |
|--------|------|-----------|---------------|
| **Sanctions / jour** | Série temporelle | `lifemod_sanction` | daily total, 90j |
| **Sanctions / serveur / jour** | Formule | `lifemod_sanction` / `lifemod_startup` | A/B ratio |
| **Répartition par type** | Camembert | `lifemod_sanction` | breakdown `sanction_type` |
| **Bans silencieux vs publics** | Barres | `lifemod_sanction` | Filtre BAN, breakdown `silent` |
| **Durées des sanctions** | Barres | `lifemod_sanction` | breakdown `duration_human` |
| **Durée médiane par type** | Tableau | `lifemod_sanction` | médiane `duration_ms` par `sanction_type` |
| **Taux d'auto-punish** | Série temporelle | `lifemod_sanction` / `lifemod_auto_punish` | % auto-punish / total |
| **Warns / jour** | Série temporelle | `lifemod_warn` | daily total |
| **Pardons / jour** | Série temporelle | `lifemod_sanction_pardon` | daily total |
| **Ratio sanction/pardon** | Formule | `lifemod_sanction` / `lifemod_sanction_pardon` | A/B |
| **Expirations / jour** | Série temporelle | `lifemod_sanction_expire` | daily total |
| **Warn threshold** | Barres | `lifemod_auto_punish` | breakdown `threshold_triggered` |

### Dashboard E — Staff Tools & Productivité

| Widget | Type | Événement | Configuration |
|--------|------|-----------|---------------|
| **Sessions staff / jour** | Série temporelle | `lifemod_staff_mode` | daily total |
| **Durée session staff** | Box plot | `lifemod_staff_mode` | valeur `duration_seconds` (action=disable) |
| **Actions par session** | Formule | (sanctions+freezes+cmds)/sessions | Ratio productivité |
| **Vanish toggles / jour** | Série temporelle | `lifemod_vanish` | daily total |
| **Freezes / jour** | Série temporelle | `lifemod_freeze` | daily total |
| **Durée freeze moyenne** | Box plot | `lifemod_freeze` | valeur `duration_seconds` (unfreeze) |
| **Invsee / jour** | Série temporelle | `lifemod_invsee` | daily total |
| **Gamemode usage** | Camembert | `lifemod_gamemode` | breakdown `gamemode` |
| **Auth fails / jour** | Série temporelle | `lifemod_mod_auth_fail` | daily total |
| **Taux succès auth** | Formule | `lifemod_mod_auth` (success=true) / total | % succès |
| **Noclip toggles** | Série temporelle | `lifemod_noclip` | daily total |
| **Scan results** | Barres | `lifemod_scan` | breakdown `results_count` |
| **Staff activity heatmap** | Heatmap | `lifemod_staff_mode` | X=hour, Y=day |

### Dashboard F — Sécurité & Anti-Cheat

| Widget | Type | Événement | Configuration |
|--------|------|-----------|---------------|
| **Blocages VPN / jour** | Série temporelle | `lifemod_antivpn_block` | daily total |
| **Raisons de blocage** | Camembert | `lifemod_antivpn_block` | breakdown `reason` |
| **Ratio block/pass** | Formule | `lifemod_antivpn_block` / `lifemod_antivpn_pass` | A/B |
| **Détections Anti-Alt / jour** | Série temporelle | `lifemod_antialt` | daily total |
| **Sévérité des détections** | Camembert | `lifemod_antialt` | breakdown `severity` |
| **Score ranges** | Barres | `lifemod_antialt` | breakdown `score_range` |
| **Actions prises** | Barres | `lifemod_antialt` | breakdown `action_taken` |
| **CPS high / jour** | Série temporelle | `lifemod_cps_high` | daily total |
| **Filtre chat / jour** | Série temporelle | `lifemod_chat_filter` | daily total |
| **Menaces par heure** | Heatmap | `lifemod_antivpn_block` | X=hour, Y=day |

### Dashboard G — Performance, Erreurs & Santé

| Widget | Type | Événement | Configuration |
|--------|------|-----------|---------------|
| **Erreurs / jour** | Série temporelle | `lifemod_error` | daily total, 90j |
| **Top 10 exceptions** | Barres | `lifemod_error` | breakdown `exception_class`, 30j |
| **Erreurs par version** | Barres groupées | `lifemod_error` | breakdown `plugin_version` |
| **Erreurs par contexte** | Barres | `lifemod_error` | breakdown `origin` |
| **Taux d'erreur / serveur** | Formule | `lifemod_error` uniq / `lifemod_startup` uniq | % serveurs impactés |
| **Erreur rate (5min)** | Série temporelle | `lifemod_error_rate` | Valeur `error_count_5m` |
| **Performance DB** | Série temporelle | `lifemod_database_perf` | Valeur `avg_query_time_ms`, breakdown `db_type` |
| **Mémoire max** | Box plot | `lifemod_environment` | valeur `max_memory_mb` |
| **Alertes mémoire** | Série temporelle | `lifemod_memory_warning` | daily total |
| **Connection pool** | Série temporelle | `lifemod_database_perf` | breakdown `pool_active`, `pool_idle` |

---

## 7. Funnels & Parcours

### Funnel A — Adoption d'une version

```
Objectif : % de serveurs qui migrent vers la dernière version

Étape 1 : lifemod_startup (plugin_version = ANCIENNE)
Étape 2 : lifemod_startup (plugin_version = NOUVELLE)

Fenêtre : 14 jours

Segments : par plateforme (Bukkit vs Bungee), par type DB
Interprétation : < 30% → problème d'adoption
```

### Funnel B — Activation → Utilisation (par module)

```
Objectif : % de serveurs qui activent un module puis l'utilisent

Étape 1 : lifemod_config_snapshot (module_X = true)
Étape 2 : lifemod_X (première action dans les 7j)

Modules :
  - AntiVPN : config(module_antivpn=true) → lifemod_antivpn_block
  - AntiAlt : config(module_antialt=true) → lifemod_antialt
  - Chat filter : config(module_chat_manager=true) → lifemod_chat_filter
  - Auto-punish : config(module_auto_punish=true) → lifemod_auto_punish

Fenêtre : 7 jours
Interprétation : drop > 50% → module mal compris ou inutile
```

### Funnel C — Workflow modération

```
Objectif : Les staff qui freeze finissent-ils par sanctionner ?

Étape 1 : lifemod_freeze (action = freeze)
Étape 2 : lifemod_sanction (sanction_type = BAN ou KICK)
Étape 3 : lifemod_freeze (action = unfreeze)

Fenêtre : 30 min (1→2), 5 min (2→3)

Segments : par heure, par gamemode
Interprétation : < 50% 1→2 → les staff freeze sans raison
```

### Funnel D — Signalement → Traitement

```
Objectif : Les reports sont-ils traités ?

Étape 1 : lifemod_report
Étape 2 : lifemod_freeze (action=freeze) OU lifemod_sanction

Fenêtre : 24h
Interprétation : < 20% → personne ne regarde les reports
```

### Funnel E — Session staff productive

```
Objectif : Les staff font-ils des actions utiles pendant leur session ?

Étape 1 : lifemod_staff_mode (action = enable)
Étape 2 : (lifemod_sanction OU lifemod_freeze OU lifemod_invsee) dans les 30 min
Étape 3 : lifemod_staff_mode (action = disable)

Fenêtre : 30 min (1→2)
Interprétation : sessions sans actions = staff AFK ou mode inutile
```

### Funnel F — Détection → Réaction

```
Objectif : Les staff réagissent-ils aux détections ?

Étape 1 : lifemod_antialt (severity = suggest_ban)
Étape 2 : lifemod_sanction (sanction_type = BAN)

Fenêtre : 1h
Segments : par score_range
Interprétation : < 30% → alertes ignorées
```

### Funnel G — Erreur → Survie

```
Objectif : Les serveurs qui ont une erreur restartent-ils ?

Étape 1 : lifemod_error
Étape 2 : lifemod_startup (dans les 24h)

Fenêtre : 24h
Breakdown : exception_class
Interprétation : < 100% → erreur fatale = abandon
```

### Funnel H — Premier contact → Activation

```
Objectif : Les nouveaux serveurs configurent-ils le plugin ?

Étape 1 : lifemod_startup (1er demarrage du serveur)
Étape 2 : lifemod_config_snapshot (J+1)

Fenêtre : 24h
Interprétation : drop > 30% → config par défaut pas bonne
```

---

## 8. Comparaisons & Segmentation

### 8.1 Version over Version (VoV)

Comparer TOUTES les métriques entre versions :

| Métrique | Comparaison | Format |
|----------|------------|--------|
| Nombre de serveurs | v1 vs v2 | Curves superposées |
| Erreurs / serveur | v1 vs v2 | Ratio |
| Commandes / serveur | v1 vs v2 | Ratio |
| Sanctions / serveur | v1 vs v2 | Ratio |
| Rétention J7 | v1 vs v2 | Tables côte à côte |
| Modules activés | v1 vs v2 | Heatmap différence |

### 8.2 Segmentation clé

| Segment | Critère | Utilité |
|---------|---------|---------|
| **Par taille de serveur** | `player_max` 0-20 / 20-50 / 50-100 / 100+ | Les petits serveurs utilisent-ils moins de modules ? |
| **Par type DB** | MySQL vs SQLite | Y a-t-il des différences de perf ? |
| **Par plateforme** | Bukkit vs Bungee | Quelles features sont Bungee-only ? |
| **Par OS** | Linux vs Windows | Y a-t-il plus d'erreurs sur Windows ? |
| **Par version** | v2.0.0 vs v2.1.0 | Adoption, régressions |
| **Par Redis** | Redis true vs false | Impact performance |
| **Par mémoire** | < 1GB / 1-2GB / 2-4GB / 4GB+ | Configuration recommandée |
| **Par âge serveur** | < 7j / 7-30j / 30-90j / 90j+ | Comportement par maturité |

### 8.3 Tableaux de bord comparatifs

```yaml
Dashboard "Comparaison v2.0.0 vs v2.1.0":
  - Courbes d'adoption superposées
  - Erreurs par version (barres groupées)
  - Sanctions par version (barres groupées)
  - Rétention J7 par version
  - Modules désactivés (Heatmap des différences)

Dashboard "MySQL vs SQLite":
  - Nombre de serveurs par type DB
  - Temps de requête moyen (box plot)
  - Erreurs par type DB
  - Commandes/serveur par type DB
```

---

## 9. HogQL & Requêtes avancées

Requêtes HogQL pour analyse approfondie :

### 9.1 Taux de rétention réel

```sql
SELECT 
  count(DISTINCT person_id) FILTER (WHERE day = 0) as "J0",
  count(DISTINCT person_id) FILTER (WHERE day = 7) as "J7",
  count(DISTINCT person_id) FILTER (WHERE day = 30) as "J30"
FROM (
  SELECT 
    person_id,
    dateDiff('day', min(timestamp) OVER (PARTITION BY person_id), timestamp) as day
  FROM events
  WHERE event = 'lifemod_startup'
)
```

### 9.2 Serveurs les plus actifs (top 10% sanctions)

```sql
SELECT 
  person_id,
  count() as sanctions_count
FROM events
WHERE event = 'lifemod_sanction'
  AND timestamp > now() - INTERVAL 30 DAY
GROUP BY person_id
ORDER BY sanctions_count DESC
LIMIT 10
```

### 9.3 Corrélation modules ↔ rétention

```sql
SELECT 
  properties.server_version as version,
  count(DISTINCT person_id) as serveurs,
  count(DISTINCT person_id) FILTER (
    WHERE event = 'lifemod_error'
  ) as avec_erreurs,
  count(DISTINCT person_id) * 1.0 / 
    (SELECT count(DISTINCT person_id) FROM events WHERE event = 'lifemod_startup') as ratio
FROM events
WHERE event IN ('lifemod_startup', 'lifemod_error')
  AND timestamp > now() - INTERVAL 30 DAY
GROUP BY version
```

### 9.4 Distribution des durées de sanction

```sql
SELECT 
  properties.sanction_type,
  round(avg(properties.duration_ms) / 3600000, 1) as avg_hours,
  round(median(properties.duration_ms) / 3600000, 1) as median_hours,
  count() as total
FROM events
WHERE event = 'lifemod_sanction'
  AND properties.duration_ms > 0
  AND timestamp > now() - INTERVAL 90 DAY
GROUP BY sanction_type
```

### 9.5 Sessions staff avec actions

```sql
SELECT 
  person_id,
  countIf(event = 'lifemod_staff_mode' AND properties.action = 'enable') as sessions,
  countIf(event = 'lifemod_sanction') as sanctions,
  countIf(event = 'lifemod_freeze') as freezes
FROM events
WHERE event IN ('lifemod_staff_mode', 'lifemod_sanction', 'lifemod_freeze')
  AND timestamp > now() - INTERVAL 30 DAY
GROUP BY person_id
```

---

## 10. Alerts & Monitoring

| Alerte | Condition | Canal | Action |
|--------|-----------|-------|--------|
| **Crash massif** | `lifemod_error` count > 100 en 1h (×5 moyenne 7j) | Email + Slack | Hotfix immédiat |
| **Nouvelle exception** | Nouvelle `exception_class` inconnue depuis 30j | Slack | Investiguer le bug |
| **Serveurs en chute** | `lifemod_startup` uniq < 50% moyenne 7j | Email | Problème d'adoption |
| **Sanctions à zéro** | `lifemod_sanction` = 0 pendant 48h | Slack | Plus aucun modo actif ? |
| **Staff mode à zéro** | `lifemod_staff_mode` = 0 pendant 7j | Slack | Staff désœuvré ? |
| **Anti-Alt suggest_ban** | `lifemod_antialt` severity=suggest_ban > 10 en 1h | Slack | Attaque d'alts |
| **Pic VPN** | `lifemod_antivpn_block` > 50 en 1h (×3 moyenne) | Slack | Attaque proxy |
| **Nouvelle version** | Nouveau `plugin_version` dans `lifemod_startup` | Slack | Surveiller les erreurs |
| **Mémoire critique** | `lifemod_environment` allocated_memory_mb > 90% max | Slack | Serveur sous-dimensionné |
| **DB ralentie** | `lifemod_database_perf` avg_query_time_ms > 1000ms | Slack | Problème de perf DB |

---

## 11. Volume de données estimé

| Type d'événement | Volume / serveur / jour | Pour 100 serveurs / mois |
|-----------------|------------------------|-------------------------|
| startup          | 1 (restart) | 3 000 |
| shutdown         | 1 | 3 000 |
| config_snapshot  | 1 | 3 000 |
| environment      | 1 | 3 000 |
| player_join      | 50 | 150 000 |
| player_quit      | 50 | 150 000 |
| command          | 200 | 600 000 |
| command_failed   | 5 | 15 000 |
| sanction         | 3 | 9 000 |
| auto_punish      | 1 | 3 000 |
| warn             | 2 | 6 000 |
| report           | 1 | 3 000 |
| staff_mode       | 5 | 15 000 |
| vanish           | 2 | 6 000 |
| freeze           | 2 | 6 000 |
| invsee           | 5 | 15 000 |
| gamemode         | 3 | 9 000 |
| fly              | 2 | 6 000 |
| teleport         | 10 | 30 000 |
| antivpn_block    | 5 | 15 000 |
| antivpn_pass     | 50 | 150 000 |
| antialt          | 3 | 9 000 |
| antialt_pass     | 50 | 150 000 |
| chat_filter      | 2 | 6 000 |
| error            | 1 | 3 000 |
| database_perf    | 4 (6h) | 12 000 |
| memory_warning   | 0.1 | 300 |
| **Total**        | **~459 events/j** | **~1 377 000 events/mois** |

PostHog Cloud gratuit : 1 000 000 events/mois → 100 serveurs = limite atteinte
PostHog Cloud Scale : à partir de ~$0.00028/event au-delà

**Recommandation :** Commencer par les events essentiels (~50/j/serveur), ajouter le reste progressivement.

---

## 12. Plan d'implémentation

### Phase 1 — Fondations (50% de la data utile)
```
Feature    : Cycle de vie + Sanctions + Commandes + Player join/quit
Événements : 1-14 (startup, shutdown, player join/quit, command, sanction, warn)
Fichiers   : LifeMod.java, PlayerJoin.java, PlayerQuit.java, SanctionService.java,
             WarnCommand.java, BanCommand.java, KickCommand.java
Volume     : ~50 events/j/serveur
```

### Phase 2 — Staff (+25%)
```
Feature    : Tous les outils staff
Événements : 23-40 (staff_mode, vanish, freeze, invsee, gamemode, fly, etc.)
Fichiers   : StaffModeManager.java, FreezeManager.java, VanishCommand.java,
             InvseeCommand.java, GamemodeCommand.java, etc.
Volume     : ~30 events/j/serveur
```

### Phase 3 — Sécurité (+15%)
```
Feature    : Pass events, CPS, détails Anti-Alt/VPN
Événements : 41-47 (antivpn_pass, antialt_pass, cps_high)
Fichiers   : AntiVPNService.java, AntiAltManager.java, CPSListener.java
Volume     : ~60 events/j/serveur
```

### Phase 4 — Performance & Admin (+10%)
```
Feature    : Erreurs, DB perf, mémoire, utilitaires
Événements : 48-65 (error_rate, database_perf, memory_warning, etc.)
Fichiers   : Partout (try/catch errors), DatabaseManager, DebugManager
Volume     : ~10 events/j/serveur
```
