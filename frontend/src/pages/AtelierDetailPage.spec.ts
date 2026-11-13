import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { router } from '../router'
import AtelierDetailPage from './AtelierDetailPage.vue'
import * as api from '../services/atelierApi'
import type { AtelierResponse } from '../services/atelierApi'

const vuetify = createVuetify({ components, directives })

// T021 (US4, spec 006 FR-014/FR-017): the detail page renders the scoreboard above the legs.
function fakeAtelier(overrides: Partial<AtelierResponse> = {}): AtelierResponse {
  return {
    id: 'atelierX',
    createdAt: '2026-11-11T10:00:00Z',
    featureHtml: '<p>feature</p>',
    playerCount: 2,
    players: [
      { name: 'Joueur-1', archetype: 'GRUMPY' },
      { name: 'Joueur-2', archetype: 'HAPPY' }
    ],
    turnOrder: ['Joueur-1', 'Joueur-2'],
    finishedAt: '2026-11-11T11:00:00Z',
    language: 'fr',
    selectedFamilies: ['webapp:VE'],
    deck: [],
    sets: [],
    legs: [
      {
        cardName: 'VE2', scenario: 'scénario 1', explanation: 'expl 1',
        validation: 'VALIDATED',
        entries: [
          { kind: 'PRECISION', author: 'Joueur-2', text: 'une précision' },
          { kind: 'QUESTION', author: 'Joueur-2', text: 'une question répondue ?', knows: true, answer: 'oui' },
          { kind: 'QUESTION', author: 'Joueur-1', text: 'une question sans réponse ?', knows: false }
        ],
        turnIndex: 1, majorSuit: 'VE', suitId: 'VE', cardValue: '2', points: 1
      },
      {
        cardName: 'AT1', scenario: 'scénario 2', explanation: 'expl 2',
        validation: 'REJECTED',
        entries: [],
        turnIndex: 1, majorSuit: 'VE', suitId: 'AT', cardValue: '3', points: 0
      }
    ],
    tours: [{ index: 1, leader: 'Joueur-1', majorSuit: 'VE', legIndices: [1, 2], winner: 'Joueur-2', bonusAwarded: true }],
    scores: { 'Joueur-1': 1, 'Joueur-2': 1 },
    winner: ['Joueur-1', 'Joueur-2'],
    ...overrides
  } as AtelierResponse
}

describe('AtelierDetailPage', () => {
  beforeEach(async () => {
    vi.spyOn(api, 'getAtelier')
    await router.push('/ateliers/atelierX')
    await router.isReady()
  })

  it('renders the scoreboard with scores and winner, and the legs list', async () => {
    vi.mocked(api.getAtelier).mockResolvedValue(fakeAtelier())
    const wrapper = mount(AtelierDetailPage, { global: { plugins: [router, vuetify] } })
    await flushPromises()
    const text = wrapper.text()
    expect(wrapper.html()).toContain('atelierX')
    expect(text).toContain('Joueur-1')
    expect(text).toContain('Joueur-2')
    expect(text).toContain('vainqueur')
    expect(text).toContain('VE2')
    expect(text).toContain('AT1')
    expect(wrapper.html()).toContain('VALIDATED')
    expect(wrapper.html()).toContain('REJECTED')
    // expand the first leg's panel to reveal couleur majeure + entrées (Q/R)
    const titles = wrapper.findAll('.v-expansion-panel-title')
    expect(titles.length).toBe(2)
    await titles[0].trigger('click')
    await flushPromises()
    const html = wrapper.html()
    expect(html).toContain('Couleur majeure du tour')
    // les 3 sortes d'entrées sont visibles et distinguées
    expect(html).toContain('Précision')
    expect(html).toContain('une précision')
    expect(html).toContain('Question')
    expect(html).toContain('une question répondue ?')
    expect(html).toContain('répondu')
    expect(html).toContain('Réponse du joueur')
    expect(html).toContain('oui')
    expect(html).toContain('une question sans réponse ?')
    expect(html).toContain('je sais pas')
  })

  it('shows "données partielles" for a legacy atelier without tours', async () => {
    vi.mocked(api.getAtelier).mockResolvedValue(fakeAtelier({ tours: [], scores: {}, winner: [] }))
    const wrapper = mount(AtelierDetailPage, { global: { plugins: [router, vuetify] } })
    await flushPromises()
    expect(wrapper.text()).toContain('Données partielles')
  })

  it('shows "en cours" when finishedAt is null', async () => {
    vi.mocked(api.getAtelier).mockResolvedValue(fakeAtelier({ finishedAt: null, winner: [] }))
    const wrapper = mount(AtelierDetailPage, { global: { plugins: [router, vuetify] } })
    await flushPromises()
    expect(wrapper.text()).toContain('en cours')
  })

  // T004 (US1, spec 010 FR-001/FR-002/FR-004): agent texts are markdown — rendered with styles,
  // and an embedded ```json block becomes a styled language-json code block (no <em> on `_`).
  it('renders markdown agent texts with a styled language-json code block', async () => {
    const atelier = fakeAtelier()
    atelier.legs[0].explanation =
      'Analyse :\n```json\n{"card_id": "VE2", "threat_actor": "admin"}\n```\nfin de l\'explication'
    atelier.legs[0].entries![0].text = '**Précision** importante'
    vi.mocked(api.getAtelier).mockResolvedValue(atelier)
    const wrapper = mount(AtelierDetailPage, { global: { plugins: [router, vuetify] } })
    await flushPromises()
    const titles = wrapper.findAll('.v-expansion-panel-title')
    await titles[0].trigger('click')
    await flushPromises()
    const html = wrapper.html()
    const code = wrapper.find('pre code.language-json')
    expect(code.exists()).toBe(true)
    expect(code.text()).toContain('card_id')
    expect(html).toContain('<strong>Précision</strong>')
    expect(html).not.toContain('<em>')
    // surrounding prose is preserved
    expect(wrapper.text()).toContain('Analyse :')
    expect(wrapper.text()).toContain('fin de l\'explication')
  })

  // T004 (US1, spec 010 FR-007/FR-010): plain prose texts (legacy ateliers) render as simple
  // readable paragraphs — no code block, no markdown artifacts.
  it('renders plain prose leg texts as simple paragraphs (legacy behavior)', async () => {
    vi.mocked(api.getAtelier).mockResolvedValue(fakeAtelier())
    const wrapper = mount(AtelierDetailPage, { global: { plugins: [router, vuetify] } })
    await flushPromises()
    const titles = wrapper.findAll('.v-expansion-panel-title')
    await titles[0].trigger('click')
    await flushPromises()
    expect(wrapper.find('pre code.language-json').exists()).toBe(false)
    const agentTexts = wrapper.findAll('.agent-text')
    expect(agentTexts.length).toBeGreaterThan(0)
    expect(wrapper.text()).toContain('expl 1')
    expect(wrapper.text()).toContain('une précision')
  })
})
