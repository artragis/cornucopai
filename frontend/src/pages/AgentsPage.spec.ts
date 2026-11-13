import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'
import type { AgentDto } from '../services/agentsApi'

// T011 (spec 009, US1): the /agents page lists agents, creates (validating FR-019 client-side),
// edits keeping the same id (FR-004), and deletes with confirmation (FR-005).

vi.mock('../services/agentsApi', () => ({
  listAgents: vi.fn(),
  getAgent: vi.fn(),
  createAgent: vi.fn(),
  updateAgent: vi.fn(),
  deleteAgent: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

import AgentsPage from './AgentsPage.vue'
import { listAgents, createAgent, updateAgent, deleteAgent } from '../services/agentsApi'

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

const agent: AgentDto = {
  id: '11111111-2222-4333-8444-555555555555',
  agentName: 'Analyste',
  openAiCompatibleUrl: 'http://localhost:1235/v1',
  apiKeyVar: '',
  modelName: 'qwen3-coder',
  personalityPrompt: 'Tu es tatillon.'
}

function mountPage() {
  return mount(AgentsPage, {
    global: {
      plugins: [vuetify],
      stubs: {
        // Vuetify overlays need real layout APIs; render the slot inline instead
        VDialog: {
          name: 'VDialog',
          props: ['modelValue'],
          template: '<div class="v-dialog-stub" v-if="modelValue"><slot /></div>'
        }
      }
    }
  })
}

describe('AgentsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows an empty state when no agent exists', async () => {
    vi.mocked(listAgents).mockResolvedValue([])
    const wrapper = mountPage()
    await flushPromises()
    expect(wrapper.find('[data-testid="agents-empty"]').exists()).toBe(true)
  })

  it('lists agents with their names', async () => {
    vi.mocked(listAgents).mockResolvedValue([agent])
    const wrapper = mountPage()
    await flushPromises()
    expect(wrapper.find('[data-testid="agents-list"]').exists()).toBe(true)
    expect(wrapper.find(`[data-testid="agent-name-${agent.id}"]`).text()).toContain('Analyste')
  })

  it('create dialog validates required fields client-side (FR-019)', async () => {
    vi.mocked(listAgents).mockResolvedValue([])
    const wrapper = mountPage()
    await flushPromises()
    await wrapper.find('[data-testid="create-agent-button"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-testid="save-agent-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="form-error"]').exists()).toBe(true)
    expect(createAgent).not.toHaveBeenCalled()
  })

  it('creates an agent with valid fields', async () => {
    vi.mocked(listAgents).mockResolvedValue([])
    vi.mocked(createAgent).mockResolvedValue(agent)
    const wrapper = mountPage()
    await flushPromises()
    await wrapper.find('[data-testid="create-agent-button"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-testid="form-agent-name"] input').setValue('Analyste')
    await wrapper.find('[data-testid="form-url"] input').setValue('http://localhost:1235/v1')
    await wrapper.find('[data-testid="form-model"] input').setValue('qwen3-coder')
    await wrapper.find('[data-testid="form-personality"] textarea').setValue('Tu es tatillon.')
    await wrapper.find('[data-testid="save-agent-button"]').trigger('click')
    await flushPromises()
    expect(createAgent).toHaveBeenCalledWith({
      agentName: 'Analyste',
      openAiCompatibleUrl: 'http://localhost:1235/v1',
      apiKeyVar: '',
      modelName: 'qwen3-coder',
      personalityPrompt: 'Tu es tatillon.'
    })
  })

  it('edit keeps the same id in the PUT (FR-004)', async () => {
    vi.mocked(listAgents).mockResolvedValue([agent])
    vi.mocked(updateAgent).mockResolvedValue({ ...agent, agentName: 'Analyste v2' })
    const wrapper = mountPage()
    await flushPromises()
    await wrapper.find(`[data-testid="edit-agent-${agent.id}"]`).trigger('click')
    await flushPromises()
    await wrapper.find('[data-testid="form-agent-name"] input').setValue('Analyste v2')
    await wrapper.find('[data-testid="save-agent-button"]').trigger('click')
    await flushPromises()
    expect(updateAgent).toHaveBeenCalledWith(
      agent.id,
      expect.objectContaining({ agentName: 'Analyste v2' })
    )
  })

  it('delete asks for confirmation then calls the API (FR-005)', async () => {
    vi.mocked(listAgents).mockResolvedValue([agent])
    vi.mocked(deleteAgent).mockResolvedValue(undefined)
    const wrapper = mountPage()
    await flushPromises()
    await wrapper.find(`[data-testid="delete-agent-${agent.id}"]`).trigger('click')
    await flushPromises()
    await wrapper.find('[data-testid="confirm-delete-button"]').trigger('click')
    await flushPromises()
    expect(deleteAgent).toHaveBeenCalledWith(agent.id)
  })
})
