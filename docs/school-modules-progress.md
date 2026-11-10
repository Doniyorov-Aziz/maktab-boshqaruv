# Maktab modullari (v5) — holat

Branch: `feature/school-modules` (lokal, push/merge/deploy qilinmagan).

| # | Modul | Admin panel | Ota-onalar boti | API |
|---|-------|-------------|-----------------|-----|
| 1 | Uy vazifasi | `/homework` | 📝 Uy vazifasi (+ fayl), xabar | `/api/homework` |
| 2 | Chorak tabeli | `/report-cards`, o'quvchi profili «Tabel» | ➕ Yana → 📄 Tabel (PDF) | `/api/report-cards` |
| 3 | Ish yuklamasi | `/workload` (Excel) | — | `/api/workload` |
| 4 | Imtihonlar jadvali | `/exams` (natijalar) | ➕ Yana → 🧪 Nazorat ishlari, eslatma | `/api/exams` |
| 5 | Uchrashuvlar | `/meetings` | ➕ Yana → 📅 Uchrashuv (yozilish/bekor) | `/api/meeting-slots` |
| 6 | To'lovlar (standart o'chiq) | `/payments` | ➕ Yana → 💳 To'lovlar, eslatma | `/api/payments` |
| 7 | Yutuqlar | `/achievements`, profil «Yutuqlar» | ➕ Yana → 🏆 Yutuqlar, tabrik | `/api/achievements` |

## Qoidalar
- Chorak bahosi taklifi: chorakdagi CURRENT/HOMEWORK/EXAM baholar o'rtachasi, 4.5 → 5; tasdiqlangani `QUARTERLY` baho
  (chorak oxirgi kuni yoki bugun). Yillik baho — barcha chorak baholari bo'lsa, ularning o'rtachasi.
- Ish yuklamasi: yakshanba, Taqvim bayram/ta'tillari va o'quv yilidagi ta'til choraklari hisoblanmaydi; kelajakdagi kunlar
  sanalmaydi; o'rinbosarlik ayiriladi/qo'shiladi.
- Imtihon natijasi faqat imtihon kunidan keyin; `grade.exam_id` bilan EXAM baho (GradeService orqali — ota-onaga baho xabari).
- Uchrashuv: bir vaqtni bitta ota-ona oladi (atomik), bir o'qituvchiga bitta faol yozilish; 1 soatdan kam qolsa bot
  orqali bekor qilib bo'lmaydi.
- To'lov: oy muddat kunidan keyin qarz bo'ladi; eslatma muddat kunining ertasi 10:00 da (yoqilgan bo'lsa) va tugma bilan.
  VIEWER va demo mehmon ko'rmaydi.

## Deploy
Yangi jadvallar: `homework, exam, meeting_slot, payment, payment_settings, student_fee, achievement`; ustunlar
`grade.exam_id`, `parent_session.notify_homework`; `staff_notification_kind_check` olib tashlanadi. Avval `maktab_db` zaxirasi,
keyin backend qayta deploy (`maktab-live\update.ps1`).

## Testlar
`SchoolModulesIntegrationTest` (8 ta, test bazasi `maktab_tg_test`). Playwright UI testlari yangi sahifalar uchun hali yo'q.
