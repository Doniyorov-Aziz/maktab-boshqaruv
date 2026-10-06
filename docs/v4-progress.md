# v4 — progress

Branch: `feature/v4` (`main` = v3 dan). Worktree: `.claude/worktrees/v4` — jonli bot ishlayotgan
`telegram-bot-v2` papkasining jar fayliga tegmaslik uchun alohida. Sessiya uzilsa — shu fayldan davom eting.
Har band alohida commit, push yo'q.

## 0. Audit (kod yozishdan oldin)

| # | Band | Holat | Reja |
|---|---|---|---|
| 1.1 | Login: «Muvaffaqiyatli ✓» animatsiyasi | BOR (olib tashlanishi kerak) — `LoginPage.vue` `success` holati | darhol dashboard, tugmada spinner |
| 1.2 | Xodim holati ISHDA/TA'TILDA/ISHDAN_KETGAN, login 403, JWT filtrda 401, avto-qaytish, oxirgi admin | YO'Q — `Employee` da holat yo'q, `JwtAuthFilter` faqat imzoni tekshiradi | `EmployeeStatus` + sanalar, `UserStatusService` (1 daqiqa kesh), kunlik `@Scheduled` |
| 2.1 | Jadval bitta ekranda, yotiq to'r, scrollsiz | QISMAN — `TimetablePage.vue` bor, scroll bor | CSS grid `calc(100vh − header)`, 1fr qatorlar |
| 2.2 | Sinflar tab qatori, `?class=` | QISMAN — select | tablar + URL |
| 2.3 | PDF A4 landscape, 1 sahifa; Ctrl+P | YO'Q | backend PDF (OpenPDF, shrift embed) + `@page` CSS |
| 3.1 | Amallar doim ko'rinadi, sticky ustun va sarlavha | YO'Q — faqat hover da (`CrudPage.vue` `.row-actions`) | sticky CSS |
| 3.2 | Maktab kartochkasi bosilsa — maktab tanlanadi | YO'Q | `CrudPage` cards click |
| 3.3 | O'quvchilar: sinf filtri + qidiruv | QISMAN — umumiy qidiruv bor | `filters` sinf select |
| 3.4 | Sinflar kartochkalari qidiruv (debounce 250ms) | QISMAN | debounce |
| 3.5 | Xodim: lavozim, fanlar, sinflar, sinf rahbari; lavozim/holat filtri | YO'Q | DTO ga yig'ma maydonlar (1 so'rov) |
| 3.6 | Fanlar nofaol qilinadi | YO'Q — DELETE o'chiradi | `Subject.active`, toggle, qayta faollashtirish |
| 4.x | Davomat oylik kalendari | YO'Q — `AttendancePage.vue` dars bo'yicha belgilash | yangi kalendar ko'rinishi (tab), oylik API |
| 5.1 | Yakshanba/kelajak sanaga baho = 409 | YO'Q | `GradeService` validatsiya + `DateField` cheklov |
| 6.1 | Xulq yozuvlarida ota-ona, sinf, muallif | QISMAN — o'quvchi, tur, sana | DTO kengaytirish, `createdBy`, sinf filtri |
| 6.2 | Murojaat sanasi/vaqtini qo'lda tanlash | YO'Q | murojaatni qo'lda ro'yxatga olish (date-time picker, default hozir) |
| 7 | Global LoadingBar, skeleton, tugma loading | QISMAN — plugin ulangan, interceptor yo'q | axios interceptor, skeleton, `loading` |
| 8.1 | Sinf bo'yicha ota-onalar + Telegram holati | QISMAN — sinf kodlari sahifasi | yangi sahifa, 3 filtr, chat_id maskasi, deep link |
| 8.2 | Xabar: rasm/video/albom, preview, file_id qayta ishlatish | QISMAN — `BroadcastPage` faqat matn | media biriktirish, sendMediaGroup |
| 8.3 | Yuborilgan xabarlar tarixi + oluvchilar | QISMAN | oluvchilar ro'yxati, filtrlar |
| 8.4 | Bot statistikasi | QISMAN — `BotStatsPage` bor | sinf bo'yicha ulanish %, murojaatlar, javob vaqti |
| 9.x | «✉️ Ma'muriyatga xat» — har xil media, saqlash, chat sahifa | QISMAN — `ParentMessage` (matn + 1 rasm) | `Appeal` + `AppealMessage`, storage, chat UI |
| 10.1 | Runtime Java2D ni olib tashlash | BOR (olib tashlanadi) — `bot/image/*`, `WeeklyReportCardService` | statik banner PNG + file_id kesh, matnli hisobot |
| 10.2 | Thread pool, rate limit, kesh | QISMAN | bounded pool, global 25/s, Caffeine |
| 10.3 | 3000 foydalanuvchi yuklama testi | YO'Q | mock bilan test, natija shu faylga |
| R | Maktab bo'yicha cheklov (boshqa maktab = 403) | YO'Q — `schoolId` faqat parametr | `SchoolAccessService` + interceptor |

## Bajarilish
(ish davomida to'ldiriladi)
