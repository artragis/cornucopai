# 2026-11-08 — Sélection des vraies cartes : premiers vrais bugs, et un agent qui contourne

Nouvelle étape : la sélection des vraies cartes Cornucopia
(`specs/003-card-set-selection/`). Au lieu des cartes placeholder, l'application
lit maintenant les vrais sets du dépôt Cornucopia (chemin passé en variable
d'environnement) : 7 sets exposés, chacun en version la plus récente —
`companion 1.0`, `cumulus 1.1`, `dbd 1.0`, `eop 5.0`, `mlsec 1.0`,
`mobileapp 2.0`, `webapp 3.0` — avec leurs familles sélectionnables
individuellement dans le formulaire « Configurer une partie » (langue des
cartes par défaut en français, repli sur l'anglais par set quand le français
n'existe pas).

## Deux vrais bugs, pour la première fois

Côté spec-kit, la feature avance. Mais pour la première fois, j'ai de vrais
bugs — deux en fait :

- un bug backend, avec une stacktrace bien visible ;
- un bug frontend : les checkbox des familles **n'apparaissent pas cochées**
  alors que la sélection est bien prise en compte. Le binding est pourtant
  là :

```vue
<v-checkbox
  :model-value="isFamilySelected(set.id, family.id)"
  :data-testid="`family-${set.id}:${family.id}`"
  :label="`${family.name} (${family.cardCount})`"
  @update:model-value="(v: boolean) => toggleFamily(set.id, family.id, v)"
/>
```

## Full vibe code, et le contournement

Cette fois, j'ai volontairement laissé l'agent coder en roue libre — du full
Mistral Vibe Code, sans repasser par les flux spec-kit. Résultat : n'arrivant
pas à corriger le bug des checkbox, le LLM a choisi de le **contourner** plutôt
que de le fixer, en ajoutant des indicateurs de sélection ailleurs : une chip
avec le nombre de familles sélectionnées, et des chips qui listent les familles
choisies (supprimables une à une) :

```vue
<v-chip v-if="selectedFamilies.size" size="small" color="primary" data-testid="selected-count">
  {{ selectedFamilies.size }} sélectionnée{{ selectedFamilies.size > 1 ? 's' : '' }}
</v-chip>
<!-- ... -->
<v-chip v-for="f in [...selectedFamilies]" :key="f" size="small" closable @click:close="removeFamily(f)">
  {{ f }}
</v-chip>
```

Moralité provisoire : quand l'agent ne sait pas réparer, il décore à côté. La
sélection fonctionne, l'UI le montre — mais la checkbox reste aveugle. À
reprendre proprement, la prochaine fois.
