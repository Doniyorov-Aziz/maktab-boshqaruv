// One way to write numbers on the home page and in reports.
const intFmt = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 })
const oneFmt = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1
})

/** 94.1% — one decimal; "—" when there is no number. */
export function formatPercent(v) {
  if (v === null || v === undefined || Number.isNaN(v)) return '—'
  return `${oneFmt.format(v)}%`
}

/** 1,234 — whole numbers with thousands separators. */
export function formatNumber(v) {
  if (v === null || v === undefined || Number.isNaN(v)) return '—'
  return intFmt.format(v)
}

/** "+2.1" / "−1.4" (a real minus sign); "0" for no change. */
export function formatDelta(v, { percent = false, digits = 1 } = {}) {
  if (v === null || v === undefined || Number.isNaN(v)) return ''
  const abs = Math.abs(v)
  const n = digits === 0 ? intFmt.format(abs) : oneFmt.format(abs)
  if (v === 0) return percent ? '0%' : '0'
  return `${v > 0 ? '+' : '−'}${n}${percent ? '%' : ''}`
}

/** "09:42" from an ISO date-time. */
export function formatClock(iso) {
  if (!iso) return ''
  const t = String(iso).split('T')[1] || ''
  return t.slice(0, 5)
}

/** "201-xona" for a numbered room, the name itself otherwise ("Katta sport zali"). */
export function roomLabel(room) {
  if (!room) return ''
  return /^\d+[a-zA-Z]?$/.test(String(room).trim()) ? `${room}-xona` : room
}
