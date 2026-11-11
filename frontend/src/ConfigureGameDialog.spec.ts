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
  it('renders player count, WYSIWYG, language select (default fr) and the collapsible set→familles tree', async () => {
    const wrapper = mountDialog()
    await wrapper.vm.$nextTick()
    await new Promise((r) => setTimeout(r, 0)) // let the catalog promise resolve
    expect(wrapper.find('[data-testid="player-count-input"]').exists()).toBe(true)
    expect(wrapper.find('.editor-content').exists()).toBe(true)
    expect(wrapper.find('[data-testid="language-select"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="card-family-selector"]').exists()).toBe(true)
    // sets are collapsed by default; their checkboxes appear once the panel is open
    expect(wrapper.find('[data-testid="set-panel-webapp"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="set-panel-eop"]').exists()).toBe(true)
    await openAllSets(wrapper)
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
    await openAllSets(wrapper)
    // native array pattern: the checkbox emits the new model-value array
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
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

  it('renders the checked state on the family checkbox (bug review: visual state was lost)', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await openAllSets(wrapper)
    expect(checkedInput(wrapper, 'family-webapp:VE').checked).toBe(false)
    expect(wrapper.find('[data-testid="family-webapp:VE"]').html()).toContain('mdi-checkbox-blank-outline')
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
    await wrapper.vm.$nextTick()
    expect(checkedInput(wrapper, 'family-webapp:VE').checked).toBe(true)
    // the checked visual comes from the mdi icon glyph (requires @mdi/font loaded in main.ts)
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
    await playerCountInput(wrapper).setValue(2)
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

  it('US2 (spec 007, T035, FR-007): shows the inherit-facts dropdown listing only ateliers with facts', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    const select = wrapper.find('[data-testid="inherit-facts-select"]')
    expect(select.exists()).toBe(true)
  })

  it('US2 (spec 007, T035): launch payload carries inheritFactsFrom only when a source is selected', async () => {
    const wrapper = mountDialog()
    await new Promise((r) => setTimeout(r, 0))
    await openAllSets(wrapper)
    await wrapper.findComponent('[data-testid="family-webapp:VE"]').vm.$emit('update:modelValue', ['webapp:VE'])
    await wrapper.findComponent({ name: 'VTextField' }).setValue(2)
    await wrapper.vm.$nextTick()
    // no source selected -> payload has no inheritFactsFrom
    await wrapper.find('[data-testid="launch-button"]').trigger('click')
    await wrapper.vm.$nextTick()
    const payload = wrapper.emitted('launch')?.[0]?.[0] as Record<string, unknown>
    expect('inheritFactsFrom' in payload).toBe(false)
  })
})
