En 2023, j’ai publié sur ce site [« Un atelier de threat modeling avec Cornucopia »](https://zestedesavoir.com/articles/5088/un-atelier-de-threat-modeling-avec-cornucopia/) : le compte rendu d’un atelier où nous avions fait jouer l’équipe à OWASP Cornucopia, le jeu de cartes de threat modeling. L’atelier avait été riche, mais il avait aussi buté sur des écueils classiques : le temps de préparation, la difficulté à réunir les bonnes personnes au même moment, et la fatigue qui s’installe après deux heures de débat.

Depuis, deux choses ont changé. D’un côté, l’actualité nous rappelle régulièrement que des failles critiques peuvent sommeiller des années dans nos noyaux et nos frameworks — comme [cette faille critique du noyau Linux présente depuis 2016](https://itsocial.fr/cybersecurite/cybersecurite-actualites/une-faille-critique-du-noyau-linux-expose-les-serveurs-et-le-correctif-tarde-chez-les-distributions/), presque aussi vieille que Zeste de Savoir. De l’autre, les agents LLM sont arrivés, et avec eux un débat très vif sur la place de l’IA dans l’édition — un débat qui a d’ailleurs dérapé sur ce site, alors que la discussion de départ, sans être exempte de positions radicales, avait le mérite d’être argumentée.

**CornucopAI** est mon laboratoire pour répondre à une question simple : et si des agents IA jouaient à Cornucopia à notre place, comme maître du jeu et comme joueurs ?

### Pourquoi ce billet

Ce billet est le journal de bord du projet, rédigé au fil de l’eau. Il prend la suite de l’article de 2023 et poursuit trois fils, qui sont autant de questions de retour d’expérience :

- **LangChain4j** : que vaut concrètement cette bibliothèque Java pour orchestrer des agents, des outils et de la mémoire partagée ?
- **Les LLM auto-hébergés** : que peut faire un modèle local de 4 à 27 milliards de paramètres sur une machine de salon, à quelle vitesse, et avec quelle qualité ?
- **Le développement « vibecodé » avec spec-kit** : que vaut un workflow [spec-kit](https://github.com/github/spec-kit) de GitHub quand il est piloté de bout en bout par un agent IA, du bootstrap à la fonctionnalité ?

Et une quatrième question, plus personnelle : que vaudra ce billet écrit de ma main, comparé à ce que l’IA écrira à ma place quand le projet sera terminé ?

### Mon setup

- **Mistral Vibe Code** sur **macOS** (un Mac mini), avec un **abonnement Pro à Mistral** qui me donne accès à **Mistral Large 4** ;
- **JetBrains Air**, l’expérience agentique intégrée nativement à **IntelliJ IDEA 2026.3** (sorti début novembre), pour lancer plusieurs agents en parallèle et faire la revue de leur code dans l’IDE.

Le projet lui-même : un backend **Quarkus 3.40.x sur Java 25** avec **LangChain4j** (via l’extension Quarkiverse), un frontend **Vue 3 + Vuetify 4** buildé avec **Vite 8**, le tout construit et testé en CI sur GitHub Actions. Le code source est dans le dépôt du projet, et chaque partie jouée est persistée en JSON dans `backend/ateliers/` — ce qui permet de citer les échanges réels dans ce billet.

### Plan

Le billet suit quatre chapitres : la prise en main de spec-kit et le vibecoding de la première feature, l’arrivée des vraies cartes et des premiers bugs, le banc d’essai de LLM locaux, puis un run complet d’une partie contre une vraie feature de Zeste de Savoir. La conclusion fait le bilan des trois retours d’expérience.

*[LLM]: Large Language Model (grand modèle de langage)*
*[API]: Application Programming Interface*
*[IDE]: Integrated Development Environment (environnement de développement intégré)*
