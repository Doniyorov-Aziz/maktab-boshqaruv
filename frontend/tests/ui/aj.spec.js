// The new attendance and journal pages in a browser, on fixtures from the TEST database
// (aj-mock.js): 1920 / 1366 / 390, dark and light, ADMIN and VIEWER.
//   AJ_FIXTURES=…/aj.json UI_BASE=http://localhost:9102 npx playwright test tests/ui/aj.spec.js
import { test, expect } from '@playwright/test'
import { hasFixtures, fixtures, mockContext, open } from './aj-mock.js'

test.skip(!hasFixtures(), 'AJ_FIXTURES is not set')

const WIDE = { width: 1920, height: 1080 }
const LAPTOP = { width: 1366, height: 768 }
const PHONE = { width: 390, height: 844 }

/** A page that fails the test on any console error or uncaught exception. */
async function watched(context) {
  const page = await context.newPage()
  page.errors = []
  page.on('pageerror', e => page.errors.push(String(e)))
  page.on('console', m => m.type() === 'error' && page.errors.push(m.text()))
  return page
}

async function attendance(browser, size = WIDE, opts = {}) {
  const context = await mockContext(browser, size, opts)
  await context.addInitScript(
    id => localStorage.setItem('attendance.classId', String(id)),
    fixtures().classA
  )
  const page = await watched(context)
  await open(page, opts.path || '/attendance', { at: opts.at || '10:40' })
  return { context, page }
}

async function journal(browser, size = WIDE, opts = {}) {
  const context = await mockContext(browser, size, opts)
  const page = await watched(context)
  const f = fixtures()
  const q =
    opts.query ??
    `class=${f.classA}&subject=${f.subjectA}&month=${f.today.slice(0, 7)}`
  await open(page, `/gradebook?${q}`, { at: opts.at || '11:30' })
  return { context, page }
}

const saves = context =>
  context.requests.filter(
    r => r.method === 'POST' && r.path === '/api/pages/attendance/save'
  )
const gradePosts = context =>
  context.requests.filter(r => r.method === 'POST' && r.path === '/api/grades')

// ================================================================ attendance

test.describe('attendance', () => {
  test('status buttons are four separate buttons with a gap', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    const btns = page
      .locator('[data-roster-row]')
      .first()
      .locator('[data-status]')
    await expect(btns).toHaveCount(4)
    const boxes = await Promise.all(
      [0, 1, 2, 3].map(i => btns.nth(i).boundingBox())
    )
    for (let i = 1; i < 4; i++)
      expect(
        boxes[i].x - (boxes[i - 1].x + boxes[i - 1].width)
      ).toBeGreaterThanOrEqual(3.5)
    for (const b of boxes) expect(Math.round(b.height)).toBe(32)
    expect(page.errors).toEqual([])
    await context.close()
  })

  test('lessons follow the chosen date: Monday 5, Saturday 4, Sunday is skipped', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    await expect(page.locator('[data-timeline-lesson]')).toHaveCount(5)
    await page.locator('[data-date-prev]').click()
    await expect(page.locator('[data-date-label]')).toContainText(
      '17-okt, Shanba'
    )
    await expect(page.locator('[data-timeline-lesson]')).toHaveCount(4)
    const days = context.requests
      .filter(r => r.path === '/api/pages/attendance/day')
      .map(r => r.search)
    expect(days.some(s => s.includes('date=2026-10-17'))).toBe(true)
    expect(days.some(s => s.includes('date=2026-10-18'))).toBe(false)
    // nothing after today
    await page.locator('[data-date-next]').click()
    await expect(page.locator('[data-date-label]')).toContainText(
      '19-okt, Dushanba'
    )
    await expect(page.locator('[data-date-next]')).toBeDisabled()
    await context.close()
  })

  test('the lesson going on now is picked by itself; no empty state', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser, WIDE, { at: '10:40' })
    const on = page.locator('[data-timeline-lesson][aria-pressed="true"]')
    await expect(on).toContainText("O'zbek adabiyoti")
    await expect(on).toContainText('Hozir')
    await expect(page.locator('[data-empty]')).toHaveCount(0)
    await expect(page.locator('[data-roster-row]')).toHaveCount(21)
    await context.close()
  })

  test('class picker: chosen filled, others see-through, dots for state', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    const on = page.locator(
      '[data-class-picker] [data-class][aria-pressed="true"]'
    )
    const off = page
      .locator('[data-class-picker] [data-class][aria-pressed="false"]')
      .first()
    const bg = el => el.evaluate(e => getComputedStyle(e).backgroundColor)
    expect(await bg(on)).not.toBe('rgba(0, 0, 0, 0)')
    expect(await bg(off)).toBe('rgba(0, 0, 0, 0)')
    await expect(
      page.locator('[data-class-picker] .cp-dot').first()
    ).toBeVisible()
    await context.close()
  })

  test('keyboard: 2 marks absent, Enter goes down, autosave sends only the changed row', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    const list = page.locator('[data-roster-list]')
    await list.focus()
    await page.keyboard.press('2')
    await expect(page.locator('[data-roster-row]').first()).toHaveAttribute(
      'data-row-status',
      'ABSENT'
    )
    await page.keyboard.press('Enter')
    await page.keyboard.press('3')
    await expect(page.locator('[data-roster-row]').nth(1)).toHaveAttribute(
      'data-row-status',
      'LATE'
    )
    await expect(page.locator('[data-savebar]')).toContainText("2 ta o'zgarish")
    await expect(page.locator('[data-savebar]')).toContainText('Saqlandi', {
      timeout: 4000
    })
    const sent = saves(context)
    expect(sent).toHaveLength(1)
    const body = JSON.parse(sent[0].body)
    expect(body.entries.map(e => e.status)).toEqual(['ABSENT', 'LATE'])
    expect(sent[0].search).toContain('schoolId=1')
    await context.close()
  })

  test('autosaved marks are still there after a reload', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    await page.locator('[data-roster-list]').focus()
    await page.keyboard.press('2')
    await expect(page.locator('[data-savebar]')).toContainText('Saqlandi', {
      timeout: 4000
    })
    await page.reload({ waitUntil: 'networkidle' })
    await expect(page.locator('[data-roster-row]').first()).toHaveAttribute(
      'data-row-status',
      'ABSENT'
    )
    await expect(page.locator('[data-roster-row]').nth(1)).not.toHaveAttribute(
      'data-row-status',
      'ABSENT'
    )
    expect(page.errors).toEqual([])
    await context.close()
  })

  test('a day without lessons offers "Boshqa kunni tanlash"', async ({
    browser
  }) => {
    const context = await mockContext(browser, WIDE)
    const f = fixtures()
    const day =
      f[`/api/pages/attendance/day?classId=${f.classA}&date=${f.today}`]
    context.overrides['GET /api/pages/attendance/day'] = {
      body: { ...day, lessons: [] }
    }
    await context.addInitScript(
      id => localStorage.setItem('attendance.classId', String(id)),
      f.classA
    )
    const page = await watched(context)
    await open(page, '/attendance')
    const empty = page.locator('[data-empty="lessons"]')
    await expect(empty).toBeVisible()
    await empty.getByRole('button', { name: 'Boshqa kunni tanlash' }).click()
    await expect(page.locator('.q-date')).toBeVisible()
    await context.close()
  })

  test('Shift-click selects a range; one key marks all of it', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    const rows = page.locator('[data-roster-row]')
    await rows.nth(0).locator('.rr__who').click()
    await rows
      .nth(3)
      .locator('.rr__who')
      .click({ modifiers: ['Shift'] })
    await expect(page.locator('[data-selection]')).toContainText(
      '4 ta tanlandi'
    )
    await page.keyboard.press('4')
    for (let i = 0; i < 4; i++)
      await expect(rows.nth(i)).toHaveAttribute('data-row-status', 'EXCUSED')
    await expect(rows.nth(4)).not.toHaveAttribute('data-row-status', 'EXCUSED')
    await context.close()
  })

  test('Kechikdi asks for minutes, Sababli for a reason', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    const row = page.locator('[data-roster-row]').nth(2)
    await row.locator('[data-status="LATE"]').click()
    await page.locator('[data-late="15"]').click()
    await expect(row).toContainText('15 daq')
    await row.locator('[data-status="EXCUSED"]').click()
    await page.locator('[data-reason="Oilaviy sabab"]').click()
    await page.locator('[data-reason-note]').fill("to'y")
    await page.locator('[data-reason-save]').click()
    await expect(row).toContainText("Oilaviy sabab — to'y")
    await expect(page.locator('[data-savebar]')).toContainText('Saqlandi', {
      timeout: 4000
    })
    const last = JSON.parse(saves(context).at(-1).body).entries[0]
    expect(last).toMatchObject({
      status: 'EXCUSED',
      comment: "Oilaviy sabab — to'y",
      lateMinutes: null
    })
    await context.close()
  })

  test('cards: a tap cycles the status', async ({ browser }) => {
    const { context, page } = await attendance(browser)
    await page.getByRole('tab', { name: 'Kartochkalar' }).click()
    const card = page.locator('[data-card]').first()
    const before = await card.getAttribute('data-row-status')
    await card.click()
    expect(await card.getAttribute('data-row-status')).not.toBe(before)
    await card.click({ button: 'right' })
    await page.locator('[data-menu-status="EXCUSED"]').click()
    await expect(card).toHaveAttribute('data-row-status', 'EXCUSED')
    await context.close()
  })

  test('"Hammasi keldi" with undo; automatic marks can be confirmed', async ({
    browser
  }) => {
    const { context, page } = await attendance(browser)
    await expect(page.locator('[data-confirm-auto]')).toBeVisible()
    const rows = page.locator('[data-roster-row]')
    await rows.first().locator('[data-status="ABSENT"]').click()
    await page.locator('[data-all-present]').click()
    await expect(rows.first()).toHaveAttribute('data-row-status', 'PRESENT')
    await page.getByRole('button', { name: 'Bekor qilish' }).click()
    await expect(rows.first()).toHaveAttribute('data-row-status', 'ABSENT')
    await context.close()
  })

  test('VIEWER sees the list but no controls', async ({ browser }) => {
    const { context, page } = await attendance(browser, WIDE, {
      role: 'VIEWER'
    })
    await expect(page.locator('[data-roster-row]')).toHaveCount(21)
    await expect(page.locator('[data-all-present]')).toHaveCount(0)
    await expect(page.locator('[data-savebar]')).toHaveCount(0)
    await expect(
      page.locator('[data-roster-row] [data-status]').first()
    ).toBeDisabled()
    await page.locator('[data-roster-list]').focus()
    await page.keyboard.press('2')
    await page.waitForTimeout(1800)
    expect(saves(context)).toHaveLength(0)
    await context.close()
  })

  for (const dark of [true, false]) {
    test(`1366 ${dark ? 'dark' : 'light'}: nothing sticks out sideways`, async ({
      browser
    }) => {
      const { context, page } = await attendance(browser, LAPTOP, { dark })
      const over = await page.evaluate(
        () => document.documentElement.scrollWidth - innerWidth
      )
      expect(over).toBeLessThanOrEqual(0)
      expect(page.errors).toEqual([])
      await context.close()
    })
  }

  test('390: opens on cards, no sideways page scroll', async ({ browser }) => {
    const { context, page } = await attendance(browser, PHONE)
    await expect(page.locator('[data-roster-cards]')).toBeVisible()
    const over = await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth
    )
    expect(over).toBeLessThanOrEqual(0)
    await context.close()
  })
})

// ================================================================ journal

test.describe('journal', () => {
  test('subject is picked from the lesson going on now (no subject in the link)', async ({
    browser
  }) => {
    const { context, page } = await journal(browser, WIDE, {
      query: `class=${fixtures().classA}`,
      at: '11:30'
    })
    await expect(
      page.locator('[data-subject][aria-pressed="true"]')
    ).toContainText('Ingliz tili')
    await expect(page.locator('[data-gradebook]')).toBeVisible()
    await expect(page.locator('[data-lesson-state]')).toContainText('4-dars')
    await expect(page.locator('[data-grade-lesson]')).toBeVisible()
    expect(page.errors).toEqual([])
    await context.close()
  })

  test('outside a lesson the panel offers "Bugungi ustunga o\'tish"', async ({
    browser
  }) => {
    const { context, page } = await journal(browser, WIDE, { at: '15:30' })
    await expect(page.locator('[data-grade-lesson]')).toHaveCount(0)
    await expect(page.locator('[data-go-today]')).toBeVisible()
    await context.close()
  })

  test('quick entry: 5 grades the cell and goes DOWN', async ({ browser }) => {
    const { context, page } = await journal(browser)
    const ci = await page.locator('thead .gb--today').getAttribute('data-col')
    await page.locator(`td[data-row="2"][data-col="${ci}"]`).click()
    await page.keyboard.press('Escape')
    await page.locator(`td[data-row="2"][data-col="${ci}"]`).focus()
    await page.keyboard.press('5')
    await expect(
      page.locator(`td[data-row="2"][data-col="${ci}"] [data-value="5"]`)
    ).toBeVisible()
    await expect(
      page.locator(`td[data-row="3"][data-col="${ci}"]`)
    ).toBeFocused()
    expect(JSON.parse(gradePosts(context).at(-1).body)).toMatchObject({
      score: 5,
      gradeDate: fixtures().today
    })
    await context.close()
  })

  test('class picker: only the chosen class is filled', async ({ browser }) => {
    const { context, page } = await journal(browser)
    const bg = el => el.evaluate(e => getComputedStyle(e).backgroundColor)
    const on = page.locator(
      '[data-class-picker] [data-class][aria-pressed="true"]'
    )
    await expect(on).toHaveCount(1)
    expect(await bg(on)).not.toBe('rgba(0, 0, 0, 0)')
    const off = page.locator(
      '[data-class-picker] [data-class][aria-pressed="false"]'
    )
    for (let i = 0; i < (await off.count()); i++)
      expect(await bg(off.nth(i))).toBe('rgba(0, 0, 0, 0)')
    await context.close()
  })

  test('the column of today says "Bugun"', async ({ browser }) => {
    const { context, page } = await journal(browser)
    await expect(page.locator('thead .gb--today .gb__dtoday')).toHaveText(
      'Bugun'
    )
    await expect(page.locator('thead .gb__dtoday')).toHaveCount(1)
    await context.close()
  })

  test('dragging over cells selects a block that takes one grade', async ({
    browser
  }) => {
    const { context, page } = await journal(browser)
    const ci = await page.locator('thead .gb--today').getAttribute('data-col')
    const from = await page
      .locator(`td[data-row="5"][data-col="${ci}"]`)
      .boundingBox()
    const to = await page
      .locator(`td[data-row="7"][data-col="${ci}"]`)
      .boundingBox()
    await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2)
    await page.mouse.down()
    await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, {
      steps: 8
    })
    await page.mouse.up()
    await expect(page.locator('.gb__cell--sel')).toHaveCount(3)
    await expect(page.locator('[data-grade-popover]')).toHaveCount(0)
    const before = gradePosts(context).length
    await page.keyboard.press('4')
    await expect.poll(() => gradePosts(context).length).toBe(before + 3)
    const rows = gradePosts(context)
      .slice(before)
      .map(r => JSON.parse(r.body).score)
    expect(rows).toEqual([4, 4, 4])
    await context.close()
  })

  test('Sunday and future cells say why and send nothing', async ({
    browser
  }) => {
    const { context, page } = await journal(browser)
    const sunday = await page
      .locator('thead .gb--sunday')
      .first()
      .getAttribute('data-col')
    await page.locator(`td[data-row="0"][data-col="${sunday}"]`).click()
    await expect(page.locator('.q-notification')).toContainText('yakshanba')
    const future = await page
      .locator('thead .gb--future')
      .first()
      .getAttribute('data-col')
    await page.locator(`td[data-row="0"][data-col="${future}"]`).focus()
    await page.keyboard.press('4')
    await expect(
      page.locator('.q-notification', { hasText: 'Kelajak' })
    ).toBeVisible()
    expect(gradePosts(context)).toHaveLength(0)
    await context.close()
  })

  test('the popover: big 2–5, type, comment', async ({ browser }) => {
    const { context, page } = await journal(browser)
    const ci = await page.locator('thead .gb--today').getAttribute('data-col')
    await page.locator(`td[data-row="2"][data-col="${ci}"]`).click()
    const pop = page.locator('[data-grade-popover]')
    await expect(pop).toBeVisible()
    const box = await pop.boundingBox()
    expect(box.x).toBeGreaterThan(0)
    expect(box.y + box.height).toBeLessThanOrEqual(WIDE.height)
    await pop.getByRole('tab', { name: 'Nazorat' }).click()
    await pop.locator('[data-grade-comment]').fill('test')
    await pop.locator('[data-pick="4"]').click()
    expect(JSON.parse(gradePosts(context).at(-1).body)).toMatchObject({
      score: 4,
      type: 'EXAM',
      comment: 'test'
    })
    await expect(
      page.locator(`td[data-row="2"][data-col="${ci}"] .gb__chip--exam`)
    ).toBeVisible()
    await context.close()
  })

  test('a homework grade is sent as HOMEWORK and marked with a dashed ring', async ({
    browser
  }) => {
    const { context, page } = await journal(browser)
    const ci = await page.locator('thead .gb--today').getAttribute('data-col')
    // an empty cell of today: a new grade is a POST
    const cell = page
      .locator(`td[data-col="${ci}"]:not(:has([data-grade]))`)
      .first()
    const ri = await cell.getAttribute('data-row')
    await cell.click()
    const pop = page.locator('[data-grade-popover]')
    await expect(pop.getByRole('tab')).toHaveText([
      'Joriy',
      'Nazorat',
      'Uy vazifasi',
      'Chorak'
    ])
    const box = await pop.boundingBox()
    expect(box.x + box.width).toBeLessThanOrEqual(WIDE.width)
    await pop.getByRole('tab', { name: 'Uy vazifasi' }).click()
    await pop.locator('[data-pick="5"]').click()
    expect(JSON.parse(gradePosts(context).at(-1).body)).toMatchObject({
      score: 5,
      type: 'HOMEWORK'
    })
    const chip = page.locator(
      `td[data-row="${ri}"][data-col="${ci}"] .gb__chip--hw`
    )
    await expect(chip).toBeVisible()
    await expect(chip).not.toHaveClass(/gb__chip--exam/)
    await context.close()
  })

  test('Delete removes with 5 s undo; undo sends nothing', async ({
    browser
  }) => {
    const { context, page } = await journal(browser)
    const first = page.locator('td[data-row]:has([data-grade])').first()
    const [r, c] = [
      await first.getAttribute('data-row'),
      await first.getAttribute('data-col')
    ]
    const cell = page.locator(`td[data-row="${r}"][data-col="${c}"]`)
    const count = await cell.locator('[data-grade]').count()
    await cell.focus()
    await page.keyboard.press('Delete')
    await expect(cell.locator('[data-grade]')).toHaveCount(count - 1)
    await page.getByRole('button', { name: 'Bekor qilish' }).click()
    await expect(cell.locator('[data-grade]')).toHaveCount(count)
    await page.waitForTimeout(500)
    expect(context.requests.filter(r => r.method === 'DELETE')).toHaveLength(0)
    await context.close()
  })

  test('a block of cells takes one grade', async ({ browser }) => {
    const { context, page } = await journal(browser)
    const ci = Number(
      await page.locator('thead .gb--today').getAttribute('data-col')
    )
    await page.locator(`td[data-row="14"][data-col="${ci}"]`).focus()
    await page.keyboard.press('Shift+ArrowDown')
    await page.keyboard.press('Shift+ArrowDown')
    await expect(page.locator('.gb__cell--sel')).toHaveCount(3)
    const before = gradePosts(context).length
    await page.keyboard.press('3')
    await expect.poll(() => gradePosts(context).length).toBe(before + 3)
    await context.close()
  })

  test('column header opens "Butun sinfga baho"; student name opens the card', async ({
    browser
  }) => {
    const { context, page } = await journal(browser)
    await page.locator('thead .gb--today').click()
    await expect(
      page.locator('[data-column-list] [data-column-student]')
    ).toHaveCount(21)
    await page.locator('[data-fill="5"]').click()
    await expect(page.locator('[data-column-count]')).not.toContainText('0 /')
    await page.locator('[data-drawer-close]').click()
    await page.locator('[data-open-student]').first().click()
    await expect(page.locator('[data-student-drawer]')).toBeVisible()
    await expect(
      page.locator('[data-student-drawer] [data-kpi="attendance"]')
    ).toContainText('%')
    await context.close()
  })

  test('heat mode, sticky header, name and stats columns, export menu', async ({
    browser
  }) => {
    const { context, page } = await journal(browser, LAPTOP)
    await page.locator('[data-heat-toggle]').click()
    await expect(page.locator('[data-gradebook]')).toHaveClass(/gb--heat/)
    const name = page.locator('tbody .gb__name').first()
    const x0 = (await name.boundingBox()).x
    await page.locator('[data-gradebook]').evaluate(e => (e.scrollLeft = 0))
    await page.locator('[data-gradebook]').evaluate(e => (e.scrollLeft = 400))
    expect((await name.boundingBox()).x).toBe(x0)
    // the header row stays on top while the table scrolls down
    const head = page.locator('thead .gb__day').first()
    const y0 = (await head.boundingBox()).y
    await page.locator('[data-gradebook]').evaluate(e => (e.scrollTop = 300))
    expect(
      await page.locator('[data-gradebook]').evaluate(e => e.scrollTop)
    ).toBeGreaterThan(0)
    expect((await head.boundingBox()).y).toBe(y0)
    await page.locator('[data-gradebook]').evaluate(e => (e.scrollTop = 0))
    const avg = page.locator('tbody .gb__r3').first()
    const ax = (await avg.boundingBox()).x
    await page.locator('[data-gradebook]').evaluate(e => (e.scrollLeft = 0))
    expect((await avg.boundingBox()).x).toBe(ax)
    await page.locator('[data-export]').click()
    await expect(page.locator('[data-export-xlsx]')).toBeVisible()
    await expect(page.locator('[data-export-pdf]')).toBeVisible()
    const over = await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth
    )
    expect(over).toBeLessThanOrEqual(0)
    await context.close()
  })

  test('VIEWER: no "+" and no popover', async ({ browser }) => {
    const { context, page } = await journal(browser, WIDE, { role: 'VIEWER' })
    await expect(page.locator('.gb__plus')).toHaveCount(0)
    const ci = await page.locator('thead .gb--today').getAttribute('data-col')
    await page.locator(`td[data-row="2"][data-col="${ci}"]`).click()
    await page.keyboard.press('5')
    await expect(page.locator('[data-grade-popover]')).toHaveCount(0)
    expect(gradePosts(context)).toHaveLength(0)
    await context.close()
  })

  test('390: the table scrolls sideways, the page does not', async ({
    browser
  }) => {
    const { context, page } = await journal(browser, PHONE, { dark: false })
    const table = page.locator('[data-gradebook]')
    expect(await table.evaluate(e => e.scrollWidth > e.clientWidth)).toBe(true)
    const over = await page.evaluate(
      () => document.documentElement.scrollWidth - innerWidth
    )
    expect(over).toBeLessThanOrEqual(0)
    await context.close()
  })

  test('40 students × 31 days stays quick', async ({ browser }) => {
    const context = await mockContext(browser, WIDE, { dark: true })
    const base =
      fixtures()[`/api/grades/journal?subjectId=${fixtures().subjectA}`]
    const days = Array.from({ length: 31 }, (_, i) => {
      const date = `2026-10-${String(i + 1).padStart(2, '0')}`
      const wd = new Date(`${date}T12:00:00`).getDay()
      return {
        date,
        weekday: 'Du',
        isSunday: wd === 0,
        isHoliday: false,
        holidayName: null,
        isFuture: i + 1 > 19,
        hasLesson: true
      }
    })
    const students = Array.from({ length: 40 }, (_, i) => ({
      id: 5000 + i,
      fullName: `Test O'quvchi ${i + 1}`
    }))
    let id = 1
    const grades = []
    for (const s of students)
      for (const d of days)
        if (!d.isSunday && !d.isFuture)
          grades.push({
            id: id++,
            studentId: s.id,
            date: d.date,
            value: 2 + (id % 4),
            type: 'CURRENT',
            comment: null,
            source: 'MANUAL'
          })
    context.overrides['GET /api/grades/journal'] = {
      body: { ...base, days, students, grades }
    }
    const page = await watched(context)
    const t0 = Date.now()
    await open(
      page,
      `/gradebook?class=${fixtures().classA}&subject=${fixtures().subjectA}&month=2026-10`
    )
    await expect(page.locator('tbody tr')).toHaveCount(40)
    expect(Date.now() - t0).toBeLessThan(6000)
    await page.locator('td[data-row="0"][data-col="18"]').focus()
    const t1 = Date.now()
    for (let i = 0; i < 20; i++) await page.keyboard.press('ArrowDown')
    await expect(page.locator('td[data-row="20"][data-col="18"]')).toBeFocused()
    expect((Date.now() - t1) / 20).toBeLessThan(100)
    expect(page.errors).toEqual([])
    await context.close()
  })
})
