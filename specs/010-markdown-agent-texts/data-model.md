# Data Model — Rendu markdown des textes des agents (spec 010)

No persistence change: the feature is purely presentational on the frontend plus prompt text on
the backend. Entities below are in-memory, frontend-side.

## TexteMarkdownAgent (input)

An agent-produced free-text field displayed on the atelier detail page: `leg.scenario`,
`leg.explanation`, `entry.text`, `entry.answer`. Content is **markdown** (correction
2026-11-13); embedded structured data is expected inside ```json code fences.

| Field | Type | Description |
|-------|------|-------------|
| `text` | `string` | Raw markdown source of the agent text. |

## HtmlSanitise (rendering pipeline output)

| Field | Type | Description |
|-------|------|-------------|
| `html` | `string` | `marked.parse(text)` output, then DOMPurify-sanitized; the only string ever injected into the DOM (via `v-html` on the `AgentText` component). |

Rules:
- `text` empty → `html` is `''` (nothing rendered).
- Sanitization profile: standard HTML allowed; `<script>`, event-handler attributes and
  `javascript:` URLs removed (FR-005).
- `breaks: true` (FR-003): single newlines render as `<br>`.

## BlocDeCode (rendered element)

A fenced code block of the markdown source renders as `<pre><code class="language-{lang}">…`.
A ```json fence therefore carries `language-json` and gets a distinct CSS style (FR-002). No
marker inside a code block is ever interpreted as emphasis (FR-004).
