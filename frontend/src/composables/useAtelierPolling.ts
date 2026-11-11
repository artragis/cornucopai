import { onUnmounted, ref, type Ref } from 'vue'
import { getAtelier, type AtelierResponse } from '../services/atelierApi'

// T030 (US5, spec 006 FR-015/FR-016, R-050): poll GET /api/ateliers/{id} every 5 s
// while the atelier is unfinished; stop when finishedAt is set or on unmount.
export function useAtelierPolling(atelierId: string, intervalMs = 5000): { data: Ref<AtelierResponse | null> } {
  const data: Ref<AtelierResponse | null> = ref(null)
  let timer: ReturnType<typeof setInterval> | null = null

  function tick() {
    getAtelier(atelierId)
      .then((a) => {
        data.value = a
        if (a.finishedAt != null && timer != null) {
          clearInterval(timer)
          timer = null
        }
      })
      .catch(() => {
        // polling error: keep the last good state, retry next cycle (edge case FR-015)
      })
  }

  timer = setInterval(tick, intervalMs)
  onUnmounted(() => {
    if (timer != null) {
      clearInterval(timer)
      timer = null
    }
  })

  return { data }
}
