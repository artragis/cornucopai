import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import 'vuetify/styles'

// TipTap needs real ProseMirror DOM APIs; stub it in jsdom
vi.mock('@tiptap/vue-3', () => ({
  // useEditor returns a Ref<Editor | undefined>
  useEditor: () => ({
    value: {
      getHTML: () => '<p>Feature de test</p>'
    }
  }),
  EditorContent: { name: 'EditorContent', template: '<div class="editor-content" />' }
}))
vi.mock('@tiptap/starter-kit', () => ({ default: { configure: () => ({}) } }))

import ConfigureGameDialog from './components/ConfigureGameDialog.vue'

// jsdom lacks visualViewport, required by Vuetify overlay components (v-dialog)
if (!('visualViewport' in globalThis)) {
  Object.defineProperty(globalThis, 'visualViewport', {
    value: {
      width: 1024,
      height: 768,
      offsetLeft: 0,
      offsetTop: 0,
      pageLeft: 0,
      pageTop: 0,
      scale: 1,
      addEventListener: () => {},
      removeEventListener: () => {}
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
        // render the dialog content inline instead of teleporting to body
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
  it('renders player count field and WYSIWYG editor, and no card-family selector', () => {
    const wrapper = mountDialog()
    expect(wrapper.find('[data-testid="player-count-input"]').exists()).toBe(true)
    expect(wrapper.find('.editor-content').exists()).toBe(true)
    expect(wrapper.find('[data-testid="card-family-selector"]').exists()).toBe(false)
  })

  it('blocks launch when playerCount < 2', async () => {
    const wrapper = mountDialog()
    await playerCountInput(wrapper).setValue(1)
    const button = wrapper.find('[data-testid="launch-button"]')
    expect(button.attributes('disabled')).toBeDefined()
    await button.trigger('click')
    expect(wrapper.emitted('launch')).toBeUndefined()
  })

  it('emits launch with playerCount and featureHtml when valid', async () => {
    const wrapper = mountDialog()
    await playerCountInput(wrapper).setValue(4)
    const button = wrapper.find('[data-testid="launch-button"]')
    expect(button.attributes('disabled')).toBeUndefined()
    await button.trigger('click')
    const events = wrapper.emitted('launch')
    expect(events).toBeTruthy()
    expect(events![0][0]).toEqual({ playerCount: 4, featureHtml: '<p>Feature de test</p>' })
  })
})
