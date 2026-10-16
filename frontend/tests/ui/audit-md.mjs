// docs/ui-system/AUDIT.md from audit.json and a scan of the source for raw sizes.
//   node tests/ui/audit-md.mjs
import { readFileSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { summarize, layoutRow } from './summarize.js'
import { findHardcoded } from './hardcoded.js'

const root = fileURLToPath(new URL('../../', import.meta.url))
const docs = new URL('../../../docs/ui-system/', import.meta.url)
const audit = JSON.parse(readFileSync(new URL('audit.json', docs), 'utf8'))
const esc = s => String(s).replace(/\|/g, '\\|')

let md = `# UI audit — oldingi holat\n\nOlingan: ${audit.takenAt.slice(0, 16).replace('T', ' ')}. ` +
  `O'lchovlar 1366×768 (light) da, \`getComputedStyle\` dan. Skrinshotlar: \`before/\` ` +
  `(1366×768, 1920×1080, 2560×1440, 390×844 — light; 1920×1080 — dark).\n\n` +
  `Belgilar: \`40px / r4 / solid\` = balandlik / burchak radiusi / chegara turi; \`×N\` — nechta element.\n\n`

md += `## 1. Forma maydonlari (.q-field)\n\n| Sahifa | Qiymatlar |\n|---|---|\n`
for (const [name, p] of Object.entries(audit.pages)) md += `| ${name} | ${esc(summarize(p).fields)} |\n`
md += `\n## 2. Tugmalar (.q-btn)\n\n| Sahifa | Qiymatlar |\n|---|---|\n`
for (const [name, p] of Object.entries(audit.pages)) md += `| ${name} | ${esc(summarize(p).buttons)} |\n`
md += `\n## 3. Kartochkalar (.q-card, .brand-card), chip'lar, ikonka-kvadratlar\n\n| Sahifa | Kartochkalar | Chip'lar | Ikonka-kvadrat / avatar |\n|---|---|---|---|\n`
for (const [name, p] of Object.entries(audit.pages)) {
  const s = summarize(p)
  md += `| ${name} | ${esc(s.cards)} | ${esc(s.chips)} | ${esc(s.tiles)} |\n`
}
md += `\n## 4. Sahifa kengligi\n\n\`kontent / mavjud\` — sahifa kontenti kengligi va sidebar'dan qolgan joy (px); foiz — qanchasi ishlatilgan; "bo'sh" — ishlatilmagan joy.\n\n` +
  `| Sahifa | 1366×768 | 1920×1080 | 2560×1440 | 390×844 |\n|---|---|---|---|---|\n`
for (const [name, p] of Object.entries(audit.pages)) md += `| ${name} | ${layoutRow(p).map(esc).join(' | ')} |\n`

const hits = findHardcoded(root)
const byRule = {}
for (const h of hits) byRule[h.rule] = (byRule[h.rule] || 0) + 1
md += `\n## 5. Kodda qattiq yozilgan qiymatlar\n\nJami: **${hits.length}** — ` +
  Object.entries(byRule).map(([k, v]) => `${k}: ${v}`).join(', ') + `.\n\n| Fayl:qator | Tur | Qiymat |\n|---|---|---|\n`
for (const h of hits) md += `| ${h.file}:${h.line} | ${h.rule} | \`${esc(h.text)}\` |\n`

const errors = Object.entries(audit.pages).filter(([, p]) => p.errors?.length)
if (errors.length) {
  md += `\n## 6. Sahifadagi JS xatolari\n\n`
  for (const [name, p] of errors) md += `- **${name}**: ${esc([...new Set(p.errors)].join('; '))}\n`
}
writeFileSync(new URL('AUDIT.md', docs), md)
console.log('AUDIT.md:', Object.keys(audit.pages).length, 'pages,', hits.length, 'hardcoded values')
