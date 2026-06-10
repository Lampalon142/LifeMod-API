# Audit — Système de Scan de Map (ScanManager)

> Branch: `audit/scan-map`  
> Fichiers audités: `ScanManager.java`, `ScanCommand.java`, `ScanResult.java`, `docs/scan.md`, `en_US.yml`, `fr_FR.yml`, `config.yml`, `plugin.yml`, `NMSHandler_*.java`, `IItemsAdderService.java`

---

## Résumé

Le système de scan permet aux admins de rechercher un item précis dans :
- les inventaires des joueurs en ligne
- les enderchests des joueurs en ligne
- les conteneurs de la map (chunks chargés + fichiers région `.mca`)

Architecture : `CompletableFuture.supplyAsync()` pour l'async, lecture directe des fichiers région pour couvrir les chunks non chargés, support ItemsAdder.

---

## 1. Problèmes Fonctionnels (❌ Cassé / Bug)

### 1.1 — ScanManager: clés de langue erronées (BUG CONFIRMÉ)

**Fichier:** `ScanManager.java:45,89,102,109,117,127,135,199,213`

Le ScanManager appelle `lang.getMessage("scan.progress.starting")` mais le YAML définit ces clés sous `commands.scan.progress.starting`.

Résultat : **les messages de progression ne s'affichent pas correctement** — le joueur verra la clé brute `"scan.progress.starting"` au lieu du texte formaté.

Preuve: `en_US.yml:672-681` définit `commands.scan.progress.{starting,inventories-all,...}`.

**Toutes les occurrences dans ScanManager:**

| Ligne | Clé utilisée | Clé correcte |
|-------|-------------|--------------|
| 45 | `scan.progress.starting` | `commands.scan.progress.starting` |
| 89 | `scan.progress.containers` | `commands.scan.progress.containers` |
| 102 | `scan.progress.error` | `commands.scan.progress.error` |
| 109 | `scan.progress.inventories-all` | `commands.scan.progress.inventories-all` |
| 117 | `scan.progress.inventory-player` | `commands.scan.progress.inventory-player` |
| 127 | `scan.progress.enderchests-all` | `commands.scan.progress.enderchests-all` |
| 135 | `scan.progress.enderchest-player` | `commands.scan.progress.enderchest-player` |
| 199 | `scan.progress.region-files` | `commands.scan.progress.region-files` |
| 213 | `scan.progress.region-found` | `commands.scan.progress.region-found` |

### 1.2 — Contournement du NMS Provider

**Fichier:** `ScanManager.java:70-82`

`scanMap()` réimplémente `getLoadedContainers()` en dur au lieu d'utiliser `NMSProvider.getLoadedContainers()` de la couche NMS. Les optimisations version-spécifiques dans `NMSHandler_v1_20_R1` et `NMSHandler_v1_21_R1` sont ignorées par le scan.

### 1.3 — Parsing NBT par réflexion (fragile)

**Fichier:** `ScanManager.java:257-382`

Les méthodes `parseChunkNBT()` et `countNBTItemsReflect()` utilisent la réflexion Java pour accéder aux classes NMS internes (`net.minecraft.nbt.NbtIo`, `net.minecraft.nbt.NbtAccounter`, etc.). Conséquences :
- Casse à chaque mise à jour majeure de Minecraft (noms de méthodes/champs changent)
- Lent (overhead de réflexion par chunk)
- Les erreurs sont avalées silencieusement (simples `warnings`)
- Le fallback `NbtAccounter.unlimitedHeap()` est correct pour 1.20.5+ mais la double approche est risquée

### 1.4 — Pas de scan récursif des shulker boxes dans les fichiers région

**Fichier:** `ScanManager.java:314-349` vs `142-162`

Dans les inventaires joueurs (live), `countItems()` scanne récursivement les shulker boxes. Dans les fichiers région, `countNBTItemsReflect()` ne regarde que le niveau direct du conteneur. Un item caché dans une shulker box posée dans un coffre ne sera PAS trouvé par le scan de map.

### 1.5 — `lifemod.admin.scan` non déclaré dans plugin.yml

**Fichier:** `plugin.yml:109-110`

La commande `scan` existe sans champ `permission:` et le noeud `lifemod.admin.scan` n'est pas déclaré dans la section `permissions:`. La permission fonctionne en runtime (vérifiée dans `ScanCommand.java:30`) mais n'apparaît pas dans les plugins de permission.

### 1.6 — Aucune config scan dans config.yml

**Fichier:** `config.yml` (scan.md §5 étape 6 jamais implémentée)

Aucune option de configuration pour le scan : pas de limite de fichiers région, pas de timeout, pas de nombre max de résultats. Rien n'est personnalisable.

---

## 2. Problèmes de Performance (⚠ Optimisation)

### 2.1 — Traitement séquentiel des fichiers région

**Fichier:** `ScanManager.java:206-212`

Chaque fichier `.mca` est traité un par un sur un seul thread. Pour un monde avec 500+ fichiers région, le temps d'attente est linéaire. Aucune parallélisation (`parallelStream`, plusieurs `CompletableFuture`).

### 2.2 — Aucun mécanisme d'annulation

**Fichier:** `ScanManager.java:41-61`

Aucun `CancellationToken`, `AtomicBoolean` ou `CompletableFuture.cancel()`. Un admin qui lance `/scan all` par erreur ne peut pas l'arrêter sans redémarrer le serveur.

### 2.3 — Aucune limite mémoire des résultats

**Fichier:** `ScanResult.java:9-13`

`foundLocations` peut contenir des milliers d'entrées si l'item recherché est abondant. Il faudrait une limite configurable avec arrêt précoce.

### 2.4 — Scan de tous les mondes, toujours

**Fichier:** `ScanManager.java:97-98`

`scanMap()` boucle sur `Bukkit.getWorlds()` — tous les mondes sont scannés, y compris ceux sans rapport avec la cible.

### 2.5 — Blocage du thread principal

**Fichier:** `ScanManager.java:69-85`

`loadedFuture.get(10, TimeUnit.SECONDS)` attend sur le thread async que le thread principal ait collecté les chunks chargés. Sur un serveur chargé avec beaucoup de chunks, ça peut geler.

### 2.6 — Décompression NBT par chunk

**Fichier:** `ScanManager.java:243-251`

Chaque chunk dans chaque fichier région subit une décompression (Inflater/GZIP) et un parsing réflexif. L'overhead est significatif.

---

## 3. Problèmes de Fiabilité (⚠ Marche / Stabilité)

### 3.1 — `isContainerType()` utilisant `contains()` (risque de faux positifs)

**Fichier:** `ScanManager.java:384-389`

```java
return id.contains("chest") || id.contains("barrel") || ...
```

Avec des mods, un ID comme `"better_chest_painting"` ou `"chest_boat"` passerait le filtre. Risque faible avec les IDs vanilla mais réel avec des mods customs.

### 3.2 — Accès aux inventaires joueurs depuis un thread async

**Fichier:** `ScanManager.java:110-121, 128-139`

`Bukkit.getOnlinePlayers()` et `player.getInventory()` / `player.getEnderChest()` sont appelés depuis le thread async de `CompletableFuture`. Non thread-safe sur Spigot (toléré sur Paper).

### 3.3 — `endTime` jamais setté si exception

**Fichier:** `ScanResult.java:29-31`

Si une exception interrompt le scan avant `result.complete()`, `getDuration()` retourne la différence avec `endTime=0` → temps aberrant.

### 3.4 — `Location` comme clé de HashMap

**Fichier:** `ScanResult.java:10`

`HashMap<Location, Integer>`. `Location.hashCode()` inclut pitch/yaw. Si un même bloc est ajouté avec des `Location` ayant des pitch/yaw différents (un scénario improbable mais possible), il serait compté en double. Plus sûr : utiliser une clé personnalisée `(world, x, y, z)`.

### 3.5 — Imports morts

**Fichier:** `ScanManager.java:14-15`

```java
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
```

Ces classes ne sont jamais utilisées (le code utilise la réflexion NMS). Deux imports superflus.

---

## 4. Écarts avec le Document de Conception

| § du `docs/scan.md` | Statut |
|---------------------|--------|
| §2 — Syntaxe `/scan <map\|...>` | ✅ OK |
| §3A — `CompletableFuture` pour async | ✅ OK |
| §3B — Scan récursif shulker dans inventaires | ✅ OK (live seulement) |
| §3C — Deep Scan chunks non chargés | ✅ Implémenté via fichiers région |
| §3D — Service ItemsAdder générique | ✅ `BukkitItemsAdderService` |
| §4 — Rapport avec limite 50 résultats | ❌ Non implémenté (pas de limite, affiche 10 max) |
| §5-6 — Options de config dans config.yml | ❌ Jamais implémenté |
| §3C — Chunks chargés *uniquement* pour perf | ❌ Fichiers région lourds sans parallélisation |

---

## 5. Correctifs Recommandés

### Priorité Haute

1. **Corriger les clés de langue** dans `ScanManager.java` — remplacer `"scan.progress.*"` par `"commands.scan.progress.*"`
2. **Utiliser `NMSProvider.getLoadedContainers()`** au lieu de la réimplémentation inline dans `scanMap()`
3. **Remplacer la réflexion NMS** par l'API Paper/Mojang pour le parsing des fichiers région

### Priorité Moyenne

4. **Ajouter un mécanisme d'annulation** (`AtomicBoolean cancelled`) et un timeout
5. **Paralléliser le traitement des fichiers région** avec un pool de threads (ex: `.parallelStream()` ou `CompletableFuture.allOf()`)
6. **Limiter les résultats en mémoire** (max 1000 positions, arrêt précoce)
7. **Déclarer `lifemod.admin.scan`** dans la section permissions de `plugin.yml`

### Priorité Basse

8. **Remplacer `Location` par `BlockPosition`** ou une clé `(String world, int x, int y, int z)` dans `ScanResult`
9. **Retirer les imports morts** (`se.llbit.nbt.*`)
10. **Ajouter des options de configuration** dans `config.yml` pour le scan
11. **Appeler `result.complete()` dans un `finally`** pour garantir `endTime` valide

---

*Audit réalisé le 10/06/2026 — 31 problèmes identifiés (6 fonctionnels, 6 perf, 5 fiabilité, 2 écarts docs, 11 correctifs)*
