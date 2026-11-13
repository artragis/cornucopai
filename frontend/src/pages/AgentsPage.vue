<script setup lang="ts">
// Spec 009 (US1, FR-001…FR-005): dedicated page to list, create, edit and delete
// configurable agents. Route /agents (see router.ts).
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createAgent,
  deleteAgent,
  listAgents,
  updateAgent,
  type AgentDto,
  type AgentInput
} from '../services/agentsApi'

const router = useRouter()

const agents = ref<AgentDto[]>([])
const loading = ref(false)
const error = ref('')

const dialogOpen = ref(false)
const editingId = ref<string | null>(null)
const form = reactive<AgentInput>({
  agentName: '',
  openAiCompatibleUrl: '',
  apiKeyVar: '',
  modelName: '',
  personalityPrompt: ''
})
const formError = ref('')
const saving = ref(false)

const confirmDeleteId = ref<string | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    agents.value = await listAgents()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    agentName: '',
    openAiCompatibleUrl: '',
    apiKeyVar: '',
    modelName: '',
    personalityPrompt: ''
  })
  formError.value = ''
  dialogOpen.value = true
}

function openEdit(agent: AgentDto) {
  editingId.value = agent.id
  Object.assign(form, {
    agentName: agent.agentName,
    openAiCompatibleUrl: agent.openAiCompatibleUrl,
    apiKeyVar: agent.apiKeyVar,
    modelName: agent.modelName,
    personalityPrompt: agent.personalityPrompt
  })
  formError.value = ''
  dialogOpen.value = true
}

function validateForm(): string {
  if (!form.agentName.trim()) return 'Le nom lisible est obligatoire.'
  if (!/^https?:\/\/.+/i.test(form.openAiCompatibleUrl.trim()))
    return "L'URL du fournisseur doit être une URL http(s) absolue."
  if (form.apiKeyVar && !/^[A-Za-z_][A-Za-z0-9_]*$/.test(form.apiKeyVar))
    return "La variable d'environnement est vide (pas de clé) ou un nom valide."
  if (!form.modelName.trim()) return 'Le nom du modèle est obligatoire.'
  if (!form.personalityPrompt.trim()) return 'Le prompt de personnalité est obligatoire.'
  return ''
}

async function save() {
  formError.value = validateForm()
  if (formError.value) return
  saving.value = true
  try {
    const input: AgentInput = {
      agentName: form.agentName.trim(),
      openAiCompatibleUrl: form.openAiCompatibleUrl.trim(),
      apiKeyVar: form.apiKeyVar.trim(),
      modelName: form.modelName.trim(),
      personalityPrompt: form.personalityPrompt.trim()
    }
    if (editingId.value) {
      await updateAgent(editingId.value, input)
    } else {
      await createAgent(input)
    }
    dialogOpen.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : String(e)
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  if (!confirmDeleteId.value) return
  try {
    await deleteAgent(confirmDeleteId.value)
    confirmDeleteId.value = null
    await load()
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  }
}
</script>

<template>
  <v-app>
    <v-main>
      <v-container>
        <v-card title="Agents configurables" class="mb-4">
          <v-card-text>
            Les agents définissent le fournisseur LLM, le modèle, la clé (via une variable
            d'environnement) et la personnalité de chaque joueur IA.
          </v-card-text>
          <v-card-actions>
            <v-btn
              color="primary"
              data-testid="create-agent-button"
              @click="openCreate"
            >
              Créer un agent
            </v-btn>
            <v-btn
              data-testid="back-home-button"
              @click="router.push('/')"
            >
              Accueil
            </v-btn>
          </v-card-actions>
        </v-card>

        <v-alert v-if="error" type="error" class="mb-4" data-testid="agents-error">
          {{ error }}
        </v-alert>
        <v-progress-linear v-if="loading" indeterminate class="mb-4" />

        <v-alert
          v-if="!loading && agents.length === 0"
          type="info"
          data-testid="agents-empty"
        >
          Aucun agent configuré. Créez-en un pour pouvoir lancer une partie.
        </v-alert>

        <v-list v-if="agents.length" data-testid="agents-list" density="comfortable">
          <v-list-item
            v-for="agent in agents"
            :key="agent.id"
            :data-testid="`agent-item-${agent.id}`"
          >
            <v-list-item-title :data-testid="`agent-name-${agent.id}`">
              {{ agent.agentName }}
            </v-list-item-title>
            <v-list-item-subtitle>
              {{ agent.openAiCompatibleUrl }} — modèle {{ agent.modelName }}
              <span v-if="agent.apiKeyVar">— clé via {{ agent.apiKeyVar }}</span>
              <span v-else>— sans clé</span>
            </v-list-item-subtitle>
            <template #append>
              <v-btn
                size="small"
                class="mr-2"
                :data-testid="`edit-agent-${agent.id}`"
                @click="openEdit(agent)"
              >
                Modifier
              </v-btn>
              <v-btn
                size="small"
                color="error"
                :data-testid="`delete-agent-${agent.id}`"
                @click="confirmDeleteId = agent.id"
              >
                Supprimer
              </v-btn>
            </template>
          </v-list-item>
        </v-list>

        <v-dialog v-model="dialogOpen" max-width="640">
          <v-card :title="editingId ? 'Modifier un agent' : 'Créer un agent'">
            <v-card-text>
              <v-alert v-if="formError" type="error" density="compact" class="mb-3" data-testid="form-error">
                {{ formError }}
              </v-alert>
              <v-text-field
                v-model="form.agentName"
                label="Nom lisible (agentName)"
                data-testid="form-agent-name"
                required
              />
              <v-text-field
                v-model="form.openAiCompatibleUrl"
                label="URL du fournisseur LLM (openAiCompatibleUrl)"
                data-testid="form-url"
                hint="URL absolue http(s) : serveur local, API Mistral, Azure Foundry…"
                persistent-hint
                required
              />
              <v-text-field
                v-model="form.apiKeyVar"
                label="Variable d'environnement de la clé (apiKeyVar)"
                data-testid="form-apikeyvar"
                hint="Vide = aucune clé requise. Sinon, nom de la variable d'env (ex. MISTRAL_API_KEY). La valeur de la clé n'est jamais stockée."
                persistent-hint
              />
              <v-text-field
                v-model="form.modelName"
                label="Nom du modèle (modelName)"
                data-testid="form-model"
                required
              />
              <v-textarea
                v-model="form.personalityPrompt"
                label="Prompt de personnalité (personalityPrompt)"
                data-testid="form-personality"
                rows="5"
                required
              />
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn data-testid="cancel-button" @click="dialogOpen = false">Annuler</v-btn>
              <v-btn
                color="primary"
                data-testid="save-agent-button"
                :loading="saving"
                @click="save"
              >
                Enregistrer
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>

        <v-dialog v-model="confirmDeleteId" max-width="420">
          <v-card title="Supprimer cet agent ?">
            <v-card-text>
              L'agent sera supprimé et ne sera plus sélectionnable pour une nouvelle partie.
              Les ateliers déjà lancés conservent leur configuration copiée au lancement.
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn data-testid="cancel-delete-button" @click="confirmDeleteId = null">Annuler</v-btn>
              <v-btn
                color="error"
                data-testid="confirm-delete-button"
                @click="confirmDelete"
              >
                Supprimer
              </v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </v-container>
    </v-main>
  </v-app>
</template>
