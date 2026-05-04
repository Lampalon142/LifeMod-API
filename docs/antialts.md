# 📑 Cahier des Charges : Système d'Analyse Heuristique & IA

## 1. Objectif du Système

Développer une couche d'intelligence capable de calculer un **Score de Dangerosité (SD)** pour chaque joueur entrant. L'IA doit être capable de faire la différence entre un pseudo original (ex: `Pika_Killeur`) et un pseudo généré/volé (ex: `Entitilo_nQXup` ou `Sv25lg4XoW`).

---

## 2. Les Modules d'Analyse (Le "Cœur" de l'IA)

### A. Analyse de l'Entropie de Shannon

Ce module mesure le "chaos" dans le pseudo.

* **Logique :** Les générateurs utilisent des suites de caractères imprévisibles.
* **Formule :** L'IA calcule la distribution des caractères. Si le score dépasse un seuil (ex: **3.8 bits**), le pseudo est marqué comme "Généré".
* **Exemple :** `Sv25lg4XoW` a une entropie très élevée car presque aucun caractère ne se répète et leur suite n'a aucune logique statistique.

### B. Analyse des Bigrammes & Trigrammes (Phonétique)

C'est le module "linguistique" qui détecte le charabia.

* **Base de données :** L'IA possède une table de fréquence des paires de lettres (ex: 'th' est fréquent, 'qX' est quasi-inexistant).
* **Calcul :** L'IA découpe le pseudo. Si elle trouve des suites comme `lE`, `v2`, `nQ`, `Xu`, elle attribue des points de pénalité massifs.
* **Efficacité :** Redoutable contre les pseudos comme `Entitilo_nQXup` (le segment `nQXup` échoue au test de prononciation).

### C. Détection de Suffixes et Segmentation

L'IA doit savoir "découper" le pseudo pour analyser les zones suspectes.

* **Séparateurs :** Détection automatique des `_`, `-`, ou des passages du texte aux chiffres.
* **Logique de Hash :** Si un segment après un séparateur a une longueur fixe (ex: 5 ou 6 caractères) et qu'il est composé d'un mélange incohérent (lettres/chiffres), l'IA le flag comme "Signature de Générateur".

### D. Analyse de la Casse (Majuscules/Minuscules)

* **Positionnement :** Un humain met des majuscules au début ou pour séparer des mots (`PseudoStyle`).
* **Anomalie :** L'IA flag les majuscules placées de manière "random" (ex: `sV25Lg`).

---

## 3. Système de Corrélation et Mémoire

L'IA ne doit pas analyser les joueurs de manière isolée.

* **Pattern Matching :** Si deux joueurs se connectent avec la même structure (ex: `Nom_5caractères`), l'IA crée un "Modèle de Menace" temporaire.
* **Voisinage IP :** Si une IP a déjà généré un compte avec un SD (Score de Dangerosité) élevé, tous les futurs comptes de cette IP partent avec un malus de +50 points.

---

## 4. Spécifications Techniques du Moteur d'Exécution

### Paliers de Réaction (Thresholds)

Le plugin doit lire le score renvoyé par l'IA et appliquer les actions définies dans la config :

| Score (SD) | Niveau | Actions Types (Configurables) |
| --- | --- | --- |
| **0 - 30** | Légitime | Aucune action. |
| **30 - 60** | Suspect | `[STAFF]` Alerte modération + `[LOG]`. |
| **60 - 85** | Critique | `[CONSOLE]` Mute/Freeze + `[STAFF]` Alerte visuelle. |
| **85 - 100** | Alt de Nuisance | `[CONSOLE]` Ban immédiat + `[DISCORD]` Webhook. |

### Variables pour les Actions Custom

L'IA doit fournir ces données pour les commandes :

* `%player%` : Pseudo.
* `%score%` : Score de 0 à 100.
* `%reason%` : La règle qui a sauté (ex: `EntropyHigh`, `BadBigram`).
* `%fingerprint%` : ID unique généré par l'IA pour ce pattern.

---

## 5. Administration et Whitelist

* **Mode Debug :** Permet de voir en temps réel le calcul de l'IA pour chaque joueur dans la console.
* **Force Whitelist :** Commande pour ignorer un joueur si l'IA fait un faux positif (rare mais nécessaire).

---