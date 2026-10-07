# Baholar jurnali (katakli jurnal) — progress

Branch: `feature/journal` (`main` = 557dd55 dan). Worktree: `.claude/worktrees/v4`. Push qilinmagan.

## 0. Audit

| Narsa | Qayerda | Holat |
|---|---|---|
| Baholar sahifasi | `frontend/src/pages/GradebookPage.vue` — sinf/fan select, Dan/Gacha, sanalar jadvali, bitta baho dialogi | qayta qurildi ✅ |
| Baho | `Grade` (student, subject, gradeDate, score 2–5, type CURRENT/EXAM/QUARTERLY, comment, createdDate) | `createdBy` qo'shildi ✅ |
| Baho validatsiyasi | `GradeService.validateGradeDate`, `@Min(2) @Max(5)` | yakshanba matni spec bo'yicha ✅ |
| Bahoni o'chirish | `DELETE /api/grades/{id}` faqat ADMIN edi | EDITOR+ ✅ |
| Sinf rahbari | `SchoolClass.classTeacher` | BOR |
| Xodim telefoni | `Employee.phone` | BOR |
| Dars jadvali | `LessonSlot` (class, subject, employee, room, weekday, startTime, endTime) | BOR |
| Qo'ng'iroq jadvali | alohida jadval yo'q — sinfning turli boshlanish vaqtlari tartibi = dars raqami (PDF bilan bir xil qoida) | ishlatildi |
| Bayramlar | `CalendarEvent` HOLIDAY / VACATION | BOR |
| Vaqt | `Clock` bean (Asia/Tashkent) | BOR; demo/test uchun `app.fake-now` qo'shildi |
| Sinf tanlash | `AttendanceCalendarPage.vue` ichida edi | umumiy `ClassPicker.vue` ✅ |

## Holat

| # | Band | Holat |
|---|---|---|
| 0 | Audit + shu fayl | ✅ |
| 1.1 | Umumiy `ClassPicker` (davomat va jurnal): [1]…[11] kvadrat, harflar pill, o'quvchisiz sinf/parallel xira va bosilmaydi; URL `?class&subject&month` | ✅ |
| 1.2 | Fan: sinfda o'tiladigan faol fanlar (bo'lmasa — barcha faol fanlar) + «O'qituvchi: Karimova D.» | ✅ |
| 1.3 | «◀ Oktabr 2026 ▶» + «Bugun»; Dan/Gacha olib tashlandi | ✅ |
| 2.1 | Sinf rahbari: avatar, ism, telefon (+998 90 123 45 67, `tel:`, 📋 «Nusxalandi ✓»); yo'q bo'lsa xira matn + ADMIN uchun «Biriktirish» (sinf tahrirlash formasi `?edit=`); «Telefon kiritilmagan» | ✅ |
| 2.2 | Hozirgi dars: 🟢 dars (o'qituvchi, telefon, progress, «yana N daq») / ☕ tanaffus / 🌅 boshlanmagan / ✅ tugadi / 😴 dam olish; har 30 s frontendda qayta hisoblanadi | ✅ |
| 2.3 | «Shu darsga baho qo'yish»: fan → dars fani, oy → joriy, bugungi ustunga scroll + 2 s pulsatsiya; dars bo'lmasa yo'q | ✅ |
| 2.4 | Bugungi jadval chizig'i: hozirgi ajratilgan, o'tganlar xira, bosilsa fan tanlanadi | ✅ |
| 2.5 | O'qituvchi uchun avto tanlov (dars paytida yoki tugaganiga ≤ 15 daq), «Sizning hozirgi darsingiz: 1-A · Algebra»; URL da sinf/fan bo'lsa ishlamaydi | ✅ |
| 3.1 | № · O'quvchi (familiya bo'yicha) · oyning har kuni (dd.MM + Du…Ya, dars kuni •) · O'rtacha · Soni; kataklar 44px | ✅ |
| 3.2 | Sticky: sarlavha, №, ism, o'rtacha (o'ngda); mobilda faqat ism | ✅ |
| 3.3 | Bugungi ustun yoritilgan, «Bugun» belgisi, ochilganda unga scroll | ✅ |
| 3.4 | Rangli chiplar (5 yashil, 4 ko'k, 3 sariq, 2 qizil), tooltip: kim, qachon, izoh; bir kunda bir nechta — ikkitasi yonma-yon + «+N» | ✅ |
| 3.5 | Bo'sh katakda «+» doim ko'rinadi; bitta umumiy popover: [2][3][4][5], izoh, tur; darhol saqlash (optimistik), xatoda qaytariladi + qizil toast | ✅ |
| 3.6 | Chip → popover (joriy baho belgilangan) + «🗑 O'chirish»; 5 s «Bekor qilish» (undo) | ✅ |
| 3.7 | Klaviatura: 2/3/4/5, strelkalar, Delete, Enter, Esc | ✅ |
| 4.1–4.3 | Yakshanba / bayram — chiziqli kulrang, «+» yo'q, emoji xabar 3 s; kelajak — xira, «+» yo'q, xabar | ✅ |
| 4.4 | Backend: yakshanba 409 (aynan «Uzr, bu kun yakshanba — maktab ishlamaydi. Baho qo'yib bo'lmaydi.»), kelajak 409, 1/6 → 400 (Asia/Tashkent) | ✅ |
| 5 | «Sinf o'rtachasi» pastki qator, rangli o'rtachalar, statistika chiplari, bo'sh holat, «Baho qo'yish» — butun sinfga bir kunga tez baho (yakshanba va kelajak sanalar o'chirilgan) | ✅ |
| 6 | 3 ta endpoint; so'rovlar soni o'lchandi: journal 3, overview 3, current-lesson 1–2 (`JournalQueryCountTest`) | ✅ |
| 7 | Ko'rish VIEWER+, baho/o'chirish EDITOR+ (VIEWER uchun «+» va tugmalar yo'q), school_id himoyasi (`/api/classes/{id}` qo'shildi), telefonlar faqat login ortida | ✅ |
| 8 | Tokenlar (app.scss), dark/light, Inter, 1366/1920, mobil | ✅ |
| T | Tekshiruv 1–5 | ✅ (pastda) |

## Yangi endpointlar

| Endpoint | Ruxsat | Javob | SQL |
|---|---|---|---|
| `GET /api/grades/journal?classId=&subjectId=&month=2026-10` | VIEWER+ | `{classId, subjectId, month, today, days: [{date, weekday, isSunday, isHoliday, holidayName, isFuture, hasLesson}], students: [{id, fullName}], grades: [{id, studentId, date, value, type, comment, createdBy, createdAt}], subjectTeacher: {id, fullName, phone}}` | 3 |
| `GET /api/classes/{id}/overview` | VIEWER+ | `{classId, className, homeroomTeacher: {id, fullName, phone} \| null, today, weekday, todayLessons: [{lessonNo, start, end, subjectId, subjectName, teacher}], isDayOff, dayOffReason, subjects: [{id, name, teacher}]}` | 3 |
| `GET /api/me/current-lesson` | VIEWER+ | `{classId, className, subjectId, subjectName, lessonNo, start, end}` yoki 204 | 1–2 |
| `DELETE /api/grades/{id}` | endi EDITOR+ | — | — |

Yangi sozlama (faqat demo va brauzer testlari uchun): `app.fake-now=2026-10-07T10:40` — ilova soati shu
paytdan boshlanib yuradi (logda «DIQQAT: soxta vaqt yoqilgan»). Standart: bo'sh = haqiqiy vaqt. Prod da qo'ymang.

## Tekshiruv natijalari

| # | Tekshiruv | Natija |
|---|---|---|
| 1 | `./gradlew build` (testlar bilan), `npm run lint`, `npm run build` | ✅ 224 test, 0 xato |
| 2 | Backend testlar (`JournalIntegrationTest`, `JournalQueryCountTest`, `GradeDateIntegrationTest`): yakshanba 409 + aynan matn; kelajak 409; 1 va 6 → 400; jurnal kunlari (isSunday, hasLesson); overview — rahbar va telefon / null, yakshanba dam olish; current-lesson soxta `Clock` bilan — dars vaqtida to'g'ri dars, bo'sh paytda 204; boshqa maktab sinfi 403; VIEWER baho qo'ya olmaydi | ✅ |
| 3 | Playwright 1440×900 (vaqt `page.clock` + backend `app.fake-now`): a–m hammasi | ✅ 37/37, konsolda xato yo'q |
| 4 | Skrinshotlar `docs/images/journal/`: journal-dark, journal-light, journal-panel-lesson, journal-panel-break, journal-popover, journal-sunday, journal-1366, journal-mobile — har biri ochib tekshirildi | ✅ |
| 5 | Shu fayl | ✅ |

Davomat kalendari umumiy `ClassPicker` ga o'tkazilgandan keyin ham tekshirildi (8/8 ✅).

## O'zgargan fayllar

Backend: `entity/Grade` (createdBy), `service/GradeService`, `service/JournalService` (yangi),
`controller/GradeController`, `controller/JournalController` (yangi), `dto/GradeResponseDto`,
`repository/{GradeRepository,LessonSlotRepository,CalendarEventRepository,SchoolClassRepository}`,
`config/SchoolScopeConfig`, `telegram/TelegramConfig` (app.fake-now).
Frontend: `pages/GradebookPage.vue` (qayta yozildi), `components/ClassPicker.vue` (yangi),
`components/ClassOverviewPanel.vue` (yangi), `utils/tashkent.js` (yangi), `pages/AttendanceCalendarPage.vue`
(umumiy ClassPicker), `pages/CrudPage.vue` (`?edit=<id>`).
Testlar: `JournalIntegrationTest`, `JournalQueryCountTest`, `support/MutableClock`, `GradeDateIntegrationTest` (matn).

## Ishga tushirish

Backend qayta ishga tushirilishi kerak (`grade.created_by` ustuni `ddl-auto=update` bilan qo'shiladi; eski
baholarda bo'sh — tooltip'da muallif ko'rinmaydi). Jonli botda: `C:\Users\A.Doniyorov\maktab-live\update.ps1`.
