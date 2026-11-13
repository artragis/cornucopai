import { describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'

// T013 (spec 009, US1): the home page has a "Gérer les agents" navigation button to /agents.

vi.mock('@tiptap/vue-3', () => ({
  useEditor: () => ({ value: { getHTML: () => '<p>f</p>' } }),
  EditorContent: { name: 'EditorContent', template: '<div />' }
}))
vi.mock('@tiptap/starter-kit', () => ({ default: { configure: () => ({}) } }))

vi.mock('./services/atelierApi', () => ({
  listAteliers: vi.fn(async () => []),
  getCardCatalog: vi.fn(async () => ({ availableLanguages: ['fr'], sets: [] })),
  createAtelier: vi.fn()
}))

const pushMock = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock })
}))

import HomePage from './HomePage.vue'

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

describe('HomePage', () => {
  it('has a nav button to /agents', async () => {
    const wrapper = mount(HomePage, { global: { plugins: [vuetify] } })
    await flushPromises()
    const btn = wrapper.find('[data-testid="agents-button"]')
    expect(btn.exists()).toBe(true)
    await btn.trigger('click')
    expect(pushMock).toHaveBeenCalledWith('/agents')
  })
})
