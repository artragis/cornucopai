import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { useAtelierPolling } from './useAtelierPolling'
import * as api from '../services/atelierApi'

// T029 (US5, spec 006 FR-015/FR-016, R-050): 5 s polling, stops when finished, stops on unmount.
describe('useAtelierPolling', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.spyOn(api, 'getAtelier')
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  function mountWithComposable(id: string) {
    let exposed: ReturnType<typeof useAtelierPolling> | null = null
    const Comp = defineComponent({
      setup() {
        exposed = useAtelierPolling(id, 5000)
        return () => h('div')
      }
    })
    const wrapper = mount(Comp)
    return { wrapper, getExposed: () => exposed! }
  }

  it('calls getAtelier every 5 seconds', async () => {
    vi.mocked(api.getAtelier).mockResolvedValue({ id: 'a1', finishedAt: null } as never)
    mountWithComposable('a1')
    expect(api.getAtelier).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(5000)
    expect(api.getAtelier).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(5000)
    expect(api.getAtelier).toHaveBeenCalledTimes(2)
  })

  it('stops polling when finishedAt becomes non-null', async () => {
    vi.mocked(api.getAtelier).mockResolvedValue({ id: 'a1', finishedAt: '2026-11-11T10:00:00Z' } as never)
    mountWithComposable('a1')
    await vi.advanceTimersByTimeAsync(5000)
    expect(api.getAtelier).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(15000)
    expect(api.getAtelier).toHaveBeenCalledTimes(1) // no more calls after finished
  })

  it('stops polling on unmount', async () => {
    vi.mocked(api.getAtelier).mockResolvedValue({ id: 'a1', finishedAt: null } as never)
    const { wrapper } = mountWithComposable('a1')
    wrapper.unmount()
    await vi.advanceTimersByTimeAsync(20000)
    expect(api.getAtelier).not.toHaveBeenCalled()
  })

  it('keeps the last good state when a poll fails', async () => {
    vi.mocked(api.getAtelier)
      .mockResolvedValueOnce({ id: 'a1', finishedAt: null } as never)
      .mockRejectedValueOnce(new Error('network down'))
    const { getExposed } = mountWithComposable('a1')
    await vi.advanceTimersByTimeAsync(5000)
    expect(getExposed().data.value).not.toBeNull()
    await vi.advanceTimersByTimeAsync(5000) // this one rejects — must not throw
    expect(getExposed().data.value).not.toBeNull() // last good state kept
  })
})
