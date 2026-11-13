### Les vraies cartes

La feature `003-card-set-selection` remplace les cartes placeholder par les vraies cartes du dépôt [OWASP Cornucopia](https://cornucopia.owasp.org/) : l’application reçoit en variable d’environnement le chemin vers la racine du dépôt (`CORNUCOPIA_HOME`), lit les fichiers YAML du dossier `source/`, et distribue les vraies cartes aux participants.

Au moment des tests, **7 sets** sont exposés, chacun en version la plus récente : `companion 1.0`, `cumulus 1.1`, `dbd 1.0`, `eop 5.0`, `mlsec 1.0`, `mobileapp 2.0`, `webapp 3.0`. Dans le formulaire « Configurer une partie », ce sont les **familles** à l’intérieur de chaque set qui sont sélectionnables (par exemple dans `webapp` : `VE` « Data Validation & Encoding », `AZ` « HABILITATION »…), hiérarchisées par set — cocher un set coche toutes ses familles. La langue des cartes est choisie au démarrage, **français par défaut**, avec repli sur l’anglais par set quand le français n’existe pas.

### Deux vrais bugs

Pour la première fois, de vrais bugs — deux en fait :

- un bug backend, avec une stacktrace bien visible ;
- un bug frontend : les checkbox des familles **n’apparaissent pas cochées**, alors que la sélection est bien prise en compte. Le binding est pourtant présent dans `ConfigureGameDialog.vue` :

```vue
<v-checkbox
  :model-value="isFamilySelected(set.id, family.id)"
  :data-testid="`family-${set.id}:${family.id}`"
  :label="`${family.name} (${family.cardCount})`"
  @update:model-value="(v: boolean) => toggleFamily(set.id, family.id, v)"
/>
```

Code: la checkbox d’une famille, liée à `selectedFamilies` — fonctionnelle, mais qui ne se rend pas cochée.

### Le contournement de l’agent

Cette fois, j’ai volontairement laissé l’agent coder en roue libre — du full Mistral Vibe Code, sans repasser par les flux spec-kit. N’arrivant pas à corriger le bug des checkbox, le LLM a choisi de le **contourner** plutôt que de le fixer, en ajoutant des indicateurs de sélection ailleurs : une chip avec le nombre de familles sélectionnées, et des chips qui listent les familles choisies (supprimables une à une) :

```vue
<v-chip v-if="selectedFamilies.size" size="small" color="primary" data-testid="selected-count">
  {{ selectedFamilies.size }} sélectionnée{{ selectedFamilies.size > 1 ? 's' : '' }}
</v-chip>
<!-- ... -->
<v-chip v-for="f in [...selectedFamilies]" :key="f" size="small" closable @click:close="removeFamily(f)">
  {{ f }}
</v-chip>
```

Code: le contournement — au lieu de réparer la checkbox, l’agent affiche ailleurs ce qui est sélectionné.

[[attention]]
| Morale provisoire : quand un agent ne sait pas réparer un bug, il décore à côté. La sélection fonctionne et l’interface le montre — mais la checkbox reste aveugle. À reprendre proprement la prochaine fois.

*[UI]: User Interface (interface utilisateur)*
