// The demo visitor's page views, for the administrator's "Tashriflar" page. Pages are queued as
// the route changes and sent at most every 30 s (the first one at once); while the tab stays
// open and visible a ping every minute keeps the visit's duration honest.
import { onBeforeUnmount, watch } from 'vue'
import { api } from '@/boot/axios'
import { visitorId, demoRef } from '@/utils/visitor'

const MIN_GAP = 30_000
const HEARTBEAT = 60_000

export function useDemoVisits(route) {
  const queue = []
  let last = 0

  function send() {
    const now = Date.now()
    if (now - last < MIN_GAP || document.hidden) return
    if (!queue.length && now - last < HEARTBEAT) return
    const paths = queue.splice(0, queue.length)
    last = now
    api
      .post(
        '/api/demo/visit/ping',
        { visitorId: visitorId(), ref: demoRef(), paths },
        { background: true }
      )
      .catch(() => {
        // not counted this time; the pages are not worth retrying
      })
  }

  watch(
    () => route.path,
    path => {
      if (queue[queue.length - 1] !== path) queue.push(path)
      send()
    },
    { immediate: true }
  )
  const timer = setInterval(send, 10_000)
  document.addEventListener('visibilitychange', send)
  onBeforeUnmount(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', send)
  })
}
