<script setup lang="ts">
import { computed } from 'vue'
import { renderAgentMarkdown } from '../utils/markdown'

// T006 (US1, spec 010 FR-001/FR-002/FR-005, R-003): renders an agent-produced markdown text.
// The HTML is sanitized in renderAgentMarkdown before being injected (v-html), and styled here
// (markdown elements + distinct style for ```json code blocks).
const props = defineProps<{ text?: string }>()

const html = computed(() => renderAgentMarkdown(props.text ?? ''))
</script>

<template>
  <span
    class="agent-text markdown-body"
    v-html="html"
  />
</template>

<style scoped>
.agent-text {
  white-space: normal;
  overflow-wrap: anywhere;
}
.agent-text :deep(p) {
  margin: 0 0 0.5rem;
}
.agent-text :deep(p:last-child) {
  margin-bottom: 0;
}
.agent-text :deep(h1),
.agent-text :deep(h2),
.agent-text :deep(h3) {
  margin: 0.5rem 0 0.25rem;
  font-weight: 600;
}
.agent-text :deep(ul),
.agent-text :deep(ol) {
  margin: 0 0 0.5rem;
  padding-left: 1.25rem;
}
.agent-text :deep(a) {
  color: rgb(var(--v-theme-primary));
}
.agent-text :deep(pre) {
  margin: 0.25rem 0 0.5rem;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  background-color: rgb(var(--v-theme-surface-variant));
  overflow-x: auto;
}
.agent-text :deep(code) {
  font-family: ui-monospace, monospace;
  font-size: 0.8125rem;
}
.agent-text :deep(pre code) {
  background: none;
  padding: 0;
}
/* ```json blocks get a distinct accent so embedded JSON stands out */
.agent-text :deep(pre code.language-json) {
  color: rgb(var(--v-theme-primary));
}
</style>
