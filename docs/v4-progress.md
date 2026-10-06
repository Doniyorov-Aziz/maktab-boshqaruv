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

| Bo'lim | Holat | Commit |
|---|---|---|
| 1. Login + ta'tildagi xodim bloki | ✅ | 97dcd73 |
| 2. Dars jadvali bitta ekranda + PDF | ✅ | 4a347f2 |
| 3. CRUD: sticky amallar, kartochkalar, filtrlar, nofaol fanlar | ✅ | 8c2beed |
| 4. Davomat oylik kalendari | ✅ | d738ff4 |
| 5. Yakshanba/kelajak bahosi 409 | ✅ | 7f6222d |
| 6.1 Xulq yozuvlari | ✅ | ad00b5f |
| 6.2 Murojaat sanasi/vaqti (qo'lda) | ⏳ 9-bo'lim ichida qilinadi | — |
| 7. Global loading | ✅ | 5314824 |
| 10. Bot unumdorligi | ⏳ WIP (pastga qarang) | WIP commit |
| 8. Telegram admin sahifalari | ❌ hali yo'q | — |
| 9. Ota-ona murojaatlari (bot + saqlash + chat) | ❌ hali yo'q | — |
| R. Maktab bo'yicha cheklov (403) | ❌ hali yo'q | — |
| Yakuniy tekshiruv | ❌ | — |

### Sessiya uzilsa — shu yerdan davom eting
**10-bo'lim (WIP commit) holati:** Java2D butunlay olib tashlandi (`bot/image/*`, `WeeklyReportCardService`),
statik bannerlar `resources/bot/banners/*.png` + `BotBanners` (file_id `bot_asset` jadvalida),
rasmli tugmalar → «📱 Batafsil» (`MiniAppLinks`), haftalik hisobot matnli (`WeeklyReportService`),
`BotUpdateDispatcher` (8–16 thread, navbat 2000, chat bo'yicha tartib), `BotCache` (Caffeine:
chat→farzandlar 5 daq, sinf jadvali 10 daq, maktab tadbirlari 5 daq, yozishda evict), fetch-join so'rovlar,
statistika JDBC batch, sessiya faqat o'zgarganda saqlanadi, outbox oralig'i 250 ms.
Yuklama testi (`gradlew loadTest`, natija pastda) — p95 5–6 ms, issiq trafikda 2.78 so'rov/update.

**Keyingi qadam (aniq):**
1. Unit-testlarni tuzatish — 11 ta yiqilgan: `BotCache` mock qo'shish (EmployeeServiceTest, LessonSlotServiceTest,
   LinkingServiceTest, NotificationSenderTest, ParentAccessServiceTest), `BotScreensSeedIntegrationTest:172`
   (banner endi statik: sendPhoto/sendPhotoById tekshiruvi), so'ng `gradlew test` yashil → 10-bo'lim yakuniy commit.
2. 9-bo'lim (Appeal/AppealMessage, bot oqimi, storage, chat sahifa, 6.2 date-time) → commit.
3. 8-bo'lim (ota-onalar sahifasi, media bilan xabar + file_id, tarix, statistika) → commit.
4. R: SchoolAccess (schoolId interceptor + murojaat fayli 403) + testlar.
5. Yakuniy tekshiruv (TEKSHIRUV 1–9), skrinshotlar, shu faylni to'ldirish.

Ish muhiti: worktree `.claude/worktrees/v4`, branch `feature/v4`; test backend skripti
`~/.claude/jobs/924c677b/tmp/v4/run-v4.ps1` (port 8082, jar nusxasi — jonli bot 8080 ga tegilmaydi);
Playwright skriptlari shu papkada (`check-*.cjs`, `lib.cjs`). Testlar: `DB_URL=…/maktab_tg_test`.

### 10.3 yuklama testi natijasi
```
Foydalanuvchilar: 3000, har raundda 12000 update (/start, jadval, baholar, davomat), 200/s, Telegram mock
1-raund (birinchi murojaat): o'rtacha 3.3 ms, p95 6.3 ms, p99 9.6 ms; 4.19 so'rov/update; heap ≤113 MB; CPU ~1.2%
2-raund (qaytgan ota-onalar): o'rtacha 3.0 ms, p95 5.1 ms, p99 6.0 ms; 2.78 so'rov/update; heap ≤114 MB; CPU ~0.9%
Optimizatsiyadan oldin: 37.4 so'rov/update (eager N+1).
```
