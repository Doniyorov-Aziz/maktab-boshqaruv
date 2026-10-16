// UI audit: every page at four screen sizes (light) and 1920 (dark) — screenshots
// plus the measured size/radius/border of fields, buttons, cards, chips and the
// width the content uses.
//   node tests/ui/audit.mjs before      → docs/ui-system/before/, docs/ui-system/audit.json
//   node tests/ui/audit.mjs after       → docs/ui-system/after/,  docs/ui-system/audit-after.json
// Needs the built app on UI_BASE and its API on UI_API (see session.js).
import { chromium } from 'playwright'
import { mkdirSync, writeFileSync } from 'node:fs'
import { pages, viewports } from './pages.js'
import { openContext, visit } from './session.js'
import { measurePage } from './measure.js'

const phase = process.argv[2] || 'before'
const only = process.argv[3] ? process.argv[3].split(',') : null
const docs = new URL('../../../docs/ui-system/', import.meta.url)
const shots = new URL(phase + '/', docs)
mkdirSync(shots, { recursive: true })

const browser = await chromium.launch()
const result = { phase, takenAt: new Date().toISOString(), pages: {} }

for (const p of pages) {
  if (only && !only.includes(p.name)) continue
  const entry = { path: p.path, layout: {}, errors: [] }
  const runs = [
    ...viewports.map(v => ({ v, dark: false })),
    { v: viewports[1], dark: true }
  ]
  for (const { v, dark } of runs) {
    const context = await openContext(browser, v, { dark, signedIn: !p.public })
    const page = await context.newPage()
    page.on('pageerror', e =>
      entry.errors.push(String(e.message).slice(0, 200))
    )
    await visit(page, p.path)
    const tag = `${v.width}x${v.height}${dark ? '-dark' : ''}`
    await page.screenshot({
      path: new URL(`${p.name}-${tag}.jpg`, shots).pathname.replace(
        /^\/([A-Z]:)/,
        '$1'
      ),
      fullPage: true,
      type: 'jpeg',
      quality: 70
    })
    const m = await page.evaluate(measurePage)
    entry.layout[tag] = m.layout
    if (v.width === 1366 && !dark)
      Object.assign(entry, {
        fields: m.fields,
        buttons: m.buttons,
        cards: m.cards,
        chips: m.chips,
        tiles: m.tiles
      })
    await context.close()
  }
  result.pages[p.name] = entry
  console.log(
    p.name,
    'ok',
    entry.errors.length ? entry.errors.length + ' errors' : ''
  )
}
await browser.close()

const file = phase === 'before' ? 'audit.json' : `audit-${phase}.json`
writeFileSync(new URL(file, docs), JSON.stringify(result, null, 1))
console.log('written', file)
