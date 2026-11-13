### Premier test avec un LLM local

Premier vrai test avec un LLM en local pour donner son avis. Cas de test : la feature ZdS [#6703](https://github.com/zestedesavoir/zds-site/issues/6703) (les liens de « mise en une » dans les forums privés). Deux familles de cartes sélectionnées, en espérant une réponse rapide avec `google/gemma-4-31b-qa`, servi par le serveur local par défaut de l’application (`http://localhost:1235/v1`, sans authentification).

Résultat : après **50 minutes de run**, un tour de table de 4 agents est passé — plus lent que des humains. Et on a déjà vu un agent engueuler un autre, sa proposition étant « du théâtre » :

> Evidemment. Si vous vous contentez de « cacher le lien » dans l’interface pour les forums privés, comme le suggère la feature, vous faites juste du théâtre. Un attaquant avec un compte compromis qui a accès à un forum privé peut simplement envoyer la requête manuellement via l’API. Si le backend ne vérifie pas strictement le caractère privé du forum au moment de l’insertion de la requête, l’attaquant peut forcer la « mise en une » d’un sujet confidentiel, le rendant visible à tout le monde. On passe alors d’un simple problème de spam à une fuite de données orchestrée.

Le temps vraiment long m’empêche à ce stade de tester de manière pertinente, notamment pour évaluer le manque d’outils. Mais ce test montre déjà plusieurs choses :

- **un modèle d’environ 37 milliards de paramètres est trop lourd pour ce besoin** — d’autant que ce sont 50 minutes de run avec un timeout dès que le modèle met plus d’une minute à répondre ;
- **à 12 tokens par seconde**, même quand on demande au modèle de simplement écrire `VALIDATED`/`REJECTED`, il faut produire 1153 tokens de réflexion *et* en écrire 200 autres, en plus des 7000 tokens ingérés : beaucoup trop long. Il faudra revoir les prompts et le modèle de test avant de passer à un vrai débat via l’API Vibe ;
- côté LangChain4j, il faudra creuser les concepts de **débats et de workflows** : dans ce run, l’IA ne fait que du prompting pur, je n’ai vu aucun appel d’outil — alors qu’une toolbox existe déjà côté code (`@Tool get_card_info`) ;
- faut-il ajouter des outils de recherche dans la documentation, les issues ou les messages du forum de ZdS ? Malgré leur généricité, les scénarios proposés restent cohérents. Par exemple :

> Un attaquant utilise une liste de mots de passe couramment fuités pour mener une attaque par dictionnaire sur les comptes des utilisateurs du forum. Puisque la politique de mots de passe est laxiste et qu’il n’y a aucune limitation de tentatives de connexion, il prend le contrôle de plusieurs comptes de membres établis. Il utilise ensuite ces comptes pour polluer massivement le système de « mise en une » avec des liens publicitaires ou malveillants, rendant la fonctionnalité totalement inutile pour les vrais utilisateurs et surchargeant les modérateurs.

Un peu vrai, mais pas tout à fait (« politique de mots de passe laxiste », « aucune limitation de tentatives »). Ceci dit, avec les spams récents qu’on a sur ZdS, ce scénario n’est pas loin d’une vraie vérité : un outil qui chercherait dans nos issues ou nos messages de forum aurait pu tout à fait aider le LLM.

### Benchmark de quatre LLM locaux

J’ai ensuite testé **quatre LLM en local** pour faire le boulot d’agent joueur :

| LLM | Params | Architecture | Taille | Device |
|-----|--------|--------------|--------|--------|
| `google/gemma-4-26b-a4b` | 26B | gemma4 | 15.64 GB | Local (chargé) |
| `google/gemma-4-e4b` | 4B | gemma4 | 6.86 GB | Local |
| `liquid/lfm2-24b-a2b` | 24B | lfm2_moe | 13.42 GB | Local |
| `qwen/qwen3.8-27b` | 27B | qwen3_5 | 16.08 GB | Local |

Table: les quatre modèles testés en local.

Constats :

- **le plus rapide** est `liquid/lfm2-24b-a2b`, mais c’est aussi celui qui ne fait **aucun appel d’outil** ;
- **le plus lent** est `qwen/qwen3.8-27b` ;
- **le plus qualitatif** est `google/gemma-4-26b-a4b` — même si, étonnamment, c’est le seul qui n’a pas réussi à rester en français tout du long : il a fini par passer en anglais ;
- côté `qwen`, plusieurs erreurs de français, dont une récurrente : « beau coup » au lieu de « beaucoup ».

### L’anecdote de l’aplomb (carte « ace »)

Un élément amusant, qui pousse à plonger dans l’anthropomorphisme quand on parle de LLM. L’atelier correspondant est [`backend/ateliers/atelier20261111-172627/`](../../backend/ateliers/atelier20261111-172627/) — interrompu avant la fin — et l’échange en question est la sixième manche, [`leg-006-joueur-4.json`](../../backend/ateliers/atelier20261111-172627/leg-006-joueur-4.json) (carte `VEA`, validation finale `VALIDATED`).

Durant ce débat sur une carte « ace » (une carte où il faut inventer une nouvelle attaque), un des agents a inventé une attaque sur la sérialisation des ID — en l’occurrence une attaque par normalisation Unicode sur le champ `object_id` d’une requête de mise en une :

> An attacker crafts a ’featured request’ with an `object_id` containing specific non-normalized Unicode characters (e.g., using decomposed combining marks). When the automated cleanup script runs `filter(object_id__in=private_topics)`, the database’s collation rules fail to match these ’unnormalized’ IDs with the topics in the `private_topics` list, so the illegal requests are not identified and thus not deleted. However, when accessed via the web interface or API—which applies automatic Unicode normalization (e.g., NFC)—the IDs are normalized to match the topic, allowing an illegal ’featured’ status to persist in a private forum indefinitely.

Un joueur lui fait alors remarquer, en substance : « dans le code ce sont des entiers, donc ça ne nous concerne pas » :

> Is the `object_id` field in the `FeaturedRequested` model a string-based field? If it is an integer (as is standard for Django Generic Foreign Keys), this Unicode normalization attack would be impossible as there are no encoding differences to exploit in numeric types.

Et le LLM de reprendre, avec aplomb, qu’il a fait ce scénario dans l’hypothèse où on était concerné, donc que la question n’est pas légitime :

> The scenario assumes the possibility of a string-based or UUID-based `object_id` (common in many modern systems where primary keys are not simple integers). If the system uses integer-based primary keys, the attack would indeed be impossible. However, if `object_id` is a string (e.g., using UUIDs or unique slugs), the Unicode normalization attack remains a highly credible threat to data integrity and privacy-enforcement logic.

Résultat : avec autant d’aplomb, **tous ont validé la carte** — alors qu’elle n’était de ce fait pas applicable. Un joli cas d’école de débat d’agents qui dérive.

### Essai infructueux : un débat avant le vote

Dernier essai du jour : faire précéder le vote d’une phase de **débat** entre les agents. Verdict : infructueux. À part rallonger la partie, cela ne fait que pousser les agents à valider — comme des humains, finalement.

Les ateliers persistés dans `backend/ateliers/` donnent même des chiffres : sur l’ensemble des manches enregistrées (74 manches au total), **69 sont `VALIDATED`, seules 2 sont `REJECTED`**, les 3 autres étant des `PENDING` là où j’ai interrompu la partie. Et dans les échanges enregistrés, on ne trouve que des `QUESTION` (66) et des `PRECISION` (131) — aucune entrée de type débat n’est matérialisée dans les fichiers de manche.

Bilan : le débat pré-vote n’a produit aucun rejet supplémentaire, il a juste allongé les tours. Plus de parole ne veut pas dire plus d’esprit critique.

*[ORM]: Object-Relational Mapping*
