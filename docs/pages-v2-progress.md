# Sahifalar v2: Dars jadvali · Taqvim · Sinflar · Xonalar

Branch: `feature/pages-v2` (worktree `.claude/worktrees/pages-v2`, `main` 40eb701 dan). **Push qilinmagan.**
Sessiya uzilsa — quyidagi ❌ bandlardan davom eting.

> ⚠️ **Muhim (2026-10-19):** backend testlari bir necha marta `DB_URL` siz ishga tushdi va **jonli `maktab_db`** ga ulandi
> (`application.properties` standarti). Natijada jonli bazada: 2 ta yangi «Faollar zali» xonasi (id 61, 62, `ASSEMBLY_HALL` —
> jonli eski jar bu turni bilmaydi), barcha xonalarda yangi `equipment/status/computers` ustunlari to'ldirildi, yangi
> jadvallar/ustunlar qo'shildi va 2 ta CHECK cheklov olib tashlandi. Test maktablari o'z-o'zidan o'chirilgan (qoldiq yo'q).
> Tuzatish SQL (foydalanuvchi ruxsati bilan): «Jonli bazani tiklash» bo'limiga qarang. Zaxira:
> `maktab-live/backups/maktab_db-before-test-cleanup-20261019-1501.dump`. Endi testlar faqat
> `DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test` bilan ishga tushiriladi.

## Holat

### 0. Umumiy
- ✅ Dashboard v3 komponentlari qayta ishlatildi: KpiCard (+ `compact`, `clickable`), ChartCard, Sparkline, DeltaPill, SegTabs (= SegmentedTabs), SideDrawer, IconTile, PageLayout (= PageHeader), chartTheme, tokenlar
- ✅ Yangi umumiy komponentlar: `EmptyState`, `AvatarInitials`, `SubjectChip`, `NowLine`, `HeatTile`, `SignalRow`, `confirmUndoToast` (ConfirmUndoToast, `composables/useUndoToast.js`), `ringArc`
- ✅ Fan ranglari: `Subject.color` + 12 rangli palitra (`--subj-1..12`, dark/light), `subjectId` bo'yicha deterministik; 4 sahifada va PDF da bir xil; «Fanlar» formasida rang tanlash
- ✅ Holatlar: skeleton (shimmer), bo'sh holat (neytral ikonka + keyingi qadam; ✅ faqat «muammo yo'q»), xato («Qayta urinish»)
- ✅ Harakat: kirish animatsiyasi, count-up, hover ko'tarilish, drawer; `prefers-reduced-motion` da o'chiq
- ✅ 1366 / 1920 / 2560 / 390: gorizontal scroll yo'q, konsolda xato yo'q (testda tekshiriladi: 1366, 1920, 390 × dark/light)
- ✅ Hamma so'rov `schoolId` bilan (school-scope guard + boshqa id lar serverda tekshiriladi), Asia/Tashkent, og'ir hisoblar keshda 90 s
- ✅ Ruxsat: ko'rish VIEWER+, tahrirlash EDITOR+, obuna havolalari ADMIN; VIEWER ga telefon raqamlari berilmaydi

### 1. Dars jadvali
- ✅ 1.1 HERO: haftalik darslar · o'qituvchi yuklamasi (soat/hafta) · xonalar bandligi % · konfliktlar (0 — yashil, bor — qizil, bosilsa ro'yxat)
- ✅ 1.2 Ko'rinishlar (URL da): Sinf (1366×768 da ham bitta ekranda, ichki scroll yo'q) · O'qituvchi (soat, bo'sh «oyna»lar) · Xona (bandlik %) · Bugun (sinflar × vaqt, qizil `NowLine` har daqiqa) · Yuklama
- ✅ 1.3 Dars kartochkasi: fan qalin, 4px rang chizig'i, yengil fon, avatar + familiya, xona chipi, «Hozir · N daq qoldi» glow, o'tganlar xira, tanaffus qatorlari («☕ 10 daq», katta tanaffus ajralgan), bugungi ustun «Bugun»
- ✅ 1.4 Fan legendasi: chip bosilsa shu fan yorqin, qolganlar xira
- ✅ 1.5 Drawer: fan, sinf, vaqt, o'qituvchi (telefon bosiladi), xona, bugungi davomat (✓ / 🤖 / ⏳), [Davomat olish] [Jurnal] [Tahrirlash]
- ✅ 1.6 Drag & drop: har katak jonli tekshiriladi (sinf / o'qituvchi / xona band — qizil + sabab, tashlab bo'lmaydi), 5 s «Bekor qilish», bo'sh katakda «+» (faqat bo'sh o'qituvchi va xonalar), backend 409 aniq xabar bilan
- ✅ 1.7 O'rinbosar: shu fan → yaqin fan → boshqa, yuklama bo'yicha; faqat SHU SANA (`lesson_substitution`), jadvalda «o'rinbosar» belgisi
- ✅ 1.8 Yuklama: issiqlik xaritasi (o'qituvchi × kun) + haftalik soat bar
- ✅ 1.9 PDF (A4 yotiq, bitta sahifa, endi fan ranglari sahifalar bilan bir xil) va «Barcha sinflar»; chop etish

### 2. Taqvim
- ✅ 2.1 O'quv yili chizig'i (choraklar, ta'tillar, «Siz shu yerdasiz», «I chorak: 77% o'tdi · 12 ish kuni qoldi») + yaqin 3 tadbir; KPI: keyingi tadbir (teskari sanoq), shu oy, keyingi imtihon, ta'tilgacha
- ✅ 2.2 Ko'rinishlar: Oy · Hafta · Ro'yxat · Yil (12 mini oy, nuqtalar)
- ✅ 2.3 Oy: bugun halqa, yakshanba/bayram yengil chiziqli, ko'p kunlik tadbir — kataklar bo'ylab tasma, pill lar, «+N» popover, o'tgan ish kunlarida davomat nuqtasi
- ✅ 2.4 Turlar (rang + ikonka) va filtr chiplari (sonlari bilan); yangi tur «Maktab tadbiri»
- ✅ 2.5 Bo'sh kunga bosish → tez yaratish (nom, tur, vaqt, xona/joy, kimlar uchun), sudrab ko'chirish + undo (oy ko'rinishida), tasmani chetidan cho'zish, xona bandligi tekshiriladi, «Ota-onalarga xabar» (yaratilganda + 1 kun oldin, tanlangan sinflarga; demo rejimda faqat log), takrorlanish (har hafta / oy)
- ✅ 2.6 Kun drawer: tadbirlar, darslar soni, davomat va baholar soni, o'quvchilar tug'ilgan kuni
- ❌ 2.6 Xodimlar tug'ilgan kuni — `Employee` da tug'ilgan sana maydoni yo'q
- ✅ 2.7 `.ics` (RFC 5545, Asia/Tashkent) va maxfiy obuna havolasi (bekor qilinadi)

### 3. Sinflar
- ✅ 3.1 HERO: sinflar · o'quvchilar · o'rtacha hajm · bugungi davomat · eng yaxshi sinf
- ✅ 3.2 Kartochkalar parallel bo'yicha guruhlangan: gradient avatar, rahbar, o'quvchilar, bugun halqasi, sifat/o'zlashtirish + delta, Telegram %, 20 kun sparkline, xavf badge, hover
- ❌ 3.2 O'g'il/qiz bar — `Student` da jins maydoni yo'q
- ✅ 3.3 Qidiruv, parallel, saralash (nom / davomat / sifat / xavf), Karta | Jadval
- ✅ 3.4 Solishtirish (2–3 sinf): ko'rsatkichlar, fanlar bo'yicha sifat (guruhlangan bar), davomat dinamikasi (bir grafikda)
- ✅ 3.5 Sinf sahifasi `/classes/:id`: hero + 4 KPI, tablar Umumiy / O'quvchilar / Jadval / Davomat / Ota-onalar / Xulq, amallar

### 4. Xonalar
- ✅ 4.1 `Room.type` (+ Faollar zali, Boshqa), `status` (FAOL / TA'MIRDA), `equipment`, `computers`; demo rejimda faqat bo'sh xonalarga qiymat (`RoomDetailsDemoFill`)
- ✅ 4.2 HERO: jami · hozir band (N/M, %) · haftalik bandlik · ta'mirda
- ✅ 4.3 Jonli xarita: bino → qavat, plitkalar (band — sinf/fan/o'qituvchi/«11:05 gacha» progress; bo'sh — «13:30 gacha bo'sh» + jihozlar; ta'mirda — chiziqli), har daqiqa yangilanadi, kun + dars sirpanchig'i
- ✅ 4.4 Bo'sh xona topish: kun, darslar, sig'im, tur, jihozlar → moslik bo'yicha saralangan, [Band qilish]
- ✅ 4.5 `RoomBooking`: konflikt 409; taqvim tadbiri xona bilan bo'lsa avtomatik band qiladi
- ✅ 4.6 Xona drawer: tur ikonkasi, jihozlar, haftalik bandlik issiqlik xaritasi, bandlik %, sinflar va o'qituvchilar, yaqin bandlar
- ❌ 4.6 Xona rasmi (ixtiyoriy) — qilinmadi, tur ikonkasi ishlatiladi
- ✅ 4.7 Analitika: eng band 5, eng kam 5, tur bo'yicha

### 5. Bosh sahifa
- ✅ Salomatlik halqalari: endi `pathLength="100"` — har halqa aniq o'z qiymatigacha to'ladi (0.6% — deyarli bo'sh; 3% dan kichik qiymatda uchlari to'rtburchak). 4 halqa uchun test (hisob + brauzerda chizilgan piksellar). *Izoh: eski kodni statik chizganimda ham qisqa yoy chiqdi — xatoning aniq sababini qayta hosil qila olmadim; yangi usul har qanday holatda aniq.*
- ✅ Sinflar xaritasi: Y o'qi yozuvi uchun joy; nomlar 4 joyda sinab ko'riladi (o'ng, chap, tepa, past), sig'masa yashiriladi (hover tooltip da bor)
- ❌ Bosh sahifa o'zgarishlarini jonli ma'lumot bilan ko'rib chiqish — kirish paroli (UI_PASS) yo'q

## Tekshiruv

| | Natija |
|---|---|
| `./gradlew build` (testlar bilan, `DB_URL=…maktab_tg_test`) | ✅ 300 test, 0 xato |
| `npm run lint` && `npm run build` | ✅ |
| `PagesV2IntegrationTest` | ✅ 8/8: konfliktlar 409 (o'qituvchi, xona, sinf) aniq xabar bilan; o'rinbosar faqat bitta sanaga; bo'sh xona qidiruvi jadval va bandlarni hisobga oladi; tadbir ko'chirilganda xona qayta tekshiriladi; `.ics` RFC 5545; yangi tadbir → outbox (demo rejimda faqat log); boshqa maktab aralashmaydi; sinf kartochkasi formulalari |
| Playwright `pages-v2-logic.spec.js` | ✅ 7/7 (brauzersiz) |
| Playwright `pages-v2.spec.js` | ✅ 43/43 — 5 sahifa × 1366/1920/390 × dark/light (scroll, konsol); jadval 1366×768 da scrollsiz; NowLine; fan chipi; drag konflikt → qizil va tashlanmaydi; drop + undo; o'rinbosar oqimi; konflikt ro'yxati; taqvim 4 ko'rinish, ta'til tasmasi, bo'sh kunda yaratish, sudrash + undo; sinflar guruhi, saralash, solishtirish, sinf tablari; xonalar holati, qidiruv, band qilish 409 |
| Playwright `rings.spec.js` | ✅ 2/2 brauzersiz/loginsiz; ❌ 1 ta (uy sahifasi, real backend) — UI_PASS yo'q |
| Skrinshotlar `docs/images/pages-v2/` | ✅ 22 ta (9 × dark/light 1920 + 4 mobil 390) |

**Brauzer testlari qanday ishlaydi:** login paroli yo'qligi uchun sahifalar API si Playwright da `page.route` bilan
almashtiriladi: javoblar backendning o'zi test bazasidan yozgan JSON (`PagesFixtureDump`, faqat `maktab_tg_test` da
ishlaydi). Haqiqiy backendga hech narsa bormaydi. Ishga tushirish:

```
DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test PAGES_FIXTURES=C:\...\fixtures.json ./gradlew test --tests '*PagesFixtureDump'
cd frontend && npm run build && npx vite preview --outDir dist/spa --port 9101
PAGES_FIXTURES=C:\...\fixtures.json UI_BASE=http://localhost:9101 npx playwright test tests/ui/pages-v2*.spec.js tests/ui/rings.spec.js
```

### Skrinshotlarni ko'rib chiqish — «Bosh sahifa darajasidami? Nima hali oddiy ko'rinadi?»
- Dars jadvali — ha: bitta ekranda hafta, «Bugun» ko'rinishida qizil chiziq va hozirgi darslar porlashi. Tuzatildi: konflikt kartochkasi bo'sh edi → turlar bo'yicha son; sudrashda o'z sinfi band bo'lsa endi «sinfida dars bor» deyiladi.
- Taqvim — ha. Tuzatildi: yil chizig'i kartochkasi yarim bo'sh edi → yaqin 3 tadbir; bayram nomi ikki marta chiqardi; ikonka chiqmasdi; mini oylar tekis emasdi; mobil KPI da «6 kun qoldi» ikki qatorga tushardi.
- Sinflar — ha. Tuzatildi: har parallel alohida qatorda 2 tadan → guruhlar ham to'rda (1920 da 4 tadan).
- Sinf sahifasi — ha. Tuzatildi: «Baholar taqsimoti» yarim bo'sh → 4 ta raqam.
- Xonalar — ha (asosiy «wow»). Tuzatildi: hero kartochkalari bo'sh joyli → ixcham; bo'sh xonada jihoz ikonkalari; mobilda 2 tadan.
- Hali oddiy: Hafta ko'rinishida drag yo'q (faqat Oy da); demo ma'lumotda 38–39 ta haqiqiy o'qituvchi konflikti bor (seeder ma'lumoti) — sahifa ularni to'g'ri qizil ko'rsatadi.

## Yangi endpointlar

| Endpoint | Rol |
|---|---|
| `GET /api/pages/timetable/week`, `/kpis`, `/day?date`, `/substitutes?lessonSlotId&date` | VIEWER+ |
| `POST /api/pages/timetable/substitutions`, `DELETE …/{id}` | EDITOR+ |
| `GET /api/pages/calendar/overview`, `/attendance?from&to`, `/day?date`, `/export.ics` | VIEWER+ |
| `GET/POST/DELETE /api/pages/calendar/feeds` | ADMIN |
| `GET /api/public/calendar/{token}.ics` | ochiq (maxfiy token) |
| `GET /api/pages/classes/overview`, `/compare?ids`, `/{id}`, `/{id}/month?date` | VIEWER+ |
| `GET /api/pages/rooms/live?date&time`, `/find`, `/{id}/detail`, `/analytics`, `/bookings?from&to` | VIEWER+ |
| `POST /api/pages/rooms/bookings`, `DELETE …/{id}` | EDITOR+ |
| `/api/calendar-events` — yangi maydonlar: `startTime/endTime`, `roomId`, `location`, `classIds`, `notifyParents`, `recurrence`, `recurrenceUntil` | avvalgidek |

## Yangi entity va maydonlar
- `RoomBooking` (`room_booking`), `LessonSubstitution` (`lesson_substitution`), `CalendarFeedToken` (`calendar_feed_token`)
- `Subject.color`; `Room.status/equipment/computers`, `RoomType.ASSEMBLY_HALL/OTHER`; `CalendarEvent.startTime/endTime/room/location/classIds/notifyParents/seriesId`, `CalendarEventType.SCHOOL_EVENT`
- Hammasi nullable — eski yozuvlar o'zgarmaydi (ddl-auto=update). `room_type_check`, `calendar_event_type_check` cheklovlari yangi turlar uchun olib tashlanadi.

## Qisqa qo'llanma
- **Dars jadvali:** tepada 4 raqam (konfliktlar bosilsa ro'yxat). «Sinf / O'qituvchi / Xona / Bugun / Yuklama». Darsga bosing — drawer (davomat, jurnal, o'rinbosar). Tahrirlovchi: darsni sudrang (qizil katakka tushmaydi), 5 s ichida «Bekor qilish»; bo'sh katakda «+». Pastdagi fan chipi — shu fanni ajratadi.
- **Taqvim:** yil chizig'i, 4 raqam. Oy/Hafta/Ro'yxat/Yil. Bo'sh kunga bosing — tadbir; sudrang — boshqa kunga; tasmani o'ng chetidan cho'zing. Kun raqamiga bosing — kun xulosasi. «Kalendarga qo'shish» — .ics va obuna havolasi.
- **Sinflar:** qidiruv, parallel, saralash, Karta/Jadval. 2–3 sinf katakchasini belgilang → «Solishtirish». Kartochka — sinf sahifasi.
- **Xonalar:** jonli xarita (har daqiqa), «Tanlash» — istalgan kun va dars. Holat chiplari filtrlaydi. «Bo'sh xona topish» → «Band qilish». Xonaga bosing — haftasi va bandlari.

## Jonli bazani tiklash (foydalanuvchi ruxsati kerak)
```sql
-- testlar qo'shgan 2 xona (hech narsaga bog'lanmagan) va bugun paydo bo'lgan ustunlardagi qiymatlar
DELETE FROM room WHERE id IN (61, 62) AND room_number = 'Faollar zali' AND type = 'ASSEMBLY_HALL';
UPDATE room SET equipment = NULL, status = NULL, computers = NULL;
```
Yangi jadval/ustunlar va olib tashlangan 2 ta CHECK cheklov eski kodga zarar qilmaydi (keyingi deploy da baribir kerak).
