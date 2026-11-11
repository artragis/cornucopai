<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ConfigureGameDialog from './components/ConfigureGameDialog.vue'
import { createAtelier, listAteliers, type AtelierSummaryDto } from './services/atelierApi'

const router = useRouter()
const dialogOpen = ref(false)
const loading = ref(false)
const error = ref('')
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
    const atelier = await createAtelier(payload)
    previous.value = await listAteliers()
    // T027 (US4): after launching, navigate to the dedicated detail route (FR-013)
    await router.push(`/ateliers/${atelier.id}`)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
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
          v-if="previous.length"
          title="Ateliers précédents"
        >
          <v-list density="compact">
            <!-- T027 (US4, FR-013): each history item is a router-link to /ateliers/:id -->
            <router-link
              v-for="a in previous"
              :key="a.id"
              :to="`/ateliers/${a.id}`"
              class="text-decoration-none"
            >
              <v-list-item>
                <v-list-item-title>{{ a.id }}</v-list-item-title>
                <v-list-item-subtitle>{{ a.playerCount }} joueurs — {{ a.createdAt }}</v-list-item-subtitle>
              </v-list-item>
            </router-link>
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
