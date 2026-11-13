import { describe, it, expect } from 'vitest'
import { router } from './router'

// T020 (US4, spec 006 FR-013, R-049): the two dedicated routes resolve.
describe('router', () => {
  it('has a home route at /', async () => {
    await router.push('/')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('has a detail route at /ateliers/:id', async () => {
    await router.push('/ateliers/atelier20261110-111343')
    expect(router.currentRoute.value.path).toBe('/ateliers/atelier20261110-111343')
    expect(router.currentRoute.value.params.id).toBe('atelier20261110-111343')
  })

  // T012 (spec 009, US1): the agents management page lives at /agents
  it('has an agents route at /agents', async () => {
    await router.push('/agents')
    expect(router.currentRoute.value.path).toBe('/agents')
  })
})
