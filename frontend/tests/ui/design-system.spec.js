// Design-system checks on every page (npm test). A failure names the page and the
// element, e.g. "crud-students: AppButton "Excel" height 36 ≠ 40".
// Needs the built app on UI_BASE and its API on UI_API (see session.js / playwright.config.js).
import { test, expect } from '@playwright/test'
import { pages } from './pages.js'
import { openContext, visit } from './session.js'

const DESKTOP = { width: 1440, height: 900 }

// Runs in the page: every rule broken on it, as readable lines.
function checkControls() {
  const bad = []
  const visible = el => {
    const r = el.getBoundingClientRect()
    const s = getComputedStyle(el)
    return (
      r.width > 0 &&
      r.height > 0 &&
      s.visibility !== 'hidden' &&
      s.display !== 'none'
    )
  }
  const px = v => Math.round(parseFloat(v) * 10) / 10
  const name = el => {
    const t = (el.getAttribute('aria-label') || el.innerText || '')
      .trim()
      .split('\n')[0]
      .slice(0, 30)
    return t
      ? `"${t}"`
      : el.className.toString().split(' ').slice(0, 3).join('.')
  }
  const root = getComputedStyle(document.documentElement)
  const token = n => px(root.getPropertyValue(n))
  const H = token('--control-h')
  const HS = token('--control-h-sm')
  const RS = token('--radius-sm')
  const RM = token('--radius-md')
  const RL = token('--radius-lg')

  for (const field of document.querySelectorAll('.q-field')) {
    // Quasar's own pagination select inside tables is not ours
    if (field.closest('.q-table__bottom')) continue
    const control = field.querySelector('.q-field__control')
    if (!control || !visible(control)) continue
    const what = `field ${name(field)}`
    if (!field.classList.contains('app-field'))
      bad.push(`${what} is not an App* field`)
    const before = getComputedStyle(control, '::before')
    if (before.borderTopStyle === 'dashed')
      bad.push(`${what} has a dashed border`)
    const radius = px(getComputedStyle(control).borderTopLeftRadius)
    if (radius !== RM) bad.push(`${what} radius ${radius} ≠ ${RM}`)
    const multi =
      field.classList.contains('app-field--multiline') ||
      field.classList.contains('q-select--with-chips')
    const h = px(control.getBoundingClientRect().height)
    if (!multi && h !== H) bad.push(`${what} height ${h} ≠ ${H}`)
  }

  for (const btn of document.querySelectorAll('.app-btn')) {
    if (!visible(btn)) continue
    const sm = btn.classList.contains('app-btn--sm')
    const h = px(btn.getBoundingClientRect().height)
    const r = px(getComputedStyle(btn).borderTopLeftRadius)
    if (h !== (sm ? HS : H))
      bad.push(`AppButton ${name(btn)} height ${h} ≠ ${sm ? HS : H}`)
    if (r !== (sm ? RS : RM))
      bad.push(`AppButton ${name(btn)} radius ${r} ≠ ${sm ? RS : RM}`)
  }
  // every button we write is an AppButton (Quasar's own inside dates, tables, toggles aside)
  for (const btn of document.querySelectorAll('.q-btn:not(.app-btn)')) {
    if (
      !visible(btn) ||
      btn.closest(
        '.q-date, .q-time, .q-table__bottom, .q-btn-toggle, .q-btn-group, .q-tabs, .q-dialog__inner > .q-card > .q-card__actions .q-btn-group'
      )
    )
      continue
    bad.push(`raw q-btn ${name(btn)}`)
  }

  for (const card of document.querySelectorAll('.q-card, .brand-card')) {
    if (!visible(card)) continue
    const r = px(getComputedStyle(card).borderTopLeftRadius)
    if (r !== RL) bad.push(`card ${name(card)} radius ${r} ≠ ${RL}`)
  }

  const sizes = [token('--tile-sm'), token('--tile-md'), token('--tile-lg')]
  for (const tile of document.querySelectorAll('.icon-tile')) {
    if (!visible(tile)) continue
    const r = tile.getBoundingClientRect()
    const w = px(r.width)
    const radius = px(getComputedStyle(tile).borderTopLeftRadius)
    if (!sizes.includes(w) || px(r.height) !== w)
      bad.push(`IconTile ${w}×${px(r.height)} not one of ${sizes.join('/')}`)
    const want = w === sizes[2] ? RM : RS
    if (radius !== want) bad.push(`IconTile ${w} radius ${radius} ≠ ${want}`)
  }
  return bad
}

for (const p of pages) {
  test(`controls: ${p.name}`, async ({ browser }) => {
    const context = await openContext(browser, DESKTOP, { signedIn: !p.public })
    const page = await context.newPage()
    await visit(page, p.path)
    const bad = await page.evaluate(checkControls)
    await context.close()
    expect(bad, `${p.name}:\n  ${bad.join('\n  ')}`).toEqual([])
  })
}
