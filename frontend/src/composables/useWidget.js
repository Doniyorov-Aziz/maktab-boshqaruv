// Loads one home-page widget: data, "as of" time, loading / error states, a retry,
// and a refresh every 60 s while the page is visible (not in a background tab).
import { ref, onMounted, onBeforeUnmount, watch, isRef } from 'vue'
import { api } from '@/boot/axios'
import { useSchoolStore } from '@/stores/school'

const REFRESH_MS = 60_000

// params may be a ref / computed (a widget's tab): a change reloads the widget.
// path: 'health' → /api/dashboard/health; a path starting with '/' is used as it is.
export function useWidget(path, { params = {}, enabled = true } = {}) {
  const school = useSchoolStore()
  const data = ref(null)
  const asOf = ref(null)
  const loading = ref(false)
  const error = ref(null)
  let timer = null

  async function load({ quiet = false } = {}) {
    const on = isRef(enabled) ? enabled.value : enabled
    if (!school.activeSchoolId || !on) return
    if (!quiet) loading.value = true
    try {
      const url = path.startsWith('/') ? path : `/api/dashboard/${path}`
      const res = await api.get(url, {
        params: {
          schoolId: school.activeSchoolId,
          ...(isRef(params) ? params.value : params)
        },
        background: quiet
      })
      // widgets answer { asOf, data }; plain answers are used as they are
      const body = res.data
      if (body && Object.hasOwn(body, 'asOf') && Object.hasOwn(body, 'data')) {
        data.value = body.data
        asOf.value = body.asOf
      } else {
        data.value = body
        asOf.value = new Date().toISOString()
      }
      error.value = null
    } catch (e) {
      // a quiet refresh keeps the last good data on screen
      if (!quiet || !data.value)
        error.value = e.friendlyMessage || 'Yuklab bo’lmadi'
    } finally {
      loading.value = false
    }
  }

  function tick() {
    if (document.visibilityState === 'visible') load({ quiet: true })
  }

  function onVisible() {
    if (document.visibilityState === 'visible') load({ quiet: true })
  }

  onMounted(() => {
    load()
    timer = setInterval(tick, REFRESH_MS)
    document.addEventListener('visibilitychange', onVisible)
  })
  onBeforeUnmount(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', onVisible)
  })
  watch(
    () => school.activeSchoolId,
    () => load()
  )
  if (isRef(enabled)) watch(enabled, v => v && load())
  if (isRef(params)) watch(params, () => load(), { deep: true })

  return { data, asOf, loading, error, reload: () => load() }
}
