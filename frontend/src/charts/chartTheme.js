// Chart.js for the whole app: fonts, grid, tooltip and colours come from the CSS
// tokens (src/css/tokens.scss), so charts follow the light / dark theme. A chart
// re-reads them when the theme changes: bind :key="themeKey" from useChartTheme().
import { ref, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { Chart } from 'chart.js'

/** The value of a CSS custom property on <body> (where the theme lives). */
export function token(name) {
  if (typeof document === 'undefined') return ''
  return getComputedStyle(document.body).getPropertyValue(name).trim()
}

/** Status / grade / heat colours by meaning. */
export function palette() {
  return {
    present: token('--st-present'),
    late: token('--st-late'),
    absent: token('--st-absent'),
    excused: token('--st-excused'),
    unmarked: token('--st-unmarked'),
    grade5: token('--gr-5'),
    grade4: token('--gr-4'),
    grade3: token('--gr-3'),
    grade2: token('--gr-2'),
    primary: token('--chart-primary'),
    primaryFill: token('--chart-primary-fill'),
    secondary: token('--chart-secondary'),
    text: token('--chart-text'),
    grid: token('--chart-grid'),
    target: token('--chart-target'),
    card: token('--card-bg'),
    textPrimary: token('--text-primary'),
    border: token('--brand-border')
  }
}

const reducedMotion = () =>
  typeof window !== 'undefined' &&
  window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

/** Global Chart.js defaults from the current theme. */
export function applyChartTheme() {
  const p = palette()
  const d = Chart.defaults
  d.font.family = "'Inter', -apple-system, 'Segoe UI', sans-serif"
  d.font.size = 11
  d.color = p.text
  d.borderColor = p.grid
  d.responsive = true
  d.maintainAspectRatio = false
  d.animation = reducedMotion()
    ? false
    : { duration: 400, easing: 'easeOutQuad' }
  // plugin defaults exist only once the plugin is registered (Legend is not used at all)
  if (d.plugins.legend) d.plugins.legend.display = false
  const tip = d.plugins.tooltip
  if (tip) {
    tip.backgroundColor = p.card
    tip.titleColor = p.textPrimary
    tip.bodyColor = p.textPrimary
    tip.borderColor = p.border
    tip.borderWidth = 1
    tip.padding = 10
    tip.cornerRadius = 8 // = --radius-sm
    tip.displayColors = true
    tip.boxPadding = 4
    tip.titleFont = { weight: '600', size: 12 }
    tip.bodyFont = { size: 12 }
  }
  // scales: faint horizontal grid only, no axis lines
  for (const type of ['linear', 'category', 'time']) {
    const s = d.scales?.[type]
    if (!s) continue
    s.grid = { ...s.grid, color: p.grid, drawTicks: false }
    s.border = { ...s.border, display: false }
    s.ticks = { ...s.ticks, color: p.text, padding: 6 }
  }
}

/**
 * In a chart component: themeKey changes when light/dark flips; use it as :key so the
 * chart is rebuilt with the new colours. colors is reactive too.
 */
export function useChartTheme() {
  const $q = useQuasar()
  const themeKey = ref(0)
  const colors = ref(palette())
  const refresh = () => {
    applyChartTheme()
    colors.value = palette()
    themeKey.value++
  }
  onMounted(refresh)
  // body--dark is set in the same tick as $q.dark: read the tokens after the DOM updates
  watch(
    () => $q.dark.isActive,
    () => requestAnimationFrame(refresh)
  )
  return { themeKey, colors }
}
