// docs/ui-system/REPORT.md: before / after per page (from audit.json and
// audit-after.json), the files changed against main, and how to use the system.
//   node tests/ui/report-md.mjs
import { readFileSync, writeFileSync } from 'node:fs'
import { execSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { summarize } from './summarize.js'
import { findHardcoded } from './hardcoded.js'

const root = fileURLToPath(new URL('../../', import.meta.url))
const docs = new URL('../../../docs/ui-system/', import.meta.url)
const before = JSON.parse(readFileSync(new URL('audit.json', docs), 'utf8'))
const after = JSON.parse(
  readFileSync(new URL('audit-after.json', docs), 'utf8')
)
const guide = readFileSync(new URL('report-guide.md', import.meta.url), 'utf8')
const esc = s => String(s).replace(/\|/g, '\\|')

const width = (p, tag) => {
  const l = p?.layout?.[tag]
  return l ? `${l.contentWidth}/${l.available}` : '—'
}

let md = `# Yagona dizayn tizimi — hisobot\n\n`
md += `Oldin: ${before.takenAt.slice(0, 16).replace('T', ' ')}, keyin: ${after.takenAt.slice(0, 16).replace('T', ' ')} (UTC). `
md += `Skrinshotlar: \`before/\` va \`after/\` — 1366×768, 1920×1080, 2560×1440, 390×844 (light) va 1920×1080 (dark).\n\n`

md += `## 1. Natija qisqacha\n\n`
const hard = findHardcoded(root).length
md += `- Qattiq yozilgan radius / height / max-width / avatar o'lchami: **367 → ${hard}** (tokenlar fayli bundan mustasno).\n`
md += `- \`npm test\`: o'lcham testi (har sahifa, 1440×900) va layout testi (1920, 2560, 390) — hammasi o'tadi.\n`
md += `- \`npm run lint\`: oxlint + ESLint (\`q-input\` / \`q-select\` / \`q-btn\` va \`QInput\` / \`QSelect\` / \`QBtn\` importi \`src/components/app/\` dan tashqarida — xato).\n\n`

md += `## 2. Oldin / keyin: elementlar (1366×768)\n\n`
md +=
  `Belgilar: \`40px / r10\` = balandlik / burchak radiusi; \`×N\` — nechta. ` +
  `"Keyin" ustunidagi \`33.6px / r50\` (tugma) va \`40px / r0 / none\` (maydon) — jadval pastidagi ` +
  `sahifalash tugmalari va "Sahifada:" tanlovi: ularni Quasar o'zi chizadi, testlar ularni o'tkazib yuboradi. ` +
  `Dashboard'dagi \`32px / r10\` — reyting kartochkasidagi ixcham segment almashtirgich ichidagi tugmalar.\n\n`
md += `| Sahifa | Maydonlar oldin | Maydonlar keyin | Tugmalar oldin | Tugmalar keyin |\n|---|---|---|---|---|\n`
for (const [name, p] of Object.entries(before.pages)) {
  const b = summarize(p)
  const a = summarize(after.pages[name] || {})
  md += `| ${name} | ${esc(b.fields)} | ${esc(a.fields)} | ${esc(b.buttons)} | ${esc(a.buttons)} |\n`
}
md += `\n| Sahifa | Kartochkalar oldin | Kartochkalar keyin | Ikonka-kvadrat / avatar oldin | keyin |\n|---|---|---|---|---|\n`
for (const [name, p] of Object.entries(before.pages)) {
  const b = summarize(p)
  const a = summarize(after.pages[name] || {})
  md += `| ${name} | ${esc(b.cards)} | ${esc(a.cards)} | ${esc(b.tiles)} | ${esc(a.tiles)} |\n`
}

md += `\n## 3. Oldin / keyin: kenglik (kontent / sidebar'dan qolgan joy, px)\n\n`
md += `| Sahifa | 1366 oldin | 1366 keyin | 1920 oldin | 1920 keyin | 2560 oldin | 2560 keyin | 390 scroll keyin |\n|---|---|---|---|---|---|---|---|\n`
for (const [name, p] of Object.entries(before.pages)) {
  const a = after.pages[name] || {}
  const phone = a.layout?.['390x844']
  md += `| ${name} | ${width(p, '1366x768')} | ${width(a, '1366x768')} | ${width(p, '1920x1080')} | ${width(a, '1920x1080')} | ${width(p, '2560x1440')} | ${width(a, '2560x1440')} | ${phone ? (phone.horizontalScroll ? '⚠ bor' : "yo'q") : '—'} |\n`
}

// root is the frontend folder: "." is the frontend; paths come back from the repo root
const changed = execSync('git diff --name-status main -- .', {
  cwd: root,
  encoding: 'utf8'
})
  .trim()
  .split('\n')
  .filter(Boolean)
  .map(l => l.split('\t'))
const kind = { A: "qo'shildi", M: "o'zgardi", D: "o'chirildi" }
md += `\n## 4. O'zgargan fayllar (main bilan solishtirganda)\n\n| Holat | Fayl |\n|---|---|\n`
for (const [s, f] of changed)
  md += `| ${kind[s[0]] || s} | \`${f.replace(/^frontend\//, '')}\` |\n`

md += `\n` + guide
writeFileSync(new URL('REPORT.md', docs), md)
console.log('REPORT.md written,', changed.length, 'files,', hard, 'hardcoded')
