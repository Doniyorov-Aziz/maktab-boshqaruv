# 📚 Kitob mutolaasi — reja va holat

Branch: `feature/library` (`main` = `a656be5` dan). Push qilinmagan.
Testlar faqat test bazasida: `DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test`
(jonli tizim `maktab_db` da ishlaydi, bu ishda unga tegilmagan).

## 0. Audit — nima qayta ishlatildi

| Mavjud mexanizm | Kutubxonada qanday ishlatildi |
|---|---|
| `BotRouter` + `Screen` + `InputHandler` + `ParentSession.pendingAction` | `LibraryScreen` (kod `lib`) — sahifalar `editMessageText` bilan, qidiruv rejimi `pendingAction=LIB` |
| `Keyboards.MAIN` / `HomeScreen` | «📚 Kitob mutolaasi» tugmasi pastki menyuda va bosh sahifada («Farzandlarim» yonida) |
| `BotI18n` (`bot/i18n/messages_*.properties`) | barcha matnlar `lib.*` kalitlari — uz, ru va cy |
| Outbox: `NotificationLog` + `NotificationSender` (25/s, 1/s/chat, 429 → `retry_after`) | PDF yuborish — `BOOK_DOWNLOAD` qatori, `media="book:<id>"`, tinch soatlarsiz (ota-ona o'zi so'radi) |
| `OutboxMedia.Resolver` (`BroadcastMedia` kabi) | `BookMedia` — birinchi yuborishdan keyin `Book.telegramFileId`, keyingilar faqat file_id |
| `FileStorage` (uuid nomlar, storage ildizidan chiqib bo'lmaydi, `.gitignore` da) | `save("library", …)` overload qo'shildi — nusxa emas |
| `BotBanners` (file_id kesh) | `Banner.cover(...)` — kitob muqovasi ham bir marta yuklanadi, keyin file_id |
| `SchoolScopeConfig` + `SchoolOwnership` | `books/{id}`, `categories/{id}`, `?bookId=`, `?categoryId=` — boshqa maktab uchun 403 |
| `BotScheduledJobs` + `enqueueDirect` (tinch soatlar, obuna, dedup) | juma 19:00 «📚 Dam olish kunlari uchun 3 ta kitob» (`LIBRARY_WEEKLY`) |
| `SettingsScreen.TOGGLES` / `BotSetting` | ota-ona kaliti `notifyLibrary`, maktab kaliti `libraryWeeklyEnabled` |
| `TelegramSchemaMigration` | yangi `NotificationType` qiymatlari uchun eski CHECK cheklovi allaqachon olib tashlanadi |
| `ClassPicker`, `DateField`, `PageLayout`, `vue-chartjs`, JWT bilan blob (`AuthImageDialog` usuli) | statistika sahifasi; muqova va PDF JWT bilan blob qilib olinadi |

Ota-ona = Telegram chat (`ParentSession`); ismi/telefoni — bog'langan farzandning `guardianName/guardianPhone`.
Farzand va sinf `BookDownload` ga yozilmaydi — hisobotda `parent_telegram_link → students → school_class` orqali.

## Bandlar

| № | Band | Holat |
|---|---|---|
| 0 | Audit va reja (shu fayl) | ✅ |
| 1 | Huquqiy qoida: `legalBasis` majburiy (DTO `@NotNull` + servis), forma va ommaviy yuklashda ogohlantirish matni, kitob faqat sayt orqali yuklangan fayldan, seed faqat `dev` + generatsiya qilingan «Namuna kitob 1…12» | ✅ |
| 2 | Model: BookCategory, Book, BookDownload, BookFavorite, BookRating + indekslar; `pg_trgm` GIN indeks (bo'lsa) | ✅ |
| 3.1 | Bot: menyu tugmasi, bitta «sahifa» xabari (keyingi o'tishlar edit bilan) | ✅ |
| 3.2 | Qidiruv: registr, o'/o‘/oʻ, g'/g‘, kirill↔lotin normalizatsiyasi; 5 tadan; topilmasa o'xshash bo'limlar; 10 daq harakatsizlik / boshqa menyu tugmasi / buyruq → rejim yopiladi (Ma'muriyatga xat rejimiga tegilmaydi) | ✅ |
| 3.3 | Bo'lim ro'yxati 5 tadan: «📕 Nomi — Muallif · 230 bet», har biri tugma | ✅ |
| 3.4 | Kitob kartochkasi: muqova (rasm + caption) yoki matn; annotatsiya ≤ 600; «📥 N marta», 👍/👎; tugmalar | ✅ |
| 3.5 | Yuklab olish: sendDocument «Muallif - Nomi.pdf», caption + «Yoqimli mutolaa! 📖», file_id kesh, BookDownload, kuniga 20 ta, outbox/429 | ✅ |
| 3.6 | Tavsiyalar: Siz uchun / Bu hafta mashhur / Maktab tavsiya qiladi / Farzandingiz bilan o'qing (1–4 bolalar, 5–11 o'smirlar); yangi foydalanuvchiga featured + mashhur | ✅ |
| 3.7 | Haftalik tavsiya (juma 19:00, maktab va ota-ona kalitlari, tinch soatlar, obuna, outbox) | ✅ |
| 3.8 | i18n: uz, ru (va cy) — kodda matn yo'q | ✅ |
| 3.9 | Unumdorlik: ro'yxat/qidiruv = 1 select + count, bo'lim fetch bilan (N+1 yo'q); bo'limlar va «mashhur» Caffeine kesh 10 daq; rasm chizish yo'q | ✅ |
| 4.1 | Sayt: kitoblar to'ri / jadval, qidiruv va 4 filtr + holat, forma (drag & drop PDF, magic bytes, 50 MB, sha256 dublikat ogohlantirishi, muqova jpg/png ≤ 2 MB), PDFBox avto-muqova (faqat yuklashda), PDF ni ko'rish (yangi tab, JWT), nofaol qilish, ommaviy yuklash (qoralamalar) | ✅ |
| 4.2 | Sayt: bo'limlar CRUD, emoji tanlash, sudrab (va ↑/↓, mobilda menyu) tartiblash | ✅ |
| 4.3 | Sayt: mutolaa statistikasi — KPI, 30 kunlik chiziq, bo'limlar donut, sinflar bar, TOP-10, jurnal (sana, ClassPicker, bo'lim, kitob, ota-ona; server sahifalash), ota-ona yon paneli, sinf hisoboti, Excel (.xlsx, joriy filtr) | ✅ |
| 4.4 | Maxfiylik: statistika faqat ADMIN (EDITOR/VIEWER 403, sidebar va router ham yashiradi); telefon faqat shu sahifada; `school_id` bilan cheklangan | ✅ |
| 5 | Xavfsizlik: PDF/muqova faqat backend orqali (JWT + maktab tekshiruvi), fayl nomi uuid, callback_data da faqat ID lar (kitob maktab va faollik bo'yicha qayta tekshiriladi), Telegram fayl URL lari hech qayerga yozilmaydi | ✅ |
| T1 | `gradlew build` (236 test) ✅; `npm run lint` ✅; `npm run build` ✅ | ✅ |
| T2 | Backend testlar — `LibraryIntegrationTest` 10/10 | ✅ |
| T3 | Bot simulyatsiyasi — `LibraryBotFlowIntegrationTest` → `docs/library-bot-demo.md` | ✅ |
| T4 | Playwright — `docs/images/library/` (25 ta rasm, har biri ko'rib chiqildi) | ✅ |
| T5 | grep: token/parol/secret va `api.telegram.org/file/bot` — kodda, hujjatlarda, logda va test bazasida yo'q | ✅ |
| T6 | Shu fayl | ✅ |

## Tekshiruv natijalari

**T1.** `DB_URL=…/maktab_tg_test ./gradlew build` — BUILD SUCCESSFUL, 236 test, 0 xato. `npm run lint` (oxfmt + oxlint) va
`npm run build` — xatosiz. (`npm run lint` frontend config fayllarida faqat qator oxirini CRLF→LF qiladi — ular asl holiga
qaytarildi, mazmun o'zgarmagan.)

**T2 — `LibraryIntegrationTest`:**
1. `legalBasis` siz kitob → 400 «Huquqiy asos…», bazada yozuv yo'q.
2. `.exe` ni `.pdf` deb yuklash → 400 «Fayl PDF emas».
3. 50 MB + 1 bayt → 400 «PDF juda katta».
4. «o'tkan kunlar», «O‘TKAN KUNLAR», «ўткан кунлар» — bir xil natija (muallif kirillchada ham).
5. Nofaol kitob qidiruvda yo'q, `book()` = null, yuklab bo'lmaydi (NOT_FOUND), tarix saqlanadi.
6. Boshqa maktab admini: `GET/PDF/DELETE /api/library/books/{id}` → 403; boshqa maktab ota-onasiga ko'rinmaydi.
7. Kunlik limit: 20-chisi o'tadi, 21-chisi — LIMIT.
8. Detektivdan 3 ta o'qigan ota-ona — «Siz uchun» da u o'qimagan Detektivlar birinchi (500 marta yuklangan boshqa kitobdan oldin).
9. Ikkinchi yuborish — Telegram'ga qayta yuklanmaydi, o'sha `file_id` (mock bilan).
10. Statistika EDITOR uchun 403 (overview, journal, export), ADMIN uchun 200, `.xlsx` (zip), boshqa maktab — 403.

Test topgan xato tuzatildi: outbox dedup kaliti `(student, type, reference_id, record_date, chat)` — kitob ID si bilan
bir kitobni kunda ikki marta so'rash 500 berardi; endi `reference_id` = yuklash (BookDownload) ID si.

**T3** — `docs/library-bot-demo.md`: menyu → bo'lim → kitob (muqova bilan kartochka) → PDF yuklash (BookDownload yozildi,
outbox `sendDocument` yubordi) → kirillcha qidiruv → topilmagan qidiruv → tavsiyalar (4 blok) → boshqa menyu tugmasi
qidiruv rejimini yopadi → haftalik tavsiya xabari. Transkriptdagi ulanish kodi yashirilgan.
Qayta yaratish: `LIBRARY_DEMO_OUT=docs/library-bot-demo.md ./gradlew test --tests '*LibraryBotFlowIntegrationTest'`.

**T4** — test backend (8082, `local,dev`, mock Telegram, test bazasi) + build qilingan frontend:
`01-books-grid`, `02-categories`, `03-stats` — dark / light / mobile; `04` jadval; `05` filtr; `06` bo'sh forma
(ogohlantirish); `07` PDF emas → rad; `08` to'ldirilgan forma + dublikat ogohlantirishi; `09` dublikat tasdig'i;
`10` qo'shilgan kitob (avto-muqova); `11` tahrirlash; `12` ommaviy yuklash; `13` ClassPicker bilan jurnal;
`14`/`15` ota-ona yon paneli (light/dark); `16` bot sozlamalaridagi kalit. Excel eksport: 4871 bayt, haqiqiy zip.
Konsol xatosi / ogohlantirishi va 4xx/5xx API javoblari — 0. Ko'rib chiqishda topilgan kamchiliklar tuzatilib, rasmlar
qayta olindi (bo'limlar sahifasi mobilda, holat tugmalari, KPI izohi, ommaviy yuklashda PDF ni darhol tekshirish).
Sinov kitobi va uning fayllari keyin o'chirildi.

## Yangi sozlamalar (`src/main/resources/application.properties`)

| Kalit | Standart | Muhit o'zgaruvchisi |
|---|---|---|
| `app.library.max-pdf-mb` | 50 | `LIBRARY_MAX_PDF_MB` |
| `app.library.max-cover-mb` | 2 | `LIBRARY_MAX_COVER_MB` |
| `app.library.daily-download-limit` | 20 | `LIBRARY_DAILY_LIMIT` |
| `app.library.search-idle-minutes` | 10 | — |
| `app.library.weekly-day` / `weekly-time` | FRIDAY / 19:00 | — |
| `spring.servlet.multipart.max-file-size` | 20MB → **60MB** (ilovalar 20 MB chegarasi kodda tekshiriladi) | — |

Maktab kaliti: «Bot sozlamalari» → «📚 Haftalik kitob tavsiyasi». Ota-ona kaliti: botdagi «⚙️ Sozlamalar».
Yangi kutubxona: `org.apache.pdfbox:pdfbox:3.0.5` (bet soni va avto-muqova).

## O'zgargan fayllar

**Backend — yangi:** `entity/{Book, BookCategory, BookDownload, BookFavorite, BookRating, LibraryEnums}`,
`repository/Book*Repository`, `service/library/{LibraryCatalog, LibraryAdminService, LibraryStatsService, LibraryFiles,
LibraryText, LibrarySettings, LibraryDefaults, LibraryWeekly, BookMedia}`, `controller/{LibraryController,
LibraryStatsController}`, `dto/LibraryDtos`, `bot/screens/LibraryScreen`, `config/{LibraryDemoSeeder (faqat dev),
LibrarySearchIndex}`, `util/XlsxWriter`.

**Backend — o'zgargan:** `BotRouter` (qidiruv rejimini yopish), `Keyboards`, `HomeScreen`, `SettingsScreen`, `BotBanners`
(muqova), `NotificationType` (+BOOK_DOWNLOAD, LIBRARY_WEEKLY), `NotificationSettings`, `ParentSession` (+notifyLibrary),
`BotSetting`/`BotSettingDto`/`BotSettingService` (+libraryWeeklyEnabled), `NotificationService` (on-demand → tinch
soatlarsiz), `NotificationSender` (kutubxona xabarlari — ota-ona darajasida obuna), `BotScheduledJobs`, `FileStorage`
(area + delete), `SchoolOwnership`/`SchoolScopeConfig`, `GlobalExceptionHandler` (413 matni), i18n uz/ru/cy,
`application.properties`, `build.gradle`.

**Frontend — yangi:** `pages/{LibraryBooksPage, LibraryCategoriesPage, LibraryStatsPage}.vue`, `components/BookCover.vue`,
`utils/library.js`. **O'zgargan:** `router/routes.js`, `layouts/MainLayout.vue` («Kutubxona» guruhi),
`pages/BotSettingsPage.vue`, `utils/telegram.js`.

**Testlar:** `LibraryIntegrationTest`, `LibraryBotFlowIntegrationTest` (yangi), `KeyboardsTest` (menyu: «📚 Kitob
mutolaasi» «Farzandlarim» yonida).

**Hujjatlar:** shu fayl, `docs/library-bot-demo.md`, `docs/images/library/*.png`.

## Eslatmalar

- 🇬🇧 kabi bayroq emojilari Windows brauzerida «GB» harflari bo'lib ko'rinadi (Windows shriftida bayroqlar yo'q);
  Telegram'da to'g'ri chiqadi.
- Jonli tizimga o'tkazish: merge → `maktab-live\update.ps1` (avval `maktab_db` zaxirasi). Jadvallar Hibernate
  `ddl-auto=update` bilan o'zi yaratiladi; standart bo'limlar maktab kutubxonasi birinchi ochilganda yaratiladi;
  `dev` seed jonli (`local`) profilda ishlamaydi.
