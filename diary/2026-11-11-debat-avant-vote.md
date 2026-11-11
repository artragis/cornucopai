# 2026-11-11 — Essai infructueux : un débat avant le vote

Autre essai du jour : faire précéder le vote d'une phase de **débat** entre les
agents. Verdict : infructueux. À part rallonger la partie, ça ne fait que
pousser les agents à valider — comme des humains finalement.

Les ateliers persistés dans `backend/ateliers/` donnent même des chiffres :
sur l'ensemble des manches enregistrées des parties de ces derniers jours
(74 legs au total), **69 sont `VALIDATED`, seules 2 sont `REJECTED`**
(`atelier20261109-170346` leg 2, carte `AT7` ; `atelier20261110-110926` leg 1,
carte `AZ2`), les 3 autres étant des `PENDING` là où j'ai interrompu la
partie. Et dans les échanges enregistrés, on ne trouve que des `QUESTION` (66)
et des `PRECISION` (131) — aucune entrée de type débat n'est matérialisée dans
les fichiers de manche.

Bilan : le débat pré-vote n'a pas produit de rejet supplémentaire, il a juste
allongé les tours. À garder en tête pour la conception des prochains workflows
(débats LangChain4j) : plus de parole ne veut pas dire plus de esprit critique.
