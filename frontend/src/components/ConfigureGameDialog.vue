<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { getCardCatalog, listAteliers, type AtelierSummaryDto, type CardCatalog, type SetDto } from '../services/atelierApi'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  launch: [payload: { playerCount: number; featureHtml: string; language: string; families: string[]; inheritFactsFrom?: string }]
}>()

const playerCount = ref<number | null>(null)

const editor = useEditor({
  extensions: [StarterKit.configure({})],
  content: ''
})

const featureHtml = computed(() => editor.value?.getHTML() ?? '')
const isFeatureEmpty = computed(() => featureHtml.value.replace(/<[^>]*>/g, '').trim().length === 0)

// ---- card catalog + family selection (spec 003) ----
// Array model + :value per checkbox = Vuetify's NATIVE checkbox-group pattern:
// the visual checked state is managed by Vuetify itself (model-value.includes(value)).
// The old controlled-boolean pattern desynchronized the visual state (review: "Cloud").
const catalog = ref<CardCatalog | null>(null)
const catalogError = ref('')
const language = ref('fr') // FR-015: default French
const selectedFamilies = ref<string[]>([])
const openPanels = ref<number[]>([]) // collapsed sets by default (review request)

// ---- spec 007 (FR-007): optional inheritance of a previous atelier's project facts ----
const ateliersWithFacts = ref<AtelierSummaryDto[]>([])
const inheritFactsFrom = ref<string | null>(null)

watch(() => props.modelValue, (open) => {
  if (open) {
    loadCatalog()
    loadAteliersWithFacts()
  }
}, { immediate: true })

async function loadAteliersWithFacts() {
  try {
    const all = await listAteliers()
    ateliersWithFacts.value = all.filter((a) => a.hasProjectFacts)
  } catch {
    ateliersWithFacts.value = []
  }
}

async function loadCatalog() {
  catalogError.value = ''
  try {
    catalog.value = await getCardCatalog()
  } catch (e) {
    catalog.value = null
    catalogError.value = e instanceof Error ? e.message : String(e)
  }
}

function familyKey(setId: string, familyId: string): string {
  return `${setId}:${familyId}`
}

function removeFamily(key: string) {
  selectedFamilies.value = selectedFamilies.value.filter((k) => k !== key)
}

function allFamiliesOf(set: SetDto): string[] {
  return set.families.map((f) => familyKey(set.id, f.id))
}

function isSetFullySelected(set: SetDto): boolean {
  return allFamiliesOf(set).every((k) => selectedFamilies.value.includes(k))
}

function isSetPartiallySelected(set: SetDto): boolean {
  const n = selectedInSet(set)
  return n > 0 && n < set.families.length
}

function selectedInSet(set: SetDto): number {
  return allFamiliesOf(set).filter((k) => selectedFamilies.value.includes(k)).length
}

function toggleSet(set: SetDto, selected?: boolean) {
  const keys = allFamiliesOf(set)
  const target = selected ?? !isSetFullySelected(set)
  if (target) {
    selectedFamilies.value = [...new Set([...selectedFamilies.value, ...keys])]
  } else {
    selectedFamilies.value = selectedFamilies.value.filter((k) => !keys.includes(k))
  }
}

const languageOptions = computed(() => catalog.value?.availableLanguages?.length
  ? catalog.value.availableLanguages
  : ['fr'])

const canLaunch = computed(() =>
  (playerCount.value ?? 0) >= 2 && !isFeatureEmpty.value && selectedFamilies.value.length >= 1)

const errorMessage = computed(() => {
  if ((playerCount.value ?? 0) < 2) return 'Il faut au moins 2 joueurs.'
  if (isFeatureEmpty.value) return 'Décrivez la feature à analyser.'
  if (selectedFamilies.value.length < 1) return 'Sélectionnez au moins une famille de cartes.'
  return ''
})

function onLaunch() {
  if (!canLaunch.value) return
  emit('launch', {
    playerCount: playerCount.value as number,
    featureHtml: featureHtml.value,
    language: language.value,
    families: [...selectedFamilies.value],
    ...(inheritFactsFrom.value ? { inheritFactsFrom: inheritFactsFrom.value } : {})
  })
  emit('update:modelValue', false)
}

function onClose() {
  emit('update:modelValue', false)
}
</script>

<template>
  <v-dialog
    :model-value="props.modelValue"
    max-width="720"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <v-card title="Configurer une partie">
      <v-card-text>
        <v-text-field
          v-model.number="playerCount"
          data-testid="player-count-input"
          label="Nombre de joueurs"
          type="number"
          min="2"
          :hint="errorMessage"
          persistent-hint
        />
        <p class="text-body-2 mt-4 mb-1">
          Feature à analyser (threat modeling)
        </p>
        <div class="border rounded pa-2 editor-wrapper">
          <EditorContent :editor="editor" />
        </div>

        <v-select
          v-model="language"
          data-testid="language-select"
          class="mt-4"
          label="Langue des cartes"
          :items="languageOptions"
        />

        <v-select
          v-if="ateliersWithFacts.length"
          v-model="inheritFactsFrom"
          data-testid="inherit-facts-select"
          class="mt-4"
          label="Réutiliser les faits projet d'un atelier précédent"
          :items="ateliersWithFacts"
          item-title="id"
          item-value="id"
          clearable
          hint="Optionnel : les découvertes projet de l'atelier source seront copiées dans la nouvelle partie"
          persistent-hint
        />

        <p class="text-body-2 mt-4 mb-1 d-flex align-center gap-2">
          Familles de cartes
          <v-chip
            v-if="selectedFamilies.length"
            size="small"
            color="primary"
            data-testid="selected-count"
          >
            {{ selectedFamilies.length }} sélectionnée{{ selectedFamilies.length > 1 ? 's' : '' }}
          </v-chip>
        </p>
        <v-alert
          v-if="catalogError"
          type="warning"
          density="compact"
          class="mb-2"
          data-testid="catalog-error"
        >
          Catalogue indisponible : {{ catalogError }}
        </v-alert>
        <div
          v-if="catalog && catalog.sets.length"
          class="border rounded pa-2"
          data-testid="card-family-selector"
        >
          <!-- arbre dépliable par set (demande review) -->
          <v-expansion-panels
            v-model="openPanels"
            variant="accordion"
            multiple
          >
            <v-expansion-panel
              v-for="set in catalog.sets"
              :key="set.id"
              :data-testid="`set-panel-${set.id}`"
            >
              <v-expansion-panel-title>
                <div class="d-flex align-center flex-wrap ga-2">
                  <strong>{{ set.id }} {{ set.version }}</strong>
                  <span class="text-caption">({{ set.totalCards }} cartes)</span>
                  <v-chip
                    v-if="selectedInSet(set)"
                    size="x-small"
                    color="primary"
                  >
                    {{ selectedInSet(set) }}/{{ set.families.length }}
                  </v-chip>
                </div>
              </v-expansion-panel-title>
              <v-expansion-panel-text>
                <!-- tout sélectionner : contrôlé (pattern select-all Vuetify) -->
                <v-checkbox
                  :model-value="isSetFullySelected(set)"
                  :indeterminate="isSetPartiallySelected(set)"
                  :data-testid="`set-${set.id}`"
                  :label="`Tout sélectionner (${set.id})`"
                  density="compact"
                  hide-details
                  @update:model-value="(v: boolean|null) => toggleSet(set, !!v)"
                />
                <!-- familles : pattern natif v-model tableau + :value (état visuel géré par Vuetify) -->
                <div class="ml-6">
                  <v-checkbox
                    v-for="family in set.families"
                    :key="family.id"
                    v-model="selectedFamilies"
                    :value="familyKey(set.id, family.id)"
                    :data-testid="`family-${set.id}:${family.id}`"
                    :label="`${family.name} (${family.cardCount})`"
                    density="compact"
                    hide-details
                  />
                </div>
              </v-expansion-panel-text>
            </v-expansion-panel>
          </v-expansion-panels>
        </div>
        <div
          v-if="selectedFamilies.length"
          class="d-flex flex-wrap gap-1 mt-2"
          data-testid="selected-families"
        >
          <v-chip
            v-for="f in selectedFamilies"
            :key="f"
            size="small"
            closable
            @click:close="removeFamily(f)"
          >
            {{ f }}
          </v-chip>
        </div>
        <p
          v-else-if="catalog && !catalog.sets.length"
          class="text-body-2 text-warning"
          data-testid="catalog-empty"
        >
          Aucun set de cartes disponible — configurez CORNUCOPIA_HOME.
        </p>
      </v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          variant="text"
          @click="onClose"
        >
          Annuler
        </v-btn>
        <v-btn
          color="primary"
          data-testid="launch-button"
          :disabled="!canLaunch"
          @click="onLaunch"
        >
          Lancer
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>
.editor-wrapper {
  min-height: 160px;
}
.editor-wrapper :deep(.tiptap) {
  outline: none;
  min-height: 140px;
}
</style>
