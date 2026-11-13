import { marked } from 'marked'
import DOMPurify from 'dompurify'

// T005 (US1, spec 010 FR-002/FR-003/FR-005, R-001/R-002): markdown pipeline for agent texts —
// marked (gfm + breaks so single line breaks are kept, legacy-friendly) then DOMPurify, because
// agent texts come from LLMs and must never inject scripts/event handlers into the page.

marked.setOptions({ gfm: true, breaks: true })

/** Parse a markdown text and return sanitized HTML, safe to inject via v-html. */
export function renderAgentMarkdown(text: string): string {
  if (!text) return ''
  const html = marked.parse(text, { async: false })
  return DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
}
