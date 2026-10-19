# O'ngdan ochiladigan panel (SideDrawer): scroll tuzatildi + xavf paneli

Branch: `feature/drawer-scroll` (commit qilinmagan, push qilinmagan).

## Xato sababi

Quasar `q-dialog` ichki qatlamiga `pointer-events: none` beradi va hodisalarni faqat
o'zining to'g'ridan-to'g'ri `<div>` farzandlariga qaytaradi; eski `SidePanel` ning ildizi
`<section>` edi, shuning uchun sichqoncha g'ildiragi va touch hodisalari panel ichidagi
ro'yxatga emas, orqa fonga ketardi (ro'yxat ~14 qatorda qolib ketardi).

## Nima qilindi

### 1. Umumiy `SideDrawer`
- Ildiz — `<div>` (asosiy tuzatish), `height: 100vh / 100dvh`, `display: flex; flex-direction: column; overflow: hidden`.
- Sarlavha, filtrlar qatori (`#toolbar`) va pastki qator (`#footer`) — `flex: 0 0 auto`, ular joyida qotib turadi; faqat ro'yxat (`flex: 1 1 auto; min-height: 0; overflow-y: auto`) aylanadi.
- `overscroll-behavior: contain`, `-webkit-overflow-scrolling: touch`, `touch-action: pan-y` — orqa sahifa qimirlamaydi, iOS "rubber band" o'tmaydi.
- Ingichka scrollbar — `--scrollbar-thumb`, `--scrollbar-thumb-hover`, `--scrollbar-w` tokenlari (dark va light).
- 390px da to'liq ekran.
- Ochilganda fokus ro'yxatga o'tadi (PageDown/PageUp/strelkalar ishlaydi), Esc yopadi, fokus ochgan tugmaga qaytadi.
- `persistent` — forma uchun (tashqariga bosish yopmaydi).

Shu komponentga o'tkazilgan panellar:
- Bosh sahifa: **Xavf ostidagi o'quvchilar**, **E'tibor talab qiladi** (Signals).
- CrudPage formasi — **Dars jadvali** (`lesson-slots`), **Taqvim** (`calendar-events`), **Xonalar** (`rooms`) va qolgan barcha CRUD bo'limlari. "Saqlash / Bekor qilish" pastki qatorda qotib turadi (`form="crud-form"`).
- Kutubxona statistikasi: ota-ona paneli.

### 2. "Xavf ostidagi o'quvchilar" paneli (`RiskDrawer`, `RiskRow`)
- Sticky qator: qidiruv (ism, 250 ms debounce) · sinf · saralash (xavf balli / kelmagan kunlar / baho pasayishi) · darajalar: Yuqori ≥70 / O'rta 50–69 / Past <50, har birida soni.
- "76 tadan 12 tasi" — natija soni.
- 50 dan ko'p bo'lsa `QVirtualScroll`.
- Qatorda sabab chiplari ("4 kun kelmagan", "3.5 → 2.7", "2 ta «2»"), halqa o'ngda; rang: ≥70 qizil, 50–69 to'q sariq, <50 sariq.
- Qator bosilsa profil; hover da: qo'ng'iroq (`tel:`, VIEWER ga telefon berilmaydi), Telegram, profil.
- Pastda "Excel ga yuklab olish" — joriy filtr bilan (`GET /api/dashboard/risk/export`).
- Bo'sh natija: "Bu filtr bo'yicha o'quvchi topilmadi" + "Filtrni tozalash".

## Tekshiruv

| | Natija |
|---|---|
| `npm run lint` | ✅ xatosiz, hech bir faylni o'zgartirmadi |
| `npm run build` (`quasar build`) | ✅ |
| `HomeDashboardIntegrationTest` | ✅ 10/10 |
| Playwright `tests/ui/drawer.spec.js` | ⏳ ishga tushmadi: `ui_audit` paroli (`UI_PASS`) yo'q — login 401 |
| Skrinshotlar | ⏳ shu test bilan olinadi (`risk panel screenshots`) |

Playwright testlari (`tests/ui/drawer.spec.js`):
- xavf paneli 1440×900 va 390×844, dark va light: g'ildirak bilan oxirgi o'quvchigacha (`toBeVisible`), orqa sahifa `scrollY` o'zgarmaydi, sarlavha va filtrlar ko'rinadi;
- 390 da touch swipe bilan oxirgi qatorga;
- qidiruv va sinf filtri natija sonini o'zgartiradi, bo'sh holat, halqa ranglari;
- Esc yopadi, fokus tugmaga qaytadi, PageDown ro'yxatni aylantiradi;
- E'tibor talab qiladi — har bir signal paneli;
- Dars jadvali, Taqvim, Xonalar, O'quvchilar, Xodimlar formalari — har biri alohida test;
- kutubxona ota-ona paneli;
- skrinshotlar: `docs/images/dashboard/risk-drawer-top.png`, `risk-drawer-bottom.png`, `risk-drawer-mobile.png`.

Ishga tushirish (backend 8081, frontend 9100):

```
cd frontend
$env:UI_PASS='...'; npx playwright test tests/ui/drawer.spec.js
```

## O'zgargan fayllar

Frontend:
- `src/components/dashboard/SideDrawer.vue` (yangi, eski `SidePanel.vue` o'rniga)
- `src/components/dashboard/v3/RiskDrawer.vue` (yangi)
- `src/components/dashboard/v3/RiskRow.vue` (yangi)
- `src/components/dashboard/v3/RiskStudents.vue`
- `src/components/dashboard/v3/Signals.vue`
- `src/components/dashboard/ScoreRing.vue`
- `src/pages/CrudPage.vue`
- `src/pages/LibraryStatsPage.vue`
- `src/css/tokens.scss`
- `tests/ui/session.js`, `tests/ui/drawer.spec.js` (yangi)

Backend:
- `controller/DashboardController.java` — `/risk` 2000 tagacha, `/risk/export`
- `controller/RiskFilter.java` (yangi) — eksport filtri
- `service/dashboard/HomeDashboardService.java` — `RiskRow` ga ota-ona telefoni va Telegram
