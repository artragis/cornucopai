import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'

// TipTap needs real ProseMirror DOM APIs; stub it in jsdom
vi.mock('@tiptap/vue-3', () => ({
  useEditor: () => ({
    value: {
      getHTML: () => '<p>Feature de test</p>'
    }
  }),
  EditorContent: { name: 'EditorContent', template: '<div class="editor-content" />' }
}))
vi.mock('@tiptap/starter-kit', () => ({ default: { configure: () => ({}) } }))

// Mock the API: catalog with 2 sets / languages en+fr
vi.mock('./services/atelierApi', () => ({
  getCardCatalog: vi.fn(async () => ({
    availableLanguages: ['en', 'fr'],
    sets: [
      {
        id: 'webapp', version: '3.0', languages: ['en', 'fr'], totalCards: 4,
        families: [
          { id: 'VE', name: 'DATA VALIDATION & ENCODING', cardCount: 2 },
          { id: 'AT', name: 'AUTHENTICATION', cardCount: 2 }
        ]
      },
      {
        id: 'eop', version: '5.0', languages: ['en'], totalCards: 2,
        families: [{ id: 'SP', name: 'Spoofing', cardCount: 2 }]
      }
    ]
  })),
  // spec 007 (R-056): one atelier has project facts, one does not
  listAteliers: vi.fn(async () => ([
    { id: 'atelier20261111-100000', createdAt: '2026-11-11T10:00:00Z', playerCount: 4, hasProjectFacts: true },
    { id: 'atelier20261110-090000', createdAt: '2026-11-10T09:00:00Z', playerCount: 2, hasProjectFacts: false }
  ]))
}))

// spec 009 (US2): the dialog fetches the configurable agents for its selector (FR-007)
vi.mock('./services/agentsApi', () => ({
  listAgents: vi.fn(async () => ([
    {
      id: '11111111-1111-4111-8111-111111111111',
      agentName: 'Analyste',
      openAiCompatibleUrl: 'http://localhost:1235/v1',
      apiKeyVar: '',
      modelName: 'qwen3-coder',
      personalityPrompt: 'Tu es tatillon.'
    },
    {
      id: '22222222-2222-4222-8222-222222222222',
      agentName: 'Expert',
      openAiCompatibleUrl: 'https://api.mistral.ai/v1',
      apiKeyVar: 'MISTRAL_API_KEY',
      modelName: 'mistral-large-latest',
      personalityPrompt: 'Tu es pédagogue.'
    }
  ]))
}))

import ConfigureGameDialog from './components/ConfigureGameDialog.vue'
import { getCardCatalog } from './services/atelierApi'
import { listAgents } from './services/agentsApi'

// jsdom lacks visualViewport, required by Vuetify overlay components (v-dialog)
if (!('visualViewport' in globalThis)) {
  Object.defineProperty(globalThis, 'visualViewport', {
    value: {
      width: 1024, height: 768, offsetLeft: 0, offsetTop: 0,
      pageLeft: 0, pageTop: 0, scale: 1,
      addEventListener: () => {}, removeEventListener: () => {}
    }
  })
}

const vuetify = createVuetify({ components, directives })

function mountDialog() {
  return mount(ConfigureGameDialog, {
    props: { modelValue: true },
    global: {
      plugins: [vuetify],
      stubs: {
        VDialog: {
          name: 'VDialog',
          props: ['modelValue'],
          template: '<div class="v-dialog-stub" v-if="modelValue"><slot /></div>'
        },
        RouterLink: { name: 'RouterLink', props: ['to'], template: '<a :href="to"><slot /></a>' }
      }
    }
  })
}

const AGENT_A = '11111111-1111-4111-8111-111111111111'
const AGENT_B = '22222222-2222-4222-8222-222222222222'

/** Emits the v-autocomplete model (array of agent ids) — same pattern as the family checkboxes. */
async function selectAgents(wrapper: ReturnType<typeof mountDialog>, ids: string[]) {
  await wrapper.findComponent('[data-testid="agent-selector"]').vm.$emit('update:modelValue', ids)
  await wrapper.vm.$nextTick()
}

/** Sets are collapsed by default — open every panel before touching the checkboxes. */
async function openAllSets(wrapper: ReturnType<typeof mountDialog>) {
  for (const title of wrapper.findAll('.v-expansion-panel-title')) {
    await title.trigger('click')
  }
  await wrapper.vm.$nextTick()
}

function checkedInput(wrapper: ReturnType<typeof mountDialog>, testid: string): HTMLInputElement {
  return wrapper.find(`[data-testid="${testid}"] input`).element as HTMLInputElement
}

describe('ConfigureGameDialog', () => {
  it('renders the agent selector, WYSIWYG, language select (default fr) and the collapsible set→familles tree', async () => {
    const wrapper = mountDialog()
    await wrapper.vm.$nextTick()
    await new Promise((r) => setTimeout(r, 0)) // let the catalog + agents promises resolve
    // spec 009: the old player-count input is GONE, replaced by the agent selector (FR-007)
    expect(wrapper.find('[data-testid="player-count-input"]').exists()).toBe(false)
    const selector = wrapper.find('[data-testid="agent-selector"]')
    expect(selector.exists()).toBe(true)
    // accessibility (FR-008): the autocomplete exposes a combobox role for keyboard/AT users
    expect(selector.find('input').attributes('role')).toBe('combobox')
    expect(wrapper.find('.editor-content').exists()).toBe(true)
    expect(wrapper.find('[data-testid="language-select"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="card-family-selector"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="set-panel-webapp"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="set-panel-eop"]').exists()).toBe(true)
    await openAllSets(wrapper)
    expect(wrapper.find('[data-testid="set-webapp"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="set-eop"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="family-webapp:VE"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="family-eop:SP"]').exists()).toBe(true)
  })

  it('blocks launch until at least 2 agents AND one family are selected (FR-010)', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await selectAgents(wrapper, [AGENT_A]) // only one agent
    const button = wrapper.find('[data-testid="launch-button"]')
    expect(button.attributes('disabled')).toBeDefined()
    await button.trigger('click')
    expect(wrapper.emitted('launch')).toBeUndefined()
  })

  it('emits launch with agentIds, featureHtml, language=fr and selected families', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await selectAgents(wrapper, [AGENT_A, AGENT_B])
    await openAllSets(wrapper)
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
    const button = wrapper.find('[data-testid="launch-button"]')
    expect(button.attributes('disabled')).toBeUndefined()
    await button.trigger('click')
    const events = wrapper.emitted('launch')
    expect(events).toBeTruthy()
    expect(events![0][0]).toEqual({
      agentIds: [AGENT_A, AGENT_B],
      featureHtml: '<p>Feature de test</p>',
      language: 'fr',
      families: ['webapp:VE']
    })
  })

  // T023 (spec 009, FR-009): the same agent may be selected twice — two players, one agent
  it('allows selecting the same agent twice (two participants sharing one agent)', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await selectAgents(wrapper, [AGENT_A, AGENT_A])
    await openAllSets(wrapper)
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
    await wrapper.find('[data-testid="launch-button"]').trigger('click')
    const payload = wrapper.emitted('launch')![0][0] as { agentIds: string[] }
    expect(payload.agentIds).toEqual([AGENT_A, AGENT_A])
  })

  // T023 (spec 009, US2 scenario 4): with zero agents configured, an empty state links to /agents
  it('shows an empty-state with a link to /agents when no agent exists', async () => {
    vi.mocked(listAgents).mockResolvedValueOnce([])
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    const empty = wrapper.find('[data-testid="agents-empty"]')
    expect(empty.exists()).toBe(true)
    expect(wrapper.find('[data-testid="agents-empty-link"]').attributes('href')).toBe('/agents')
    // launch stays blocked
    expect(wrapper.find('[data-testid="launch-button"]').attributes('disabled')).toBeDefined()
  })

  it('renders the checked state on the family checkbox (bug review: visual state was lost)', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await openAllSets(wrapper)
    expect(checkedInput(wrapper, 'family-webapp:VE').checked).toBe(false)
    expect(wrapper.find('[data-testid="family-webapp:VE"]').html()).toContain('mdi-checkbox-blank-outline')
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
    await wrapper.vm.$nextTick()
    expect(checkedInput(wrapper, 'family-webapp:VE').checked).toBe(true)
    expect(wrapper.find('[data-testid="family-webapp:VE"]').html()).toContain('mdi-checkbox-marked')
  })

  it('the set header shows indeterminate state when only some families are selected', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await openAllSets(wrapper)
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
    await wrapper.vm.$nextTick()
    const setCb = wrapper.findComponent('[data-testid="set-webapp"]')
    expect(setCb.props('indeterminate')).toBe(true)
    expect(setCb.props('modelValue')).toBe(false)
  })

  it('checking a set selects all its families', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await selectAgents(wrapper, [AGENT_A, AGENT_B])
    await openAllSets(wrapper)
    await wrapper.findComponent('[data-testid="set-webapp"]').vm.$emit('update:modelValue', true)
    await wrapper.vm.$nextTick()
    await wrapper.find('[data-testid="launch-button"]').trigger('click')
    const payload = wrapper.emitted('launch')![0][0] as { families: string[] }
    expect(payload.families).toEqual(expect.arrayContaining(['webapp:VE', 'webapp:AT']))
    expect(payload.families).toHaveLength(2)
  })

  it('US4: shows an empty-catalog message when the repo is not configured', async () => {
    vi.mocked(getCardCatalog).mockResolvedValueOnce({ availableLanguages: [], sets: [] })
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    expect(wrapper.find('[data-testid="catalog-empty"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="card-family-selector"]').exists()).toBe(false)
  })

  it('US4: shows a catalog error message when the catalog call fails', async () => {
    vi.mocked(getCardCatalog).mockRejectedValueOnce(new Error('HTTP 500'))
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    expect(wrapper.find('[data-testid="catalog-error"]').exists()).toBe(true)
  })

  it('shows a visual counter and chips of selected families after selection', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    expect(wrapper.find('[data-testid="selected-count"]').exists()).toBe(false)
    await openAllSets(wrapper)
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
    await wrapper.findComponent('[data-testid="family-eop:SP"]').vm.$emit('update:modelValue', ['webapp:VE', 'eop:SP'])
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="selected-count"]').text()).toContain('2')
    const chips = wrapper.find('[data-testid="selected-families"]')
    expect(chips.exists()).toBe(true)
    expect(chips.text()).toContain('webapp:VE')
    expect(chips.text()).toContain('eop:SP')
  })
})
