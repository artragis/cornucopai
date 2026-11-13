import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AgentText from './AgentText.vue'

// T003 (US1, spec 010 FR-002/FR-005/FR-007, R-001/R-003): AgentText renders an agent-produced
// markdown text as sanitized, styled HTML inside .agent-text.
describe('AgentText', () => {
  it('renders markdown styles inside .agent-text', () => {
    const wrapper = mount(AgentText, { props: { text: '**gras**\n# Titre' } })
    const el = wrapper.find('.agent-text')
    expect(el.exists()).toBe(true)
    expect(el.html()).toContain('<strong>gras</strong>')
    expect(el.html()).toContain('<h1>Titre</h1>')
  })

  it('renders a ```json block as a language-json code block, without <em> on underscores', () => {
    const wrapper = mount(AgentText, {
      props: { text: 'analyse :\n```json\n{"card_id": "VE2"}\n```' }
    })
    const code = wrapper.find('.agent-text pre code.language-json')
    expect(code.exists()).toBe(true)
    expect(code.text()).toContain('card_id')
    expect(wrapper.html()).not.toContain('<em>')
    expect(wrapper.text()).toContain('analyse :')
  })

  it('keeps line breaks of a plain multi-line text', () => {
    const wrapper = mount(AgentText, { props: { text: 'ligne1\nligne2' } })
    expect(wrapper.find('.agent-text').html()).toContain('ligne1<br>')
    expect(wrapper.find('.agent-text').text()).toContain('ligne2')
  })

  it('sanitizes injected HTML (no script survives)', () => {
    const wrapper = mount(AgentText, { props: { text: '<script>alert(1)</script>texte' } })
    expect(wrapper.html()).not.toContain('<script>')
    expect(wrapper.text()).toContain('texte')
  })

  it('renders plain text as a readable paragraph', () => {
    const wrapper = mount(AgentText, { props: { text: 'une simple explication' } })
    expect(wrapper.find('.agent-text').text()).toContain('une simple explication')
  })

  it('renders nothing for an empty text', () => {
    const wrapper = mount(AgentText, { props: { text: '' } })
    expect(wrapper.find('.agent-text').text()).toBe('')
  })
})
