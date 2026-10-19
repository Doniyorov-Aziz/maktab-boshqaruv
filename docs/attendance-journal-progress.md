# Davomat olish va Baholar jurnali — v2

Branch: `feature/attendance-journal` (worktree `.claude/worktrees/attendance-journal`, `main` fb3ac3c dan). **Push qilinmagan.**
Sessiya uzilsa — ❌ bandlardan davom eting. **Backend testlari faqat `DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test` bilan.**

## Reja
1. Audit va «oldin» rasmlari.
2. Xatolar 1.1–1.5.
3. Backend: `/api/pages/attendance/*`, `/api/pages/journal/*`, testlar.
4. Davomat sahifasi.
5. Jurnal sahifasi.
6. Playwright, «keyin» rasmlari, hisobot.

## Holat

### 0. Audit
- ✅ Sabablar: 1.1 — `q-btn-toggle` tugmalari orasida bo'shliq va chegara yo'q edi; 1.2 — «Dars» ro'yxati sinfning BUTUN haftasini ko'rsatardi (hafta kuni nomi bo'yicha alifbo tartibida); 1.4 — parallel tugmalari `AppButton` primary (hammasi to'ldirilgan) edi; 1.5 — fan faqat o'qituvchining joriy darsidan olinardi, admin uchun bo'sh qolardi.
- ✅ «Oldin» rasmlari: `docs/images/attendance-journal/before-*.png` (8 ta).

### 1. Xatolar
- ✅ 1.1 `StatusSegment`: 4 ta alohida tugma, oraliq `--space-1` (4px), balandlik `--control-h-sm`, radius `--radius-sm`; 1024px dan torda faqat ikonka. Test: oraliq ≥ 4px, balandlik 32px.
- ✅ 1.2 Darslar TANLANGAN sana hafta kuni bo'yicha (`/api/pages/attendance/day`); saqlashda ham tekshiriladi (boshqa kunning darsiga yozib bo'lmaydi). Backend testi: har bir `DayOfWeek` → «Dushanba»…«Shanba», dushanba 3 dars, chorshanba 1, yakshanba — dam olish. Brauzer testi: dushanba 5, shanba 4, yakshanba o'tkazib yuboriladi.
- ✅ 1.3 Eski «Sinf» select o'rniga compact `ClassPicker`; vaqt chizig'ida faqat tanlangan sinfning darslari.
- ✅ 1.4 `ClassPicker` komponent ichida tuzatildi (hamma sahifaga ta'sir qiladi): tanlangan — brand rang bilan to'ldirilgan, oq matn; tanlanmagan — shaffof, xira chegara; o'quvchisiz parallel — xira (0.35) va bosilmaydi. Davomatda holat nuqtalari: yashil FULL / sariq PARTIAL / kulrang NONE.
- ✅ 1.5 Fan avtomatik: (a) hozirgi yoki oxirgi boshlangan dars fani → (b) oxirgi tanlangan (localStorage) → (c) sinfning birinchi fani. Sinf tanlanganda bo'sh holat yo'q (faqat sinfda o'quvchi bo'lmasa). Davomatda dars ham avtomatik: bugun — hozirgi/oxirgi boshlangan, boshqa kun — birinchi olinmagani.

### 2. Davomat
- ✅ 2.1 Sarlavha «1-maktab · 1-A · Dushanba, 19-oktabr»; ◀ [19-okt, Dushanba] ▶ + «Bugun» (+ kalendar), yakshanba va kelajak o'tkazib yuboriladi; compact ClassPicker nuqtalar bilan.
- ✅ 2.2 `LessonTimeline`: proporsional segmentlar (raqam, fan rangi, vaqt, o'qituvchi), ✓ / ◐ / 🤖 / ⏳, HOZIR porlaydi «Hozir · N daq qoldi», keyingilari xira, tanaffus — bo'shliq, hozirgi daqiqa chizig'i; «Dars» select olib tashlandi; tanlangan dars telefonda ko'rinishga suriladi.
- ✅ 2.3 `AttendanceSummary`: halqa (keldi %), 4 chip, «21/21 belgilandi» chiziq bilan, «Saqlangach N ta ota-onaga xabar boradi» / demo matni, [✓ Hammasi keldi] (5 s bekor qilish), [🤖 Avtomatikni tasdiqlash (N)], qidiruv.
- ✅ 2.4 `RosterRow`: № · avatar · ism, ostida ota-ona va telefon (VIEWER uchun telefonsiz); 10 kunlik nuqtalar; «N kun ketma-ket kelmagan», «Bu hafta N marta kechikdi»; 4 tugma; Kechikdi oynasi 5/10/15/20+ daq; Sababli oynasi kasallik/oilaviy/boshqa + izoh; 🤖 belgisi; 3px chap chiziq, kelmagan/kechikkan qatorlar och fon.
- ✅ 2.5 Kartochkalar: SegTabs «Ro'yxat | Kartochkalar»; bosish Keldi→Kelmadi→Kechikdi→Sababli; uzoq bosish / o'ng tugma — menyu.
- ✅ 2.6 Klaviatura (pastda), Shift-bosish oraliq, avtosaqlash 1.5 s (faqat o'zgargan qatorlar), sticky saqlash paneli («Saqlandi ✓ 10:21» / «Saqlanmoqda…» / «Xato — qayta urinish»), saqlanmagan o'zgarish bilan chiqishda ogohlantirish (route + beforeunload).
- ✅ 2.7 Bo'sh holatlar: «Bu kuni dars yo'q», «😴 Dam olish kuni», «Sinfda o'quvchi yo'q»; skeletonlar.

### 3. Jurnal
- ✅ 3.1 Sarlavha «1-A · Ingliz tili · Oktabr 2026»; ◀ oy ▶ + «Bugun» + [Eksport ▾ Excel / PDF]; compact ClassPicker + fan chiplari (select olib tashlandi).
- ✅ 3.2 `JournalPanel` (≤120px, 1920 va 1366 da bir qator): sinf rahbari va fan o'qituvchisi (avatar, telefon + nusxa), hozirgi dars holati, 4 mini KPI (o'rtacha, o'zlashtirish %, sifat %, soni) o'tgan oyga DeltaPill va sparkline bilan; dars vaqtida «Shu darsga baho qo'yish», aks holda «Bugungi ustunga o'tish».
- ✅ 3.3 `GradeTable`: sticky ism ustuni (240px) o'rtacha pill va trend o'qi bilan; kun ustunlari 44px, bugun ajratilgan, yakshanba chiziqli, kelajak xira; 5/4/3/2 rangli chiplar, nazorat/chorak — halqa, DEMO_SIM — binafsha nuqta; bo'sh katakda xira «+»; o'ngda sticky o'rtacha / soni / taqsimot; pastda sticky sinf o'rtachasi; kesishgan hover (jadval qayta chizilmaydi — bitta `<style>` yangilanadi); issiqlik xaritasi.
- ✅ 3.4 Tez kiritish: 2–5 baho qo'yib PASTGA o'tadi; strelkalar; Delete — 5 s bekor qilish; Enter — oyna (katta 2–5, tur, izoh, o'chirish, «bu kuni yo'q edi» belgisi); Shift+strelka / Shift-bosish blok, Ctrl-bosish alohida kataklar → bitta baho hammasiga; ustun sarlavhasi — «Butun sinfga baho» paneli (kelmaganlar xira va belgili, «belgilanmaganlarga 5»); yakshanba / kelajak — toast.
- ✅ 3.5 O'quvchi paneli: yil o'rtachasi, 30 kunlik davomat %, xavf, 30 kunlik o'rtacha chizig'i, oylar bo'yicha baholar, ota-ona kontakti, [Ota-onaga xabar] (sms:), [Profil].

### 4. Umumiy
- ✅ Faqat tokenlar (`hardcoded.js` tekshiruvi — 0 ta topilma), dark va light, 1366 sig'adi (gorizontal scroll yo'q — test).
- ✅ 390px: davomat kartochkalarda ochiladi; jurnal yonga suriladi, ism ustuni sticky, sahifa suriladi emas — test.
- ✅ Belgilash va baho qo'yish EDITOR+; VIEWER boshqaruvlarni ko'rmaydi (testlar).
- ✅ 40 × 31 test: render < 6 s (mock, butun sahifa), strelka < 100 ms. Endpointlar: `day` 1 SQL, `roster` 3 SQL, `summary`/`day-attendance` 1 SQL, N+1 yo'q.

## Tekshiruv
- ✅ `npm run lint` va `npm run lint:check` toza, `npm run build` — xato yo'q.
- ✅ Playwright (fixturelar test bazasidan): `aj-logic.spec.js` 13/13, `aj.spec.js` 25/25 (davomat 13, jurnal 12: 1920 / 1366 / 390, dark / light, ADMIN / VIEWER, konsol xatolari yo'q), `pages-v2-logic` ham o'tdi.
- ✅ Backend: `AttendanceJournalIntegrationTest` 5/5 (shu jumladan «tanlangan sana — faqat shu hafta kuni darslari»).
- ✅ `./gradlew build` (test bazasi bilan): BUILD SUCCESSFUL, 306 test, 0 xato, 2 o'tkazilgan (fixture dumplar — faqat `AJ_FIXTURES` bilan).
- ✅ «Keyin» rasmlari `docs/images/attendance-journal/after-*.png` (11 ta), har biri ko'rib chiqildi. Topilgan va tuzatilgan: popoverlar (0, 1216) da ochilardi — element reaktiv proxy bo'lib qolgandi va menyu nishonsiz yaratilardi (`markRaw` + nishon bilan mount); jadval pastki qatori ekrandan chiqib ketardi (`--gb-chrome-h`); «−0.0» delta (bir xonagacha yaxlitlash); telefonda xulosa kartochkasi siqilgan edi (ustma-ust); telefonda hozirgi dars ko'rinmasdi (avtomatik surish); Shift+strelka blokining boshi (0,0) dan olinardi.

## Klaviatura
| Sahifa | Tugma | Ish |
|---|---|---|
| Davomat (ro'yxat) | ↑ / ↓ | qator |
| | 1 / 2 / 3 / 4 | Keldi / Kelmadi / Kechikdi / Sababli (tanlangan bo'lsa — hammasiga) |
| | Enter | keyingi qator |
| | Shift+↑↓, Shift-bosish | oraliq tanlash; Ctrl-bosish — bittalab |
| | Esc | tanlovni bekor qilish |
| Davomat (kartochka) | bosish / uzoq bosish, o'ng tugma | keyingi holat / menyu |
| Jurnal | ← ↑ → ↓ | katak |
| | 2 / 3 / 4 / 5 | baho va pastga (blok bo'lsa — hammasiga) |
| | Enter / Space | baho oynasi (unda 2–5, Esc) |
| | Delete / Backspace | oxirgi bahoni o'chirish (5 s bekor qilish) |
| | Shift+strelka, Shift-bosish | blok; Ctrl-bosish — bittalab; Esc — bekor |
| | ustun sarlavhasi | butun sinfga baho |

## O'zgargan fayllar
Backend: `controller/AttendanceJournalController.java` (yangi), `service/pages/AttendancePageService.java` (yangi), `service/pages/JournalPageService.java` (yangi), `service/attendance/AttendanceWriter.java` (`late_minutes`), `service/TimetablePdfService.java` (`font` public); testlar `AttendanceJournalIntegrationTest`, `AttendanceJournalFixtureDump` (faqat `AJ_FIXTURES` va test bazasi).

Frontend: `pages/AttendancePage.vue`, `pages/GradebookPage.vue` (qayta yozildi); `components/ClassPicker.vue`; `components/attendance/` (attendance.js, StatusSegment, LessonTimeline, AttendanceSummary, RosterRow, RosterCards, MarkPopover); `components/journal/` (journal.js, JournalPanel, GradeTable, GradePopover, ColumnDrawer, StudentDrawer); `css/tokens.scss` (davomat va jurnal o'lchamlari); testlar `tests/ui/aj-mock.js`, `aj-before.spec.js`, `aj-shots.spec.js`, `aj-logic.spec.js`, `aj.spec.js`.

Eslatma: `components/ClassOverviewPanel.vue` endi hech qayerda ishlatilmaydi (jurnal paneli uning o'rnini oldi) — so'ralmagani uchun o'chirilmadi.

## Ishga tushirish
```
# fixturelar (faqat test bazasi)
DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test AJ_FIXTURES=…/aj.json ./gradlew test --tests '*AttendanceJournalFixtureDump'
# frontend
cd frontend && npm run build && npx vite preview --outDir dist/spa --port 9102
AJ_FIXTURES=…/aj.json UI_BASE=http://localhost:9102 npx playwright test tests/ui/aj-logic.spec.js tests/ui/aj.spec.js
AJ_FIXTURES=…/aj.json UI_BASE=http://localhost:9102 npx playwright test tests/ui/aj-shots.spec.js   # rasmlar
```
