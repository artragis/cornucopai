<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ConfigureGameDialog from './components/ConfigureGameDialog.vue'
import { createAtelier, listAteliers, type AtelierResponse, type AtelierSummaryDto } from './services/atelierApi'

const dialogOpen = ref(false)
const loading = ref(false)
const error = ref('')
const atelier = ref<AtelierResponse | null>(null)
const previous = ref<AtelierSummaryDto[]>([])

onMounted(async () => {
  try {
    previous.value = await listAteliers()
  } catch {
    // backend may be down; the list stays empty
  }
})

async function onLaunch(payload: { playerCount: number; featureHtml: string; language: string; families: string[] }) {
  loading.value = true
  error.value = ''
  try {
    atelier.value = await createAtelier(payload)
    previous.value = await listAteliers()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

function validationColor(validation: string): string {
  return validation === 'VALIDATED' ? 'success' : validation === 'REJECTED' ? 'error' : 'warning'
}
</script>

<template>
  <v-app>
    <v-main>
      <v-container>
        <v-card
          title="CornucopAI"
          class="mb-4"
        >
          <v-card-text>
            Playground où des agents IA jouent à Cornucopia (threat modeling). Le programme fait
            office de maître du jeu.
          </v-card-text>
          <v-card-actions>
            <v-btn
              color="primary"
              data-testid="configure-button"
              @click="dialogOpen = true"
            >
              Configurer une partie
            </v-btn>
          </v-card-actions>
        </v-card>

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

        <v-card
          v-if="atelier"
          :title="`Atelier ${atelier.id}`"
          class="mb-4"
        >
          <v-card-text>
            <div
              class="mb-2"
              v-html="atelier.featureHtml"
            />
            <p class="text-body-2">
              Joueurs : {{ atelier.players.map((p) => `${p.name} (${p.archetype})`).join(', ') }}
              — ordre des tours : {{ atelier.turnOrder.join(' → ') }}
            </p>
            <v-list density="compact">
              <v-list-item
                v-for="(leg, i) in atelier.legs"
                :key="i"
              >
                <v-list-item-title>
                  {{ leg.cardName }} — {{ leg.scenario }}
                </v-list-item-title>
                <v-list-item-subtitle>
                  {{ leg.explanation }} — précisions : {{ leg.precisions.length }}
                </v-list-item-subtitle>
                <template #append>
                  <v-chip
                    :color="validationColor(leg.validation)"
                    size="small"
                  >
                    {{ leg.validation }}
                  </v-chip>
                </template>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>

        <v-card
          v-if="previous.length"
          title="Ateliers précédents"
        >
          <v-list density="compact">
            <v-list-item
              v-for="a in previous"
              :key="a.id"
            >
              <v-list-item-title>{{ a.id }}</v-list-item-title>
              <v-list-item-subtitle>{{ a.playerCount }} joueurs — {{ a.createdAt }}</v-list-item-subtitle>
            </v-list-item>
          </v-list>
        </v-card>

        <ConfigureGameDialog
          v-model="dialogOpen"
          @launch="onLaunch"
        />
      </v-container>
    </v-main>
  </v-app>
</template>
