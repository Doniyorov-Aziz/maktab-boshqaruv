# Bosh sahifa v3 ("WOW" panel) va to'liq demo avtomatika — jarayon

Branch: `feature/dashboard-v3` (worktree `.claude/worktrees/dashboard-v3`). Push qilinmaydi.
Tekshiruv muhiti: backend 8081 (`maktab_tg_test`), frontend build 9100.

## 0. Audit
- ✅ Davomat simulyatori, baho modeli (`grade`: dars bilan bog'lanmagan, manba yo'q), GradeService (Telegram GradeSavedEvent orqali), dashboard servis/endpointlari o'qildi
- ❌ "Oldin" rasmlari: `docs/images/dashboard/v3-before-*.png` (v2 dagi `before-*` lar v1 ga tegishli, ustidan yozilmadi)

## 1. Demo rejim — umumiy
- ❌ `app.demo.enabled` (default true), prod da har doim o'chiq + ogohlantirish
- ❌ Startup log: "DEMO enabled, profile=..., zone=Asia/Tashkent"; har ishlashda "SIM attendance: ..." va "SIM grades: ..."
- ❌ Baho: `source`, `lesson_slot_id` ustunlari (mavjudlar MANUAL), partial unique index, insert-if-absent
- ❌ Xodim DEMO/avto bahoni o'zgartirsa → MANUAL
- ❌ DEMO_SIM baho bo'yicha Telegram yo'q
- ❌ Banner matni, 🤖 jurnalda/baholarda
- ❌ Sozlamalar: "Hozir to'ldirish", "Oxirgi 30 kunni to'ldirish", "Demo ma'lumotlarini tozalash" (davomat + baho)

## 2. Davomat simulyatori — nega ishlamadi
- ✅ Sabab topildi (pastda)
- ❌ Tuzatish: demo default yoqiq; demo rejimda simulyator AUTO_CARRY dan oldin ishlaydi
- ❌ Startupda oxirgi 30 ish kuni (bir marta, mavjud kunlar takrorlanmaydi)
- ❌ Taqsimot tekshiruvi (92–97 / 1–3 / 2–5 / ~1)

## 3. Baho simulyatori
- ❌ Dars tugagach, kelganlarning 30–40% iga baho; createdBy = o'qituvchi
- ❌ Qobiliyat (25/55/20), fan farqi, sinf kuchi, pasayish trendi, nazorat (EXAM) ~5%
- ❌ Yakshanba / bayram / kelajak yo'q; startup 30 kun
- ❌ 2–3 o'qituvchi darslarining 15–30% ini ataylab to'ldirmaydi; demo da DEMO_SIM intizomda "kiritilgan"

## 4. Hisob-kitob
- ❌ Davomat % (sababli maxrajdan chiqariladi), "qisman" (<50%), "—"
- ❌ O'zlashtirish / sifat (chorak, oy, hafta)
- ❌ Salomatlik indeksi (og'irliklar sozlamada), xavf balli — bitta servis, testlangan
- ❌ Bo'sh holatda ✅ yo'q
- ❌ Sinf dublikati: sabab, o'quvchisiz sinflar yashiriladi, unique (409)

## 5–8. Dizayn, bosh sahifa, joylashuv, grafiklar
- ❌ Tokenlar: kartochka highlight, glow, shimmer, harakat (reduced-motion)
- ❌ MultiRing, RiskAvatar, SignalRow, count-up, bubble grafik
- ❌ 6.1 Hero · 6.2 ixcham davomat · 6.3 bubble · 6.4 xavf · 6.5 dinamika · 6.6 signallar + drawer · 6.7 dumbbell · 6.8 taqsimot · 6.9 lenta · 6.10 pastki qator
- ❌ Joylashuv 1024–1439 / <1024 / 390

## 9. Rollar
- ❌ O'qituvchi: shaxsiy indeks, darslarim, xavf, murojaatlar, sinfim
- ❌ VIEWER: amallarsiz

## Tekshiruv
- ❌ gradle build, npm lint + build
- ❌ Backend qayta ishga tushirish, 2 daqiqa, log + DB sonlari
- ❌ Backend testlari (ro'yxat bo'yicha)
- ❌ Playwright
- ❌ v3 rasmlar + video, tanqidiy ko'rib chiqish
- ❌ Yakuniy hujjat

## Davomat simulyatori nega ishlamadi (2026-10-16 15:13, jonli)
- a) `@EnableScheduling` bor (TelegramConfig), `AttendanceAutomation.tick` har daqiqa ishlaydi — sabab emas.
- b) Profil: jonli `local`; simulyator faqat `app.demo.attendance-simulator.enabled=true` bo'lsa yoqiladi, standart qiymat `false` edi va jonli config'da yozilmagan — **ASOSIY SABAB**.
- c) Zona Asia/Tashkent (Clock bean va cron zone) — to'g'ri.
- d) Hafta kuni: `lesson_slot.weekday` = "Dushanba".."Shanba", `SchoolDays.WEEKDAY_UZ` mos; o'quv yili filtri yo'q (2026-2027 yil bazada yo'q, lekin filtr yo'qligi uchun xalaqit bermaydi).
- e) Advisory lock tranzaksiya bilan tugaydi (`pg_try_advisory_xact_lock`), qotib qolmaydi.
- f) Xato `log.error` bilan yoziladi, jim yutilmaydi; logda xato yo'q.
- Qo'shimcha: AUTO_CARRY faqat shu kunning oldingi darsidagi belgini ko'chiradi — bugun 1 ta qo'lda belgi bo'lgani uchun deyarli hech narsa yozmadi. Demo yoqilganda esa AUTO_CARRY simulyatordan OLDIN ishlab, demo yozuvlarini "ko'chirildi" deb yozib yuborardi.

**Bir gap bilan:** jonli tizimda demo simulyator sozlamada yoqilmagan edi (standart `false`), AUTO_CARRY esa faqat oldingi darsdagi qo'lda belgini ko'chiradi.
