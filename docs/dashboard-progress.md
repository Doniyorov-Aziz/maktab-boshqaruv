# Bosh sahifa (qaror markazi) va davomat avtomatikasi

Branch: `feature/dashboard-v2` (worktree `.claude/worktrees/dashboard`). Push, merge va deploy qilinmagan.

Tekshiruv muhiti: backend 8081 (`maktab_tg_test`), frontend build 9100 — `docs/ui-system/REPORT.md` §9 dagidek.

## 0. Audit
- ✅ Eski vidjetlar ko'rib chiqildi: har biri qaysi savolga javob beradi, qaysi endpoint'dan oladi, qarorga yordam beradimi
- ✅ Davomat modeli, saqlash, outbox, qo'ng'iroq jadvali
- ✅ "Oldin" skrinshotlari: `docs/images/dashboard/before-{1366,1920,390}-{light,dark}.png`

## 1. Hozirgi xatolar
- ✅ 1.1 Bugungi davomat 0% edi → foiz faqat davomati olingan sinflardan hisoblanadi; hech bir sinfda olinmagan bo'lsa `null` qaytadi va kartochkada "— Hali olinmagan" yoziladi; dars bo'lmagan kuni — "Bugun dars yo'q"
- ✅ 1.2 "Bugun kelmagan" sparkline: oxirgi ish kunlari bo'yicha haqiqiy qatorlar, bo'sh kun `null` (SVG'da NaN yo'q — testda tekshiriladi)
- ✅ 1.3 Hozirgi darslar: "tugashiga N daqiqa" faqat bir marta (sarlavhada), har qatorda holat belgisi (olingan / 🤖 / olinmagan)
- ✅ 1.4 "-xona" faqat raqamli xonaga qo'shiladi (`roomLabel`: "12" → "12-xona", "Sport zal" → o'zgarishsiz)
- ✅ 1.5 30 kunlik grafik kartochkadan chiqmaydi (ChartCard tanasi flex; testda canvas/svg chegarasi tekshiriladi)
- ✅ 1.6 Grafikda yo'q to'lqinlar yo'q: `cubicInterpolationMode: 'monotone'`, maqsad chizig'i, eng past kun belgilangan
- ✅ 1.7 Bitta o'qituvchi bir nechta sinf rahbari edi: `OperationalDataSeeder.assignClassTeachers` endi bitta o'qituvchini faqat bitta sinfga rahbar qiladi
- ✅ 1.8 Sinflar reytingi qayta chizildi (`ClassRanking`: eng yaxshi / eng past, foiz chiziqlari, sinfga havola); eski `ClassRankingCard.vue` o'chirildi
- ✅ 1.9 Sarlavhadagi "ADMIN" chip olib tashlandi
- ✅ 1.10 Hazil e'lonlar seeder'da **yo'q**: ular jonli `maktab_db` bazasida (id 25, 26) 30.09 kuni sayt orqali qo'lda yaratilgan. Ularga tegilmadi — kerak bo'lmasa, "E'lonlar" sahifasidan o'chirish mumkin.

## 2. Davomat manbasi va ustunlik
- ✅ `attendance` ga ustunlar: `source` (MANUAL / AUTO_CARRY / DEMO_SIM / DEVICE, NOT NULL DEFAULT 'MANUAL' — eski yozuvlar MANUAL), `created_by`, `auto_at`, `late_minutes`, `auto_note`
- ✅ Ustunlik bitta joyda — `service/attendance/AttendanceWriter`:
  qo'lda yozish upsert va doim MANUAL; avtomatik yozish `ON CONFLICT DO NOTHING` (MANUAL'ga hech qachon tegmaydi);
  qurilma (DEVICE) avtomatik yozuvni yangilaydi, MANUAL'ni emas; `confirmAutomatic` (avtomatikni MANUAL qiladi); `deleteDemo`
- ✅ Bildirishnoma: DEMO hech qachon yubormaydi; AUTO_CARRY faqat `notifyOnAuto` yoqilganda (`NotificationService.enqueueAttendance` da ham himoya)
- ✅ UI: davomat sahifasida 🤖 / 📟 belgilari + tooltip (kim, qachon, nima sababdan), "✓ Hammasini tasdiqlash"; kalendarda 🤖

## 3. Haqiqiy rejim avtomatikasi (`AttendanceAutomation`, har daqiqa, advisory lock)
- ✅ AUTO_CARRY: dars boshlanganidan `autoCarryDelayMinutes` keyin belgisiz o'quvchiga shu kundagi oldingi darsdagi holati qo'yiladi (kechikkan → keldi). Oldingi belgi bo'lmasa (yoki birinchi dars) hech narsa yozilmaydi — "olinmagan" bo'lib qoladi
- ✅ Eslatma: `reminderMinutes` dan keyin haqiqiy (MANUAL/DEVICE) belgisiz darsning o'qituvchisiga ilova ichida xabar (`staff_notification`, qo'ng'iroqcha). O'qituvchilar Telegram'ga ulanmagani uchun eslatma **faqat ilova ichida**
- ✅ Eskalatsiya: `escalationMinutes` dan keyin ham olinmagan dars bosh sahifadagi "E'tibor talab qiladi" ro'yxatida 🔴 bilan chiqadi
- ✅ Qurilma: `POST /api/attendance/checkin` (`X-Api-Key`, kalit SHA-256 xesh bilan saqlanadi), `Student.cardId`; hujjat: `docs/attendance-device-api.md`

## 4. Demo simulyator
- ✅ Faqat `app.demo.attendance-simulator.enabled=true` bilan yoqiladi; `prod` profilida yoqilsa ham ishlamaydi (logda ogohlantirish)
- ✅ Har daqiqa, deterministik taqsimot (`DemoAttendancePlan`: kun, sinf, o'quvchi bo'yicha; "surunkali" kelmaydiganlar bor) — `random` yo'q
- ✅ 30 kunni to'ldirish / tozalash (`POST /api/demo/attendance/backfill`, `DELETE /api/demo/attendance`) — faqat DEMO_SIM yozuvlariga tegadi
- ✅ Sariq DEMO banner, xaritada DEMO ulushi, Excel eksportida "Manba" ustuni, DEMO uchun Telegram xabari yo'q

### Demo rejimni yoqish
1. Backend'ni `--app.demo.attendance-simulator.enabled=true` bilan (yoki shu qatorni `application-local.properties` ga yozib) ishga tushiring. `prod` profilida ishlamaydi.
2. Saytda: **Maktab sozlamalari → "Demo: oxirgi 30 kunni to'ldirish"**. Shu yerda "Demo ma'lumotni tozalash" ham bor.
3. Har sahifa tepasida sariq banner chiqadi; bugungi darslar dars boshlanganidan `app.demo.attendance-simulator.delay-minutes` (standart 10) keyin to'ladi.

## 5–7. Bosh sahifa

Rolga qarab: ADMIN — butun maktab; VIEWER — shu sahifa, lekin havolasiz (faqat o'qish); TEACHER (EDITOR + xodim) — o'z sahifasi.
12 ustunli grid, bir qatordagi kartochkalar bir xil balandlikda, bo'sh joy qolmaydi (1366 / 1920 / 2560 da test qilinadi), 390 da bitta ustun.

| Vidjet | Qaysi savolga javob beradi | Endpoint |
|---|---|---|
| KpiRow (6.1) | Bugun davomat, kelmagan, kechikkan, o'zlashtirish, sifat, javobsiz murojaatlar — kechagiga nisbatan qanday? | `/api/dashboard/kpis` |
| AttendanceMap (6.2) | Qaysi sinfda davomat yomon / hali olinmagan? (katak → kalendar, sinf filtri bilan) | `/today-map` |
| AttentionList (6.3) | Hozir nimani hal qilishim kerak? (olinmagan dars, ketma-ket kelmaganlar, muddati o'tgan murojaatlar, baho qo'yilmagan, Telegram ulanmagan, kutubxona ruxsatnomalari) | `/attention` |
| LiveLessons (6.4) | Hozir qaysi darslar ketyapti va davomati olinganmi? | `/live-lessons` |
| AttendanceTrend (6.5) | 30 kunda davomat maqsadga (95%) yetyaptimi, eng yomon kun qaysi? | `/attendance-trend` |
| ClassRanking (6.6) | Qaysi sinflar eng yaxshi / eng past (davomat va baho)? | `/class-ranking` |
| SubjectQuality (6.7) | Qaysi fanda sifat past? | `/subject-quality` |
| GradeDistribution (6.8) | Baholar taqsimoti (5/4/3/2) qanday? | `/grade-distribution` |
| TeacherDiscipline (6.9) | Qaysi o'qituvchi davomatni o'z vaqtida olmaydi? (faqat MANUAL va DEVICE hisoblanadi) | `/teacher-discipline` |
| ParentsTelegram (6.10) | Nechta oila bot orqali xabar oladi, xabarlar yetib boryaptimi? | `/parents-telegram` |
| UpcomingEvents (6.11) | Yaqin kunlarda nima bor (e'lonlar, tadbirlar)? | `/events` |
| O'qituvchi: darslarim, olinmaganlar, e'tibor, murojaatlar, sinfim | Bugun mening darslarim, nimani belgilamaganman, sinfimda kim kelmayapti? | `/my-lessons`, `/my-missing`, `/my-attention`, `/my-appeals`, `/my-class` |

Boshqa: `/api/dashboard/home` (qaysi ko'rinish), `/api/dashboard/activity` (Faollik jurnali, sahifalangan).
Javoblar 90 soniya keshlanadi (`DashboardCache`), davomat yoki baho saqlanganda tozalanadi. Frontend vidjeti 60 soniyada, faqat sahifa ko'rinib turganda yangilanadi.

Frontend: ranglar faqat tokenlardan (`src/css/tokens.scss`: `--st-*`, `--gr-*`, `--chart-*`, `--heat-*`, light va dark),
`src/charts/chartTheme.js` (Chart.js mavzu bilan almashadi), `components/dashboard/` (ChartCard, DeltaPill, Sparkline, RingChart, KpiCard, HeatTile),
AppButton / IconTile / PageHeader. Yangi sahifalar: **Maktab sozlamalari** va **Faollik jurnali** (faqat ADMIN). Qo'ng'iroqcha: xodim eslatmalari + E'tibor guruhlari.

## Sozlamalar

`application.properties` dagi standart qiymatlar; ADMIN ularni maktab bo'yicha **Maktab sozlamalari** sahifasida o'zgartiradi (`GET/PUT /api/school-settings`).

| Kalit | Standart | Ma'nosi |
|---|---|---|
| `attendance.auto-carry.enabled` | `true` | AUTO_CARRY yoqiq |
| `attendance.auto-carry.delay-minutes` | 10 | dars boshlanib necha daqiqadan keyin |
| `attendance.reminder-minutes` | 5 | o'qituvchiga eslatma |
| `attendance.escalation-minutes` | 10 | "E'tibor" ro'yxatida 🔴 |
| `attendance.notify-on-auto` | `false` | AUTO_CARRY ota-onaga xabar yuboradimi |
| `attendance.automation-cron` | `5 * * * * *` | rejalashtiruvchi (testlarda `-`, o'chiq) |
| `dashboard.attendance-target` | 95 | maqsad chizig'i |
| `dashboard.absent-streak-days` | 3 | necha kun ketma-ket kelmasa |
| `dashboard.discipline-threshold` | 30 | o'qituvchi intizomi chegarasi (%) |
| `dashboard.appeal-overdue-hours` | 24 | murojaat muddati |
| `dashboard.telegram-low-percent` | 50 | Telegram ulanishi past sinf |
| `dashboard.heat.good / ok / warn` | 95 / 90 / 85 | xarita ranglari chegarasi |
| `app.demo.attendance-simulator.enabled` | `false` | demo simulyator |
| `app.demo.attendance-simulator.delay-minutes` | 10 | demo belgisi necha daqiqada |

## Tekshiruv
- ✅ `gradlew build` (test bazasi `maktab_tg_test`): 274 test, 0 xato. Test kontekstlari ko'p bo'lgani uchun test heap 1 GB, Hikari puli kichik
- ✅ Avtomatika testlari: `AttendanceAutomationIntegrationTest` (7), `DemoSimulatorIntegrationTest` (5)
- ✅ Dashboard testlari: `HomeDashboardIntegrationTest` (7), soxta soat bilan (`support/TestSchool`)
- ✅ `npm run lint` — 0 xato; `npm run build` — muvaffaqiyatli; `npm test` (Playwright) — 164 test o'tdi; qattiq yozilgan rang/raqam yo'q
- ✅ Playwright: `tests/ui/dashboard.spec.js` — teng qatorlar, toshib chiqmaslik, havolalar va filtrlar, NaN yo'qligi, mavzu ranglari, DEMO banner, qo'lda qo'yilgan belgi avtomatikadan keyin ham MANUAL, o'qituvchi sahifasi
- ✅ "Keyin" skrinshotlari: `docs/images/dashboard/after-admin-{1920-dark,1920-light,1366-dark,2560-dark,mobile}.png`, `after-teacher-1920-dark.png`

## Jonli tizimga chiqarishdan oldin
- Merge'dan keyin backend'ni qayta deploy qilish kerak (`update.ps1`), yangi ustunlar va jadvallarni Hibernate (`ddl-auto=update`) o'zi qo'shadi; eski davomat yozuvlari MANUAL bo'ladi.
- AUTO_CARRY standart holatda **yoqiq**: o'qituvchi dars boshlanib 10 daqiqada belgilamasa, oldingi darsdagi holat 🤖 bilan yoziladi (ota-onaga xabar ketmaydi). Kerak bo'lmasa, Maktab sozlamalarida o'chiriladi.
- Demo simulyator jonli tizimda o'chiq (yoqilmaydi).
