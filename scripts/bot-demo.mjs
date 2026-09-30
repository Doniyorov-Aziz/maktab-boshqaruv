// Ota-onalar botining to'liq demosi (telegram.mock=true rejimida).
//
// Ishlatish (backend mock rejimida ishlab turganda, loyiha ildizidan):
//   API_URL=http://localhost:8080 node scripts/bot-demo.mjs
//
// Skript ota-ona sifatida botning barcha sahifalarini ketma-ket "bosib" chiqadi,
// admin tomonidagi harakatlarni (davomat, baho, javob, ariza qarori, umumiy xabar)
// API orqali bajaradi va natijani docs/bot-demo.md ga chat ko'rinishida yozadi.
// Bot yuborgan rasmlar docs/images/bot/ ga saqlanadi.

import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const API = process.env.API_URL || 'http://localhost:8080'
const ADMIN_USER = process.env.ADMIN_USER || 'admin'
const ADMIN_PASS = process.env.ADMIN_PASS || 'admin123'
const SCHOOL_ID = Number(process.env.SCHOOL_ID || 1)
const CLASS_ID = Number(process.env.CLASS_ID || 1)
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const DOCS = path.join(ROOT, 'docs')
const IMG_DIR = path.join(DOCS, 'images', 'bot')
const CHAT = 900001
const PARENT = { firstName: 'Dilnoza', username: 'dilnoza_opa' }
const PAUSE = 650 // the bot allows 2 updates per second per chat

let token
let page = null // message id of the current "page" (last message with inline buttons)
let chatCursor = 0
let imageNo = 0
const md = []
const sleep = ms => new Promise(r => setTimeout(r, ms))

async function api(method, url, body) {
  const res = await fetch(API + url, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  if (!res.ok) throw new Error(`${method} ${url} -> ${res.status}: ${text}`)
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

// ---------------------------------------------------------------- rendering

function decode(s) {
  return s.replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&amp;/g, '&')
}

/** Telegram HTML -> Markdown lines (without the outer chat quote). */
function htmlToMd(html) {
  let s = html || ''
  s = s.replace(/<blockquote>([\s\S]*?)<\/blockquote>/g, (_, inner) =>
    inner.split('\n').map(l => '\u0000' + l).join('\n'))
  s = s.replace(/<b>([\s\S]*?)<\/b>/g, '**$1**').replace(/<i>([\s\S]*?)<\/i>/g, '_$1_')
  s = s.replace(/<code>([\s\S]*?)<\/code>/g, '`$1`')
  s = decode(s)
  return s.split('\n').map(l => (l.startsWith('\u0000') ? '> ' + l.slice(1) : l))
}

function keyboardMd(json) {
  if (!json) return []
  let kb
  try {
    kb = JSON.parse(json)
  } catch {
    return []
  }
  const lines = []
  if (kb.inline_keyboard) {
    for (const row of kb.inline_keyboard) {
      lines.push(row.map(b => '`[' + b.text.trim() + (b.web_app ? ' ↗' : '') + ']`').join(' '))
    }
  } else if (kb.keyboard) {
    lines.push('⌨️ _Pastki klaviatura:_')
    for (const row of kb.keyboard) lines.push(row.map(b => '`' + b.text + '`').join(' '))
  } else if (kb.remove_keyboard) {
    lines.push('⌨️ _Pastki klaviatura yopildi_')
  }
  return lines
}

async function renderOps(ops) {
  for (const op of ops) {
    if (op.op === 'delete') continue
    const out = []
    if (op.op === 'edit') out.push('✏️ _(shu xabar tahrirlandi)_', '')
    if (op.op === 'photo') {
      imageNo++
      const name = `${String(imageNo).padStart(2, '0')}-${op.photoId}.png`
      const res = await fetch(`${API}/api/telegram/mock/photos/${op.photoId}`, { headers: { Authorization: `Bearer ${token}` } })
      fs.writeFileSync(path.join(IMG_DIR, name), Buffer.from(await res.arrayBuffer()))
      out.push(`![rasm](images/bot/${name})`, '')
    }
    if (op.text) out.push(...htmlToMd(op.text))
    const kb = keyboardMd(op.keyboard)
    if (kb.length) out.push('', ...kb)
    // Leading spaces become no-break spaces: 4+ plain spaces after "> " would turn a line into a code block.
    const keepIndent = l => l.replace(/^ +/, m => ' '.repeat(m.length))
    md.push('🤖 **Bot:**', '', ...out.map(l => (l.startsWith('> ') ? '>> ' + keepIndent(l.slice(2)) : l === '' ? '>' : '> ' + keepIndent(l))), '')
    if ((op.op === 'send' || op.op === 'edit') && op.keyboard && op.keyboard.includes('inline_keyboard')) {
      page = op.messageId
    }
  }
}

function heading(title) {
  md.push(`### ${title}`, '')
}

function userSays(text) {
  md.push(`👤 **Ota-ona:** ${text}`, '')
}

function adminDid(text) {
  md.push(`🏫 **Maktab (admin panel):** ${text}`, '')
}

// ---------------------------------------------------------------- actions

async function sim(payload) {
  await sleep(PAUSE)
  const ops = await api('POST', '/api/telegram/mock/updates', { chatId: CHAT, ...PARENT, ...payload })
  const tail = await api('GET', `/api/telegram/mock/messages?chatId=${CHAT}&from=${chatCursor}`)
  chatCursor = tail.next
  return ops
}

async function say(text, label = text) {
  userSays(label.startsWith('/') ? '`' + label + '`' : '«' + label + '»')
  await renderOps(await sim({ text }))
}

async function tap(data, label) {
  userSays(`tugmani bosdi \`[${label}]\``)
  const ops = await sim({ callbackData: data, messageId: page })
  if (!ops.length) md.push('> _(bot hech narsa ko\'rsatmadi — so\'rov rad etildi)_', '')
  await renderOps(ops)
  return ops
}

/** Finds a button on the current page whose callback_data matches. */
function findButton(ops, predicate) {
  for (const op of [...ops].reverse()) {
    if (!op.keyboard) continue
    const kb = JSON.parse(op.keyboard)
    for (const row of kb.inline_keyboard || []) for (const b of row) if (b.callback_data && predicate(b)) return b
  }
  return null
}

async function drainNotifications(title, waitMs = 3500) {
  await sleep(waitMs)
  const tail = await api('GET', `/api/telegram/mock/messages?chatId=${CHAT}&from=${chatCursor}`)
  chatCursor = tail.next
  if (title) md.push(`_${title}_`, '')
  await renderOps(tail.messages)
}

// ---------------------------------------------------------------- scenario

async function main() {
  fs.mkdirSync(IMG_DIR, { recursive: true })
  for (const f of fs.readdirSync(IMG_DIR)) if (f.endsWith('.png')) fs.unlinkSync(path.join(IMG_DIR, f))
  token = (await api('POST', '/api/auth/login', { username: ADMIN_USER, password: ADMIN_PASS })).toString().trim()
  const status = await api('GET', '/api/telegram/status')
  if (status.mode !== 'MOCK') throw new Error('Backend telegram.mock=true rejimida emas: ' + status.mode)
  const tail = await api('GET', `/api/telegram/mock/messages?chatId=${CHAT}&from=0`)
  chatCursor = tail.next

  const codes = await api('GET', `/api/telegram/classes/${CLASS_ID}/codes`)
  const [child1, child2] = codes.students
  const otherClass = await api('GET', `/api/telegram/classes/${CLASS_ID + 1}/codes`)
  const stranger = otherClass.students[0]
  // Start clean: undo links a previous demo run left behind.
  for (const s of [child1, child2]) {
    const info = await api('GET', `/api/telegram/students/${s.studentId}`)
    for (const l of info.links) if (l.telegramUsername === PARENT.username) await api('DELETE', `/api/telegram/links/${l.id}`)
  }
  // Automatic messages must go out during the demo whatever the time of day.
  const schoolSettings = await api('GET', `/api/notifications/settings?schoolId=${SCHOOL_ID}`)
  await api('PUT', `/api/notifications/settings?schoolId=${SCHOOL_ID}`, { ...schoolSettings, quietHoursEnabled: false })

  md.push(
    '# Ota-onalar boti — to\'liq demo',
    '',
    `> Bu fayl \`scripts/bot-demo.mjs\` tomonidan avtomatik yaratildi (${new Date().toLocaleString('uz-UZ', { timeZone: 'Asia/Tashkent' })}),`,
    '> backend `telegram.mock=true` rejimida, seed ma\'lumot bilan. Har bir 👤 qadam — ota-onaning harakati,',
    '> 🤖 — bot javobi, `[...]` — inline tugmalar, ✏️ — xabar yangisi yuborilmay, o\'rnida tahrirlangani.',
    '> Botni Telegram\'da ochmasdan turib barcha sahifalarni shu yerda ko\'rish mumkin.',
    '',
    '**Mundarija:** ',
    '[1. Kutib olish](#1-kutib-olish-va-ulanish) · [2. Bosh menyu](#2-bosh-menyu--bugun) · [3. Dars jadvali](#3-dars-jadvali) · ' +
      '[4. Davomat](#4-davomat) · [5. Baholar](#5-baholar) · [6. Hisobot](#6-hisobot) · [7. Xulq](#7-xulq) · ' +
      '[8. E\'lonlar](#8-elonlar) · [9. Tadbirlar](#9-tadbirlar) · [10. O\'qituvchilar](#10-oqituvchilar) · ' +
      '[11. Maktabga yozish](#11-maktabga-yozish) · [12. Sababli ariza](#12-sababli-ariza) · [13. Maktab haqida](#13-maktab-haqida) · ' +
      '[14. Sozlamalar](#14-sozlamalar-3-til) · [15. Farzandlarim](#15-farzandlarim) · [16. Xavfsizlik](#16-xavfsizlik) · ' +
      '[17. Avtomatik xabarlar](#17-avtomatik-xabarlar)',
    ''
  )

  // 1 -------------------------------------------------------------------------
  md.push('## 1. Kutib olish va ulanish', '')
  heading('1.1. Birinchi /start (hali ulanmagan)')
  await say('/start')
  heading('1.2. Sinf rahbari bergan QR kod / havola orqali ulanish')
  userSays(`QR kodni skanerladi → Telegram \`/start ${child1.linkCode}\` yubordi`)
  await renderOps(await sim({ text: `/start ${child1.linkCode}` }))
  heading('1.3. Noto\'g\'ri kod')
  await say('/start NotoGriKod1', '/start NotoGriKod1')

  // 2 -------------------------------------------------------------------------
  md.push('## 2. Bosh menyu — "Bugun"', '')
  heading('2.1. Bosh menyu')
  let ops = await tap('home', '🏠 Bosh menyu')

  // 3 -------------------------------------------------------------------------
  md.push('## 3. Dars jadvali', '')
  heading('3.1. Bugun')
  await tap('sch', '📅 Dars jadvali')
  heading('3.2. Ertaga')
  await tap('sch:t:tomorrow', 'Ertaga')
  heading('3.3. Hafta')
  await tap('sch:t:week', 'Hafta')
  heading('3.4. Pastki klaviatura orqali ochish')
  await say('📅 Dars jadvali')

  // 4 -------------------------------------------------------------------------
  md.push('## 4. Davomat', '')
  heading('4.1. Oylik xulosa')
  ops = await tap('att', '✅ Davomat')
  const prevMonth = findButton(ops, b => b.callback_data.startsWith('att:k:m:m:') && b.text.startsWith('◀️'))
  heading('4.2. Oldingi oy')
  if (prevMonth) await tap(prevMonth.callback_data, prevMonth.text)
  heading('4.3. Kalendar')
  await tap('att:k:m:v:cal', '🗓 Kalendar')
  heading('4.4. Batafsil — kelmagan va kechikkan darslar')
  await tap('att:k:m:v:det', '📋 Batafsil')
  heading('4.5. Fanlar bo\'yicha')
  await tap('att:k:m:v:sub', '📚 Fanlar bo\'yicha')
  heading('4.6. Kalendar rasm ko\'rinishida')
  await tap('att:k:m:v:img', '🖼 Rasm ko\'rinishida')
  heading('4.7. Chorak bo\'yicha')
  await tap('att:k:q', '🏷 Chorak')
  heading('4.8. O\'quv yili bo\'yicha')
  await tap('att:k:y', '🎓 O\'quv yili')

  // 5 -------------------------------------------------------------------------
  md.push('## 5. Baholar', '')
  heading('5.1. So\'nggi baholar')
  await tap('gr', '📘 Baholar')
  heading('5.2. Fanlar bo\'yicha o\'rtacha (tendensiya bilan)')
  ops = await tap('gr:v:subj', '📚 Fanlar')
  const subject = findButton(ops, b => b.callback_data.startsWith('gr:v:det:id:'))
  heading('5.3. Fan tafsiloti — barcha baholar va o\'qituvchi')
  if (subject) await tap(subject.callback_data, subject.text)
  heading('5.4. Chorak baholari')
  await tap('gr:v:qtr', '🏅 Chorak')
  heading('5.5. Grafik (rasm)')
  await tap('gr:v:chart', '📈 Grafik')

  // 6 -------------------------------------------------------------------------
  md.push('## 6. Hisobot', '')
  heading('6.1. Hafta')
  await tap('rep', '📊 Hisobot')
  heading('6.2. Oy')
  await tap('rep:t:month', 'Oy')
  heading('6.3. Hisobot kartochkasi (rasm)')
  await tap('rep:t:month:v:img', '🖼 Rasm ko\'rinishida')

  // 7 -------------------------------------------------------------------------
  md.push('## 7. Xulq', '')
  heading('7.1. Joriy oy')
  ops = await tap('beh', '⭐ Xulq')
  const behPrev = findButton(ops, b => b.callback_data.startsWith('beh:m:') && b.text.startsWith('◀️'))
  heading('7.2. Oldingi oy')
  if (behPrev) await tap(behPrev.callback_data, behPrev.text)

  // 8 -------------------------------------------------------------------------
  md.push('## 8. E\'lonlar', '')
  heading('8.1. Ro\'yxat')
  ops = await tap('ann', '📢 E\'lonlar')
  const ann = findButton(ops, b => b.callback_data.startsWith('ann:id:'))
  heading('8.2. To\'liq matn')
  if (ann) await tap(ann.callback_data, ann.text)

  // 9-10 ----------------------------------------------------------------------
  md.push('## 9. Tadbirlar', '')
  heading('9.1. Yaqinlashayotgan tadbirlar')
  await tap('ev', '🗓 Tadbirlar')
  md.push('## 10. O\'qituvchilar', '')
  heading('10.1. Sinf rahbari va fan o\'qituvchilari')
  await tap('tch', '👩‍🏫 O\'qituvchilar')

  // 11 ------------------------------------------------------------------------
  md.push('## 11. Maktabga yozish', '')
  heading('11.1. Bo\'lim')
  await tap('msg', '💬 Maktabga yozish')
  heading('11.2. Sinf rahbariga yozish')
  await tap('msg:a:to:to:CT', '👩‍🏫 Sinf rahbariga')
  await say('Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin olib ketsam bo\'ladimi?')
  const messages = await api('GET', `/api/parent-messages?schoolId=${SCHOOL_ID}&status=NEW&size=5`)
  const mine = messages.content.find(m => m.parentUsername === PARENT.username)
  heading('11.3. Maktab javob berdi')
  adminDid('«Murojaatlar» sahifasida javob yozdi')
  await api('POST', `/api/parent-messages/${mine.id}/reply`, { text: 'Va alaykum assalom! Albatta, 3-darsdan so\'ng olib ketishingiz mumkin. Sinf rahbari.' })
  await drainNotifications('(bir necha soniyadan so\'ng botdan avtomatik xabar keldi)')

  // 12 ------------------------------------------------------------------------
  md.push('## 12. Sababli ariza', '')
  heading('12.1. Bo\'lim — sana tanlash')
  ops = await tap('abs', '🤒 Sababli ariza')
  const allDates = []
  for (const row of JSON.parse(ops[ops.length - 1].keyboard).inline_keyboard) {
    for (const b of row) if (b.callback_data?.startsWith('abs:d:')) allDates.push(b)
  }
  const chosen = allDates[1] || allDates[0] // "Ertaga"
  heading('12.2. Necha kun?')
  await tap(chosen.callback_data, chosen.text)
  heading('12.3. Sabab')
  await tap(`${chosen.callback_data}:n:2`, '2 kun')
  heading('12.4. Izoh')
  await tap(`${chosen.callback_data}:n:2:r:I`, '🤒 Kasallik')
  await say('Isitmasi bor, shifokor 2 kun uyda dam olishni buyurdi')
  heading('12.5. Ma\'lumotnoma rasmi')
  userSays('shifokor ma\'lumotnomasining rasmini yubordi 📎')
  const tiny = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=='
  await renderOps(await sim({ photoBase64: tiny }))
  heading('12.6. Tasdiqlash va yuborish')
  await tap('abs:a:send', '📨 Yuborish')
  const requests = await api('GET', `/api/absence-requests?schoolId=${SCHOOL_ID}&status=PENDING&size=5`)
  const req = requests.content.find(r => r.studentId === child1.studentId)
  heading('12.7. Sinf rahbari arizani tasdiqladi')
  adminDid('«Sababli arizalar» sahifasida «Tasdiqlash»ni bosdi — o\'sha kunlarning davomati avtomatik «sababli» bo\'ldi')
  const approved = await api('POST', `/api/absence-requests/${req.id}/approve`)
  md.push(`> _Admin panel: ${approved.excusedLessons} ta dars «sababli» (EXCUSED) deb belgilandi._`, '')
  await drainNotifications('(botdan avtomatik xabar)')

  // 13 ------------------------------------------------------------------------
  md.push('## 13. Maktab haqida', '')
  heading('13.1. Kontaktlar va qo\'ng\'iroq jadvali')
  await tap('info', '🏫 Maktab haqida')

  // 14 ------------------------------------------------------------------------
  md.push('## 14. Sozlamalar (3 til)', '')
  heading('14.1. Xabarlar')
  await tap('set', '⚙️ Sozlamalar')
  heading('14.2. «Haftalik hisobot»ni o\'chirish')
  await tap('set:a:tg:k:wk', '✅ Haftalik hisobot')
  await tap('set:a:tg:k:wk', '▫️ Haftalik hisobot')
  heading('14.3. Vaqtlar')
  await tap('set:v:times', '⏰ Vaqtlar')
  await tap('set:a:st:t:2000', '20:00')
  await tap('set:a:st:t:1900', '19:00')
  heading('14.4. Til')
  await tap('set:v:lang', '🌐 Til / Язык')
  heading('14.5. Ўзбекча (кирилл)')
  await tap('set:a:lg:l:cy', '🇺🇿 Ўзбекча')
  await tap('home', '🏠 Бош меню')
  heading('14.6. Русский')
  await tap('set:a:lg:l:ru', '🇷🇺 Русский')
  await tap('att', '✅ Посещаемость')
  heading('14.7. O\'zbekcha (lotin) ga qaytish')
  await tap('set:a:lg:l:uz', '🇺🇿 O\'zbekcha')

  // 15 ------------------------------------------------------------------------
  md.push('## 15. Farzandlarim', '')
  heading('15.1. Ikkinchi farzandni qo\'shish')
  await tap('ch', '👨‍👩‍👧 Farzandlarim')
  await tap('ch:a:add', '➕ Farzand qo\'shish')
  await say(child2.linkCode, child2.linkCode)
  heading('15.2. Ro\'yxat va farzandni almashtirish')
  ops = await tap('ch', '👨‍👩‍👧 Farzandlarim')
  const sel = findButton(ops, b => b.callback_data.startsWith(`ch:a:sel:id:${child1.studentId}`))
  if (sel) await tap(sel.callback_data, sel.text)
  heading('15.3. Har sahifada tanlangan farzand va «🔄 Farzandni almashtirish»')
  await tap('gr', '📘 Baholar')
  await tap('ch:r:gr', '🔄 Farzandni almashtirish')
  heading('15.4. Farzandni uzish (tasdiqlash bilan)')
  await tap(`ch:a:unl:id:${child2.studentId}`, '🔗 Uzish')
  await tap(`ch:a:unly:id:${child2.studentId}`, '✅ Ha')

  // 16 ------------------------------------------------------------------------
  md.push('## 16. Xavfsizlik', '')
  heading('16.1. Boshqa o\'quvchining ID\'si bilan soxta callback')
  userSays(`qo'lda yasalgan callback yubordi: \`att:s:${stranger.studentId}\` (o'ziga bog'lanmagan o'quvchi)`)
  const denied = await sim({ callbackData: `att:s:${stranger.studentId}`, messageId: page })
  md.push(denied.length === 0
    ? '> 🔒 _Bot hech qanday ma\'lumot qaytarmadi — faqat «🔒 Bu ma\'lumot sizga ochiq emas» ogohlantirishi (answerCallbackQuery) ko\'rsatildi._'
    : '> ⚠️ Kutilmagan javob!', '')
  heading('16.2. Tushunarsiz xabar va /yordam')
  await say('salom')
  await say('/yordam')

  // 17 ------------------------------------------------------------------------
  md.push('## 17. Avtomatik xabarlar', '')
  md.push('Quyidagi xabarlarni ota-ona hech narsa bosmasdan oladi — ular outbox (NotificationLog) orqali navbatga qo\'yilib, yuboruvchi tomonidan jo\'natiladi.', '')
  const lessons = (await api('GET', `/api/attendance/today-lessons?schoolId=${SCHOOL_ID}`)).filter(l => l.schoolClassId === CLASS_ID)
  const today = new Date().toLocaleDateString('sv-SE', { timeZone: 'Asia/Tashkent' })
  if (lessons.length) {
    heading('17.1. Farzand darsga kelmadi')
    adminDid(`o'qituvchi ${lessons[0].subjectName} darsida davomat oldi: ${child1.studentName} — «Kelmadi»`)
    await api('POST', '/api/attendance/bulk', { lessonSlotId: lessons[0].lessonSlotId, recordDate: today, entries: [{ studentId: child1.studentId, status: 'PRESENT' }] })
    await api('POST', '/api/attendance/bulk', { lessonSlotId: lessons[0].lessonSlotId, recordDate: today, entries: [{ studentId: child1.studentId, status: 'ABSENT' }] })
    await drainNotifications()
  }
  if (lessons.length > 1) {
    heading('17.2. Farzand kechikdi')
    adminDid(`${lessons[1].subjectName} darsida — «Kechikdi»`)
    await api('POST', '/api/attendance/bulk', { lessonSlotId: lessons[1].lessonSlotId, recordDate: today, entries: [{ studentId: child1.studentId, status: 'PRESENT' }] })
    await api('POST', '/api/attendance/bulk', { lessonSlotId: lessons[1].lessonSlotId, recordDate: today, entries: [{ studentId: child1.studentId, status: 'LATE' }] })
    await drainNotifications()
  }
  const subjects = (await api('GET', `/api/subjects?schoolId=${SCHOOL_ID}&size=50`)).content
  heading('17.3. Yangi baho')
  adminDid(`${subjects[0].name} fanidan 5 baho qo'ydi`)
  await api('POST', '/api/grades', { studentId: child1.studentId, subjectId: subjects[0].id, gradeDate: today, score: 5, type: 'CURRENT' })
  await drainNotifications()
  heading('17.4. Past baho — alohida, mehribon ohangdagi xabar')
  adminDid(`${subjects[1].name} fanidan 2 baho qo'ydi`)
  await api('POST', '/api/grades', { studentId: child1.studentId, subjectId: subjects[1].id, gradeDate: today, score: 2, type: 'CURRENT', comment: 'Uy vazifasi bajarilmagan' })
  await drainNotifications()
  heading('17.5. Maktab e\'loni')
  adminDid('«Hamma uchun» e\'lon joyladi')
  await api('POST', '/api/announcements', { schoolId: SCHOOL_ID, title: 'Kuzgi sport musobaqasi', content: 'Shanba kuni soat 10:00 da maktab stadionida «Sog\'lom avlod» sport musobaqasi bo\'lib o\'tadi. Farzandingizga sport kiyimini olib kelishni unutmang!', audience: 'ALL', priority: 'HIGH' })
  await drainNotifications()
  heading('17.6. Ertangi dars jadvali, haftalik hisobot va tadbir eslatmasi')
  adminDid('rejalashtirilgan vazifalar ishga tushdi (odatda: har kuni 19:00, shanba 18:00, tadbirdan 1 kun oldin 18:00)')
  await api('POST', '/api/bot/mock/run-jobs')
  await drainNotifications(null, 5000)
  heading('17.7. Ota-onalarga umumiy xabar (broadcast)')
  adminDid('«Ota-onalarga xabar» sahifasidan butun maktabga xabar yubordi')
  await api('POST', '/api/broadcasts', { schoolId: SCHOOL_ID, audience: 'ALL', text: 'Hurmatli ota-onalar! 7-oktabr kuni soat 18:00 da maktab majlislar zalida umumiy ota-onalar yig\'ilishi bo\'lib o\'tadi. Ishtirokingizni so\'raymiz.' })
  await drainNotifications()

  await api('PUT', `/api/notifications/settings?schoolId=${SCHOOL_ID}`, schoolSettings)
  fs.writeFileSync(path.join(DOCS, 'bot-demo.md'), md.join('\n') + '\n')
  console.log(`docs/bot-demo.md yozildi: ${md.length} qator, ${imageNo} ta rasm`)
}

main().catch(e => {
  console.error(e)
  process.exit(1)
})
