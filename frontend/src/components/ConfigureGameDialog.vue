<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { getCardCatalog, type CardCatalog, type SetDto } from '../services/atelierApi'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  launch: [payload: { playerCount: number; featureHtml: string; language: string; families: string[] }]
}>()

const playerCount = ref<number | null>(null)

const editor = useEditor({
  extensions: [StarterKit.configure({})],
  content: ''
})

const featureHtml = computed(() => editor.value?.getHTML() ?? '')
const isFeatureEmpty = computed(() => featureHtml.value.replace(/<[^>]*>/g, '').trim().length === 0)

// ---- card catalog + family selection (spec 003) ----
const catalog = ref<CardCatalog | null>(null)
const catalogError = ref('')
const language = ref('fr') // FR-015: default French
const selectedFamilies = ref<Set<string>>(new Set())

watch(() => props.modelValue, (open) => {
  if (open) loadCatalog()
}, { immediate: true })

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

function isFamilySelected(setId: string, familyId: string): boolean {
  return selectedFamilies.value.has(familyKey(setId, familyId))
}

function toggleFamily(setId: string, familyId: string, selected?: boolean) {
  const key = familyKey(setId, familyId)
  const next = new Set(selectedFamilies.value)
  const target = selected ?? !next.has(key)
  if (target) next.add(key)
  else next.delete(key)
  selectedFamilies.value = next
}

function removeFamily(key: string) {
  const next = new Set(selectedFamilies.value)
  next.delete(key)
  selectedFamilies.value = next
}

function allFamiliesOf(set: SetDto): string[] {
  return set.families.map((f) => familyKey(set.id, f.id))
}

function isSetFullySelected(set: SetDto): boolean {
  return allFamiliesOf(set).every((k) => selectedFamilies.value.has(k))
}

function toggleSet(set: SetDto, selected?: boolean) {
  const keys = allFamiliesOf(set)
  const next = new Set(selectedFamilies.value)
  const target = selected ?? !isSetFullySelected(set)
  if (target) keys.forEach((k) => next.add(k))
  else keys.forEach((k) => next.delete(k))
  selectedFamilies.value = next
}

const languageOptions = computed(() => catalog.value?.availableLanguages?.length
  ? catalog.value.availableLanguages
  : ['fr'])

const canLaunch = computed(() =>
  (playerCount.value ?? 0) >= 2 && !isFeatureEmpty.value && selectedFamilies.value.size >= 1)

const errorMessage = computed(() => {
  if ((playerCount.value ?? 0) < 2) return 'Il faut au moins 2 joueurs.'
  if (isFeatureEmpty.value) return 'Décrivez la feature à analyser.'
  if (selectedFamilies.value.size < 1) return 'Sélectionnez au moins une famille de cartes.'
  return ''
})

function onLaunch() {
  if (!canLaunch.value) return
  emit('launch', {
    playerCount: playerCount.value as number,
    featureHtml: featureHtml.value,
    language: language.value,
    families: [...selectedFamilies.value]
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

        <p class="text-body-2 mt-4 mb-1 d-flex align-center gap-2">
          Familles de cartes
          <v-chip
            v-if="selectedFamilies.size"
            size="small"
            color="primary"
            data-testid="selected-count"
          >
            {{ selectedFamilies.size }} sélectionnée{{ selectedFamilies.size > 1 ? 's' : '' }}
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
          <div
            v-for="set in catalog.sets"
            :key="set.id"
            class="mb-2"
          >
            <v-checkbox
              :model-value="isSetFullySelected(set)"
              :data-testid="`set-${set.id}`"
              :label="`${set.id} ${set.version} (${set.totalCards} cartes)`"
              density="compact"
              hide-details
              @update:model-value="(v: boolean) => toggleSet(set, v)"
            />
            <div class="ml-6">
              <v-checkbox
                v-for="family in set.families"
                :key="family.id"
                :model-value="isFamilySelected(set.id, family.id)"
                :data-testid="`family-${set.id}:${family.id}`"
                :label="`${family.name} (${family.cardCount})`"
                density="compact"
                hide-details
                @update:model-value="(v: boolean) => toggleFamily(set.id, family.id, v)"
              />
            </div>
          </div>
        </div>
        <div
          v-if="selectedFamilies.size"
          class="d-flex flex-wrap gap-1 mt-2"
          data-testid="selected-families"
        >
          <v-chip
            v-for="f in [...selectedFamilies]"
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
