# Yagona dizayn tizimi — jarayon

Branch: `feature/ui-system` (worktree `.claude/worktrees/ui-system`). Push qilinmaydi.

Tekshiruv muhiti (sessiya uzilsa qayta ko'tarish):
- backend: `maktab_tg_test` bazasida, 8081-port (`java -jar backend.jar --server.port=8081`, `CORS_ALLOWED_ORIGINS=http://localhost:9100`, `TELEGRAM_ENABLED=false`);
- foydalanuvchi: `ui_audit` (ADMIN, maktabsiz) — parol `UI_PASS` muhit o'zgaruvchisida;
- frontend: `QCLI_API_BASE_URL=http://localhost:8081 npx quasar build`, so'ng `dist/spa/index.html` CSP'sida 8080 → 8081, `npx vite preview --outDir dist/spa --port 9100`;
- audit: `node tests/ui/audit.mjs before|after`, `node tests/ui/audit-md.mjs`.

## 0. Audit
- ✅ Playwright: 38 sahifa × 1366/1920/2560/390 (light) + 1920 (dark) → `before/`
- ✅ `.q-field/.q-btn/.q-card/.q-chip` o'lchamlari → `audit.json`, `AUDIT.md`
- ✅ Kontent kengligi / mavjud joy / bo'sh px → `AUDIT.md` §4
- ✅ Qattiq yozilgan max-width / radius / height / size → `AUDIT.md` §5
- ✅ Shu fayl

## 1. Tokenlar
- ✅ `src/css/tokens.scss` — radius, control-h, space, border (+ `controls.scss` — Quasar'ga qo'llanishi)
- ✅ Quasar SASS o'zgaruvchilari tokenlarga
- ✅ `IconTile.vue` (32/40/48)

## 2. Forma komponentlari
- ✅ AppInput, AppSelect, AppDateInput, AppButton (+ AppTimeInput)
- ✅ Lint qoidasi: `src/components/app/` dan tashqarida q-input/q-select/q-btn va QInput/QSelect/QBtn importi — xato (ESLint `eslint.config.js`)

## 3. Layout
- ✅ PageLayout: max-width yo'q, padding breakpoint bo'yicha
- ✅ Kartochkali sahifalar — auto-fill grid
- ✅ Jadval/kalendar sahifalari — ichki scroll
- ✅ Grafiklar — konteynerga moslashadi, 240/300/360
- ✅ Sidebar ≥1024 ochiq, <1024 drawer
- ✅ PageHeader bo'shliqlari --space-6

## 4. Hamma sahifalar
- ✅ Har sahifa va dialog yangi komponentlarga
- ✅ Qattiq qiymatlar grep'i bo'sh
- ✅ Dark/light tekshiruvi

## Tekshiruv
- ✅ lint + build; lint qoidasi isboti
- ✅ O'lcham testi (Playwright, 1440×900) — `npm test`
- ✅ Layout testi (1920, 2560, 390)
- ✅ `after/` skrinshotlari, ko'zdan tekshirish
- ✅ `REPORT.md`

## Holat

Hammasi bajarildi (2026-10-16). Natijalar: `REPORT.md`. Branch `feature/ui-system` — push qilinmagan, main'ga merge qilinmagan.
