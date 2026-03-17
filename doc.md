Cahier des Charges : Restructuration du système NMS (LifeMod)
1. Objectif de la Refonte
Remplacer la structure actuelle située dans fr.lampalon.lifemod.integration.nms par une architecture en Couche d'Abstraction. L'objectif est de rendre le plugin compatible avec plusieurs versions de Minecraft sans modifier le code "Core" lors des mises à jour Mojang.
2. Analyse de l'Existant (À supprimer/modifier)
D'après l'arborescence actuelle, les éléments suivants doivent être migrés :
Package cible : fr.lampalon.lifemod.integration.nms
Classe à refondre : PacketController (Actuellement probablement remplie de conditions de version ou de code NMS brut).
Point d'appel : BukkitPlatform (Doit cesser d'appeler directement le NMS).
3. Nouvelle Architecture Cible
A. Création du Module "API" (Abstraction)
On crée une interface qui définit les capacités NMS du plugin.
Nouveau Package : fr.lampalon.lifemod.common.nms.api
Contenu : Interface NMSProvider (ou NMSHandler).
Règle : Aucune importation net.minecraft ou org.bukkit.craftbukkit ne doit figurer ici.
B. Création des Implémentations (Versions)
On crée un package par version supportée.
Nouveaux Packages : * fr.lampalon.lifemod.platform.bukkit.nms.v1_20_R1
fr.lampalon.lifemod.platform.bukkit.nms.v1_21_R1
Contenu : Classes implémentant l'interface API. Ex: NMSHandler_v1_21_R1.
Règle : C'est ici et uniquement ici que l'on utilise les imports NMS.
C. Le Sélecteur (Factory)
Nouveau Package : fr.lampalon.lifemod.platform.bukkit.nms
Classe : NMSLoader
Rôle : Détecter la version du serveur au démarrage et instancier la bonne classe d'implémentation.
4. Plan d'exécution (Étape par étape)
Étape 1 : Définition des besoins (API)
Identifier toutes les méthodes de PacketController qui utilisent du code NMS et les déclarer dans l'interface NMSProvider.
Action : Extraire les signatures de méthodes (ex: sendPacket, getPing, spawnNPC).
Étape 2 : Création des "Adapters"
Pour chaque version de Minecraft que tu souhaites supporter :
Copier le code spécifique de l'ancien PacketController dans la classe de version correspondante.
Nettoyer les erreurs d'importation.
Étape 3 : Modification de BukkitPlatform
Supprimer l'instanciation directe de PacketController.
Ajouter un champ private NMSProvider nmsProvider;.
Dans le onEnable, appeler le NMSLoader pour initialiser nmsProvider.
Étape 4 : Nettoyage final
Supprimer l'ancien package fr.lampalon.lifemod.integration.nms.
Mettre à jour toutes les références dans les Managers ou Listeners pour qu'ils utilisent l'interface via BukkitPlatform.getNMSProvider().
5. Critères de Validation
Compilation : Le projet doit compiler sans l'ancien package integration.nms.
Modularité : L'ajout d'une version 1.22 ne doit nécessiter que la création d'une nouvelle classe d'implémentation et une ligne dans le NMSLoader.
Robustesse : Si le plugin tourne sur une version non supportée, il doit afficher un message d'erreur propre et se désactiver au lieu de crash (NullPointerException).
