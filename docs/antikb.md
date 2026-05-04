📋 Cahier des Charges : Module AntiVPN & Network Security
1. Objectifs Techniques

   Compatibilité : Abstraction totale pour supporter Spigot (Bukkit API) et BungeeCord (Bungee API).

   Performance : Utilisation de PacketEvents v2.7.0 pour intercepter les connexions au plus bas niveau possible (Handshake ou Login).

   Asynchronisme : Toutes les requêtes API (Lookup IP) doivent être effectuées de manière asynchrone pour ne pas freeze le thread principal du serveur.

2. Fonctionnalités Principales (Core)
   A. Détection DataCenter (DC) & VPN
   Mécanisme : Comparaison de l'IP entrante avec des bases de données de plages d'IP (ASN).
   Sources : Intégration d'API tierces (type IP-API, ProxyCheck, ou IPIntel).
   Cache Local : Système de cache (SQLite ou Caffeine Cache) pour éviter de requêter l'API plusieurs fois pour la même IP (durée de vie configurable).

B. Geo-Blocking (Whitelist/Blacklist de Pays)

    Fonctionnement : Détection du code ISO du pays (ex: FR, US, CA).
    Configuration : * mode: WHITELIST ou BLACKLIST.
        allowed-countries: Liste des pays autorisés.

C. Système de Whitelist Exceptionnelle

    Possibilité d'ajouter des pseudos ou des IPs spécifiques qui ignorent toutes les vérifications (utile pour le staff ou des partenaires sous VPN).

D. 

1. Système "Zero-Config" (Intégration Native)

   Base de Données Embarquée : Intégration d'une version légère d'une base de données ASN (type IP2Location ou GeoLite2 en local) directement dans le jar (ou téléchargée automatiquement au premier lancement).

   Fallback multi-API : Si l'utilisateur ne configure rien, le plugin utilise un système de rotation sur des API gratuites (sans clé) pour garantir que la détection fonctionne toujours.

   Auto-Update : Le module vérifie une fois par semaine si une nouvelle liste d'IPs de datacenters est disponible pour rester à jour sans intervention humaine.

2. Détection ISP (Internet Service Provider)

   Filtrage par Nom : Analyse le nom de l'organisation qui possède l'IP.

        Exemple : Si l'ISP contient "OVH", "Hetzner", "DigitalOcean", "Cloudflare", "Hostinger", la connexion est flaggée.

   Whitelisting ISP : Possibilité de forcer l'acceptation de certains fournisseurs (ex: autoriser tous les joueurs venant de "Orange" ou "Free" peu importe leur comportement).

3. Anti-Bot : Rate Limiting (Gestion de Flux)

   Seuil de Connexion (CPS) : Limite le nombre de tentatives de connexion par seconde.

        Global : Max 5 connexions / sec sur tout le serveur.

        Par IP : Max 1 connexion toutes les 10 secondes par IP.

   Burst Protection : Si 20 joueurs tentent de se connecter en 2 secondes, le plugin active un "Captcha de connexion" ou bloque temporairement les nouvelles entrées.

   Vérification de Latence : Les bots ont souvent une latence (ping) très stable ou inexistante au moment du handshake. Le module peut détecter ces patterns anormaux.