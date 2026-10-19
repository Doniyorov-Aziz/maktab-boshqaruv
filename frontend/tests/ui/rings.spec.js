// The health index rings fill exactly their value. The first two tests need no backend and no
// sign-in: the arc maths (ringArc) for each of the four rings, and the arc as the browser really
// draws it (pixels counted on a canvas) — 0.6 % is a sliver, not a full ring. The last one opens
// the home page and checks every ring against its legend (needs UI_PASS).
import { test, expect } from '@playwright/test'
import { ringArc } from '../../src/components/dashboard/ringArc.js'
import { openContext, visit } from './session.js'

const RINGS = [
  { key: 'attendance', value: 96.6 },
  { key: 'quality', value: 74.5 },
  { key: 'journal', value: 92.8 },
  { key: 'telegram', value: 0.6 }
]

test('ringArc: each of the four rings is its own value of 100', () => {
  for (const { key, value } of RINGS) {
    const arc = ringArc(value)
    const [dash, gap] = arc.dasharray.split(' ').map(Number)
    expect(dash, key).toBeCloseTo(value, 5)
    expect(dash + gap, key).toBeCloseTo(100, 5)
  }
  expect(ringArc(0.6).cap, 'a sliver keeps square ends').toBe('butt')
  expect(ringArc(41.3).cap).toBe('round')
  expect(ringArc(null)).toBeNull()
  expect(ringArc(140).value).toBe(100)
  expect(ringArc(-5).value).toBe(0)
})

test('rings as drawn: the painted share of the circle follows the value', async ({
  page
}) => {
  await page.setContent('<canvas id="c" width="200" height="200"></canvas>')
  const share = async value => {
    const arc = ringArc(value)
    return page.evaluate(async ({ dasharray, cap }) => {
      const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">
        <circle cx="100" cy="100" r="40" pathLength="100" fill="none" stroke="#d97706" stroke-width="7"
          stroke-linecap="${cap}" stroke-dasharray="${dasharray}" transform="rotate(-90 100 100)"/></svg>`
      const img = new Image()
      img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
      await img.decode()
      const c = document.getElementById('c')
      const ctx = c.getContext('2d')
      ctx.clearRect(0, 0, 200, 200)
      ctx.drawImage(img, 0, 0)
      const d = ctx.getImageData(0, 0, 200, 200).data
      let n = 0
      for (let k = 3; k < d.length; k += 4) if (d[k] > 128) n++
      return n
    }, arc)
  }
  const full = await share(100)
  for (const { key, value } of RINGS) {
    const painted = (await share(value)) / full
    expect(painted * 100, key).toBeGreaterThan(value - 3)
    expect(painted * 100, key).toBeLessThan(value + 3)
  }
  expect((await share(0.6)) / full, '0.6 % is almost empty').toBeLessThan(0.03)
})

test('home page: every health ring is drawn at its legend value', async ({
  browser
}) => {
  const context = await openContext(
    browser,
    { width: 1920, height: 1080 },
    { reducedMotion: 'reduce' }
  )
  const page = await context.newPage()
  await visit(page, '/')
  await page.waitForSelector('[data-widget="health"] .mring__arc')
  const rings = await page.$$eval('[data-widget="health"] .mring__arc', els =>
    els.map(e => ({
      key: e.dataset.ringKey,
      value: Number(e.dataset.ringValue),
      dash: Number(getComputedStyle(e).strokeDasharray.split(/[ ,]+/)[0])
    }))
  )
  const legend = await page.$$eval(
    '[data-widget="health"] .health__legend li',
    els => els.map(li => li.querySelector('.health__val').innerText)
  )
  const thin = rings.filter(r => r.key !== 'index')
  expect(thin.map(r => r.key)).toEqual([
    'attendance',
    'quality',
    'journal',
    'telegram'
  ])
  thin.forEach((r, k) => {
    expect(r.dash, r.key).toBeCloseTo(r.value, 1)
    const shown = parseFloat(legend[k].replace(',', '.'))
    if (Number.isFinite(shown)) expect(r.value, r.key).toBeCloseTo(shown, 0)
  })
  await context.close()
})
