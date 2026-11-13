import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  listAgents,
  getAgent,
  createAgent,
  updateAgent,
  deleteAgent,
  type AgentDto
} from './agentsApi'

// T010 (spec 009, US1): agentsApi hits /api/agents* with the right methods/bodies.

const agent: AgentDto = {
  id: '11111111-2222-4333-8444-555555555555',
  agentName: 'Analyste',
  openAiCompatibleUrl: 'http://localhost:1235/v1',
  apiKeyVar: '',
  modelName: 'qwen3-coder',
  personalityPrompt: 'Tu es tatillon.'
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' }
  })
}

describe('agentsApi', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('listAgents GETs /api/agents', async () => {
    const fetchMock = vi.fn(async () => jsonResponse([agent]))
    vi.stubGlobal('fetch', fetchMock)
    const result = await listAgents()
    expect(fetchMock).toHaveBeenCalledWith('/api/agents')
    expect(result).toEqual([agent])
  })

  it('getAgent GETs /api/agents/{id} encoded', async () => {
    const fetchMock = vi.fn(async () => jsonResponse(agent))
    vi.stubGlobal('fetch', fetchMock)
    await getAgent(agent.id)
    expect(fetchMock).toHaveBeenCalledWith(`/api/agents/${agent.id}`)
  })

  it('createAgent POSTs the input as JSON', async () => {
    const fetchMock = vi.fn(async () => jsonResponse(agent, 201))
    vi.stubGlobal('fetch', fetchMock)
    const input = {
      agentName: 'Analyste',
      openAiCompatibleUrl: 'http://localhost:1235/v1',
      apiKeyVar: '',
      modelName: 'qwen3-coder',
      personalityPrompt: 'Tu es tatillon.'
    }
    const created = await createAgent(input)
    expect(fetchMock).toHaveBeenCalledWith('/api/agents', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input)
    })
    expect(created.id).toBe(agent.id)
  })

  it('updateAgent PUTs to /api/agents/{id}', async () => {
    const fetchMock = vi.fn(async () => jsonResponse(agent))
    vi.stubGlobal('fetch', fetchMock)
    await updateAgent(agent.id, { agentName: 'A', openAiCompatibleUrl: 'http://x', modelName: 'm', personalityPrompt: 'p' })
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe(`/api/agents/${agent.id}`)
    expect(init.method).toBe('PUT')
  })

  it('deleteAgent DELETEs and tolerates 204', async () => {
    const fetchMock = vi.fn(async () => new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    await deleteAgent(agent.id)
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toBe(`/api/agents/${agent.id}`)
    expect(init.method).toBe('DELETE')
  })

  it('surfaces the server error message on failure', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => jsonResponse({ error: 'agentName already used by another agent: X' }, 409))
    )
    await expect(createAgent({ agentName: 'X', openAiCompatibleUrl: 'http://x', modelName: 'm', personalityPrompt: 'p' }))
      .rejects.toThrow('agentName already used by another agent: X')
  })
})
