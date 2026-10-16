// The period of the home page's grade numbers (mastery, quality, subjects): this quarter,
// month or week. One choice for the whole page, set in the page header.
import { ref } from 'vue'

export const PERIODS = [
  { label: 'Chorak', value: 'quarter' },
  { label: 'Oy', value: 'month' },
  { label: 'Hafta', value: 'week' }
]

const period = ref('quarter')

export function usePeriod() {
  return period
}

export const periodNote = (p, quarter) =>
  p === 'month' ? 'shu oy' : p === 'week' ? 'shu hafta' : `${quarter}-chorak`
