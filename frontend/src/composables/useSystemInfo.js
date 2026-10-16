// What the server says about itself (is demo mode on), asked once per page load and
// shared: the yellow banner, the empty states and the settings page all read it.
import { ref } from 'vue'
import { api } from '@/boot/axios'

const info = ref({ demoSimulator: false, demoDelayMinutes: null })
let asked = null

export function useSystemInfo() {
  if (!asked) {
    asked = api
      .get('/api/system/info', { background: true })
      .then(res => (info.value = res.data || info.value))
      .catch(() => {
        asked = null
      })
  }
  return info
}
