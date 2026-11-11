export interface CreateAtelierRequest {
  playerCount: number
  featureHtml: string
  /** Chosen language, lowercase; defaults to 'fr' (FR-015). */
  language?: string
  /** Selected families as "setId:familyId" pairs, e.g. "webapp:VE" (FR-005). */
  families: string[]
  /** spec 007 (FR-007, API v0.7.0): optional source atelier id whose projectFacts are inherited. */
  inheritFactsFrom?: string
}

export interface ParticipantDto {
  name: string
  archetype: 'GRUMPY' | 'HAPPY' | 'GREEDY' | 'PROF'
}

export interface CardDto {
  id: string
  value: string
  setId: string
  suitId: string
  suitName: string
  language: string
  description: string
  url?: string | null
  misc?: string | null
}

export interface SetResolutionDto {
  id: string
  resolvedLanguage: string
}

export interface LegEntryDto {
  kind: 'PRECISION' | 'QUESTION' | string
  author: string
  text: string
  knows?: boolean
  answer?: string
}

export interface LegDto {
  cardName: string
  scenario: string
  explanation: string
  /** @deprecated v1 field, still present in legacy ateliers */
  precisions?: string[]
  validation: 'PENDING' | 'VALIDATED' | 'REJECTED'
  // spec 005 v2: structured entries (questions + precisions with knows/answer)
  entries?: LegEntryDto[]
  // spec 006 v3 (ateliers-api v0.6.0): additive tour/score fields. Numeric fields are
  // always serialized by the backend (primitive int, 0 on legacy reads); string fields
  // are null when absent (legacy ateliers).
  turnIndex?: number
  majorSuit?: string | null
  suitId?: string | null
  cardValue?: string | null
  points?: number
}

export interface TourDto {
  index: number
  leader: string
  majorSuit?: string | null
  legIndices: number[]
  winner?: string | null
  bonusAwarded: boolean
}

export interface AtelierResponse {
  id: string
  createdAt: string
  featureHtml: string
  playerCount: number
  players: ParticipantDto[]
  turnOrder: string[]
  finishedAt: string | null
  language: string
  selectedFamilies: string[]
  deck: CardDto[]
  sets: SetResolutionDto[]
  legs: LegDto[]
  // spec 006 v0.6.0: scoreboard data (empty/absent on legacy ateliers)
  tours?: TourDto[]
  scores?: Record<string, number>
  winner?: string[]
}

export interface AtelierSummaryDto {
  id: string
  createdAt: string
  playerCount: number
  /** spec 007 (API v0.7.0): true when the atelier has persisted project facts (dropdown source). */
  hasProjectFacts?: boolean
}

// ---- Card catalog (GET /api/card-catalog) ----

export interface FamilyDto {
  id: string
  name: string
  cardCount: number
}

export interface SetDto {
  id: string
  version: string
  languages: string[]
  families: FamilyDto[]
  totalCards: number
}

export interface CardCatalog {
  availableLanguages: string[]
  sets: SetDto[]
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

/** GET /api/card-catalog — sets, families and available languages (FR-003, FR-014). */
export async function getCardCatalog(): Promise<CardCatalog> {
  const response = await fetch('/api/card-catalog')
  if (!response.ok) await handleError(response)
  return response.json()
}
