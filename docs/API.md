# LifeMod API — Guide d'intégration pour applications tierces

Ce guide explique comment utiliser l'API publique de **LifeMod** depuis un autre
plugin Spigot/Paper (ou tout addon). L'API est livrée sous forme d'un JAR
autonome, **sans aucune dépendance au code interne** de LifeMod : un original
stricte est maintenu pour que les plugins tiers puissent compiler et tourner
contre `LifeMod-API.jar` sans importer de classes du plateau.

> **Version couverte : `1.4.0`** — Plateforme serveur : Spigot/Paper (Bukkit).

---

## Sommaire

1. [Prérequis](#1-prérequis)
2. [Récupérer l'API](#2-récupérer-lapi)
3. [Déclarer la dépendance](#3-déclarer-la-dépendance)
4. [Accéder à l'API](#4-accéder-à-lapi)
5. [Les services disponibles](#5-les-services-disponibles)
6. [Sanctions en détail](#6-sanctions-en-détail)
7. [Événements](#7-événements)
8. [Webhooks (Discord)](#8-webhooks-discord)
9. [Protection VPN](#9-protection-vpn)
10. [Bonnes pratiques](#10-bonnes-pratiques)
11. [Exemple complet](#11-exemple-complet)

---

## 1. Prérequis

- Un serveur **Spigot / Paper** (ou fork compatible).
- Le plugin **LifeMod** installé et activé (l'API est **régistrée au démarrage**
  du plugin, pas au chargement de la JVM).
- **Java 8+** côté plugin tiers (le jar API est compilé avec un bytecode
  compatible serveur classique).

---

## 2. Récupérer l'API

### Option A — JAR direct

Au moment du build, LifeMod produit deux artefacts dans le dossier `out/` :

- `LifeMod-<version>.jar` — le plugin complet (fat jar).
- `LifeMod-API.jar` — l'API publique, **à utiliser comme `compileOnly`**.

Il suffit d'ajouter `LifeMod-API.jar` au classpath de compilation de votre addon.

### Option B — Maven / Gradle (GitHub Packages)

Coordonnées publiées :

```groovy
// Gradle
implementation 'fr.lampalon:lifemod-api:1.4.0'
```

```xml
<!-- Maven -->
<dependency>
    <groupId>fr.lampalon</groupId>
    <artifactId>lifemod-api</artifactId>
    <version>1.4.0</version>
    <scope>provided</scope>
</dependency>
```

> Repo : `https://maven.pkg.github.com/Lampalon142/LifeMod` (nécessite un token
> GitHub avec lecture sur le repo).

---

## 3. Déclarer la dépendance

L'API n'a **jamais** besoin d'être embarquée dans votre addon : elle est fournie
par le plugin LifeMod à l'exécution. Déclarez-la **`compileOnly`**.

Gradle :

```groovy
plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly 'org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT'
    compileOnly files('libs/LifeMod-API.jar')
}
```

Dans `plugin.yml`, indiquez que votre plugin dépend de LifeMod :

```yaml
name: MonAddon
main: fr.exemple.monaddon.MonAddon
version: 1.0.0
api-version: 1.13
depend: [LifeMod]        # si obligatoire
# ou
softdepend: [LifeMod]    # si optionnel
```

---

## 4. Accéder à l'API

L'instance est disponible **uniquement après le démarrage de LifeMod**. Le point
d'entrée unique est le holder statique :

```java
import fr.lampalon.lifemod.api.LifeModAPI;

public class MonAddon extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private LifeModAPI api() {
        return LifeModAPI.Provider.get();
    }
}
```

**Important :** `Provider.get()` peut renvoyer `null` (par ex. si LifeMod n'est
pas encore démarré, ou pendant un `/reload`). **Toujours tester `null`**
avant utilisation. Idéalement, ouvrez votre accès dans `onEnable` (ou sur
`PluginEnableEvent` de LifeMod si `softdepend`), pas dans un constructeur.

Une fois l'instance obtenue, vous disposez de **8 services** :

---

## 5. Les services disponibles

| Getter | Type de retour | Description |
|--------|----------------|-------------|
| `getSanctionService()` | `ISanctionService` | Application / révocation / historique des sanctions (ban, mute, warn…) |
| `getVanishService()` | `IVanishService` | Gestion du vanish du staff |
| `getVpnService()` | `IVpnService` | Lookup IP, détection VPN/proxy, contrôle de connexion |
| `getFreezeService()` | `IFreezeService` | Freeze / unfreeze de joueurs |
| `getChatService()` | `IChatService` | Contrôle du chat (blacklist) + capture d'input |
| `getStaffModeService()` | `IStaffModeService` | Activation du mode staff |
| `getWebhookService()` | `IWebhookService` | Envoi de messages Discord (webhooks) |
| `getPlayerService()` | `IPlayerService` | Données joueur persistées (UUID, IP, sessions…) |

Toutes les méthodes qui touchent à des données **persistées ou réseau**
retournent un `CompletableFuture` : ne jamais bloquer le thread principal.

---

## 6. Sanctions en détail

### Le modèle `Sanction`

```java
import fr.lampalon.lifemod.api.sanction.Sanction;
import fr.lampalon.lifemod.api.sanction.SanctionType;
```

Champs principaux (getters) :

- `getUuid()` / `getPlayerUuid()` / `getPlayerName()` — identification.
- `getIssuerUuid()` / `getIssuerName()` — le modérateur.
- `getServerName()` / `getCategory()` — serveur (ex. `lobby`) et catégorie (ex. `cheat`).
- `getType()` — `SanctionType` (`BAN`, `MUTE`, `WARN`, `KICK`, `NOTE`).
- `getReason()` — motif.
- `getCreatedAt()` / `getDuration()` — **millisecondes**. `duration == 0` ⇒
  **permanente**.
- `isSilent()` / `isActive()` / `getEvidence()` — silencieuse, active, preuve.
- Getters sur la révocation : `getRemovedByUuid()`, `getRemovedByName()`,
  `getRemoveReason()`, `getRemovedAt()`.

Helper :

```java
sanction.isPermanent();                 // true si duration == 0
sanction.getExpirationTime();           // -1 si permanent, sinon createdAt + duration
```

### Appliquer une sanction

```java
ISanctionService sanctions = api.getSanctionService();

Sanction ban = new Sanction(
        UUID.randomUUID(),                  // uuid sanction
        playerUuid,                         // joueur ciblé
        playerName,
        yourUuid,                           // modérateur
        yourName,
        "lobby",                            // serveur
        "cheat",                            // catégorie
        SanctionType.BAN,
        "Triche avérée",
        System.currentTimeMillis(),         // createdAt
        0,                                  // duration = 0 => permanent
        false,                              // silent
        true                                // active
);

sanctions.applySanction(ban).thenAccept(s -> {
    getLogger().info("Sanction " + s.getUuid() + " appliquée");
});
```

### Révoguer une sanction

```java
sanctions.revokeSanction(
        playerUuid,
        SanctionType.BAN,
        yourUuid,
        yourName,
        "Appel accepté",
        false     // silent
).thenAccept(success -> {
    if (success) { /* la sanction active a été levée */ }
});
```

### Historique & requêtes

```java
// Historique complet d'un joueur
sanctions.getHistory(playerUuid)
        .thenAccept(list -> list.forEach(s -> getLogger().info(s.getType() + ": " + s.getReason())));

// Sanction active d'un type donné (null si aucune)
sanctions.getActiveSanction(playerUuid, playerName, SanctionType.BAN)
        .thenAccept(s -> setBanned(s != null));

// Sanctions émises par un modérateur
sanctions.getSanctionsIssuedBy("Steve", steveUuid).thenAccept(list -> {});

// États actifs en masse (utile au join)
sanctions.getActiveSanctions(uuids, playerName, SanctionType.BAN)
        .thenAccept(map -> { /* Map<UUID, Sanction> */ });
```

---

## 7. Événements

L'API fournit deux événements `org.bukkit.event` **cancellables** dans
`fr.lampalon.lifemod.api.event`.

### `SanctionEvent`

- `getSanction()` → la `Sanction` concernée.
- Cancellable : annulez l'application de la sanction.

### `VanishEvent`

- `getPlayerUuid()` → joueur concerné.
- `isVanishing()` → `true` si vanish activé, `false` si désactivé.
- Cancellable : annulez le changement de vanish.

```java
@EventHandler
public void onSanction(SanctionEvent event) {
    if (event.getSanction().getPlayerUuid().equals(GARDIEN_UUID)) {
        event.setCancelled(true); // on bloque
    }
}

@EventHandler
public void onVanish(VanishEvent event) {
    getLogger().info(event.getPlayerUuid() + " vanish=" + event.isVanishing());
}
```

Enregistrez votre liste dès l'activation de votre addon (`getServer().getPluginManager().registerEvents(this, this)`).

---

## 8. Webhooks (Discord)

Le service webhook vous permet d'envoyer des messages Discord via la même
webhook configurée dans LifeMod.

> Vérifiez d'abord `isEnabled()` : la webhook doit être configurée dans
> `config.yml` (`modules.discord.*`).

```java
import fr.lampalon.lifemod.api.webhook.*;

IWebhookService webhooks = api.getWebhookService();

if (!webhooks.isEnabled()) return;

WebhookMessage message = new WebhookMessage.Builder()
        .setUsername("Mon Addon")
        .setContent("Un joueur a été reporté")
        .addEmbed(new WebhookEmbed.Builder()
                .setTitle("Report")
                .setDescription("Joueur : " + playerName)
                .setColor(0xFFB300)                       // couleur RGB (hex)
                .addField(new WebhookField("Serveur", "skyblock", true))
                .addField(new WebhookField("Motif", "toxicité", true))
                .setThumbnail(new WebhookThumbnail("https://.../avatar.png"))
                .setFooter(new WebhookFooter("LifeMod API", null))
                .build())
        .build();

webhooks.send(message);
```

Sous-objets du modèle `fr.lampalon.lifemod.api.webhook` :
`WebhookMessage`, `WebhookEmbed` (builders), et records `WebhookField(name, value, inline)`,
`WebhookAuthor(name, url, iconUrl)`, `WebhookFooter(text, iconUrl)`,
`WebhookThumbnail(url)`, `WebhookImage(url)`.

---

## 9. Protection VPN

```java
import fr.lampalon.lifemod.api.antivpn.IPInfo;

IVpnService vpn = api.getVpnService();

// Lookup complet d'une IP (pays, FAI, proxy…)
vpn.lookup("1.2.3.4").thenAccept(info -> {
    if (info == null) {
        getLogger().info("Lookup échoué");
        return;
    }
    getLogger().info(String.format("%s | %s | %s | proxy=%s",
            info.getIp(), info.getCountryCode(), info.getIsp(), info.isProxy()));
});

// Est-ce une IP de proxy / VPN ?
vpn.isProxy("1.2.3.4").thenAccept(blocked -> {
    if (blocked) kickPlayerForVpn();
});

// Laisse LifeMod décider (respecte la whitelist / géo / rate-limit configurés)
vpn.shouldAllowConnection("1.2.3.4", "Steve").thenAccept(allowed -> {
    if (!allowed) { /* refuser la connexion */ }
});
```

---

## Bonnes pratiques `CompletableFuture`

- Les méthodes `CompletableFuture` s'exécutent **sur un pool de vie de
  LifeMod** (pas le thread principal). Ne manipulez pas directement les
  joueurs Bukkit dans un `thenAccept` sans repasser dans le thread principal via
  `Bukkit.getScheduler().runTask(...)`.
- `CompletableFuture` peut être **incompletement terminé avec une exception**
  (lookup réseau en échec, DB indisponible). Pensez à `.exceptionally(...)`
  pour éviter des exceptions non gérées.

---

## 10. Bonnes pratiques

- **`Provider.get()` nullable** → gardez un cache local mais re-tester `null`
  à chaque usage critique.
- **Ne jamais faire de `new` sur les services** : utilisez exclusivement les
  services retournés par `LifeModAPI`.
- **Données par UUID** quand possible : le freeze, le chat et le vanish
  fonctionnent par UUID (le plugin résout lui-même le joueur en ligne).
- **Ne pas embarquer** `LifeMod-API.jar` dans votre JAR (déjà fourni par
  LifeMod). Risque de conflits `NoClassDefFoundError` / doublons.
- **Pas d'API sur le proxy** : LifeMod fonctionne sur Bukkit ET BungeeCord ;
  l'API publique est disponible uniquement côté **Bukkit**. Sur le proxy,
  `Provider.get()` sera `null`.

---

## 11. Exemple complet

Un mini-addon qui **bannit une IP de VPN** et **notifie un salon Discord** :

```java
package fr.exemple.monaddon;

import fr.lampalon.lifemod.api.LifeModAPI;
import fr.lampalon.lifemod.api.event.SanctionEvent;
import fr.lampalon.lifemod.api.webhook.WebhookMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MonAddon extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("MonAddon activé — LifeMod API : " +
                (LifeModAPI.Provider.get() != null ? "disponible" : "indisponible"));
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        LifeModAPI api = LifeModAPI.Provider.get();
        if (api == null) return;

        api.getVpnService().isProxy(event.getAddress().getHostAddress()).thenAccept(vpn -> {
            if (vpn) {
                getServer().getScheduler().runTask(this, () ->
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                                "Connection via VPN interdite"));
            }
        });
    }

    @EventHandler
    public void onSanction(SanctionEvent event) {
        LifeModAPI api = LifeModAPI.Provider.get();
        if (api == null || !api.getWebhookService().isEnabled()) return;

        api.getWebhookService().send(new WebhookMessage.Builder()
                .setContent("" + event.getSanction().getPlayerName() +
                        " reçoit : " + event.getSanction().getType())
                .build());
    }
}
```

---

## Dépannage

| Symptôme | Cause probable |
|----------|----------------|
| `Provider.get()` renvoie `null` | LifeMod pas encore démarré ; accédé avant `onEnable` ; serveur proxy (Bungee) ; `.get()` via votre plugin après un reload |
| `NoClassDefFoundError: LifeModAPI` | jar API absent du classpath de build (n'oubliez pas `compileOnly`) |
| Événements non reçus | Listener non enregistré dans `onEnable` |
| `CompletableFuture` lève une exception | Lookup IP / DB indisponibles → gérer avec `.exceptionally(...)` |

---

© LifeMod — Guide de l'API publique. Version du document : `1.4.0`.