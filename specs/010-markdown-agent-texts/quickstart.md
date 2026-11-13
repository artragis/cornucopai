# Quickstart — Validation de la feature 010 (rendu markdown des textes des agents)

## Prérequis

- Node.js ≥ 24 (`node --version`), dépendances front installées (`cd frontend && npm ci`).
- Java 25 + Maven wrapper pour le backend (`cd backend && ./mvnw -v`).

## 1. Tests frontend (TDD, vitest)

```bash
cd frontend
npm test                          # tous les specs, dont les nouveaux :
#   src/utils/markdown.spec.ts        — marked + DOMPurify (styles markdown, bloc ```json
#                                       language-json sans <em>, breaks, sanitization XSS)
#   src/components/AgentText.spec.ts  — rendu du composant
#   src/pages/AtelierDetailPage.spec.ts — intégration sur la page de résultat
npm run lint                      # eslint doit passer (0 erreur)
npm run build                     # vite build doit passer
```

Scénarios couverts : gras/titre/listes rendus ; bloc ```json → `pre code.language-json` stylé,
`_` sans italique parasite ; texte sans markdown → simple paragraphe avec sauts de ligne
conservés ; `<script>`/`onerror` sanitizés ; texte vide.

## 2. Tests backend (prompts)

```bash
cd backend
./mvnw test -Dtest='SystemPromptsTest,PromptsTest,LlmPlayerAgentTest'
```

Vérifie que le prompt système et chaque prompt utilisateur (play/react/answer/vote/débat)
contiennent la règle « textes libres en markdown, données structurées en bloc ```json».

## 3. Validation manuelle end-to-end

1. Lancer le backend (profil fake habituel) et le front (`cd frontend && npm run dev`).
2. Ouvrir un atelier dont un texte d'agent contient du markdown avec un bloc ```json :
   le texte est rendu avec les styles markdown et le JSON en bloc de code lisible.
3. Ouvrir un ancien atelier (legacy, prose simple) : rendu visuellement identique à avant.

## 4. Commandes de qualité globales (comme en CI)

```bash
cd frontend && npm run lint && npm run build && npm test
cd backend && ./mvnw verify
```
