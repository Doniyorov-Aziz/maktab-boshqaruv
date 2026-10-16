# Bosh sahifa v3 ("WOW" panel) va to'liq demo avtomatika

Branch: `feature/dashboard-v3` (worktree `.claude/worktrees/dashboard-v3`). Push, merge va deploy qilinmagan.
Tekshiruv muhiti: backend 8081 (`maktab_tg_test`), frontend build 9100.

## Davomat simulyatori nega ishlamadi (jonli, 2026-10-16 15:13)

**Bir gap bilan:** jonli tizimda demo simulyator sozlamada yoqilmagan edi (`app.demo.attendance-simulator.enabled` standart `false`, jonli config'da yozilmagan), AUTO_CARRY esa faqat shu kunning oldingi darsidagi belgini ko'chiradi — bugun bitta qo'lda belgi bo'lgani uchun deyarli hech narsa yozilmadi.

Birma-bir tekshiruv:
- a) `@EnableScheduling` bor edi (TelegramConfig ichida) va scheduler har daqiqa ishlagan — sabab emas. Endi alohida `config/SchedulingConfig`.
- b) Profil: jonli `local`. Simulyator faqat property `true` bo'lsa yoqilardi — **asosiy sabab**.
- c) Zona Asia/Tashkent (Clock bean va cron `zone`) — to'g'ri.
- d) Hafta kuni: `lesson_slot.weekday` = "Dushanba".."Shanba", `SchoolDays.WEEKDAY_UZ` mos (0/1 xatosi yo'q); o'quv yili/chorak filtri simulyatorda yo'q (bazada 2026-2027 yili yo'q, lekin bu xalaqit bermaydi); qo'ng'iroq jadvali `lesson_slot.start_time/end_time`.
- e) Lock — `pg_try_advisory_xact_lock`, tranzaksiya bilan tugaydi, qotib qolmaydi.
- f) Xato `log.error` bilan yoziladi, jim yutilmaydi; logda xato yo'q edi.
- Qo'shimcha topilgan xato: demo yoqilganda AUTO_CARRY simulyatordan OLDIN ishlab, demo belgilarni "ko'chirildi" deb yozib yuborardi. Endi tartib: qo'lda/qurilma belgisidan ko'chirish → simulyator → qolgan bo'shliqlarga ko'chirish → baholar.

## Bandlar

### 0. Audit
- ✅ Simulyator, baho modeli (`grade`: dars bilan bog'lanmagan, manba yo'q edi), GradeService (Telegram `GradeSavedEvent` orqali), dashboard endpointlari o'qildi
- ✅ "Oldin" rasmlari: `docs/images/dashboard/v3-before-{1366-dark,1920-dark,1920-light,390-light}.png` (v2 dagi `before-*` lar v1 ga tegishli, ustidan yozilmadi)

### 1. Demo rejim — umumiy
- ✅ `app.demo.enabled` (standart `true`, eski `app.demo.attendance-simulator.enabled` ham o'qiladi); `prod` profilida har doim o'chiq + WARN log
- ✅ Startup log: `DEMO enabled, profile=local, zone=Asia/Tashkent …`; har daqiqa: `SIM attendance: lessons=N inserted=M skippedManual=K` va `SIM grades: lessons=N inserted=M`
- ✅ `grade.source` (MANUAL / DEMO_SIM; eski baholar MANUAL — `default 'MANUAL'`, ma'lumot yo'qolmaydi) va `grade.lesson_slot_id`; partial unique index `uk_grade_lesson_student_day (lesson_slot_id, student_id, grade_date) where lesson_slot_id is not null`; insert `ON CONFLICT DO NOTHING` + shu kuni shu fandan MANUAL baho bo'lsa yozilmaydi
- ✅ Xodim demo bahoni tahrirlasa → MANUAL (simulyator unga tegmaydi); davomatda bu avvaldan bor edi
- ✅ DEMO_SIM baho/davomat bo'yicha Telegram yo'q (`NotificationService.enqueueGrade` / `enqueueAttendance` himoyasi + simulyator event chiqarmaydi)
- ✅ Deterministik seed (maktab + sana + o'quvchi + dars xeshi, `random` yo'q), idempotent
- ✅ Sariq ingichka banner: "🧪 Demo rejim — qo'lda kiritilmagan davomat va baholar avtomatik to'ldirilmoqda"; 🤖 davomatda va baholar jurnalida (burchakda)
- ✅ Maktab sozlamalari (ADMIN, demo yoqilganda): "Hozir to'ldirish", "Oxirgi 30 kunni to'ldirish", "Demo ma'lumotlarini tozalash" (faqat DEMO_SIM davomat va baholar)

### 2. Davomat simulyatori
- ✅ Sabab topildi va tuzatildi (yuqorida)
- ✅ Har ishga tushishda bugungi boshlanganiga 10+ daqiqa bo'lgan BARCHA darslar to'ldiriladi (ilova kech yoqilsa ham)
- ✅ Startupda oxirgi 30 ish kuni fonda to'ldiriladi; demo bahosi bor kunlar o'tkazib yuboriladi (qayta ishga tushish ~50 ms)
- ✅ Taqsimot (test bazasi, kun bo'yicha): keldi 93.6%, kechikdi 1.8%, kelmadi 3.5%, sababli 1.2%; 1-darsda kelmagan butun kun kelmagan; kechikkan keyingi darslarda "keldi"; har sinfda 1–2 surunkali; dushanba/shanba yuqoriroq; yakshanba/bayramda ishlamaydi

### 3. Baho simulyatori
- ✅ Dars tugagach, kelgan (PRESENT/LATE) o'quvchilarning 30–40% iga shu fandan baho; `createdBy` = darsning o'qituvchisi ("Familiya I."), sana = dars sanasi; kelmaganga baho yo'q
- ✅ Qobiliyat 25/55/20, sinf kuchi ±0.2, aniq fanlar −0.25, jismoniy tarbiya/musiqa/tasviriy san'at/texnologiya +0.35, ~8% o'quvchida "yomon ikki hafta" (−0.9)
- ✅ Natija (DEMO_SIM): 5 — 32.8%, 4 — 40.5%, 3 — 21.6%, 2 — 5.2%
- ✅ Haftadagi oxirgi darsda ~5% haftalarda nazorat (EXAM), unda kelganlarning hammasi baholanadi
- ✅ Yakshanba, bayram, kelajakka baho yo'q; startup 30 kun
- ✅ 6+ o'qituvchili maktabda 2–3 o'qituvchi darslarining 15–30% ini ataylab to'ldirmaydi (davomat ham, baho ham), qolganlar 97–100%; demo rejimda DEMO_SIM intizomda "kiritilgan" (prod da faqat MANUAL va DEVICE)

### 4. Hisob-kitob (bitta servis: `service/dashboard/DashboardMetrics`, `DashboardMetricsTest`)
- ✅ Davomat % = (keldi + kechikdi) / (keldi + kechikdi + kelmadi) — sababli maxrajdan chiqarilgan (barcha vidjetlarda)
- ✅ Sinfning 50% dan kami belgilangan → "qisman" (kulrang, "12/25", rangli baho yo'q); olinmagan → "—" / ⏳
- ✅ O'zlashtirish = (3,4,5)/jami, Sifat = (4,5)/jami; davr: chorak, sahifa sarlavhasida "Chorak · Oy · Hafta"
- ✅ Salomatlik indeksi = davomat×40 + sifat×30 + jurnal to'liqligi×20 + Telegram×10 (og'irliklar Maktab sozlamalarida, yig'indisi 100; ma'lumoti yo'q qism chiqarilib, qolganlari qayta taqsimlanadi); formula tooltip'da
- ✅ Xavf balli 0–100 = kelmaslik (30 kunda, 6+ kun = 35) + oxirgi 2 hafta o'rtachasi (4.0 dan past, ≤2.5 = 30) + pasayish (oldingi 4 haftaga nisbatan, 1 ball = 20) + «2»lar (30 kunda, 4+ = 15); ro'yxatda 40+
- ✅ Hammasi `school_id` bilan, Asia/Tashkent, kesh 90 s, davomat/baho yozilganda (simulyator ham) tozalanadi
- ✅ "Ma'lumot yo'q" neytral (⏳ + "Hali ma'lumot yo'q — demo to'ldirilmoqda"); yashil ✓ faqat haqiqatan muammo yo'q joyda (xavf ro'yxati bo'sh, signal 0)
- ✅ Ikkinchi "11-A": seeder xatosi emas — jonli bazada id 45, 2024-2025 o'quv yilida, o'quvchisiz va darssiz (qo'lda yaratilgan). O'chirilmadi; o'quvchisiz sinflar hech bir vidjetda ko'rinmaydi; sinf harfi normallashtiriladi ("a " = "A"), dublikat — 409

### 5–8. Dizayn va bosh sahifa
- ✅ To'q ko'k-qora fon (`#0b0d15`), kartochka `#141824`, 1px xira chegara, yuqori chetida yorug'lik chizig'i, yumshoq soya, `--radius-lg`; light rejim oq kartochkalar
- ✅ Hero glow (binafsha 7–8%), Inter + tabular-nums, KPI 38px/700/−0.02em
- ✅ Count-up 600ms, halqalar 800ms, kartochkalar 40ms oraliq bilan pastdan, hover'da ko'tariladi; `prefers-reduced-motion` da hammasi o'chiq
- ✅ Shimmer skeleton kartochka shaklida
- ✅ 6.1 Salomatlik indeksi (MultiRing: indeks + 4 ingichka halqa) + 4 KPI 2×2 (IconTile, delta, count-up, izoh, silliq sparkline + gradient)
- ✅ 6.2 Bugungi davomat ixcham (≤180px): halqa + 4 ko'rsatkich, 11 ustunli parallel to'ri, yumshoq ranglar, <85 qizil chegara, olinmagan — chiziqli fon, qisman — "12/25", legenda + "Qo'lda N · Avtomatik N"
- ✅ 6.3 Davomat × sifat pufakchalari (kvadrantlar, burchak yozuvlari, o'lcham = o'quvchilar, hover, bosilsa sinf profili, yozuvlar ustma-ust tushmaydi)
- ✅ 6.4 Xavf ostidagi o'quvchilar (top-6, avatar, chiplar, ball halqada, "Hammasi (N)" yon panelda)
- ✅ 6.5 Dinamika: davomat (gradient) + sifat, maqsad punktiri, eng past nuqta, 3 tab, monotone
- ✅ 6.6 E'tibor — 5 signal; 0 bo'lsa xira ✓; bosilsa o'ngdan yon panel, "Davomat olish" / "Ochish"
- ✅ 6.7 Fanlar dumbbell (chorak/oy), 6.8 baholar 100% stacked + 4 mini statistika
- ✅ 6.9 Hozirgi darslar lentasi (vaqt bir marta + progress), 6.10 Telegram · tadbirlar (4) · jurnal to'liqligi (bullet bar)
- ✅ Joylashuv: 12 ustun, qatorlar teng, bo'sh joy yo'q; 1024–1439 hero ustma-ust, juftlar 7+5; <1024 bitta ustun; 390 da gorizontal scroll yo'q; "So'nggi e'lonlar/harakatlar" yo'q
- ✅ chartTheme: Inter, xira grid, kartochka uslubidagi tooltip, mavzu almashganda qayta chiziladi; ranglar faqat tokenlardan

### 9. Rollar
- ✅ O'qituvchi: shaxsiy indeks (darslaridagi davomat 50% + jurnal to'liqligi 50%), "Bugungi darslarim" [Davomat] [Jurnal], sinflaridagi xavf, murojaatlar, "Mening sinfim"
- ✅ VIEWER: faqat ko'rsatkichlar, havola va amallarsiz

## Tekshiruv
- ✅ `gradlew build` — 291 test, 0 xato
- ✅ `npm run lint` — 0 xato; `npm run build` — muvaffaqiyatli; brauzer konsolida xato yo'q (testlar tekshiradi)
- ✅ Backend qayta ishga tushirildi, 2 daqiqa:
  ```
  DEMO enabled, profile=local, zone=Asia/Tashkent — attendance and grades nobody entered are simulated (DEMO_SIM)
  DEMO startup fill (30 school days): attendance inserted=0 grades inserted=0 in 49 ms   (birinchi marta: 110 964 / 41 898, 10 s)
  SIM attendance: lessons=190 inserted=0 skippedManual=401
  SIM grades: lessons=190 inserted=0
  ```
  Bazadan: bugungi DEMO_SIM davomat 3 328, bugungi DEMO_SIM baholar 1 650, oxirgi 30 kun baholari 40 449 (DEMO 35 261), taqsimot 5/4/3/2 = 32.8 / 40.5 / 21.6 / 5.2 %
- ✅ Backend testlari (soxta soat): `DemoGradeSimulatorIntegrationTest` (6), `DemoSimulatorIntegrationTest`, `AttendanceAutomationIntegrationTest`, `HomeDashboardIntegrationTest` (10), `DashboardMetricsTest` (8) — MANUAL davomat/baho o'zgarmaydi, kelmaganga baho yo'q, yakshanba/kelajak yo'q, idempotentlik, prod da demo o'chiq, DEMO_SIM uchun outbox bo'sh, formulalar, sababli % ga ta'sir qilmaydi, "dangasa" o'qituvchilar jurnal hisobida
- ✅ Playwright: `npm test` 168 test o'tdi (`tests/ui/dashboard.spec.js` 14 ta: 1366/1920/2560 dark+light qatorlar teng, toshib chiqmaydi, 6.2 ≤180px va 11 ustun bir qatorda, 20+ pufakcha, 3+ xavf, signal → yon panel, "ma'lumot yo'q" yonida ✅ yo'q, reduced-motion, havolalar, NaN yo'q, mavzu ranglari, banner, qo'lda belgi, o'qituvchi)
- ✅ Rasmlar: `docs/images/dashboard/v3-admin-{1920-dark,1920-light,1366-dark,2560-dark,mobile}.png`, `v3-teacher-1920-dark.png`, video `v3-intro.webm` (10 s)

### Rasmlarni ko'rib chiqish: "Linear/Stripe darajasidami? Nima arzon ko'rinadi?"
Birinchi variantdan keyin topilgan va tuzatilgan narsalar:
- pufakcha grafigida markazdagi sinf nomlari ustma-ust tushardi → endi to'qnashadigan nom chizilmaydi (hover'da ko'rinadi);
- "Bahosi tushganlar 147" — o'quvchi×fan juftliklari sanalardi va shovqinli edi → endi o'quvchining umumiy o'rtachasi bo'yicha (har oynada 4+ baho, ≥0.8 pasayish): 3;
- "Hammasi (115)" — juda ko'p va bosilmasdi → chegarasi 40, bosilsa yon panel;
- "Jami baholar −449" — tugamagan haftani to'liq hafta bilan solishtirish noto'g'ri → delta olib tashlandi;
- legenda nuqtalari ko'rinmasdi (Quasar `.lg` klassi bilan to'qnashuv) → klass nomi o'zgartirildi;
- 1366 da KPI nomi kesilardi, indeks legendasi butun kenglikka cho'zilardi → tuzatildi; ≥1920 da halqa 210px;
- mobil/qayta chizishda Chart.js gradient rangini "tween" qilib xato berardi → rang animatsiyasi o'chirildi.

Hozirgi holat: tipografiya, ranglar intizomi, halqalar va kartochka yuzasi Linear/Vercel uslubiga yaqin. Qolgan ochiq fikr: signallar kartochkasi past qismi siyrak (izoh qatori qo'yildi), dinamikada sifat chizig'i davomat bilan bir o'qda — farq katta bo'lgani uchun o'q 65–100%.

## Sozlamalar

| Kalit | Standart | Ma'nosi |
|---|---|---|
| `app.demo.enabled` | `true` | demo rejim (prod profilida har doim o'chiq) |
| `app.demo.delay-minutes` | 10 | dars boshlanib necha daqiqada demo davomat |
| `app.demo.startup-backfill-days` | 30 | ishga tushganda to'ldiriladigan ish kunlari (0 = yo'q) |
| `attendance.auto-carry.enabled` / `.delay-minutes` | `true` / 10 | AUTO_CARRY |
| `attendance.reminder-minutes` / `escalation-minutes` | 5 / 10 | eslatma / "E'tibor" |
| `dashboard.discipline-threshold` | **10** (avval 30) | jurnalning necha % i to'ldirilmasa "to'liq emas" |
| `dashboard.health.weight-attendance/quality/journal/telegram` | 40/30/20/10 | indeks og'irliklari (sahifada ham) |
| `dashboard.attendance-target`, `heat.good/ok/warn`, `absent-streak-days`, `appeal-overdue-hours`, `telegram-low-percent` | 95, 95/90/85, 3, 24, 50 | avvalgidek |

## Endpointlar (`/api/dashboard/*`, `?schoolId`)
`health`, `kpis?period=quarter|month|week`, `today-map`, `class-map`, `risk?limit`, `dynamics?days`, `signals`, `attention` (qo'ng'iroqcha), `live-lessons`, `subject-quality?period`, `grade-distribution`, `journal`, `parents-telegram`, `events?limit`; o'qituvchi: `my-index`, `my-lessons`, `my-risk`, `my-appeals`, `my-class` (`my-missing`, `my-attention` qoldi).
Demo: `POST /api/demo/run`, `POST /api/demo/backfill?days=30`, `DELETE /api/demo` (eski yo'llar ham ishlaydi).

## Jonli tizimga chiqarishdan oldin
- `app.demo.enabled` standart `true` va jonli profil `local` — deploy qilinsa jonli `maktab_db` darhol 30 kunlik demo davomat va baholar bilan to'ladi (qo'lda kiritilganlarga tegilmaydi, ota-onalarga xabar ketmaydi). Kerak bo'lmasa jonli `config/application-local.properties` ga `app.demo.enabled=false` yozing; tozalash — Maktab sozlamalari → "Demo ma'lumotlarini tozalash".
- Qorong'i mavzuning fon ranglari butun ilova uchun biroz to'qroq (ko'k-qora) qilindi.
