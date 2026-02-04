Features LifeMod V2

Informations
<> : Optional(s) arg(s)
[]: Required arg(s)

Features

Full report system with menus, guis, etc and extendable
A full system of banning like AdvancedBan/LiteBans
A full system of moderation for Moderators and Administrators.
A complete system of StaffChat with compatible.
Compatibility with Redis and MySQL/MariaDB and MongoDB
Fully customizable plugin

Commands

/mod <player>
/staff <player> (same commands of /mod)
/vanish <on|off> <player>
/heal <player>
/feed <player>
/speed <player>
/time
/invsee [player|offlinePlayer]
/staffchat <on|off>
/stafflist
/lifemod [reload|info|serverinfo]
/report [player] [reason] => Compatible avec bungee (redis ?)
/tp [player|pos(x,y,z)]
/tphere [player]
/god [player]
/weather [clear/sun/rain]
/broadcast [message]

Ban System
Un système de sanctions performant, conçu pour la précision et la synchronisation multi-serveurs (via Redis/MySQL). Il combine la puissance de plugins comme LiteBans avec une automatisation intelligente.
1. Commandes de Sanction
   /ban [player] [reason] <time> <silent> : Exclusion temporaire ou permanente.
   /mute [player] [reason] <time> <silent> : Interdiction de parler dans le chat et d'utiliser certaines commandes.
   /warn [player] [reason] <time> <silent> : Avertissement formel. Les warns peuvent expirer après une durée définie dans la config.
   /note [player] [reason] <silent> : Ajoute une information administrative sur le joueur, invisible pour lui.
   /unban [player] <silent> : Révoque un bannissement.
   /unmute [player] <silent> : Révoque un silence.
2. Consultation & Traçabilité
   /history [player] <page> : Ouvre un GUI interactif listant toutes les sanctions (actives et expirées). Possibilité de supprimer une sanction directement depuis le menu.
   /case [player] : Affiche un dossier complet : état actuel (banni/mute), nombre de warns actifs, dernières notes et compte principal lié.
   /alts [player/ip] : Identifie les comptes liés par IP pour détecter le contournement de sanction.
   /staffhistory [player] : Permet aux administrateurs de surveiller l'activité d'un modérateur (nombre de sanctions données, erreurs éventuelles).
3. Moteur de Sanctions Automatiques (Hybrid Auto-Punish) Le système s'adapte à la politique de chaque serveur grâce à deux modes de fonctionnement :
   Mode Global : Cumule tous les avertissements du joueur, quelle que soit la raison.
   Exemple : 3 avertissements (n'importe lesquels) = Kick automatique.
   Mode Catégorie : Applique des sanctions spécifiques basées sur le type de faute.
   Exemple (Triche) : 1er Warn = Ban 7 jours | 2ème Warn = Ban Permanent.
   Exemple (Chat) : 1er Warn = Mute 30 min | 3ème Warn = Mute 24h.
   Actions personnalisables : Chaque palier peut exécuter n'importe quelle commande (ex: kick, broadcast, votre_commande_perso).
4. Fonctionnalités Avancées
   Système "Silent" : L'utilisation du flag -s ou de l'argument <silent> permet de sanctionner sans polluer le chat des joueurs (seul le staff reçoit la notification).
   Preuves (Evidence Links) : Possibilité d'attacher un lien (vidéo/image) à chaque sanction pour justifier les décisions en cas d'appel.
   Support Bungee/Velocity : Les sanctions sont appliquées instantanément sur tout le réseau grâce à la technologie Redis.

StaffMod (Système Modulaire)
Un mode modérateur (/mod ou /staff) entièrement repensé pour s'adapter à tous les types de gameplay (Skyblock, Faction, RP). Le système ne se contente pas de donner des items, il écoute les interactions pour déclencher des actions complexes.
1. Moteur d'Items Dynamique (items.yml / API)
   Création d'items personnalisés : Possibilité de définir n'importe quel item via la configuration (Material, Name, Lore, Enchantments, CustomModelData, Flags).
   Système d'Actions & Commandes : Chaque item peut déclencher soit une commande (ex: /ban), soit une Action Native codée dans le plugin (voir liste ci-dessous).
   Support NBT/PDC : Les items sont identifiés par des clés de données persistantes (PersistentDataContainer), empêchant les joueurs de "crafter" des outils de modération.
2. Système de Vanish Avancé
   Silent Mode : Invisibilité totale (tablist, in-game, packets).
   Fake Events :
   Fake Join/Leave : Envoie un faux message de déconnexion quand le staff passe en Vanish, et un faux message de connexion quand il en sort (similaire à PremiumVanish).
   Interaction silencieuse avec le monde (ne déclenche pas de sculk sensors, pas de pression de plaques, etc. - configurable).
3. Outils & Actions Natives (Assignables aux items) Les fonctionnalités suivantes sont des "Actions" que l'on peut lier à n'importe quel item de la config (Clic Droit / Clic Gauche / Frapper) :
   Navigation Tool (Compass/Boussole) :
   Clic Gauche (Thru) : Passe-muraille (traverse les murs et portes instantanément).
   Clic Droit (Jump) : Téléportation instantanée sur le bloc visé ou en haut du bâtiment.
   Knockback Tester (Stick/Bâton) :
   Simule une attaque sur un joueur pour tester son Anti-Knockback.
   Zero-Damage : Annule les dégâts réels mais applique la vélocité (configurable).
   Inspecteur Silencieux (Livre/Blaze Rod) :
   Sur un bloc : Ouvre les coffres, shulkers, tonneaux, hoppers de manière totalement silencieuse (pas de son, pas d'animation d'ouverture).
   Sur un joueur : Ouvre l'inventaire du joueur (InventoryView/InvSee) en temps réel.
   Outils de Contrôle :
   Freeze : Glace le joueur (bloque mouvements/chat/commandes).
   Mount/Follow : S'assoit sur la tête du joueur ou le suit automatiquement.
   RandomTP : Téléporte le modérateur sur un joueur aléatoire (excluant les autres staffs).
   Outils d'Analyse :
   CPS Tester : Affiche les clics par seconde du joueur ciblé en temps réel.
   Information Viewer : Affiche un résumé rapide (Ping, Version client, Warns actifs, Compte alts).

Placeholders
Moderators
%lifemod_moderation_staffonline%
%lifemod_moderation_staffonline_list%
%lifemod_moderation_stafftotal%
%lifemod_moderation_vanishedtotal%
%lifemod_moderation_vanish_player%
%lifemod_moderation_vanish_<player>%
%lifemod_moderation_staffmode_player%
%lifemod_moderation_staffmode_total%
Reports
%lifemod_reports_number%
%lifemod_reports_total%
%lifemod_reports_pending%
%lifemod_reports_claimed%
%lifemod_reports_inprogress%
%lifemod_reports_closed%
%lifemod_reports_number_categories_<category>%
%lifemod_reports_assigned_player%
%lifemod_reports_handled_player%
%lifemod_reports_avgtime_player%
Sanctions
%lifemod_sanctions_bans_active%
%lifemod_sanctions_mutes_active%
%lifemod_sanctions_warns_total%
%lifemod_sanctions_banned_player%
%lifemod_sanctions_muted_player%
%lifemod_sanctions_warns_player%
%lifemod_sanctions_lastreason_player%
%lifemod_sanctions_remaining_player%
%lifemod_sanctions_given_player%
%lifemod_sanctions_bans_given_player%
%lifemod_sanctions_mutes_given_player%

Player Case
%lifemod_case_exists_player%
%lifemod_case_reports_player%
%lifemod_case_notes_player%
%lifemod_case_alts_player%

Network
%lifemod_network_server%
%lifemod_network_staffonline_global%
%lifemod_network_reports_global%
%lifemod_network_redis_status%

Plugin
%lifemod_plugin_version%
%lifemod_plugin_backend%
%lifemod_plugin_uptime%

Informations techniques :

Le plugin doit être entièrement configurable, que sa soit les items, la config, les langs etc.
Le plugin doit être entièrement compatible de la 1.8.8 à la 1.21+ en passant par Paper/SpigotMC et BungeeCord/Velocity sachant que avec Redis on y feras communiquer pour le staffmode etc
Utiliser PacketEvents pour les nms
Utiliser la Lib InvUI pour les guis et PlaceholderAPI pour les expansions
