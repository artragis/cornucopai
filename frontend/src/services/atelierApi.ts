export interface CreateAtelierRequest {
  playerCount: number
  featureHtml: string
}

export interface ParticipantDto {
  name: string
  archetype: 'GRUMPY' | 'HAPPY' | 'GREEDY' | 'PROF'
}

export interface LegDto {
  cardName: string
  scenario: string
  explanation: string
  precisions: string[]
  validation: 'PENDING' | 'VALIDATED' | 'REJECTED'
}

export interface AtelierResponse {
  id: string
  createdAt: string
  featureHtml: string
  playerCount: number
  players: ParticipantDto[]
  turnOrder: string[]
  finishedAt: string | null
  legs: LegDto[]
}

export interface AtelierSummaryDto {
  id: string
  createdAt: string
  playerCount: number
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

/** POST /api/ateliers — configure and launch a game (runs it synchronously). */
export async function createAtelier(request: CreateAtelierRequest): Promise<AtelierResponse> {
  const response = await fetch('/api/ateliers', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request)
  })
  if (!response.ok) await handleError(response)
  return response.json()
}

/** GET /api/ateliers — list previous ateliers, newest first. */
export async function listAteliers(): Promise<AtelierSummaryDto[]> {
  const response = await fetch('/api/ateliers')
  if (!response.ok) await handleError(response)
  return response.json()
}

/** GET /api/ateliers/{id} — full atelier detail. */
export async function getAtelier(id: string): Promise<AtelierResponse> {
  const response = await fetch(`/api/ateliers/${encodeURIComponent(id)}`)
  if (!response.ok) await handleError(response)
  return response.json()
}
