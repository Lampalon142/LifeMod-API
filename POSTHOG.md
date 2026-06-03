# LifeMod PostHog — Guide simple

## 1. Installer les dashboards automatiquement

Tu n'as plus besoin de tout créer à la main. Un script Java le fait pour toi :

```bash
# 1. Va dans PostHog → Settings → User API keys → Create personal API key
#    Copie la clé (commence par phx_...)

# 2. Trouve ton Project ID :
#    PostHog → Settings → Project → Project ID (un nombre)

# 3. Lance le script (depuis la racine du projet) :
java -cp common\build\classes\java\main fr.lampalon.lifemod.common.analytics.PostHogSetup us phx_ta_cle_personnelle 12345
#   ↑                   ↑
#   région (us ou eu)   project ID
```

Le script crée **7 dashboards** complets avec **~30 insights** dedans :
- Vue Générale (serveurs actifs, versions, plateformes)
- Configuration (modules activés, réglages)
- Commandes & Usage (top commandes, tendances)
- Sanctions & Modération (bans, auto-punish)
- Sécurité (blocages VPN, Anti-Alt)
- Staff Tools (staff mode, freeze, reports)
- Performance & Santé (erreurs, mémoire)

Après exécution → rafraîchis PostHog → les dashboards sont là.

## 2. Tester que les événements arrivent

Déjà fait plus tôt - `PostHogTest.java` a envoyé `lifemod_startup` et `lifemod_test`. Vérifie dans PostHog → **Live Events**.

Pour un vrai test serveur :
1. Mets `modules.posthog.enabled: true` dans `config.yml`
2. Démarre le serveur avec le JAR
3. Les events arrivent automatiquement

## 3. Plus tard (quand tu auras des données)

Quand tu auras assez de données (> quelques jours), tu pourras créer :
- **Les funnels** (parcours utilisateur) : PostHog → Funnels → New
- **Les alerts** (notifications) : PostHog → Alerts → New
- **La rétention** (combien de serveurs reviennent) : PostHog → Retention

Mais concentre-toi d'abord sur les dashboards et la réception des données.

## Audit complet des données

Voir **`POSTHOG-AUDIT.md`** pour la liste exhaustive de tout ce qui peut être tracké.

**Résumé :** 11 événements implémentés sur ~61 possibles. Il manque environ **50 événements** :
- Joueurs (join/quit/kick/chat/CPS)
- Staff avancé (vanish/modmode/auth/gamemode/fly/speed/invsee/noclip)
- Modération approfondie (warns/notes/history/pardon/expire)
- Admin/Utilitaires (broadcast/time/weather/replay)
- Performance/Technique (errors/database)

Les manquants sont listés avec priorité, fichier source et propriétés dans l'audit.

## Résumé des fichiers créés

| Fichier | Rôle |
|---------|------|
| `PostHogService.java` | Envoie les events au serveur PostHog |
| `PostHogObfuscation.java` | Cache la clé API dans le JAR |
| `PostHogSetup.java` | Configure PostHog automatiquement (dashboards + insights) |
| `POSTHOG-AUDIT.md` | Audit complet de tous les événements à tracker |

La clé API du plugin (`phc_nxL...`) est déjà obfusquée et insérée. Le script ci-dessus utilise une **clé personnelle PostHog** différente (pour l'API de configuration) — c'est normal, elles sont séparées.
