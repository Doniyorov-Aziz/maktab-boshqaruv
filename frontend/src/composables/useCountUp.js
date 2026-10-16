// A number that counts up to its value (600 ms, ease-out) when it first appears or
// changes. With prefers-reduced-motion the value is shown at once.
import { ref, watch, onBeforeUnmount } from 'vue'

export const reducedMotion = () =>
  typeof window !== 'undefined' &&
  !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

const DURATION = 600

export function useCountUp(source) {
  const shown = ref(null)
  let frame = null

  function run(target) {
    cancelAnimationFrame(frame)
    if (target === null || target === undefined || !Number.isFinite(target)) {
      shown.value = target ?? null
      return
    }
    if (reducedMotion()) {
      shown.value = target
      return
    }
    const from = Number.isFinite(shown.value) ? shown.value : 0
    const start = performance.now()
    const step = now => {
      const t = Math.min(1, (now - start) / DURATION)
      const eased = 1 - Math.pow(1 - t, 3)
      shown.value = from + (target - from) * eased
      if (t < 1) frame = requestAnimationFrame(step)
    }
    frame = requestAnimationFrame(step)
  }

  watch(source, v => run(v), { immediate: true })
  onBeforeUnmount(() => cancelAnimationFrame(frame))
  return shown
}
