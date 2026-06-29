# LifeMod — Protocole de Test

## Contexte

Deux architectures à tester :

| Scénario | Topologie | Redis | Base |
|----------|-----------|-------|------|
| **A — Serveur unique** | Skyblock seul | Non | SQLite |
| **B — Network** | Proxy + Lobby + Skyblock | Oui | MySQL (partagée) |

---

## 1. Prérequis (commun)

- **Java 21** (Temurin) installé
- Serveur Minecraft **Paper 1.21.4**
- LifeMod compilé : `./gradlew build shadowJar`
- Placer `out/LifeMod-2.0.0-SNAPSHOT-DEV.jar` dans `plugins/`
- Démarrer le serveur une fois, laisser générer `config.yml` et `plugins/LifeMod/languages/`, puis éteindre

### Fichier de config de base (`plugins/LifeMod/config.yml`)

```yaml
server:
  name: "Skyblock"
  language: "en_US"

database:
  type: "sqlite"
```

---

## 2. Scénario A — Serveur unique (Skyblock)

### 2.1 Topologie

```
[Player] ──→ Paper 1.21.4 (Skyblock)
                  │
             LifeMod.jar
             SQLite (LifeMod.db)
```

### 2.2 Installation

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-1 | Démarrer le serveur | LifeMod s'affiche dans la console avec `&aSuccessfully Enabled`, version NMS détectée, temps de démarrage |
| A-2 | Vérifier `/plugins` | `LifeMod v2.0.0` apparaît dans la liste |
| A-3 | Vérifier `plugins/LifeMod/LifeMod.db` | Fichier SQLite créé |
| A-4 | Tester `/lifemod` | Affiche les infos du plugin (version, plateforme, base, commandes) |

---

### 2.3 Commandes joueur

| # | Commande | Contexte | Résultat attendu |
|---|----------|----------|------------------|
| A-5 | `/fly` | Joueur sans permission | Message d'erreur permission |
| A-6 | `/fly` | Joueur avec `lifemod.fly` | Flight activé/désactivé, message confirmé |
| A-7 | `/gm creative` | Permission `lifemod.gm` | Mode créatif |
| A-8 | `/heal` | Permission `lifemod.heal` | Vie restaurée à 20 ❤ |
| A-9 | `/feed` | Permission `lifemod.feed` | Faim restaurée à 20 |
| A-10 | `/god` | Permission `lifemod.god` | Mode dieu activé (dégâts annulés) |
| A-11 | `/speed 3` | Permission `lifemod.speed` | Vitesse de marche modifiée |
| A-12 | `/speed fly 2` | Permission `lifemod.speed` | Vitesse de vol modifiée |
| A-13 | `/hearts Player2 30` | Permission `lifemod.hearts` | Player2 a 30❤ max |
| A-14 | `/tp Player2` | Permission `lifemod.tp` | Téléporté à Player2 |
| A-15 | `/tphere Player2` | Permission `lifemod.tp` | Player2 téléporté à soi |
| A-16 | `/clearinv` | Permission `lifemod.clearinv` | Inventaire vidé |
| A-17 | `/ecopen Player2` | Permission `lifemod.ecopen` | EnderChest de Player2 affiché |
| A-18 | `/invsee Player2` | Permission `lifemod.invsee` | Inventory de Player2 affiché (interactif) |


---

### 2.4 Modération — Sanctions

| # | Test | Résultat attendu |
|---|------|------------------|
| A-22 | `/warn Player2 Harcèlement` | Player2 reçoit un message, warn enregistré en base |
| A-23 | `/mute Player2 30m Spam` | Player2 est muted 30 min, message broadcast |
| A-24 | Player2 mute essaie de parler | Message bloqué + notification "vous êtes mute" |
| A-25 | `/kick Player2 Test` | Player2 est kické avec la raison "Test" |
| A-26 | `/ban Player2 1d Cheat` | Player2 est banni 1 jour, broadcast |
| A-27 | Player2 kické essaie de rejoindre | Message de ban avec raison + expiration |
| A-28 | `/unban Player2` | Ban révoqué, Player2 peut rejoindre |
| A-29 | `/unmute Player2` | Mute révoqué, Player2 peut parler |
| A-30 | `/mute Player2 0s Perma` | Mute permanent |
| A-31 | `/history Player2` | GUI complète avec tous les warns/mutes/bans/kicks |
| A-32 | `/case Player2` | Résumé des sanctions actives |
| A-33 | `/note Player2 A testé` | Note interne enregistrée |
| A-34 | `/note Player2` (GUI) | GUI d'historique avec notes visibles |

---

### 2.5 Modération — Staff Mode

| # | Test | Résultat attendu |
|---|------|------------------|
| A-35 | `/mod` | Mode staff activé, items donnés, vanish auto, effet night vision |
| A-36 | Cliquer sur l'item "Compass" (Navigation) | Menu de téléportation vers les joueurs |
| A-37 | Cliquer sur l'item "Book" (Inspecteur) | Inventaire silencieux du joueur ciblé |
| A-38 | Cliquer sur l'item "Freeze" | Freeze le joueur ciblé (bloqué, GUI de freeze) |
| A-39 | Cliquer sur l'item "Pearl" (Random TP) | TP aléatoire sur la map |
| A-40 | Cliquer sur l'item "Vanish" | Vanish toggle (on/off) |
| A-41 | Cliquer sur l'item "CPS" | CPS tester sur le joueur ciblé |
| A-42 | Cliquer sur l'item "KB" | Knockback testeur |
| A-43 | Cliquer sur l'item "Info" | Infos du joueur ciblé (ping, CPS, loc, gm) |
| A-44 | `/mod` (désactiver) | Mode staff désactivé, vanish off, items retirés |
| A-45 | `/freeze Player2` | Player2 freeze (bloqué, message) |
| A-46 | `/unfreeze` / re-clic | Unfreeze |

---

### 2.6 Vanish

| # | Test | Résultat attendu |
|---|------|------------------|
| A-47 | `/vanish` | Joueur disparaît de la tab, fake quit broadcast |
| A-48 | Player non-staff rejoint | Ne voit pas le joueur vanish |
| A-49 | Player avec `lifemod.vanish.see` rejoint | VOIT le joueur vanish |
| A-50 | `/vanish` (désactiver) | Joueur réapparaît, fake join broadcast |
| A-51 | `ServerListPing` | Joueur vanish PAS dans la liste du ping |

---

### 2.7 Staff Chat

| # | Test | Résultat attendu |
|---|------|------------------|
| A-52 | `/staffchat test` | Message envoyé dans le canal staff |
| A-53 | `/staffchat` (toggle) | Mode staffchat activé, messages normaux redirigés |
| A-54 | Écrire dans le chat (toggle on) | Message automatiquement en staffchat |
| A-55 | `/staffchat` (toggle off) | Retour au chat normal |

---

### 2.8 Chat Management

| # | Test | Résultat attendu |
|---|------|------------------|
| A-56 | `/togglechat` | Chat global désactivé |
| A-57 | Joueur normal essaie de parler | Message "Chat désactivé" |
| A-58 | Staff parle (`lifemod.togglechat.exempt`) | Message passe |
| A-59 | `/togglechat` (réactiver) | Chat global réactivé |
| A-60 | `/chatclear` | Chat vidé pour tous les joueurs |
| A-61 | `/broadcast Salut` | Message broadcasté à tout le serveur |

---

### 2.9 World Management

| # | Test | Résultat attendu |
|---|------|------------------|
| A-62 | `/time day` | Temps mis à jour |
| A-63 | `/time night` | Nuit |
| A-64 | `/weather storm` | Pluie activée |
| A-65 | `/weather sun` | Soleil |
| A-66 | `/difficulty peaceful` | Difficulté changée |

---

### 2.10 Follow

| # | Test | Résultat attendu |
|---|------|------------------|
| A-67 | `/follow Player2` | Suivi activé, distance + CPS en actionbar |
| A-68 | `/follow` (stop) | Suivi arrêté |
| A-69 | `/stafflist` | Liste des staff en ligne affichée |

---

### 2.11 Reports

| # | Test | Résultat attendu |
|---|------|------------------|
| A-70 | `/report Player2 Triche` | Report envoyé, notification aux staff |
| A-71 | `/reports` | GUI des reports ouverts |
| A-72 | Cliquer sur un report dans la GUI | Détails du report, possibilité de noter |
| A-73 | Changer le statut (open → in_progress) | Statut mis à jour |

---

### 2.12 Anti-Alt

| # | Test | Résultat attendu |
|---|------|------------------|
| A-74 | `/alt Player2` | GUI d'analyse heuristique (entropy, bigram, trigram, IP, patterns) |
| A-75 | `/alts Player2` | Liste des comptes alternatifs suspects |

---

### 2.13 Anti-VPN

| # | Test | Résultat attendu |
|---|------|------------------|
| A-76 | Activer AntiVPN dans config.yml (`modules.antivpn.enabled: true`) + redémarrer | |
| A-77 | Joueur avec VPN rejoint | Kické avec message "VPN détecté" |
| A-78 | Joueur normal rejoint (hors zone bloquée) | Connecté normalement |

---

### 2.14 Scan

| # | Test | Résultat attendu |
|---|------|------------------|
| A-79 | `/scan` | GUI d'analyse |
| A-80 | Scanner une map | Résultats affichés |
| A-81 | Scanner un inventaire | Items interdits listés |
| A-82 | Scanner un enderchest | Même comportement |

---

### 2.15 NoClip

| # | Test | Résultat attendu |
|---|------|------------------|
| A-83 | `/noclip` | Activé |
| A-84 | Marcher dans un mur | Passe à travers |
| A-85 | `/noclip` (désactiver) | Désactivé, collision restaurée |

---

### 2.16 Replay

| # | Test | Résultat attendu |
|---|------|------------------|
| A-86 | Staff se connecte | Enregistrement démarre automatiquement (si configuré) |
| A-87 | Staff joue (se déplace, casse des blocs, parle) | Actions enregistrées |
| A-88 | `/replay list` | Liste des replays disponibles |
| A-89 | `/replay play <id>` | Replay joué avec NPC, blocs restaurés |
| A-90 | `/replay stop` | Replay arrêté |
| A-91 | Pendant le replay : modificateurs (pause, speed, seek) | Réactifs |

---

### 2.17 Moderator Auth

| # | Test | Résultat attendu |
|---|------|------------------|
| A-92 | Activer `modules.moderator-auth.enabled: true` dans config + redémarrer | |
| A-93 | Joueur avec `lifemod.auth` rejoint | Menu PIN affiché |
| A-94 | Entrer mauvais PIN 3× | Compte temporairement bloqué |
| A-95 | Entrer bon PIN | Accès autorisé, mode staff disponible |

---

### 2.18 Auto-Punish

| # | Test | Résultat attendu |
|---|------|------------------|
| A-96 | Accumuler 3 warns sur un joueur | Auto-punish déclenché (selon config) |
| A-97 | Vérifier que la punition correspond au seuil (global/category) | Commande configurée exécutée |

---

### 2.19 Logs

| # | Test | Résultat attendu |
|---|------|------------------|
| A-98 | Joueur se connecte/se déconnecte | Événement loggé |
| A-99 | Joueur parle | Message loggé |
| A-100 | Joueur casse un bloc | Block loggé |
| A-101 | Joueur ouvre un coffre | Container loggé |
| A-102 | Joueur meurt | Death loggé |
| A-103 | `/log Player2` | GUI des logs avec filtres |
| A-104 | Filtrer par type (chat, block, etc.) | Résultats filtrés |

---

### 2.20 GUI Général

| # | Test | Résultat attendu |
|---|------|------------------|
| A-105 | Ouvrir une GUI avec + de 54 items | Pagination fonctionnelle |
| A-106 | Cliquer sur les flèches de page | Navigation entre pages |
| A-107 | Fermer la GUI | Aucune action hors contexte |

---

### 2.21 Webhook Discord

| # | Test | Résultat attendu |
|---|------|------------------|
| A-108 | Configurer webhook URL dans config.yml + `modules.discord.enabled: true` | |
| A-109 | Staff toggle `/mod` | Message webhook "Staff mod toggled" |
| A-110 | `/ban Player2` | Message webhook "Ban" |
| A-111 | `/report Player2` | Message webhook "Report" |

---

## 3. Scénario B — Network (Proxy + Lobby + Skyblock)

### 3.1 Topologie

```
[Player] ──→ Waterfall/Velocity (Proxy)
                  │
            ┌─────┴─────┐
            │           │
        Lobby       Skyblock
      (Paper)      (Paper)
            │           │
       LifeMod.jar  LifeMod.jar
            │           │
            └─────┬─────┘
                  │
              Redis
              MySQL (partagée)
```

### 3.2 Configuration

**Proxy (`plugins/LifeMod/bungee-config.yml`)**
```yaml
server:
  name: "Proxy"

database:
  type: "mysql"
  host: "localhost"
  port: 3306
  database: "lifemod"
  user: "root"
  password: "password"

redis:
  enabled: true
  host: "localhost"
  port: 6379
  password: ""
```

**Lobby (`plugins/LifeMod/config.yml`)**
```yaml
server:
  name: "Lobby"
  language: "en_US"

database:
  type: "mysql"
  host: "localhost"
  port: 3306
  database: "lifemod"
  user: "root"
  password: "password"

redis:
  enabled: true
  host: "localhost"
  port: 6379
  password: ""
```

**Skyblock (`plugins/LifeMod/config.yml`)**
```yaml
server:
  name: "Skyblock"
  language: "en_US"

database:
  type: "mysql"
  host: "localhost"
  port: 3306
  database: "lifemod"
  user: "root"
  password: "password"

redis:
  enabled: true
  host: "localhost"
  port: 6379
  password: ""
```

### 3.3 Installation

| # | Action | Résultat attendu |
|---|--------|------------------|
| B-1 | Démarrer MySQL + Redis | Services verts |
| B-2 | Démarrer Proxy | LifeMod Bungee chargé |
| B-3 | Démarrer Lobby | LifeMod chargé, connecté à MySQL |
| B-4 | Démarrer Skyblock | LifeMod chargé, connecté à MySQL |
| B-5 | Vérifier les logs des 3 serveurs | Aucune erreur de connexion DB/Redis |
| B-6 | Vérifier la table `lifemod_sanctions` en MySQL | Tables créées automatiquement |

---

### 3.4 Sanctions cross-server

| # | Test | Résultat attendu |
|---|------|------------------|
| B-7 | Sur Skyblock : `/ban Player2 1d Cheat` | Banni sur tout le réseau |
| B-8 | Player2 essaie de rejoindre le proxy | Rejeté : "Vous êtes banni" (message depuis le proxy) |
| B-9 | Sur Lobby : `/unban Player2` | Débanni sur tout le réseau |
| B-10 | Player2 peut rejoindre | OK |
| B-11 | Sur Skyblock : `/mute Player2 10m Spam` | Mute cross-server |
| B-12 | Player2 va au Lobby, essaie de parler | Bloqué (mute toujours actif) |
| B-13 | Sur Lobby : `/unmute Player2` | Démute partout |
| B-14 | Sur Skyblock : `/warn Player2 Harcèlement` | Warn visible dans `/history` depuis le Lobby |

---

### 3.5 Staff Chat cross-server

| # | Test | Résultat attendu |
|---|------|------------------|
| B-15 | Staff A sur Skyblock : `/staffchat Bonjour` | Message reçu par staff B sur Lobby |
| B-16 | Staff B sur Lobby répond | Staff A voit la réponse |
| B-17 | Message préfixé par le nom du serveur source : `[Skyblock] StaffA: Bonjour` | |
| B-18 | Toggle staffchat sur un serveur | Fonctionne localement |

---

### 3.6 Staff Mode cross-server

| # | Test | Résultat attendu |
|---|------|------------------|
| B-19 | Staff active `/mod` sur Skyblock | Statut staff propagé via Redis |
| B-20 | `/stafflist` sur Lobby | Staff visible depuis Lobby |
| B-21 | Staff désactive `/mod` | Statut retiré de la liste |

---

### 3.7 Redis Messaging

| # | Test | Résultat attendu |
|---|------|------------------|
| B-22 | Arrêter Redis | Logs d'erreur de connexion Redis (plugin continue) |
| B-23 | Relancer Redis | Reconnexion automatique |
| B-24 | Vérifier les channels Redis : `PUBSUB CHANNELS` | `lifemod:sanctions`, `lifemod:staff`, `lifemod:reload` |
| B-25 | Staff A se connecte sur Skyblock | Message Redis `lifemod:staff` publié |

---

### 3.8 Base de données partagée

| # | Test | Résultat attendu |
|---|------|------------------|
| B-26 | Lobby + Skyblock pointent la même DB MySQL | Données cohérentes |
| B-27 | Ban depuis Skyblock | Visible depuis Lobby (`/history Player2`) |
| B-28 | Arrêter Skyblock, Lobby toujours en ligne | Lobby fonctionne normalement avec MySQL |
| B-29 | Vérifier les logs : `plugins/LifeMod/logs/` | Présents sur chaque serveur individuellement |

---

### 3.9 Anti-Alt cross-server

| # | Test | Résultat attendu |
|---|------|------------------|
| B-30 | Joueur se connecte au proxy | HeuristicEngine analysé sur le proxy |
| B-31 | Même IP avec 2 comptes | Flag alt, action configurée déclenchée (LOG/KICK) |
| B-32 | `/alt Player2` depuis n'importe quel serveur | Données cohérentes (base partagée) |

---

### 3.10 Reload cross-server

| # | Test | Résultat attendu |
|---|------|------------------|
| B-33 | Proxy : `/lifemod reload` | Tous les serveurs du réseau reload |
| B-34 | Lobby : `/lifemod reload` | Proxy + Skyblock reload aussi |

---

### 3.11 Proxy — Bungee AntiVPN & Connection

| # | Test | Résultat attendu |
|---|------|------------------|
| B-35 | Joueur VPN se connecte au proxy | Kické avant d'atteindre un serveur |
| B-36 | Joueur normal se connecte au proxy | Routé vers le serveur de fallback (Lobby) |
| B-37 | Joueur banni essaie de se connecter | Rejeté par le proxy avant de joindre un serveur |

---

### 3.12 Bungee Logs

| # | Test | Résultat attendu |
|---|------|------------------|
| B-38 | Joueur se connecte au proxy | Connection loggué |
| B-39 | Joueur change de serveur (Lobby → Skyblock) | Changement de serveur loggé |
| B-40 | Joueur parle en staffchat via proxy | Message loggué |

---

## 4. Tests de régression (edge cases)

| # | Test | Résultat attendu |
|---|------|------------------|
| E-1 | Joueur avec `*` (op) | Toutes les commandes accessibles |
| E-2 | Joueur sans permissions | Toutes les commandes refusées |
| E-3 | Commande avec arguments invalides | Message d'erreur clair |
| E-4 | `/ban joueur_inexistant X` | Message "Joueur introuvable" |
| E-5 | Base SQLite verrouillée (concurrent) | Graceful degradation |
| E-6 | Redis déconnecté en cours de route | Fonctionnalités locales continuent |
| E-7 | Charger un langage inexistant (config `language: "xx_XX"`) | Fallback sur `en_US`, warning log |
| E-8 | Supprimer `LifeMod.db` puis reload | Base recréée |
| E-9 | Joueur avec caractères Unicode dans le nom | Pas d'erreur, fonctionnement normal |
| E-10 | Très petit écran (GUI) | Pagination adaptative |

---

## 5. Résumé des cas de test

| Catégorie | Scénario A | Scénario B |
|-----------|-----------|-----------|
| Installation & startup | 4 | 6 |
| Commandes joueur | 17 | — |
| Sanctions | 15 | 8 |
| Staff Mode | 12 | 3 |
| Vanish | 5 | — |
| Staff Chat | 4 | 4 |
| Chat Management | 6 | — |
| World | 5 | — |
| Follow / Stafflist | 3 | — |
| Reports | 4 | — |
| Anti-Alt | 2 | 3 |
| Anti-VPN | 3 | 2 |
| Scan | 4 | — |
| NoClip | 3 | — |
| Replay | 6 | — |
| Moderator Auth | 4 | — |
| Auto-Punish | 2 | — |
| Logs | 7 | 3 |
| GUI | 3 | — |
| Webhook | 4 | — |
| Redis Messaging | — | 4 |
| Base partagée | — | 4 |
| Reload cross-server | — | 2 |
| Edge cases | 10 | 10 |
| **Total** | **~123** | **~49** |
