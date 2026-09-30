import { reactive, computed } from 'vue'
import { parentApi } from '@/webapp/api'
import { translate } from '@/webapp/i18n'

// Shared Mini App state: who the parent is, their children and the chosen child.
export const state = reactive({
  ready: false,
  error: null,
  lang: 'uz',
  children: [],
  childId: null
})

export const child = computed(
  () => state.children.find(c => c.studentId === state.childId) || null
)

export function t(key) {
  return translate(state.lang, key)
}

export async function loadMe() {
  state.error = null
  try {
    const me = (await parentApi.get('/api/parent/me')).data
    state.lang = me.language || 'uz'
    state.children = me.children
    let remembered = null
    try {
      remembered = Number(localStorage.getItem('webappChildId')) || null
    } catch {
      remembered = null
    }
    state.childId = me.children.some(c => c.studentId === remembered)
      ? remembered
      : me.selectedStudentId
  } catch (e) {
    state.error = e.response?.status === 401 ? 'not_telegram' : 'error'
  } finally {
    state.ready = true
  }
}

export function selectChild(id) {
  state.childId = id
  try {
    localStorage.setItem('webappChildId', String(id))
  } catch {
    // per-device convenience only
  }
}
