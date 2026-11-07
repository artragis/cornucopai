# CornucopAI : faire jouer des agents IA à Cornucopia

Ce billet est le journal de bord du projet CornucopAI, rédigé au fil de l'eau
(conitution du projet, section « Project Diary »).

## La source

- L'article [« Un atelier de threat modeling avec Cornucopia »](https://zestedesavoir.com/articles/5088/un-atelier-de-threat-modeling-avec-cornucopia/) ;
- les difficultés réelles d'organiser des ateliers de threat modeling ;
- l'actualité des failles critiques parfois vieilles de plusieurs années (ex. une
  faille critique du noyau Linux présente depuis 2016) ;
- le débat sur la place de l'IA dans l'édition sur Zeste de Savoir.

## Le but

- M'améliorer dans la maîtrise des concepts de LangChain (LangChain4j ici) ;
- tester de bout en bout un workflow style spec-kit en partant d'un projet neuf ;
- comparer sans préjugé un billet écrit par un humain et ce que l'IA écrit ;
- voir l'évolution de l'IA dans le développement sécurisé, au-delà des seules
  découvertes/corrections spectaculaires de failles.

## Premier passage : le bootstrap (2026-11-07)

Le projet démarre sur une stack épinglée : backend **Quarkus 3.40.x LTS (3.40.1.1)
sur Java 25** avec **LangChain4j** (extension Quarkiverse `quarkus-langchain4j`),
build **Maven 3.9.16 via le wrapper** ; frontend **Vue 3 + Vuetify 4** buildé avec
**Vite 8**, tests Vitest, lint ESLint. CI sur **GitHub Actions** avec les mêmes
commandes qu'en local, et une règle inscrite dans la constitution : l'application
Quarkus doit toujours pouvoir être lancée en **mode debug (JDWP, port 5005)**.

Le workflow suivi est spec-kit : `spec.md` → `plan.md` → `tasks.md` →
implémentation, avec TDD systématique (constitution, principe II non négociable).
