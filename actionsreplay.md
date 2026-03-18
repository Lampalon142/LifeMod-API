Cahier des Charges : Système d'Action Replay (Minecraft 1.8 - 1.21)
Ce document définit les spécifications techniques pour le développement d'un module de "Replay" intégré à un plugin de modération, utilisant PacketEvents pour l'interception et la réémission de paquets.
1. Objectifs du Système
   Enregistrement (Record) : Capturer en temps réel les mouvements, actions et changements d'état des joueurs et de l'environnement.
   Lecture (Playback) : Recréer la scène fidèlement pour un modérateur, en utilisant des entités virtuelles.
   Compatibilité : Fonctionner de la version 1.8 à 1.21. Utiliser le système de nms mis en place. Se référé à doc.md
2. Spécifications de l'Enregistrement
   2.1. Capture des Paquets (Listeners)
   Le système doit intercepter les paquets sortants du serveur vers les joueurs aux alentours de la scène pour enregistrer :
   Mouvements : ServerboundPlayerPositionPacket, ServerboundPlayerRotationPacket.
   Animations : ServerboundAnimatePacket (coups d'épée, bras).
   Entités : Apparition/Disparition d'entités, mouvements de mobs, flèches, etc.
   Interactions : Pose de blocs, destruction, utilisation d'objets.
   Métadonnées : Changement d'équipement, effets de particules, sneak/sprint.
   2.2. Structure d'une "Frame" (Tick)
   Chaque enregistrement est une séquence de ReplayFrame.
   Timestamp : Temps relatif depuis le début de l'enregistrement (en ticks ou ms).
   Packet List : Liste des paquets sérialisés survenus durant ce tick.
   Delta Compression : Pour limiter la taille des fichiers, n'enregistrer que les changements significatifs (ex: ne pas enregistrer la position si le mouvement est < 0.01 bloc).
3. Spécifications de la Lecture (Playback)
   3.1. Gestion des Entités Virtuelles
   Puisqu'on ne peut pas utiliser de vrais joueurs pour le replay :
   NPC Injection : Utiliser des EntityPlayer factices via des paquets WrapperPlayServerPlayerInfo (pour le skin) et WrapperPlayServerSpawnPlayer.
   Entity ID Mapping : Lors de la lecture, les IDs d'entités originaux doivent être mappés vers de nouveaux IDs uniques pour éviter les conflits avec les entités réelles présentes sur le serveur.
   3.2. Synchronisation et Rendu
   Interpolation : Pour un rendu fluide (même si le serveur lag), le système doit interpoler les positions entre deux ticks.
   Render Distance : Seules les entités dans le rayon de vision du modérateur doivent être envoyées pour économiser la bande passante.
   Time Control : Possibilité de mettre en pause, d'accélérer (x2, x4) ou de revenir en arrière (nécessite des "Keyframes" toutes les 5-10 secondes pour éviter de recalculer depuis le début).
4. Contraintes Techniques avec PacketEvents
   4.1. Abstraction des Versions
   Utiliser les Wrappers de PacketEvents exclusivement pour éviter de toucher au code NMS.
   Gérer les changements majeurs de protocole (ex: le passage du système d'ID de blocs en 1.13+, ou les changements de paquets de metadata en 1.19.3+).
   4.2. Performance (Threading)
   Enregistrement : L'interception des paquets doit être asynchrone pour ne pas impacter le TPS du serveur principal.
   Stockage : Utilisation d'un format binaire compressé (type NBT ou Protobuf personnalisé) pour les fichiers .replay.
5. Fonctionnalités de Modération (UI)
   Timeline : Barre d'action ou menu inventaire pour naviguer dans le temps.
   Spectator Mode : Le modérateur est mis en mode spectateur "invisible" durant le replay.
   Pov Switch : Possibilité de "cliquer" sur un protagoniste pour voir à travers ses yeux (en utilisant le paquet Camera).
6. Défis à Relever
   Skins : Récupérer et stocker les propriétés de skin (textures/signatures) pour que les NPCs ne soient pas des "Steve" par défaut.
   Chunks : Si le replay se passe dans une zone déchargée, il faudra renvoyer les paquets de chunks au modérateur.
pro