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
  }))
}))

import ConfigureGameDialog from './components/ConfigureGameDialog.vue'
import { getCardCatalog } from './services/atelierApi'

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
        }
      }
    }
  })
}

function playerCountInput(wrapper: ReturnType<typeof mountDialog>) {
  return wrapper.find('[data-testid="player-count-input"] input')
}

describe('ConfigureGameDialog', () => {
  it('renders player count, WYSIWYG, language select (default fr) and the hierarchical set→familles selector', async () => {
    const wrapper = mountDialog()
    await wrapper.vm.$nextTick()
    await new Promise((r) => setTimeout(r, 0)) // let the catalog promise resolve
    expect(wrapper.find('[data-testid="player-count-input"]').exists()).toBe(true)
    expect(wrapper.find('.editor-content').exists()).toBe(true)
    expect(wrapper.find('[data-testid="language-select"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="card-family-selector"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="set-webapp"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="set-eop"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="family-webapp:VE"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="family-eop:SP"]').exists()).toBe(true)
  })

  it('blocks launch until at least one family is selected', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await playerCountInput(wrapper).setValue(4)
    const button = wrapper.find('[data-testid="launch-button"]')
    expect(button.attributes('disabled')).toBeDefined()
    await button.trigger('click')
    expect(wrapper.emitted('launch')).toBeUndefined()
  })

  it('emits launch with playerCount, featureHtml, language=fr and selected families', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await playerCountInput(wrapper).setValue(4)
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', true)
    const button = wrapper.find('[data-testid="launch-button"]')
    expect(button.attributes('disabled')).toBeUndefined()
    await button.trigger('click')
    const events = wrapper.emitted('launch')
    expect(events).toBeTruthy()
    expect(events![0][0]).toEqual({
      playerCount: 4,
      featureHtml: '<p>Feature de test</p>',
      language: 'fr',
      families: ['webapp:VE']
    })
  })

  it('checking a set selects all its families', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await playerCountInput(wrapper).setValue(2)
    await wrapper.findComponent('[data-testid="set-webapp"]').vm.$emit('update:modelValue', true)
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
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', true)
    await wrapper.findComponent('[data-testid="family-eop:SP"]').vm.$emit('update:modelValue', true)
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="selected-count"]').text()).toContain('2')
    const chips = wrapper.find('[data-testid="selected-families"]')
    expect(chips.exists()).toBe(true)
    expect(chips.text()).toContain('webapp:VE')
    expect(chips.text()).toContain('eop:SP')
  })
})
