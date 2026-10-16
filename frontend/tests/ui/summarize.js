// Turns an audit JSON into compact per-page summaries: each distinct
// "height / radius / border" value with how often it appears.
const tally = (list, key) => {
  const m = new Map()
  for (const x of list || []) {
    const k = key(x)
    m.set(k, (m.get(k) || 0) + 1)
  }
  return [...m.entries()].sort((a, b) => b[1] - a[1]).map(([k, n]) => (n > 1 ? `${k} ×${n}` : k)).join(', ') || '—'
}

export function summarize(entry) {
  return {
    fields: tally(entry.fields, f => `${f.height}px / r${f.radius} / ${f.borderStyle}${f.dense ? ' (dense)' : ''}`),
    buttons: tally(entry.buttons, b => `${b.height}px / r${b.radius}`),
    cards: tally(entry.cards, c => `r${c.radius} / ${c.borderWidth}px ${c.borderStyle}`),
    chips: tally(entry.chips, c => `${c.height}px / r${c.radius}`),
    tiles: tally(entry.tiles, t => `${t.size}px / r${t.radius}`)
  }
}

export function layoutRow(entry) {
  const cell = tag => {
    const l = entry.layout?.[tag]
    if (!l) return '—'
    const used = l.available ? Math.round((l.contentWidth / l.available) * 100) : null
    return `${l.contentWidth}/${l.available}px${used !== null ? ` (${used}%, bo'sh ${l.unusedPx}px)` : ''}${l.horizontalScroll ? ' ⚠ scroll' : ''}`
  }
  return ['1366x768', '1920x1080', '2560x1440', '390x844'].map(cell)
}
