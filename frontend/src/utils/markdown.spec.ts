import { describe, it, expect } from 'vitest'
import { renderAgentMarkdown } from './markdown'

// T002 (US1, spec 010 FR-002/FR-003/FR-004/FR-005, R-001/R-002): the markdown pipeline parses
// agent texts with marked (gfm + breaks) and sanitizes the produced HTML with DOMPurify.
describe('renderAgentMarkdown', () => {
  it('renders markdown inline and block styles', () => {
    const html = renderAgentMarkdown('**gras** et _italique_\n# Titre\n- a\n- b')
    expect(html).toContain('<strong>gras</strong>')
    expect(html).toContain('<em>italique</em>')
    expect(html).toContain('<h1>Titre</h1>')
    expect(html).toContain('<ul>')
    expect(html).toContain('<li>a</li>')
    expect(html).toContain('<li>b</li>')
  })

  it('renders a ```json fence as a language-json code block, without emphasis on underscores', () => {
    const html = renderAgentMarkdown('```json\n{"card_id": "VE2", "risk_level": 8}\n```')
    expect(html).toContain('<pre><code class="language-json">')
    expect(html).toContain('card_id')
    expect(html).toContain('risk_level')
    expect(html).not.toContain('<em>')
  })

  it('does not italicize intraword underscores in bare inline JSON', () => {
    const html = renderAgentMarkdown('résultat : {"card_id": "VE2"}')
    expect(html).toContain('card_id')
    expect(html).not.toContain('<em>')
  })

  it('keeps single line breaks (breaks option)', () => {
    expect(renderAgentMarkdown('ligne1\nligne2')).toContain('ligne1<br>ligne2')
  })

  it('sanitizes dangerous HTML (script tags, event handlers)', () => {
    const html = renderAgentMarkdown('<script>alert(1)</script>texte visible\n<img src="x" onerror="alert(2)">')
    expect(html).not.toContain('<script>')
    expect(html).not.toContain('alert(1)')
    expect(html).not.toContain('onerror')
    expect(html).toContain('texte visible')
  })

  it('removes javascript: link targets', () => {
    const html = renderAgentMarkdown('[clic](javascript:alert(1))')
    expect(html).not.toContain('javascript:')
  })

  it('returns an empty string for empty input', () => {
    expect(renderAgentMarkdown('')).toBe('')
  })

  it('renders plain prose as a single paragraph containing the text', () => {
    const html = renderAgentMarkdown('une simple explication')
    expect(html).toContain('<p>une simple explication</p>')
  })
})
