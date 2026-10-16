// Layout checks (npm test): on wide screens the content uses the whole width next
// to the sidebar; on a phone nothing scrolls sideways (tables scroll inside).
import { test, expect } from '@playwright/test'
import { pages } from './pages.js'
import { openContext, visit } from './session.js'

const framed = pages.filter(p => !p.public && !p.bare)

function measure() {
  const vw = document.documentElement.clientWidth
  const page = document.querySelector('.page-layout')
  const body = document.querySelector('.page-layout__body')
  // the page container is padded by exactly the room the open sidebar takes
  const container = document.querySelector('.q-page-container')
  const sidebar = container
    ? parseFloat(getComputedStyle(container).paddingLeft)
    : 0
  const pad = page ? parseFloat(getComputedStyle(page).paddingLeft) : 0
  return {
    vw,
    sidebar: Math.round(sidebar),
    pad,
    content: body ? Math.round(body.getBoundingClientRect().width) : 0,
    scrollWidth: document.documentElement.scrollWidth
  }
}

for (const width of [1920, 2560]) {
  for (const p of framed) {
    test(`full width ${width}: ${p.name}`, async ({ browser }) => {
      const context = await openContext(browser, { width, height: 1080 })
      const page = await context.newPage()
      await visit(page, p.path)
      const m = await page.evaluate(measure)
      await context.close()
      const need = m.vw - m.sidebar - 2 * m.pad - 2
      expect(
        m.content,
        `${p.name} @${width}: content ${m.content}px < ${need}px (viewport ${m.vw}, sidebar ${m.sidebar}, padding ${m.pad})`
      ).toBeGreaterThanOrEqual(need)
    })
  }
}

for (const p of pages) {
  test(`no sideways scroll 390: ${p.name}`, async ({ browser }) => {
    const context = await openContext(
      browser,
      { width: 390, height: 844 },
      { signedIn: !p.public }
    )
    const page = await context.newPage()
    await visit(page, p.path)
    const m = await page.evaluate(measure)
    await context.close()
    expect(
      m.scrollWidth,
      `${p.name} @390: page is ${m.scrollWidth}px wide`
    ).toBeLessThanOrEqual(m.vw)
  })
}
