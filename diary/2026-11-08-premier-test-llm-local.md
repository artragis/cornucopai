# 2026-11-08 — Premier vrai test avec un LLM local : 50 minutes, 4 agents, et du « théâtre »

J'ai pu faire un vrai test avec un LLM en local pour lui donner son avis. Cas
de test : la feature ZdS
[#6703](https://github.com/zestedesavoir/zds-site/issues/6703) (les liens de
« mise en une » dans les forums privés). J'ai sélectionné deux familles de
cartes en espérant une réponse assez rapide avec `google/gemma-4-31b-qa`,
servi par le serveur local par défaut de la feature 004
(`http://localhost:1235/v1`, sans authentification).

## Le résultat : un tour de table de 4 agents, en 50 minutes

Après **50 minutes de run**, un tour de table de 4 agents est passé — plus lent
que des humains. Et on a déjà vu un agent engueuler un autre : sa proposition
était « du théâtre » :

> Evidemment. Si vous vous contentez de « cacher le lien » dans l'interface pour
> les forums privés, comme le suggère la feature, vous faites juste du théâtre.
> Un attaquant avec un compte compromis qui a accès à un forum privé peut
> simplement envoyer la requête manuellement via l'API. Si le backend ne
> vérifie pas strictement le caractère privé du forum au moment de l'insertion
> de la requête, l'attaquant peut forcer la « mise en une » d'un sujet
> confidentiel, le rendant visible à tout le monde. On passe alors d'un simple
> problème de spam à une fuite de données orchestrée.

Le temps vraiment long m'empêche de tester de manière pertinente, notamment
pour voir le manque d'outils.

## Ce que ce test montre déjà

- **Un modèle ~37 B, c'est trop lourd pour ce genre de besoin** — d'autant que
  c'est 50 minutes de run avec un timeout dès que le modèle met plus d'une
  minute à répondre.
- **Les modèles mettent très longtemps à tout faire** : à 12 tokens/seconde,
  même quand on te demande de juste écrire `VALIDATED`/`REJECTED`, si tu dois
  produire 1153 tokens de réflexion **et** en écrire 200 autres, en plus des
  7000 tokens ingérés, c'est beaucoup trop long. Je vais devoir revoir les
  prompts et le modèle de test avant de passer à un vrai débat via l'API Vibe.
- **J'essaie aussi d'utiliser d'autres concepts de LangChain4j**, comme les
  débats et les workflows. J'ai l'impression que mon système actuel est
  sous-optimal : dans ce run, l'IA ne fait que du prompting pur, je n'ai vu
  aucun appel d'outil — alors qu'une toolbox existe déjà côté code
  (`@Tool get_card_info` dans `CardInfoTool.java`).
- **Faut-il ajouter des outils de recherche dans la doc/issues de ZdS ?** Pour
  l'instant je ne sais pas si c'est pertinent : malgré leur généricité, les
  scénarios proposés restent cohérents. Par exemple :

> Un attaquant utilise une liste de mots de passe couramment fuités pour mener
> une attaque par dictionnaire sur les comptes des utilisateurs du forum.
> Puisque la politique de mots de passe est laxiste et qu'il n'y a aucune
> limitation de tentatives de connexion, il prend le contrôle de plusieurs
> comptes de membres établis. Il utilise ensuite ces comptes pour polluer
> massivement le système de « mise en une » avec des liens publicitaires ou
> malveillants, rendant la fonctionnalité totalement inutile pour les vrais
> utilisateurs et surchargeant les modérateurs.

On voit des choses un peu vraies mais pas tout à fait (« politique de mots de
passe laxiste », « aucune limitation de tentatives »). Ceci dit, avec les spams
récents qu'on a sur ZdS, ce scénario n'est pas loin d'une vraie vérité. Avoir
un outil qui recherche dans nos issues ou nos messages de forum aurait donc pu
tout à fait aider le LLM.
