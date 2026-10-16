// Quick look at one or more pages while working on them:
//   node tests/ui/shot.mjs dashboard,crud-students 1366 [dark]
// Writes <name>-<width>[-dark].jpg into UI_SHOTS (default: the system temp folder).
import { chromium } from 'playwright'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { mkdirSync } from 'node:fs'
import { pages } from './pages.js'
import { openContext, visit } from './session.js'

const names = (process.argv[2] || 'dashboard').split(',')
const width = Number(process.argv[3] || 1366)
const dark = process.argv[4] === 'dark'
const dir = process.env.UI_SHOTS || join(tmpdir(), 'ui-shots')
mkdirSync(dir, { recursive: true })

const browser = await chromium.launch()
for (const name of names) {
  const p = pages.find(x => x.name === name) || { name, path: name }
  const context = await openContext(browser, { width, height: width < 600 ? 844 : 900 }, { dark, signedIn: !p.public })
  const page = await context.newPage()
  page.on('pageerror', e => console.log(name, 'JS error:', e.message))
  await visit(page, p.path)
  const file = join(dir, `${p.name.replace(/\W+/g, '_')}-${width}${dark ? '-dark' : ''}.jpg`)
  await page.screenshot({ path: file, fullPage: true, type: 'jpeg', quality: 70 })
  console.log(file)
  await context.close()
}
await browser.close()
