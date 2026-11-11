# 2026-11-07 — Prise en main de spec-kit : premier contact mitigé, premières promesses

Le projet CornucopAI est bootstrappé et le premier développement est lancé. Pour
l'outiller, j'ai suivi de bout en bout un workflow inspiré de
[spec-kit](https://github.com/github/spec-kit), en partant d'un projet neuf.

Mon setup du moment : **Mistral Vibe Code** tournant sur **macOS**, sur un **Mac
mini**, avec un **abonnement Pro à Mistral** — ce qui me donne accès à leur
dernier modèle de langage, **Mistral Large 4**. À côté de ça, j'utilise aussi
**JetBrains Air**, la nouvelle expérience agentique intégrée nativement à
**IntelliJ IDEA 2026.3** (sorti début novembre) : cet outil aura son importance
pour la suite du projet.

## Le bootstrap : des « user stories » pour un healthcheck

Premier contact mitigé. Je n'en suis qu'au bootstrap, et le temps passé à relire
les spécifications paraît déjà long. Surtout, le concept paraît étrange pour
bootstrapper : pourquoi des *stories* pour créer l'app Quarkus ? La spec du
premier livrable (`specs/001-project-bootstrap/spec.md`) démarre ainsi :

> As a developer, I want to compile, test, and package the backend from a single
> standard command inside `backend/`, so that a fresh checkout produces a
> runnable artifact with no manual steps.

Le plan conclut sobrement :

> No game logic yet — scaffolding only.

Et dans les tâches, le test-first est poussé à la lettre — le test du endpoint
est écrit pour échouer avant l'implémentation :

> T010 [US1] Contract test for `GET /api/health` per
> `contracts/health-api.yaml` … run `./mvnw test` — MUST FAIL (endpoint not
> implemented)

puis :

> T011 [US1] Implement smoke endpoint `GET /api/health` returning
> `{"status":"UP"}`

Du coup, pour prouver que tout marche, le bootstrap m'a généré un simple
endpoint de healthcheck : `GET /api/health` → `{"status":"UP"}`. Rien de plus.
C'est rassurant, mais avouez que faire rédiger quatre *user stories* pour
arriver à un healthcheck, c'est un détour surprenant.

## `specify`, `plan`, `tasks`, `implement` : des noms verbeux

Les noms des flux du bootstrap — `specify`, `plan`, `tasks`, `implement` —
paraissent aussi trop verbeux. Mais bon : on ne bootstrappe qu'une fois par
projet, alors on passe l'éponge.

## La première vraie feature : le moteur du jeu, et là ça colle

Pour la première vraie feature — le moteur du jeu de Cornucopia
(`specs/002-cornucopia-game-flow/`) — c'est déjà plus pertinent. Je me retrouve
à scoper, définir ma feature et à passer les flux un à un. Cette fois, la spec
raconte quelque chose :

> En tant que participant (agent IA), je veux que le maître du jeu (le
> programme) me demande, à mon tour, ma carte, mon scénario et mon explication,
> puis recueille un par un les précisions et questions des autres joueurs, et
> enfin leur vote de validation, en persistants chaque manche dans un fichier
> JSON sur le disque.

Avec même des archétypes de joueurs pour tester le flux sans dépendre d'un
vrai modèle :

> quatre archétypes d'agents « bêtes » (Grumpy, Happy, Greedy, Prof) aux
> comportements déterministes

Ça semble prometteur : pas de grosse erreur dans la génération des artefacts, du
moins a priori. Le bémol, c'est que ces derniers sont assez verbeux, et que
tout relire est encore plus fastidieux que de relire du code.

## Pourquoi je raconte tout ça

Ce carnet est le journal de bord du projet : je veux voir, au fil de l'eau,
ce que change un workflow spec-kit piloté par un agent IA — et plus tard,
comparer sans préjugé ce billet écrit de ma main avec ce que l'IA écrira, ou
ce que l'IA apporte (ou pas) au développement sécurisé, au-delà des seules
démos spectaculaires de découverte de failles. La suite au prochain épisode.
