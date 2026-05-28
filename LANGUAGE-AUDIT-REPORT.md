# Audit du Module de Langage — LifeMod

> Branche : `language-audit`
> Date : 27/05/2026

---

## Résumé

| Catégorie | Nb problèmes | Sévérité |
|---|---|---|
| Keys manquantes dans les YML | 10 | Haute |
| Keys mismatch code ↔ YML | 3 | Haute |
| Codes couleur `§` au lieu de `&` | 8 (4 en_US + 4 fr_FR) | Haute |
| Strings hardcodées (pas via ILangService) | ~133 | Critique |
| Problèmes de formattage (couleurs non parsées) | 12 fichiers | Haute |
| Bug `getPrefix()` Bungee | 1 | Haute |
| Keys `debug.*` complètement absentes | 5 | Haute |
| Section `antialt` manquante bungee_fr_FR | 10 keys | Moyenne |

**Taille du rapport** : ~133 strings hardcodées recensées, ~20 problèmes structurels.

---

## 1. Architecture actuelle

### 1.1 Interface `ILangService`

**Fichier :** `common/service/ILangService.java`

```java
public interface ILangService {
    String getMessage(String key);
    String getMessage(String key, String... placeholders);
    Boolean getBoolean(String key);
    String formatMessage(String message);       // parse &c + remplace %prefix%
    List<String> getStringList(String key);
    String getPrefix();
}
```

### 1.2 Implémentations

| Plateforme | Classe | Parseur couleur | Résolution prefix |
|---|---|---|---|
| Bukkit | `BukkitLangService` | `MessageUtil.parseColors()` (hex `&#RRGGBB`, `rgb()`, `&`) | `langConfig.getString("system.prefix")` |
| Bungee | `BungeeLangService` | `ChatColor.translateAlternateColorCodes('&')` | **BUG : retourne literal `"system.prefix"`** |

### 1.3 Fichiers de langue

| Fichier | Keys | Version |
|---|---|---|
| `languages/en_US.yml` | ~506 | 201 |
| `languages/fr_FR.yml` | ~506 | 201 |
| `languages/bungee_en_US.yml` | ~76 | 200 |
| `languages/bungee_fr_FR.yml` | ~66 | 200 |

---

## 2. Keys manquantes dans les YML (code référence mais n'existe pas)

### 2.1 Keys entièrement manquantes

| Key utilisée dans le code | Fichier Java | Ligne |
|---|---|---|
| `speed.provide` | `SpeedCommand.java` | 31, 39, 44 |
| `speed.success` | `SpeedCommand.java` | 54 |
| `commands.noclip.activate` | `NoClipManager.java` | 56 |
| `commands.noclip.deactivate` | `NoClipManager.java` | 73 |
| `commands.broadcast.format` | `BroadcastCommand.java` | 37 |
| `commands.broadcast.tab-completer` | `BroadcastCommand.java` | 78 |
| `sanctions.note.success` | `NoteCommand.java` | 20 |
| `vanish.usage` | `VanishCommand.java` | (check) |

### 2.2 Keys `debug.*` complètement absentes de tous les YML

| Key | Fichier | Impact |
|---|---|---|
| `debug.messages.prefix` | `DebugManager.java:41,48` | Affiche literal "debug.messages.prefix" au joueur |
| `debug.messages.user-error` | `DebugManager.java:49` | Affiche la key literal |
| `debug.messages.user-help` | `DebugManager.java:50` | Affiche la key literal |
| `debug.messages.admin-error` | `DebugManager.java:68` | Affiche la key literal |
| `debug.messages.admin-location` | `DebugManager.java:75` | Affiche la key literal |

### 2.3 Mismatch code ↔ YML

| Key utilisée dans le code | Key dans le YML | Problème |
|---|---|---|
| `commands.broadcast.format` | `commands.broadcast.prefix` | Nom différent |
| `commands.broadcast.tab-completer` | `bc.tabcompleter` | Chemin différent |
| `commands.spectate.spectate-start` | `commands.spectate.start` | Surnom différent |
| `commands.spectate.freecam-start` | `commands.spectate.freecam` | Surnom différent |

---

## 3. Codes couleur `§` au lieu de `&` dans les YML

### 3.1 en_US.yml (lignes 247-251)

```yaml
report.detail.notes.title: "§6Notes: §e"        # § non parsé par &
report.detail.notes.item.date: "§7Date: §f%date%"
report.detail.notes.item.edit: "§eLeft-click to edit"
report.detail.notes.item.delete: "§cRight-click to delete"
```

### 3.2 fr_FR.yml (lignes 210-214)

```yaml
report.detail.notes.title: "§6Notes : §e"
report.detail.notes.item.date: "§7Date : §f%date%"
report.detail.notes.item.edit: "§eClic-gauche pour éditer"
report.detail.notes.item.delete: "§cClic-droit pour supprimer"
```

**Problème :** `ChatColor.translateAlternateColorCodes('&', ...)` ne traduit QUE `&`, PAS `§`. Ces 8 lignes ne seront jamais colorées.

---

## 4. Bug `BungeeLangService.getPrefix()`

**Fichier :** `platform/bungee/adapter/BungeeLangService.java:66`

```java
@Override
public String getPrefix() {
    return "system.prefix";  // BUG : retourne la clé literal, pas la valeur !
}
```

**Impact :** Tous les messages Bungee utilisant `%prefix%` afficheront le texte literal `"system.prefix"` au lieu du préfixe configuré.

---

## 5. Strings hardcodées — ne passent PAS par ILangService

### 5.1 CRITIQUE — Commandes entièrement en dur

| Fichier | Nb strings | Langue |
|---|---|---|
| `ReplayCommand.java` | **27** | Anglais |
| `ScanCommand.java` | **16** | Français |
| `ReplayInteractionListener.java` | **6** | Français |
| `ReplayGui.java` | **5** | Anglais |
| `ReplayInventoryGui.java` | **6** | Anglais |
| `ScanManager.java` | **9** | Français |

**Exemple typique (`ReplayCommand.java:34`) :**
```java
context.getSender().sendMessage("§cOnly players can use this command.");
```
→ Devrait être : `lang.getMessage("replay.player-only")`

### 5.2 HAUTE — Messages console/config

| Fichier | Nb strings |
|---|---|
| `ConfigUpdater.java` | **10** |
| `AntiVPNService.java` | **6** |
| `LifeMod.java` (startup banner) | **8** |
| `BungeeLifeMod.java` (startup banner) | **9** |

### 5.3 MOYENNE — Couleurs et textes divers

| Fichier | Problème |
|---|---|
| `AntiAltManager.java:126-129` | Scores `§4§l`, `§c`, `§6`, `§e` en dur |
| `PlayerJoin.java:107-116` | Couleurs des alts (`&7`, `&a`, `&c`) en dur |
| `AltCommand.java:44` | Message français en dur |
| `SanctionListener.java:42,68` | `"Permanent"` / `"Jamais"` en dur |
| `BungeeConnectionListener.java:43` | `"Jamais"` en dur |
| `BungeeChatListener.java:32` | `"Jamais"` en dur |

---

## 6. Problèmes de formattage

### 6.1 `BroadcastCommand.java` — format non parsé

```java
String formatted = context.getLang().getMessage("commands.broadcast.format", "%message%", message);
```
→ `getMessage()` appelle `MessageUtil.formatMessage()` donc les couleurs `&` sont parsées.
✅ OK pour les couleurs, mais la key `commands.broadcast.format` n'existe pas.

### 6.2 `DebugManager.java` — Double prefix

```java
String prefix = lang.getMessage("debug.messages.prefix");
sender.sendMessage(prefix + userMsg);   // prefix déjà formaté + userMsg déjà formaté
```
→ Si le prefix contient `%prefix%`, il sera remplacé deux fois. De plus les keys `debug.*` n'existent pas, donc ça affiche la clé literal.

### 6.3 `AntiVPNPacketListener.java` — Utilisation de `ColorUtil` au lieu de `MessageUtil`

```java
Component.text(ColorUtil.format(reason))
```
→ `ColorUtil.format()` ne gère pas `%prefix%`. Actuellement ça marche car `reason` vient déjà de `lang.getMessage()`, mais incohérent.

---

## 7. Keys manquantes dans bungee_fr_FR.yml

Toute la section `antialt` (10 keys) est absente de `bungee_fr_FR.yml` :

```yaml
# MANQUANT dans bungee_fr_FR.yml :
antialt
antialt.debug
antialt.rules
antialt.rules.BadBigram
antialt.rules.BadTrigram
antialt.rules.EntropyHigh
antialt.rules.GeneratedSuffix
antialt.rules.IPHistory
antialt.rules.PatternMatch
antialt.rules.RandomCasing
```

---

## 8. Rapport de priorisation

### 🔴 Priorité 1 (Critique — casse le formattage visible)

| # | Problème | Correctif |
|---|---|---|
| 1 | `§` codes dans `report.detail.notes.*` (8 lignes) | Remplacer `§` par `&` dans les YML |
| 2 | `BungeeLangService.getPrefix()` retourne literal | Remplacer par lecture de la config |
| 3 | Keys `debug.*` manquantes (5 keys) | Ajouter les keys dans tous les YML |

### 🟠 Priorité 2 (Haute — i18n cassée)

| # | Problème | Correctif |
|---|---|---|
| 4 | `speed.provide`, `speed.success` manquants | Ajouter aux YML |
| 5 | `commands.noclip.*` manquants | Ajouter aux YML |
| 6 | `commands.broadcast.format` → YML a `prefix` | Uniformiser code ou YML |
| 7 | `commands.broadcast.tab-completer` → YML a `bc.tabcompleter` | Uniformiser |
| 8 | `sanctions.note.success` manquant | Ajouter aux YML |

### 🟡 Priorité 3 (Moyenne — strings hardcodées)

| # | Problème | Correctif |
|---|---|---|
| 9 | `ReplayCommand.java` (27 strings) | Migrer vers ILangService |
| 10 | `ScanCommand.java` (16 strings) | Migrer vers ILangService |
| 11 | `ReplayInteractionListener.java` (6 strings) | Migrer vers ILangService |
| 12 | `ReplayGui.java` / `ReplayInventoryGui.java` (11 strings) | Migrer vers ILangService |
| 13 | `ScanManager.java` (9 strings) | Migrer vers ILangService |
| 14 | `ConfigUpdater.java` (10 strings) | Migrer vers ILangService |
| 15 | `AntiVPNService.java` (6 strings) | Migrer vers ILangService |

### 🟢 Priorité 4 (Basse — améliorations)

| # | Problème | Correctif |
|---|---|---|
| 16 | Startup banners (`LifeMod.java`, `BungeeLifeMod.java`) | Utiliser lang keys |
| 17 | `"Permanent"`/`"Jamais"` en dur dans SanctionListener | Ajouter key `sanctions.permanent` |
| 18 | Couleurs antialt en dur (`AntiAltManager.java`) | Ajouter keys `antialt.color.*` |
| 19 | Section `antialt` absente de `bungee_fr_FR.yml` | Ajouter les 10 keys traduites |

---

## 9. Fichiers corrects (bonne utilisation d'ILangService)

Ces fichiers utilisent correctement `lang.getMessage()` :

- `FlyCommand.java`, `GamemodeCommand.java`, `TeleportCommand.java`
- `HealCommand.java`, `FeedCommand.java`, `GodModCommand.java`
- `BanCommand.java`, `KickCommand.java`, `MuteCommand.java`, `WarnCommand.java`
- `BaseSanctionCommand.java`, `BaseRevokeCommand.java`
- `SpectateManager.java`, `FreezeManager.java`
- `HistoryGui.java`, `AltsGui.java`, `StaffHistoryGui.java`
- `StaffModeManager.java`, `VanishAction.java`, `FreezeAction.java`, `CpsAction.java`
- `BukkitCommandWrapper.java`, `BukkitCommandAdapter.java`
- `SanctionItem.java`, `ReportItem.java`, `ReportDetailGui.java`

---

## 10. Actions recommandées immédiates

1. **Corriger les 8 codes `§` → `&`** dans `en_US.yml` et `fr_FR.yml` (section `report.detail.notes.*`)
2. **Corriger `BungeeLangService.getPrefix()`** pour lire la valeur réelle
3. **Ajouter les 5 keys `debug.messages.*`** dans tous les YML
4. **Ajouter les keys manquantes** (`speed.*`, `commands.noclip.*`, `sanctions.note.success`)
5. **Ajouter la section `antialt`** dans `bungee_fr_FR.yml`

---

*Fin du rapport d'audit.*
