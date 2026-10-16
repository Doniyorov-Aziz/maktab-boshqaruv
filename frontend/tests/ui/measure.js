// Runs inside the page (page.evaluate): the computed size, radius and border of
// every form field, button, card, chip and icon tile, plus how much of the screen
// the page content uses. Only visible elements count.
export function measurePage() {
  const visible = el => {
    const r = el.getBoundingClientRect()
    if (r.width === 0 || r.height === 0) return false
    const s = getComputedStyle(el)
    return s.visibility !== 'hidden' && s.display !== 'none'
  }
  const px = v => Math.round(parseFloat(v) * 10) / 10
  const describe = el => {
    const id = el.getAttribute('data-ui') || el.getAttribute('aria-label') || ''
    const text = (el.innerText || '').trim().split('\n')[0].slice(0, 40)
    return (
      (el.className && typeof el.className === 'string'
        ? el.className.split(' ').slice(0, 3).join('.')
        : el.tagName) + (id ? ' [' + id + ']' : text ? ' "' + text + '"' : '')
    )
  }
  const out = { fields: [], buttons: [], cards: [], chips: [], tiles: [] }

  for (const el of document.querySelectorAll('.q-field')) {
    const control = el.querySelector('.q-field__control')
    if (!control || !visible(control)) continue
    const before = getComputedStyle(control, '::before')
    out.fields.push({
      el: describe(el),
      kind: el.classList.contains('q-select')
        ? 'select'
        : el.querySelector('textarea')
          ? 'textarea'
          : 'input',
      outlined: el.classList.contains('q-field--outlined'),
      dense: el.classList.contains('q-field--dense'),
      readonly: el.classList.contains('q-field--readonly'),
      height: px(control.getBoundingClientRect().height),
      radius: px(getComputedStyle(control).borderTopLeftRadius),
      borderStyle: el.classList.contains('q-field--outlined')
        ? before.borderTopStyle
        : getComputedStyle(control).borderBottomStyle,
      borderWidth: el.classList.contains('q-field--outlined')
        ? px(before.borderTopWidth)
        : px(getComputedStyle(control).borderBottomWidth)
    })
  }
  for (const el of document.querySelectorAll('.q-btn')) {
    if (!visible(el)) continue
    const s = getComputedStyle(el)
    const round = el.classList.contains('q-btn--round')
    const icon =
      !el.querySelector('.q-btn__content .block') &&
      !!el.querySelector('.q-icon')
    out.buttons.push({
      el: describe(el),
      round,
      iconOnly: icon,
      flat: el.classList.contains('q-btn--flat'),
      height: px(el.getBoundingClientRect().height),
      radius: px(s.borderTopLeftRadius),
      inTable: !!el.closest('.q-table, table'),
      size: el.getAttribute('data-size') || ''
    })
  }
  for (const el of document.querySelectorAll('.q-card, .brand-card')) {
    if (!visible(el)) continue
    const s = getComputedStyle(el)
    out.cards.push({
      el: describe(el),
      radius: px(s.borderTopLeftRadius),
      borderStyle: s.borderTopStyle,
      borderWidth: px(s.borderTopWidth)
    })
  }
  for (const el of document.querySelectorAll('.q-chip')) {
    if (!visible(el)) continue
    const s = getComputedStyle(el)
    out.chips.push({
      el: describe(el),
      height: px(el.getBoundingClientRect().height),
      radius: px(s.borderTopLeftRadius)
    })
  }
  for (const el of document.querySelectorAll('.icon-tile, .q-avatar')) {
    if (!visible(el)) continue
    const r = el.getBoundingClientRect()
    out.tiles.push({
      el: describe(el),
      tile: el.classList.contains('icon-tile'),
      size: px(r.width),
      radius: px(getComputedStyle(el).borderTopLeftRadius)
    })
  }

  // how much of the width next to the sidebar the page content uses
  const vw = document.documentElement.clientWidth
  const container = document.querySelector('.q-page-container')
  const pageEl = document.querySelector('.q-page')
  const drawer = document.querySelector('.q-drawer:not(.q-drawer--mobile)')
  const drawerW =
    drawer && visible(drawer) ? drawer.getBoundingClientRect().width : 0
  const content = pageEl ? pageEl.getBoundingClientRect() : null
  const firstChild = pageEl ? [...pageEl.children].find(visible) : null
  out.layout = {
    viewport: vw,
    sidebar: Math.round(drawerW),
    container: container
      ? Math.round(container.getBoundingClientRect().width)
      : null,
    page: content ? Math.round(content.width) : null,
    pagePaddingLeft: pageEl ? px(getComputedStyle(pageEl).paddingLeft) : null,
    contentWidth: firstChild
      ? Math.round(firstChild.getBoundingClientRect().width)
      : null,
    horizontalScroll: document.documentElement.scrollWidth > vw + 1
  }
  if (container) {
    // the q-page-container pads itself by the sidebar width: what is left is the room for the page
    const cs = getComputedStyle(container)
    out.layout.available = Math.round(
      container.clientWidth -
        parseFloat(cs.paddingLeft) -
        parseFloat(cs.paddingRight)
    )
  }
  if (out.layout.available && out.layout.contentWidth) {
    out.layout.unusedPx = out.layout.available - out.layout.contentWidth
  }
  return out
}
