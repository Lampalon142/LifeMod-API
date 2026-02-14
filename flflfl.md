Voici une structure beaucoup plus robuste et "pro" pour ton fichier YAML :Configuration Optimisée des ActionsYAML  mod-mode:
items:
navigation:
enabled: true
slot: 0
material: COMPASS
name: "&6&lOUTIL DE NAVIGATION"
lore:
- "&7--------------------------"
- "&eClic-Gauche &8» &fTraverser (Thru)"
- "&eClic-Droit &8» &fSauter (Jump)"
- "&eShift + Clic &8» &fSurface (Top)"
- "&7--------------------------"
actions:
# Exécute la commande comme si le joueur la tapait (vérifie ses perms)
LEFT_CLICK:
- "[PLAYER] thru"
- "[SOUND] ENTITY_EXPERIENCE_ORB_PICKUP"

          # Exécute la commande via la console (ignore les perms du joueur)
          RIGHT_CLICK: 
            - "[CONSOLE] teleport %player_name% ^ ^ ^5" # Exemple de TP relatif
            - "[SOUND] ENTITY_ENDERMAN_TELEPORT"
            - "[MESSAGE] &8&l» &7Téléportation effectuée."

          # Action native au plugin ou message spécifique
          SHIFT_LEFT_CLICK:
            - "[PLAYER] top"
            - "[NATIVE] close_menu" # Si tu veux fermer le menu après l'action
            - "[ACTIONBAR] &6Vous avez été téléporté à la surface !"
Explication des Types d'ActionsPour que ton système soit vraiment polyvalent, voici ce que chaque tag signifie généralement dans un moteur de script Minecraft :TagRôleUtilité[PLAYER]Fait taper la commande au joueur.Utilise les permissions du modérateur (ex: /thru).[CONSOLE]La console tape la commande.Utile pour forcer une action même si le modo n'a pas la permission OP.[NATIVE]Action interne au plugin.Fermer un menu, ouvrir un autre inventaire, ou refresh.[MESSAGE]Envoie un texte privé.Confirmer l'action proprement dans le chat.[SOUND]Joue un son au joueur.Améliore le "ressenti" de l'outil (le gamefeel).[ACTIONBAR]Texte au-dessus de l'inventaire.Moins intrusif que le chat pour des notifications rapides.