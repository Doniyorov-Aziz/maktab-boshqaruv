# Bosh sahifa (qaror markazi) va davomat avtomatikasi — jarayon

Branch: `feature/dashboard-v2` (worktree `.claude/worktrees/dashboard`). Push qilinmaydi.

Tekshiruv muhiti: backend 8081 (`maktab_tg_test`), frontend build 9100 — `docs/ui-system/REPORT.md` §9 dagidek.

## 0. Audit
- ❌ Hozirgi vidjetlar: savol / endpoint / qarorga yordam
- ❌ Davomat modeli, saqlash, outbox, qo'ng'iroq jadvali
- ❌ "Oldin" skrinshotlari (1366, 1920, 390; dark/light)

## 1. Hozirgi xatolar
- ❌ 1.1 Bugungi davomat 0% (faqat olingan sinflar; hech biri bo'lmasa "Hali olinmagan")
- ❌ 1.2 "Bugun kelmagan" sparkline
- ❌ 1.3 Hozirgi darslar: qolgan vaqt bir marta, holat belgisi
- ❌ 1.4 "-xona" faqat raqamli xonaga
- ❌ 1.5 30 kunlik grafik kartochkadan chiqib ketadi
- ❌ 1.6 Grafikda yo'q to'lqinlar (monotone)
- ❌ 1.7 Bitta o'qituvchi bir nechta sinf rahbari (so'rov yoki seeder)
- ❌ 1.8 Reyting dizayni
- ❌ 1.9 "ADMIN" chip
- ❌ 1.10 Seeder'dagi hazil e'lonlar

## 2. Davomat manbasi va ustunlik
- ❌ source / createdBy / autoAt (+ mavjudlar MANUAL)
- ❌ Ustunlik: bitta servis metodi, insert-if-absent (ON CONFLICT DO NOTHING)
- ❌ UI: 🤖 belgi + tooltip, "Hammasini tasdiqlash"

## 3. Haqiqiy rejim avtomatikasi
- ❌ AUTO_CARRY
- ❌ Eslatma (5 daq) va eskalatsiya (10 daq)
- ❌ notifyOnAuto (default false)
- ❌ Qurilma: POST /api/attendance/checkin + API kalitlar + docs/attendance-device-api.md

## 4. Demo simulyator
- ❌ Faqat application.properties; prod profilida o'chiq + ogohlantirish
- ❌ Har daqiqa, deterministik realistik taqsimot
- ❌ 30 kunni to'ldirish / tozalash
- ❌ Sariq banner, DEMO ulushi, Excel manba ustuni, Telegram yo'q

## 5–7. Dashboard
- ❌ Backend vidjet endpoint'lari (kesh, rolga qarab)
- ❌ Frontend: chartTheme, palitra, ChartCard/Sparkline/RingChart/KpiCard/DeltaPill/HeatTile
- ❌ Vidjetlar 6.1–6.11, joylashuv (12 ustun, qatorlar teng)
- ❌ O'qituvchi / viewer bosh sahifasi
- ❌ "Faollik jurnali" sahifasi
- ❌ Maktab sozlamalari sahifasi

## Tekshiruv
- ❌ gradle build, npm lint + build
- ❌ Avtomatika testlari
- ❌ Dashboard testlari
- ❌ Playwright
- ❌ "Keyin" skrinshotlari
- ❌ Shu faylda yakuniy hujjat
