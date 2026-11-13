<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAtelier, type AtelierResponse } from '../services/atelierApi'
import { useAtelierPolling } from '../composables/useAtelierPolling'

// T026 (US4, spec 006 FR-014): dedicated detail page — scoreboard on top, all legs below.
const route = useRoute()
const router = useRouter()
const id = String(route.params.id)

const atelier = ref<AtelierResponse | null>(null)
const error = ref('')
const loading = ref(true)

// T031 (US5): polling every 5 s while the atelier is unfinished
const { data: polled } = useAtelierPolling(id)

onMounted(async () => {
  try {
    atelier.value = await getAtelier(id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
})

// react to polling updates
const current = computed(() => polled.value ?? atelier.value)

const isLegacy = computed(() => !current.value?.tours || current.value.tours.length === 0)
const isFinished = computed(() => current.value?.finishedAt != null)

const scoreRows = computed(() => {
  const a = current.value
  if (!a) return []
  const scores = a.scores ?? {}
  return a.players.map((p) => ({
    name: p.name,
    // spec 009: new ateliers show the driving agent id; legacy ones still show the archetype
    agentLabel: p.agentId ? `agent ${p.agentId.slice(0, 8)}…` : (p.archetype ?? ''),
    score: scores[p.name] ?? 0,
    isWinner: (a.winner ?? []).includes(p.name)
  }))
})

function validationColor(validation: string): string {
  return validation === 'VALIDATED' ? 'success' : validation === 'REJECTED' ? 'error' : 'warning'
}
</script>

<template>
  <v-app>
    <v-main>
      <v-container>
        <v-btn
          variant="text"
          class="mb-4"
          @click="router.push('/')"
        >
          ← Ateliers
        </v-btn>

        <v-alert
          v-if="error"
          type="error"
          class="mb-4"
        >
          {{ error }}
        </v-alert>
        <v-progress-linear
          v-if="loading"
          indeterminate
          class="mb-4"
        />

        <template v-if="current">
          <v-alert
            v-if="isLegacy"
            type="info"
            variant="tonal"
            class="mb-4"
          >
            Données partielles — cet atelier a été joué avant le scoreboard (0 point affiché).
          </v-alert>

          <!-- Scoreboard (FR-014) -->
          <v-card
            :title="`Atelier ${current.id}`"
            class="mb-4"
          >
            <v-card-text>
              <div
                class="mb-2"
                v-html="current.featureHtml"
              />
              <p class="text-body-2 mb-2">
                Ordre des tours : {{ current.turnOrder.join(' → ') }}
                — <v-chip
                  size="small"
                  :color="isFinished ? 'success' : 'warning'"
                >
                  {{ isFinished ? 'terminé' : 'en cours' }}
                </v-chip>
                <template v-if="isFinished && (current.winner?.length ?? 0) > 0">
                  — vainqueur :
                  <strong>{{ current.winner!.join(', ') }}</strong>
                </template>
              </p>
              <v-table density="compact">
                <thead>
                  <tr>
                    <th>Joueur</th>
                    <th>Archétype</th>
                    <th>Score</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="row in scoreRows"
                    :key="row.name"
                  >
                    <td>{{ row.name }}</td>
                    <td>{{ row.agentLabel }}</td>
                    <td><strong>{{ row.score }}</strong></td>
                    <td>
                      <v-chip
                        v-if="row.isWinner && isFinished"
                        color="success"
                        size="small"
                      >
                        vainqueur
                      </v-chip>
                    </td>
                  </tr>
                </tbody>
              </v-table>
            </v-card-text>
          </v-card>

          <!-- All legs / scénarios (FR-014), each expandable to show its entries (Q/R) -->
          <v-card title="Scénarios">
            <v-expansion-panels variant="accordion">
              <v-expansion-panel
                v-for="(leg, i) in current.legs"
                :key="i"
              >
                <v-expansion-panel-title>
                  <div class="d-flex flex-wrap align-center ga-2">
                    <strong>{{ leg.cardName }}</strong>
                    <span
                      v-if="leg.suitId"
                      class="text-caption"
                    >({{ leg.suitId }}{{ leg.cardValue ? ' ' + leg.cardValue : '' }})</span>
                    <span class="text-body-2">{{ leg.scenario }}</span>
                    <v-chip
                      :color="validationColor(leg.validation)"
                      size="x-small"
                    >
                      {{ leg.validation }}
                    </v-chip>
                    <v-chip
                      size="x-small"
                      color="primary"
                    >
                      {{ leg.points ?? 0 }} pt
                    </v-chip>
                  </div>
                </v-expansion-panel-title>
                <v-expansion-panel-text>
                  <div class="mb-2">
                    <span
                      v-if="leg.majorSuit"
                      class="text-caption d-block"
                    >
                      Couleur majeure du tour : <strong>{{ leg.majorSuit }}</strong>
                      <template v-if="leg.turnIndex">
                        — tour n°{{ leg.turnIndex }}
                      </template>
                    </span>
                  </div>
                  <p class="text-body-2 mb-3">
                    <strong>Explication :</strong> {{ leg.explanation }}
                  </p>
                  <template v-if="leg.entries && leg.entries.length">
                    <p class="text-caption font-weight-bold mb-2">
                      Précisions & questions ({{ leg.entries.length }})
                    </p>
                    <div
                      v-for="(entry, j) in leg.entries"
                      :key="j"
                      class="entry-row mb-3 pa-2 rounded"
                    >
                      <div class="d-flex align-center flex-wrap ga-2 mb-1">
                        <!-- type d'entrée -->
                        <v-chip
                          size="x-small"
                          :color="entry.kind === 'QUESTION' ? 'info' : 'grey'"
                        >
                          {{ entry.kind === 'QUESTION' ? 'Question' : 'Précision' }}
                        </v-chip>
                        <!-- état de la réponse pour une question -->
                        <v-chip
                          v-if="entry.kind === 'QUESTION'"
                          size="x-small"
                          :color="entry.knows ? 'success' : 'error'"
                          variant="outlined"
                        >
                          {{ entry.knows ? 'répondu' : 'je sais pas' }}
                        </v-chip>
                        <span class="text-caption font-weight-bold">{{ entry.author }}</span>
                      </div>
                      <p class="text-body-2 mb-1 text-wrap">
                        {{ entry.text }}
                      </p>
                      <!-- réponse du joueur de la carte, uniquement si connue -->
                      <div
                        v-if="entry.kind === 'QUESTION' && entry.knows && entry.answer"
                        class="reponse pa-2 mt-1 rounded"
                      >
                        <span class="text-caption font-weight-bold">Réponse du joueur :</span>
                        <span class="text-body-2"> {{ entry.answer }}</span>
                      </div>
                    </div>
                  </template>
                </v-expansion-panel-text>
              </v-expansion-panel>
            </v-expansion-panels>
          </v-card>
        </template>
      </v-container>
    </v-main>
  </v-app>
</template>

<style scoped>
.entry-row {
  background-color: rgb(var(--v-theme-surface-variant));
}
.entry-row .reponse {
  background-color: rgb(var(--v-theme-success));
  color: rgb(var(--v-theme-on-success));
}
</style>
