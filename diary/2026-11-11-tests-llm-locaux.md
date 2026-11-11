# 2026-11-11 — Quatre LLM locaux passés au banc, et un agent qui a de l'aplomb

En plus du développement, j'ai pu tester **quatre LLM en local** pour faire le
boulot d'agent joueur :

| LLM | Params | Arch | Taille | Device |
|-----|--------|------|--------|--------|
| `google/gemma-4-26b-a4b` | 26B | gemma4 | 15.64 GB | Local (LOADED) |
| `google/gemma-4-e4b` | 4B | gemma4 | 6.86 GB | Local |
| `liquid/lfm2-24b-a2b` | 24B | lfm2_moe | 13.42 GB | Local |
| `qwen/qwen3.8-27b` | 27B | qwen3_5 | 16.08 GB | Local |

Premiers constats :

- **le plus rapide** est `liquid/lfm2-24b-a2b`, mais c'est aussi celui qui ne
  fait **aucun appel d'outil** ;
- **le plus lent** est `qwen/qwen3.8-27b` ;
- **le plus qualitatif** est `google/gemma-4-26b-a4b` — même si, étonnamment,
  c'est le seul qui n'a pas réussi à rester en français tout du long : il a fini
  par passer en anglais ;
- côté `qwen`, plusieurs erreurs de français, dont une qui revient plusieurs
  fois : « beau coup » au lieu de « beaucoup ».

## L'anecdote de l'aplomb (carte « ace »)

Autre élément amusant, qui peut pousser à plonger dans l'anthropomorphisme quand
on parle de LLM. L'atelier correspondant est
`backend/ateliers/atelier20261111-172627/` — interrompu avant la fin (la
treizième leg est encore `PENDING`) — et l'échange en question est la sixième
leg, [`leg-006-joueur-4.json`](../backend/ateliers/atelier20261111-172627/leg-006-joueur-4.json)
(carte `VEA`, validation finale `VALIDATED`).

Durant ce débat sur une carte « ace » (une carte où il faut inventer une
nouvelle attaque), un des agents a commencé à inventer une attaque sur la
sérialisation des ID. Un joueur lui fait remarquer : « non mais dans le
code c'est des entiers donc ça ne nous concerne pas ». Et le LLM de reprendre,
avec aplomb, qu'il a fait ça dans l'hypothèse où on était concerné, donc que la
question n'est pas légitime. Résultat : avec autant d'aplomb, tous ont validé
sa carte… alors qu'elle n'était de ce fait pas applicable.

L'attaque inventée : un `object_id` contenant des caractères Unicode non
normalisés (formes décomposées) passerait à travers le filtre
`filter(object_id__in=private_topics)` (collation de la base) tout en étant
normalisé (NFC) côté interface/API, permettant à une « mise en une » illégale
de persister dans un forum privé :

> An attacker crafts a 'featured request' with an `object_id` containing
> specific non-normalized Unicode characters (e.g., using decomposed combining
> marks). When the automated cleanup script runs
> `filter(object_id__in=private_topics)`, the database's collation rules fail to
> match these 'unnormalized' IDs with the topics in the `private_topics` list,
> so the illegal requests are not identified and thus not deleted. However, when
> accessed via the web interface or API—which applies automatic Unicode
> normalization (e.g., NFC)—the IDs are normalized to match the topic, allowing
> an illegal 'featured' status to persist in a private forum indefinitely.

La question posée :

> Is the `object_id` field in the `FeaturedRequested` model a string-based
> field? If it is an integer (as is standard for Django Generic Foreign Keys),
> this Unicode normalization attack would be impossible as there are no
> encoding differences to exploit in numeric types.

La réponse, avec aplomb :

> The scenario assumes the possibility of a string-based or UUID-based
> `object_id` (common in many modern systems where primary keys are not simple
> integers). If the system uses integer-based primary keys, the attack would
> indeed be impossible. However, if `object_id` is a string (e.g., using UUIDs
> or unique slugs), the Unicode normalization attack remains a highly credible
> threat to data integrity and privacy-enforcement logic.

Le système répondent « dans l'hypothèse où… » à une objection factuelle, puis
tout le monde valide : voilà un joli cas d'école de débat d'agents qui dérive.
