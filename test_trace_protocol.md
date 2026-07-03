# LifeMod — Protocole de Test : Système de Tracabilité (/trace)

## Contexte

Le système **/trace** permet de suivre l'historique complet d'un item à travers tous les joueurs, de sa création à sa destruction. Chaque item reçoit un UUID unique stocké dans son `PersistentDataContainer` (`lifemod:trace_id`). Tous les événements sont logués dans la table `action_logs` avec `LogType.ITEM_TRACE`.

Deux architectures à tester :

| Scénario | Topologie | Base |
|----------|-----------|------|
| **A — Serveur unique** | Paper seul | SQLite |
| **B — Network** | Proxy + Lobby + Skybase (MySQL partagée) | MySQL |

---

## 1. Prérequis

- **Java 21** (Temurin) installé
- Serveur Minecraft **Paper 1.21.4**
- LifeMod compilé : `./gradlew build shadowJar`
- Placer `out/LifeMod-2.0.0-SNAPSHOT-DEV.jar` dans `plugins/`
- Démarrer le serveur une fois, laisser générer `config.yml`, puis éteindre
- Vérifier que `modules.logs.enabled: true` dans `config.yml`

```yaml
# plugins/LifeMod/config.yml (extrait)
modules:
  logs:
    enabled: true
```

---

## 2. Scénario A — Serveur unique

### 2.1 Vérification initiale

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-1 | Démarrer le serveur | LifeMod chargé sans erreur |
| A-2 | Lancer `/trace` sans item en main | Message "Vous devez tenir un item" |
| A-3 | Tenir un item, lancer `/trace` | Message "Cet item n'a pas encore d'historique" ou historique |

---

### 2.2 Craft

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-4 | Tenir un item quelconque (non tracé), faire `/trace` | "Cet item n'a pas encore d'historique" |
| A-5 | Crafter un item (ex: 3 planches de bois → 3 bâtons) | Un UUID est attribué à chaque bâton |
| A-6 | `/trace` sur un bâton crafté | Affiche : "Crafted" avec timestamp |
| A-7 | Vérifier le message : date, action "Crafted" | Correct |

---

### 2.3 Pickup (ramassage)

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-8 | Jeter un item crafté par terre | UUID conservé |
| A-9 | Ramasser l'item | `/trace` affiche "Crafted" + "Picked up" |
| A-10 | Un autre joueur ramasse l'item jeté | Historique mis à jour avec son nom |

---

### 2.4 Drop (jet)

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-11 | Jeter (`Q`) un item tracé | `/trace` affiche "Dropped" + joueur + coordonnées |
| A-12 | Item au sol, attendre ou le ramasser | Pas de perte d'UUID |

---

### 2.5 Consume (nourriture/potion)

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-13 | Crafter un pain ou une soupe | UUID attribué |
| A-14 | Manger l'item (clic droit main) | "/trace" affiche "Consumed" |
| A-15 | Potion : boire une potion craftée | "Consumed" + UUID présent avant destruction |

---

### 2.6 Despawn

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-16 | Jeter un item tracé au sol | "Dropped" loggué |
| A-17 | Attendre 5 minutes (ou régler `item-despawn-rate` à 10s dans `server.properties`) | "Despawned" loggué avec timestamp |
| A-18 | `/trace` (via un item identique ultérieur) | L'ancien item déspawné n'est plus accessible |

---

### 2.7 Destruction (lave / fire / void / cactus)

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-19 | Jeter un item tracé dans un bloc de lave | Log "Destroyed" + item disparaît |
| A-20 | Jeter un item tracé dans du feu (ex: netherrack en feu) | Log "Destroyed" + item brûle |
| A-21 | Jeter un item tracé dans du cactus | Log "Destroyed" + item détruit |
| A-22 | Jeter un item tracé dans le vide (ex: creuser jusqu'à y=-64 et jeter) | Log "Destroyed" + item disparaît |
| A-23 | Vérifier le `action_logs` | Entrée avec `actionData: {"action":"DESTROYED",...}` |

---

### 2.8 Conteneurs (coffres, etc.)

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-24 | Prendre un item tracé dans un coffre | Log "Retrieved from container" |
| A-25 | Déposer un item tracé dans un coffre | Log "Stored in container" |
| A-26 | Faire glisser (`Shift+Click`) depuis/vers un coffre | Log correct (retrieve/store) |
| A-27 | Coffre double (54 slots) | Fonctionne comme un seul inventaire |

---

### 2.9 Four

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-28 | Mettre un item tracé (ex: minerai de fer) dans un four | L'item source doit avoir un UUID |
| A-29 | Récupérer le lingot fondu | `/trace` sur le lingot : affiche "Smelted", **même UUID** que le minerai |
| A-30 | Vérifier que l'UUID est transféré (pas un nouveau) | Historique du lingot inclut l'historique du minerai |

---

### 2.10 Enclume

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-31 | Placer un item tracé à gauche dans l'enclume | |
| A-32 | Récupérer le résultat (renommé/réparé) | `/trace` sur le résultat : **même UUID** que l'item original |
| A-33 | Vérifier que l'historique est préservé | L'ancien historique n'est pas perdu |

---

### 2.11 Enchantement

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-34 | Enchanter un item tracé à la table d'enchantement | Log "Enchanted" + joueur + coordonnées |
| A-35 | `/trace` sur l'item enchanté | Affiche "Crafted" + "Enchanted" |

---

### 2.12 Drops de monstres

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-36 | Tuer un zombie | L'item droppé (ex: viande avariée) reçoit un UUID |
| A-37 | Ramasser le drop | "Picked up" après "Mob drop" |
| A-38 | Tuer un mouton (drop de laine + viande) | Chaque item drop a son propre UUID |
| A-39 | Tuer un wither squelette (drop rare) | UUID sur le charbon/épée |

---

### 2.13 Pêche

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-40 | Pêcher un item (poisson, trésor, déchet) | Log "Fished" + UUID attribué |
| A-41 | Ramasser le poisson | "Picked up" après "Fished" |

---

### 2.14 Scan à l'ouverture d'inventaire

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-42 | Donner un item via `/give` (sans UUID) | L'item n'a pas d'UUID |
| A-43 | Ouvrir son inventaire (`E`) | L'item `/give` reçoit un UUID (via le scan) |
| A-44 | `/trace` | Affiche l'historique à partir du moment du scan |
| A-45 | Ouvrir un coffre contenant des items sans UUID | Les items sont scannés et reçoivent un UUID |

---

### 2.15 Commande /trace

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-46 | `/trace` sur un item avec historique | Liste chronologique : chaque ligne = action + joueur + date + coordonnées |
| A-47 | Vérifier le format : `§aPicked up §fpar §ePlayer1` | Texte coloré, lisible |
| A-48 | Item avec un seul événement | Une seule ligne affichée |
| A-49 | Item avec 10+ événements | Tous les événements listés (scroll si nécessaire) |

---

### 2.16 Cohérence des UUID

| # | Action | Résultat attendu |
|---|--------|------------------|
| A-50 | Stacker 16 items tracés identiques | Chaque item de la stack conserve-t-il son UUID ? (dépend de Bukkit) |
| A-51 | Splitter une stack (clic droit pour en prendre la moitié) | UUID préservé sur chaque item |
| A-52 | Item dans un cadre | UUID visible (l'item n'est pas un ItemStack mais un ItemFrame, pas de trace possible) |

---

## 3. Scénario B — Network (MySQL partagée)

### 3.1 Configuration

Même topologie que le protocole principal : Proxy + Lobby + Skybase, MySQL partagée, Redis.

Configurer chaque serveur Bukkit avec `database.type: mysql` pointant vers la même base.

### 3.2 Tests

| # | Action | Résultat attendu |
|---|--------|------------------|
| B-1 | Sur Lobby : crafter un item, `/trace` | UUID créé, log visible |
| B-2 | Mettre l'item dans un coffre, puis le prendre sur Skybase via un coffre cross-server | L'UUID est le même (même item) |
| B-3 | Sur Skybase : `/trace` sur l'item | Historique complet visible : "Crafted (Lobby)" + "Stored" + "Retrieved (Skybase)" |
| B-4 | Bannir un joueur sur Skybase, l'item tracé qu'il tenait est drop | Le drop log contient le nom du serveur "Skybase" |
| B-5 | Vérifier qu'un item tracé sur serveur A est visible sur serveur B | Les logs sont dans la même DB MySQL, `/trace` affiche tout |

---

## 4. Edge Cases

| # | Test | Résultat attendu |
|---|------|------------------|
| E-1 | Essayer de tracer un item d'un plugin (ex: épée custom ItemsAdder) | UUID attribué au scan ou au pickup |
| E-2 | Item posé au sol, déco/reco du serveur | Les items au sol perdent leur UUID (nouveau chunk load) |
| E-3 | Relancer `/trace` sans changer d'item | Même résultat |
| E-4 | Item dans un NPC (via trait) | Pas de trace (NPC n'ouvre pas d'inventaire) |
| E-5 | Stack de 64 items non-tracés → ouvrir inventaire → tous reçoivent un UUID | Chaque item de la stack a son propre UUID (ou un UUID par stack selon Bukkit) |
| E-6 | Crafter un item avec des ingrédients tracés (ex: épée avec deux sticks tracés) | Le résultat a un NOUVEL UUID (ne conserve pas les UUID des ingrédients) |
| E-7 | Faire `/trace` avec un item d'avant l'installation de LifeMod | "Cet item n'a pas encore d'historique" (pas d'UUID, pas de donnée) |
| E-8 | Base SQLite corrompue → la supprimer → redémarrer | Nouvelle base, les items existants perdent leur historique mais gardent leur UUID |

---

## 5. Résumé des cas de test

| Catégorie | Scénario A | Scénario B |
|-----------|-----------|-----------|
| Vérification initiale | 3 | — |
| Craft | 3 | — |
| Pickup | 3 | — |
| Drop | 2 | — |
| Consume | 3 | — |
| Despawn | 3 | — |
| Destruction (lave/fire/void/cactus) | 5 | — |
| Conteneurs | 4 | — |
| Four | 3 | — |
| Enclume | 3 | — |
| Enchantement | 2 | — |
| Drops de monstres | 4 | — |
| Pêche | 2 | — |
| Scan inventaire | 4 | — |
| Commande /trace | 4 | — |
| Cohérence UUID | 3 | — |
| Network | — | 5 |
| Edge cases | 8 | — |
| **Total** | **57** | **5** |
