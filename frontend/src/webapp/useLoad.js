import { ref, watch, onMounted } from 'vue'
import { state } from '@/webapp/state'

/**
 * Page data with the three states every Mini App screen shows: skeleton while
 * loading, an error card with "retry", and the data. Pull-to-refresh and the
 * MainButton bump state.refresh — the page reloads quietly (old data stays
 * on screen until the new data arrives).
 */
export function useLoad(fetcher) {
  const data = ref(null)
  const error = ref(false)

  async function load() {
    error.value = false
    try {
      data.value = await fetcher()
    } catch {
      if (!data.value) error.value = true
    }
  }

  async function retry() {
    data.value = null
    await load()
  }

  onMounted(load)
  watch(() => state.refresh, load)

  return { data, error, load, retry }
}
