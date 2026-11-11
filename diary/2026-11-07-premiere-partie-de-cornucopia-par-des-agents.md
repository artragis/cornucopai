# 2026-11-07 — Première partie de Cornucopia jouée par des agents : le maître du jeu en code

Deuxième incrément du projet : `specs/002-cornucopia-game-flow`. Cette fois, pas
de scaffold — un vrai flux de jeu, avec le programme dans le rôle du maître du
jeu, exactement comme décrit dans [l'article de Zeste de Savoir](https://zestedesavoir.com/articles/5088/un-atelier-de-threat-modeling-avec-cornucopia/).

## Les décisions qui ont structuré l'implémentation

Trois choix de `research.md` méritent d'être consignés ici :

1. **L'interface `PlayerAgent` comme seam.** Les quatre archétypes (Grumpy,
   Happy, Greedy, Prof) sont déterministes : aucun appel LLM dans cet incrément.
   Toute l'orchestration passe par une interface (`playCard`, `react`,
   `answerQuestion`, `vote`), ce qui laisse la porte ouverte à un agent
   LangChain4j `AiService` branché derrière la même interface plus tard — sans
   toucher au maître du jeu. C'est le YAGNI appliqué à l'orchestration d'agents.

2. **Persistance filesystem, pas de base.** Chaque partie crée un dossier
   `ateliers/atelier{yyyyMMdd-HHmmss}/` (tirets, jamais de `:`) avec un
   `atelier.json` et un fichier `leg-{NNN}-{joueur}.json` par carte jouée,
   à exactement 5 champs (`cardName`, `scenario`, `explanation`, `precisions`,
   `validation`). Écrit en `PENDING` dès la pose de la carte, mis à jour au fil
   des précisions, puis `VALIDATED`/`REJECTED` après le vote. Relire une partie,
   c'est `ls` le dossier.

3. **La majorité stricte se calcule sur les votants, pas sur tous les
   joueurs.** Le joueur qui a posé la carte ne vote pas sur sa propre leg ;
   `VALIDATED` exige strictement plus de la moitié des « oui » parmi les autres.
   Dans une partie à 4 joueurs (3 votants par leg), le résultat est
   déterministe et vérifiable à la main : les 2 legs de Grumpy sont
   `REJECTED`, les 6 autres `VALIDATED` — ce que le test d'API vérifie
   effectivement.

## Ce que le test-first a révélé

Le TDD a attrapé deux vrais bugs avant qu'ils n'atteignent le navigateur : un
comparateur `Comparator.reversed()` mal placé qui retournait la liste des
ateliers du plus ancien au plus récent, et une sérialisation Jackson vide parce
que les accesseurs du modèle suivaient la convention record (`cardName()`)
que Jackson ne reconnaît pas comme getter. Deux rouges, deux verts.

## Le détail qui fait sourire

Greedy vote « oui » à Happy parce que Happy vote pour lui, et « non » à Grumpy.
Dans la partie de test, le JSON de le leg de Grumpy contient à la fois la
précision « excessivement longue » de Greedy (30 répétitions de « bla ») et la
réponse « Je sais » du Prof à sa question — le flux questions/réponses fonctionne
de bout en bout, sur disque.
