// Spec 009 (US1, contracts/agents-api.yaml v1.0.0): CRUD for configurable agents.
// Only the apiKeyVar NAME crosses the API — never an API key value (FR-006).

export interface AgentDto {
  id: string
  agentName: string
  openAiCompatibleUrl: string
  apiKeyVar: string
  modelName: string
  personalityPrompt: string
}

export interface AgentInput {
  agentName: string
  openAiCompatibleUrl: string
  apiKeyVar?: string
  modelName: string
  personalityPrompt: string
}

async function handleError(response: Response): Promise<never> {
  let message = `HTTP ${response.status}`
  try {
    const body = await response.json()
    if (body?.error) message = body.error
  } catch {
    // keep default message
  }
  throw new Error(message)
}

/** GET /api/agents — all agents, sorted by agentName. */
export async function listAgents(): Promise<AgentDto[]> {
  const response = await fetch('/api/agents')
  if (!response.ok) await handleError(response)
  return response.json()
}

/** GET /api/agents/{id} — one agent. */
export async function getAgent(id: string): Promise<AgentDto> {
  const response = await fetch(`/api/agents/${encodeURIComponent(id)}`)
  if (!response.ok) await handleError(response)
  return response.json()
}

/** POST /api/agents — create; the server generates the id (FR-003). */
export async function createAgent(input: AgentInput): Promise<AgentDto> {
  const response = await fetch('/api/agents', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  })
  if (!response.ok) await handleError(response)
  return response.json()
}

/** PUT /api/agents/{id} — update; the id never changes (FR-004). */
export async function updateAgent(id: string, input: AgentInput): Promise<AgentDto> {
  const response = await fetch(`/api/agents/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input)
  })
  if (!response.ok) await handleError(response)
  return response.json()
}

/** DELETE /api/agents/{id} — delete (FR-005). */
export async function deleteAgent(id: string): Promise<void> {
  const response = await fetch(`/api/agents/${encodeURIComponent(id)}`, { method: 'DELETE' })
  if (!response.ok && response.status !== 204) await handleError(response)
}
