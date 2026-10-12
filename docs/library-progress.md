# 📚 Kitob mutolaasi — reja va holat

Branch: `feature/library` (`main` = `a656be5` dan). Push qilinmaydi.
Sessiya uzilsa: shu fayldagi ❌ bandlardan davom eting. Testlar faqat test bazasida:
`DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test` (jonli tizim `maktab_db` da — unga tegilmaydi).

## 0. Audit — nima qayta ishlatiladi

| Mavjud mexanizm | Kutubxonada qanday ishlatiladi |
|---|---|
| `BotRouter` + `Screen` + `InputHandler` + `ParentSession.pendingAction` | `LibraryScreen` (kod `lib`) — sahifalar `editMessageText` bilan, qidiruv rejimi `pendingAction=LIB_SEARCH` |
| `Keyboards.MAIN` / `HomeScreen` | «📚 Kitob mutolaasi» tugmasi pastki menyu va bosh sahifada |
| `BotI18n` (`bot/i18n/messages_*.properties`) | barcha matnlar `lib.*` kalitlari (uz, ru; cy → uz ga qaytadi) |
| Outbox: `NotificationLog` + `NotificationSender` (25/s, 1/s/chat, 429 → `retry_after`) | PDF yuborish `BOOK_DOWNLOAD` qatori, `media="book:<id>"` |
| `OutboxMedia.Resolver` (file_id eslab qolinadi, `BroadcastMedia` kabi) | `BookMedia` — birinchi yuborishdan keyin `Book.telegramFileId`, keyin faqat file_id |
| `FileStorage` (uuid nomlar, storage ildizidan chiqib bo'lmaydi, `.gitignore` da) | `save("library", schoolId, ext, bytes)` overload — nusxa emas |
| `SchoolScopeConfig` + `SchoolOwnership` | `books/{id}`, `book-categories/{id}`, `?bookId=`, `?categoryId=` boshqa maktab uchun 403 |
| `BotScheduledJobs` + `enqueueDirect` (tinch soatlar, obuna, dedup) | juma 19:00 «📚 Dam olish kunlari uchun 3 ta kitob» (`LIBRARY_WEEKLY`) |
| `SettingsScreen.TOGGLES` / `BotSetting` | ota-ona kaliti `notifyLibrary`, maktab kaliti `libraryWeeklyEnabled` |
| `TelegramSchemaMigration` (eski CHECK cheklovlarini olib tashlash) | yangi `NotificationType` qiymatlari uchun ishlaydi |
| `ClassPicker`, `PageLayout`, `vue-chartjs`, `AuthImageDialog` (JWT blob) | statistika sahifasi, muqovalar va PDF JWT bilan blob qilib olinadi |

Ota-ona = Telegram chat (`ParentSession`); ismi/telefoni — bog'langan farzandning `guardianName/guardianPhone`
(«Ota-onalar (Telegram)» sahifasi bilan bir xil). Farzand va sinf `BookDownload` ga yozilmaydi — hisobotda
`parent_telegram_link` → `students` → `school_class` orqali olinadi.

## Bandlar

| № | Band | Holat |
|---|---|---|
| 0 | Audit va reja (shu fayl) | ✅ |
| 1 | Huquqiy qoida: `legalBasis` majburiy, ogohlantirish matni, faqat admin yuklagan fayllar, seed faqat dev + generatsiya qilingan PDF | ❌ |
| 2 | Model: BookCategory, Book, BookDownload, BookFavorite, BookRating + indekslar (+ pg_trgm bo'lsa) | ❌ |
| 3.1 | Bot: menyu tugmasi, kutubxona sahifasi | ❌ |
| 3.2 | Qidiruv (normalizatsiya: registr, o'/g' apostroflari, kirill↔lotin), 5 tadan, topilmasa bo'limlar, 10 daq / boshqa tugma → yopiladi | ❌ |
| 3.3 | Bo'lim ro'yxati 5 tadan | ❌ |
| 3.4 | Kitob kartochkasi (muqova/matn, 👍, yuklashlar, tugmalar) | ❌ |
| 3.5 | Yuklab olish: outbox, file_id kesh, BookDownload, kuniga 20 ta | ❌ |
| 3.6 | Tavsiyalar: Siz uchun / Bu hafta mashhur / Maktab tavsiya qiladi / Farzandingiz bilan o'qing | ❌ |
| 3.7 | Haftalik tavsiya (juma 19:00, kalitlar, tinch soatlar, outbox) | ❌ |
| 3.8 | i18n (uz/ru) | ❌ |
| 3.9 | Unumdorlik: 1–2 SQL, kesh 10 daq | ❌ |
| 4.1 | Sayt: kitoblar (to'r/jadval, filtrlar, forma, PDF/muqova, avto-muqova PDFBox, ko'rish, nofaol qilish, ommaviy yuklash) | ❌ |
| 4.2 | Sayt: bo'limlar CRUD, emoji, sudrab tartiblash | ❌ |
| 4.3 | Sayt: mutolaa statistikasi (KPI, grafiklar, TOP-10, jurnal, filtrlar, yon panel, sinf hisoboti, Excel) | ❌ |
| 4.4 | Maxfiylik: statistika faqat ADMIN, school_id | ❌ |
| 5 | Xavfsizlik: fayllar faqat backend orqali, uuid, callback'da faqat ID | ❌ |
| T1 | `gradlew build`, `npm run lint`, `npm run build` | ❌ |
| T2 | Backend testlar (10 ta holat) | ❌ |
| T3 | Bot simulyatsiyasi → `docs/library-bot-demo.md` | ❌ |
| T4 | Playwright skrinshotlar → `docs/images/library/` | ❌ |
| T5 | grep: token/parol/secret, `api.telegram.org/file/bot` | ❌ |
| T6 | Shu fayl: yakuniy holat, o'zgargan fayllar, sozlamalar | ❌ |

## Holat (sessiya to'xtadi — foydalanish limiti)

- ✅ Backend (2, 3.1–3.9, 4.4, 5 — server tomoni) yozildi va kompilyatsiya bo'ladi: entity, repository, `service/library/*`,
  `LibraryController`, `LibraryStatsController` (faqat ADMIN), bot `LibraryScreen`, haftalik tavsiya, dev seed, pg_trgm.
- ✅ T2: `LibraryIntegrationTest` 10/10 o'tdi (`maktab_tg_test`). Test topgan xato tuzatildi (outbox dedup kaliti = yuklash ID si).
- ❌ Qolgan: bot demo-testi → `docs/library-bot-demo.md`; butun frontend (4.1–4.3, sidebar, BotSettings kaliti);
  to'liq `gradlew build`; `npm run lint/build`; Playwright; grep tekshiruvi.
- Sozlamalar: `application.properties` → `app.library.*` (max-pdf-mb=50, daily-download-limit=20, weekly FRIDAY 19:00).
