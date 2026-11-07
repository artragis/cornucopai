<script setup lang="ts">
import { computed, ref } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  launch: [payload: { playerCount: number; featureHtml: string }]
}>()

const playerCount = ref<number | null>(null)

const editor = useEditor({
  extensions: [StarterKit.configure({})],
  content: ''
})

const featureHtml = computed(() => editor.value?.getHTML() ?? '')
const isFeatureEmpty = computed(() => featureHtml.value.replace(/<[^>]*>/g, '').trim().length === 0)
const canLaunch = computed(() => (playerCount.value ?? 0) >= 2 && !isFeatureEmpty.value)
const errorMessage = computed(() => {
  if ((playerCount.value ?? 0) < 2) return 'Il faut au moins 2 joueurs.'
  if (isFeatureEmpty.value) return 'Décrivez la feature à analyser.'
  return ''
})

function onLaunch() {
  if (!canLaunch.value) return
  emit('launch', { playerCount: playerCount.value as number, featureHtml: featureHtml.value })
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
