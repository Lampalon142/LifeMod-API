# 📜 Cahier des Charges Technique : Système AntiCheat Proactif (Module "Feat-AC")

## 1. Vision du Projet

Développer un AntiCheat hybride combinant **détection heuristique** (paquets invalides), **analyse statistique** (comportement non-humain) et **probabilités** (écarts types). Le système doit être **"Data-Driven"** : aucune valeur ne doit être gravée dans le code (pas de hard-coding), tout doit passer par un fichier `config.yml`.

## 2. Architecture Logicielle & Standards

* **Git Flow :** Développement exclusif sur la branche `Feat-AC`. Chaque fonctionnalité = 1 Commit clair. Utilisation des **Issues** pour chaque check et des **Pull Requests** pour la relecture de code.
* **Performance :** Utilisation intensive de l'asynchronisme pour les calculs statistiques afin de ne pas impacter le TPS du serveur.
* **Extensibilité :** Interface `Check` avec des sous-classes (Ex: `CombatCheck`, `MovementCheck`).
* **Clean Code :** Respect strict du principe **DRY** et des conventions de nommage Java/Spigot.

## 3. Le Moteur de Données (PlayerData)

Dès la connexion, le plugin doit instancier un objet `PlayerData` stockant :

* **Réseau :** Ping dynamique (via `TransactionPacket` ou `KeepAlive`), Client Brand.
* **Combat (Samples) :** * `Queue` circulaire des 100 derniers délais de clics (ms).
* `Map` des 20 derniers ticks de mouvement des entités ciblées (Backtrack/Lag compensation).


* **Rotation (Samples) :** Historique des deltas Yaw/Pitch pour analyse de courbe.

## 4. Spécifications des Détections (Checks)

### A. Combat : L'approche Probabiliste & Obvious

1. **Reach (Portée) :**
* *Obvious :* Si distance  blocs  Flag immédiat.
* *Stats :* Calcul de la distance euclidienne  en tenant compte de la bounding box de la victime. Si 90% des coups sont à  blocs sur un échantillon de 50 coups  Flag.


2. **Aimbot (Analyse Vectorielle) :**
* **Smoothness :** Calcul de la dérivée de la rotation. Si l'accélération est de 0 (mouvement parfaitement linéaire) sur 10 paquets  Flag.
* **Lock :** Vérifier si le curseur reste sur le même pixel relatif de l'ennemi pendant que les deux joueurs bougent.


3. **Hitbox (Expand) :** Ray-tracing côté serveur. Si l'intersection `Ray` vs `BoundingBox` est nulle mais qu'un `InteractPacket` est reçu  Flag.
4. **AutoClicker (Stats Avancées) :**
* **Écart-type () :** Calcul de la régularité. Un  sur 50 clics est statistiquement impossible pour un humain.
* **Kurtosis/Skewness :** Analyse de la distribution des clics pour détecter les macros.



### B. Mouvement & Monde

1. **Speed/Fly :** Vérification via le moteur de prédiction (comparaison entre position prédite et position reçue).
2. **Timer :** Mesurer si le client envoie plus de 20 paquets de type `Flying` par seconde.

## 5. Système de Configuration (Anti-Hardcode)

Tout doit être dans `config.yml`. Exemple de structure attendue :

```yaml
checks:
  reach:
    enabled: true
    max_distance: 3.2
    violation_weight: 5
    commands:
      - "ac notify %player% suspecté de Reach"
  autoclicker:
    min_deviation: 1.2
    sample_size: 50

```

## 6. Commandes Staff

* `/ac alerts` : Toggle des notifications (Actionbar ou Chat).
* `/ac verbose` : Affiche les calculs bruts (ex: "Yaw Delta: 0.002, Dist: 3.12").
* `/ac stats <joueur>` : GUI ou Message affichant les probabilités de triche calculées.

## 7. Spécifications du Moteur d'Analyse & Probabilités (Deep Analysis)

Cette partie définit comment le module doit interpréter les données accumulées dans le `PlayerData`.

### A. Analyse de la Dispersion (Anti-AutoClicker & Macro)

Le module ne doit pas se baser sur le nombre de clics, mais sur leur **distribution temporelle**.

* **Calcul de la Variance et de l'Écart-type :**


L'IA doit implémenter une fonction qui calcule la régularité des délais entre clics (). Un humain est "chaotique" : si l'écart-type  est trop faible, c'est une macro.
* **Analyse d'Entropie :** Mesurer le degré de prédictibilité des délais. Un clic humain a une entropie élevée, un logiciel de triche a une entropie faible (pattern répétitif).

### B. Analyse de la Courbe de Visée (Anti-Aimbot & AimAssist)

Pour chaque paquet de rotation (`Yaw` et `Pitch`), le module doit calculer :

* **La Dérivée Seconde (Accélération) :** Un humain bouge sa souris avec une accélération variable. Si l'accélération est constante ou nulle sur une série de mouvements vers une cible, le score de probabilité de triche augmente.
* **Le "Snap" Détection :** Calculer si la rotation s'arrête pile sur le centre de l'entité (Bounding Box center) de manière instantanée.
* **Analyse du Bruit (Jitter) :** Un humain a un "bruit" naturel (micro-tremblements). L'IA doit détecter les mouvements "chirurgicaux" qui sont trop lisses pour être organiques.

### C. Le Système de "Confidence Score" (Score de Confiance)

Plutôt qu'un simple compteur de violations (VL), le module doit gérer un **Indice de Probabilité ()** :

* Chaque check ne donne pas un "Ban", mais une probabilité (ex: 0.1 pour un coup à 3.1 blocs).
* **Calcul de la probabilité cumulée :** Si plusieurs types de checks (Reach + Aimbot + Clicker) voient leurs probabilités augmenter simultanément, le système doit déclencher une alerte prioritaire.
* **Poids Dynamique :** Un joueur avec un ping instable voit le poids de ses alertes "Reach" réduit automatiquement par le moteur d'analyse.

### D. Stockage des Échantillons (Sampling)

* **Fenêtre Glissante (Sliding Window) :** Utiliser des structures de données type `DoubleBuffer` ou `FixedSizeQueue` pour ne garder que les  derniers événements et recalculer les stats à chaque nouveau paquet sans saturer la mémoire.

* Utilise `java.util.DoubleSummaryStatistics` ou créer une classe `MathUtils` pour centraliser ces calculs de moyenne/écart-type afin de respecter le principe **DRY**.*

---