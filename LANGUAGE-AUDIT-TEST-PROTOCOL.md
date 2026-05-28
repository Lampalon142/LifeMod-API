# Language Audit — Protocole de Test

## 1. Fix `§` → `&` dans les YML (report.detail.notes.*)

**Fichiers :** `en_US.yml`, `fr_FR.yml`
**Lignes :** report → detail → notes → title / item.date / item.edit / item.delete

**Test :**
1. Lancer le serveur avec le plugin
2. Faire un `/report` sur un joueur
3. Ouvrir le GUI d'un report existant
4. Cliquer sur "Notes"

**Résultat attendu :**
- Les couleurs doivent s'afficher (or, blanc, rouge, vert)
- Si avant les couleurs ne marchaient pas (affichage littéral `§6`, `§7`, `§e`, `§c`), elles doivent maintenant fonctionner

---

## 2. Bug `BungeeLangService.getPrefix()`

**Fichier :** `BungeeLangService.java`

**Test :**
1. Lancer le proxy BungeeCord avec LifeMod
2. Exécuter n'importe quelle commande Bungee qui affiche `%prefix%` (ex: `/staffchat`, `/report`)
3. Observer le message envoyé

**Résultat attendu :**
- Le préfixe doit être `&7[&6Network&7] &f» ` (ou la valeur FR configurée)
- **Avant :** le message affichait le texte littéral `system.prefix`
- **Après :** le préfixe configuré s'affiche correctement

---

## 3. Keys `debug.messages.*`

**Fichiers :** `en_US.yml`, `fr_FR.yml`

**Test :**
1. Lancer le serveur
2. Activer le debug dans la config (`debug.enabled: true`, `debug.modules.*: true`)
3. Provoquer une erreur dans un module (ex: webhook Discord invalide)

**Résultat attendu :**
- Les messages de debug s'affichent sans clé littérale
- Plus d'affichage de `debug.messages.prefix` ou `debug.messages.user-error` en tant que texte brut

---

## 4. Keys manquantes (speed, noclip, note.success, permanent)

**Fichiers :** `en_US.yml`, `fr_FR.yml`, `bungee_en_US.yml`, `bungee_fr_FR.yml`

### 4a. `/speed`
**Test :**
1. Faire `/speed 5`
2. Le message doit afficher la vitesse définie, pas `speed.provide` ou `speed.success` en texte brut

### 4b. NoClip
**Test :**
1. Faire `/noclip` (si la commande existe)
2. Le message doit dire "NoClip activé/désactivé", pas une clé littérale

### 4c. Note
**Test :**
1. Faire `/note <joueur> <texte>`
2. Le message de confirmation doit s'afficher

### 4d. Sanction permanente
**Test :**
1. Bannir un joueur définitivement (`/ban <joueur> 0` ou `-1`)
2. Vérifier le message de login du joueur banni
3. Le texte "Permanent" ou "Permanent" doit s'afficher (pas la clé `sanctions.permanent`)

---

## 5. Mismatch broadcast.format / broadcast.tab-completer

**Fichier :** `en_US.yml`, `fr_FR.yml`, `BroadcastCommand.java`

**Test :**
1. Faire `/bc <message>` (ou `/broadcast <message>`)
2. Le message doit être diffusé avec le bon format

**Résultat attendu :**
- Le format de broadcast inclut le préfixe et le message
- La tab-complétion de `/broadcast <tab>` fonctionne et montre le placeholder

---

## 6. Section antialt dans bungee_fr_FR.yml

**Test :**
1. Lancer le proxy BungeeCord avec la config FR
2. Activer AntiAlt
3. Un joueur suspect se connecte
4. Le message de debug AntiAlt s'affiche en français (ex: "Haute Entropie", "Bigrammes Rares")

---

## 7. Double prefix dans DebugManager

**Fichier :** `DebugManager.java`

**Test :**
1. Activer le debug
2. Provoquer une erreur utilisateur
3. Vérifier le message envoyé

**Résultat attendu :**
- Le message doit avoir UN SEUL préfixe (`&7[&6Debug&7] &f`)
- **Avant :** le message avait DEUX préfixes (debug + system) à cause du `%prefix%` mal géré

---

## 8. ReplayCommand — Migration ILangService

**Fichier :** `ReplayCommand.java`

**Test :**
1. Faire `/replay` (sans arguments)
2. Le menu d'aide doit s'afficher en couleurs (pas de clés littérales ni de `§`)
3. Faire `/replay list` (sans replays)
4. Message "No replays found" / "Aucun replay trouvé"
5. Faire `/replay stop` (sans être en replay)
6. Message "You are not in a replay session" / "Vous n'êtes pas dans une session de replay"
7. Faire `/replay load <inexistant>`
8. Message "Replay file not found" / "Fichier replay introuvable"

---

## 9. ScanCommand — Migration ILangService

**Fichier :** `ScanCommand.java`

**Test :**
1. Faire `/scan` (sans arguments)
2. L'usage doit s'afficher (pas de `§cUsage...` hardcodé)
3. Faire `/scan map all hand` (sans item en main)
4. Message "You must hold an item in your hand" / "Vous devez tenir un item en main"
5. Faire `/scan map all diamant` (item invalide)
6. Message "Invalid item" / "Item invalide"

---

## 10. SpeedCommand — Correction des clés

**Fichier :** `SpeedCommand.java`

**Test :**
1. Faire `/speed`
2. Le message d'usage doit s'afficher (pas une clé littérale)
3. Faire `/speed 5`
4. Le message de confirmation s'affiche avec la bonne vitesse

---

## 11. "Permanent"/"Jamais" — Migration listeners

**Fichiers :** `SanctionListener.java`, `ConnectionListener.java`, `BungeeConnectionListener.java`, `BungeeChatListener.java`

**Test :**
1. Bannir un joueur définitivement
2. Le joueur essaie de se connecter
3. **Bukkit :** Le message de bannissement doit dire "Permanent" (anglais) ou "Permanent" (français)
4. **Bungee :** Idem — le texte "Permanent" s'affiche (pas "Jamais" en dur dans le code)

---

## Résumé des attendus

| N° | Problème | Statut attendu |
|----|----------|----------------|
| 1 | Codes `§` dans les YML → `&` | Couleurs fonctionnelles |
| 2 | `getPrefix()` Bungee | Préfixe réel affiché, pas le littéral `system.prefix` |
| 3 | Keys `debug.*` absentes | Messages de debug lisibles |
| 4 | Keys speed/noclip/note/permanent | Pas de clés littérales dans l'UI |
| 5 | Broadcast format/tab-completer | Format correct + tab-complétion |
| 6 | Antialt bungee_fr_FR | Traduction FR complète |
| 7 | DebugManager double prefix | Un seul préfixe affiché |
| 8 | ReplayCommand | Tous les messages via ILangService |
| 9 | ScanCommand | Tous les messages via ILangService |
| 10 | SpeedCommand keys | Clés valides (commands.speed.*) |
| 11 | Permanent hardcodé | "Permanent" via ILangService |

## Build

```bash
./gradlew build shadowJar
# BUILD SUCCESSFUL (vérifié)
```
