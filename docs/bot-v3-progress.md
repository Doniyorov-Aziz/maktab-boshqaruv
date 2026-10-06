# Ota-onalar boti v3 — premium

Branch: `feature/bot-v3` (`main` dan). Sessiya uzilsa — shu fayldan davom eting.

## 0. Audit (v2 holati, kod yozishdan oldin)

| # | Band | v2 holati | Reja |
|---|---|---|---|
| 1.1 | Menyuda Mini App tugmasi (WebAppInfo) | QISMAN — bor, nomi «📱 Kundalikni ochish» | «📱 Ilovani ochish» |
| 1.2 | Alohida yengil Vue sahifa, 360–430px | BOR — `/#/webapp`, `WebAppLayout` | — |
| 1.3 | initData HMAC-SHA256 + auth_date ≤ 24 soat, faqat o'z farzandi, aks holda 403 | BOR — `TelegramInitDataValidator`, `ParentApiController` | API darajasida 403 testi qo'shiladi |
| 1.4 | themeParams, haptic, expand | BOR | — |
| 1.5 | BackButton, MainButton | YO'Q | qo'shiladi |
| 1.6 | a) Bosh: «Hozir: 3-dars …» jonli progress bar, davomat badge, oxirgi 3 baho | QISMAN — «▶ hozir» belgisi, bugungi baholar | jonli kartochka + oxirgi 3 baho |
| 1.7 | b) Jadval: hafta kunlari tablari, bugun ajratilgan, hozirgi dars, fan rang/emoji | QISMAN — Bugun/Ertaga/Hafta | kun tablari, fan ranglari |
| 1.8 | c) Baholar: o'rtacha kartochkalar, 30 kunlik chiziq, so'nggi baholar, sinf o'rtachasi | QISMAN — o'rtacha bar, so'nggi baholar | 30 kunlik dinamika, sinf o'rtachasi bilan taqqoslash |
| 1.9 | d) Davomat: oylik kalendar issiqlik xaritasi, foiz | BOR | — |
| 1.10 | e) E'lonlar va tadbirlar | QISMAN — faqat e'lonlar | tadbirlar qo'shiladi |
| 1.11 | Skeleton, bo'sh holat, xato holati, pull-to-refresh | QISMAN — skeleton, xato | bo'sh holat illustratsiyasi, pull-to-refresh |
| 1.12 | Bir nechta farzand: avatar-switcher | QISMAN — matnli chiplar | avatarlar |
| 2.1 | Ertalabki digest 07:30 (ish kunlari, yakshanba/bayramda emas) | YO'Q | yangi xabar turi + jadval |
| 2.2 | Kechki jadval + «ertaga kerakli narsalar» | QISMAN — tadbirlar bor | muddatli e'lonlar qo'shiladi |
| 2.3 | Haftalik hisobot rasm-kartochka 1080×1350 + «📱 Batafsil» | QISMAN — matnli hisobot | Java2D rasm, Mini App tugmasi |
| 2.4 | Real-time: «📱 Batafsil», «💬 Sinf rahbariga yozish»; past baho 2–3 yumshoq | QISMAN — «🔎 Batafsil», chegara 2 | ikki tugma, chegara 3 |
| 2.5 | Outbox, AFTER_COMMIT, retry, 429 | BOR | — |
| 3.1 | Xabar turlarini yoqish/o'chirish (digest bilan) | QISMAN — digest yo'q | digest tugmasi |
| 3.2 | Tinch soatlar, tongda bitta jamlangan xabar | QISMAN — har xabar alohida | jamlangan xabar |
| 3.3 | Til uz/ru (bot va Mini App, messages fayllari) | BOR — uz/cy/ru | yangi matnlar ham fayllarda |
| 3.4 | Farzandni tanlash va «Botdan uzish» | BOR | — |
| 4.1 | setMyCommands (uz/ru), tavsif, menu button = Mini App | BOR | menu button nomi yangilanadi |
| 4.2 | Bo'lim = bitta «sahifa» (editMessageMedia/Text) | BOR | — |
| 4.3 | Noma'lum matn: do'stona javob + menyu | BOR | — |
| 4.4 | Rate limit: 1 s da 3 tadan ortiq — yumshoq ogohlantirish | QISMAN — 2/s | 3/s |
| 5.1 | Faqat dev profilda demo ota-ona va 1 hafta ma'lumot | YO'Q | `@Profile("dev")` seeder |
| 5.2 | docs/bot-v3-demo.md (5 daqiqalik ssenariy) | YO'Q | yoziladi |

## Bajarilish

| # | Band | Holat | Qayerda |
|---|---|---|---|
| 1.1 | «📱 Ilovani ochish» tugmasi + menu button | ✅ | `menu.webapp`, `bot.menu_button` (i18n), `Keyboards`, `HomeScreen`, `BotProfileInitializer` |
| 1.2 | Mobil sahifa 360–430px | ✅ | `frontend/src/layouts/WebAppLayout.vue`, `pages/webapp/*` |
| 1.3 | initData HMAC + auth_date ≤ 24 soat, boshqa farzand = 403 | ✅ | `ParentApiSecurityIntegrationTest` (200/403, buzilgan/eskirgan/yo'q → 401) |
| 1.4 | themeParams, haptic, expand | ✅ | `webapp/telegram.js` (impact/notification/selection haptic) |
| 1.5 | BackButton, MainButton | ✅ | ichki bo'limlarda ‹ Orqaga → «Bugun»; MainButton «🔄 Yangilash» |
| 1.6 | Bosh: «Hozir: N-dars» jonli progress, davomat badge, oxirgi 3 baho | ✅ | `WebTodayPage.vue`, `webapp/time.js` |
| 1.7 | Jadval: hafta kunlari tablari, bugun, hozirgi dars, fan rang/emoji | ✅ | `WebSchedulePage.vue`, `icons.js#subjectColor` |
| 1.8 | Baholar: o'rtacha kartochkalar, 30 kunlik chiziq, sinf o'rtachasi (ismsiz) | ✅ | `WebGradesPage.vue`; API `trend`, `classAverages` |
| 1.9 | Davomat issiqlik xaritasi + foiz | ✅ | (v2) + xato holati, pull-to-refresh |
| 1.10 | E'lonlar va tadbirlar | ✅ | `WebAnnouncementsPage.vue`, `GET /api/parent/students/{id}/events` |
| 1.11 | Skeleton, bo'sh holat illustratsiyasi, xato, pull-to-refresh | ✅ | `components/webapp/WaState.vue`, `webapp/useLoad.js`, `WebAppLayout.vue` |
| 1.12 | Avatar-switcher | ✅ | initsiallar + rang, faol farzand halqa bilan |
| 2.1 | Ertalabki digest 07:30, yakshanba/bayramda emas | ✅ | `BotScheduledJobs#runMorningDigests`, `BotScheduledJobsTest` |
| 2.2 | Kechki jadval + «🎒 Ertaga kerak» | ✅ | `notif.tomorrow_needs` (ertaga muddati tugaydigan e'lonlar) |
| 2.3 | Haftalik rasm 1080×1350 + «📱 Batafsil» | ✅ | `BotImageRenderer#weeklyCard`, `WeeklyReportCardService`, `WeeklyCardRenderTest` |
| 2.4 | «📱 Batafsil» + «💬 Sinf rahbariga yozish», 2–3 baho yumshoq | ✅ | `NotificationService#actions`, chegara 3 (yangi maktablar uchun) |
| 2.5 | Outbox, AFTER_COMMIT, retry, 429 | ✅ | (v2) + rasmli qator `sendPhoto` |
| 3.1 | Digest/haftalik hisobot yoqish-o'chirish | ✅ | `SettingsScreen` («md» tugmasi) |
| 3.2 | Tinch soatlar → tongda bitta jamlangan xabar | ✅ | `NotificationSender#deliverQuietBundle`, `NotificationSenderTest` |
| 3.3 | Til uz/ru (messages fayllari) | ✅ | yangi kalitlar uz/cy/ru, `BotI18nTest#v3TextsExistInUzbekAndRussian` |
| 3.4 | Farzand tanlash, botdan uzish | ✅ | (v2) |
| 4.1 | setMyCommands uz/ru, tavsif, menu button | ✅ | (v2) + nom «📱 Ilova» |
| 4.2 | Sahifa-xabar | ✅ | (v2) |
| 4.3 | Noma'lum matnga do'stona javob | ✅ | (v2) |
| 4.4 | Rate limit 3/s | ✅ | `telegram.chat-rate-per-second=3` |
| 5.1 | Demo ota-ona faqat `dev` profilda | ✅ | `config/DemoParentSeeder.java` (`@Profile("dev")`) |
| 5.2 | `docs/bot-v3-demo.md` | ✅ | skrinshotlar bilan |

### Tekshiruv natijalari
1. `./gradlew build` — ✅ 187 test, 0 xato. `npm run lint` + `npm run build` — ✅ xatosiz.
2. Unit/integratsiya testlari — ✅ HMAC (to'g'ri/buzilgan/eskirgan), boshqa farzand = 403,
   tinch soatlar jamlanmasi, digest yakshanba/bayramda yo'q, i18n uz/ru.
3. Haftalik rasm — ✅ `docs/images/bot/v3/weekly-report.png` (mock backend orqali haqiqiy oqim),
   `weekly-report-attention.png` (⚠️ holati, `WeeklyCardRenderTest`). Ikkalasi ko'zdan kechirildi.
4. Mini App Playwright 390×844, yorug' va qorong'i — ✅ `docs/images/bot/v3/webapp-*.png`
   (bugun, jadval, davomat, baholar, fan tafsiloti, e'lonlar, tadbirlar, skeleton, xato, pull-to-refresh).
   initData faqat test skriptida mock kalit bilan imzolangan; avtomatik tekshiruvlar: BackButton,
   MainButton, expand, 2 avatar, «Hozir» kartasi, 6 kun tabi, grafik, skeleton, xato, pull-to-refresh —
   hammasi ✅, konsol xatosi yo'q.
5. Sirlar grep (`[0-9]{8,10}:[A-Za-z0-9_-]{35}`, password/secret qiymatlari) — ✅ token yo'q;
   `application-local.properties` `.gitignore` da. (Faqat avvaldan bor dev default `ADMIN_PASSWORD:admin123`.)

### O'zgargan fayllar
Backend: `entity/{NotificationType,ParentSession,NotificationSettings,NotificationLog,BotSetting}.java`,
`service/{NotificationService,NotificationSender,BotScheduledJobs,WeeklyReportCardService}.java`,
`service/parent/{ParentDataService,ParentViews}.java`, `repository/{GradeRepository,NotificationLogRepository}.java`,
`controller/{ParentApiController,BotAdminController}.java`, `bot/image/BotImageRenderer.java`,
`bot/screens/SettingsScreen.java`, `telegram/TelegramProperties.java`, `config/DemoParentSeeder.java`,
`resources/bot/i18n/messages_{uz,cy,ru}.properties`.
Testlar: `ParentApiSecurityIntegrationTest`, `BotScheduledJobsTest`, `WeeklyCardRenderTest`,
`NotificationSenderTest`, `BotI18nTest`, `KeyboardsTest`.
Frontend: `layouts/WebAppLayout.vue`, `pages/webapp/{WebToday,WebSchedule,WebGrades,WebAttendance,WebAnnouncements}Page.vue`,
`components/webapp/WaState.vue`, `webapp/{telegram,state,icons,i18n,time,useLoad}.js`.
Hujjatlar: `docs/bot-v3-progress.md`, `docs/bot-v3-demo.md`, `docs/images/bot/v3/*`.

### Qanday ishga tushiriladi
Backendni **qayta ishga tushirish shart** (yangi ustunlar `ddl-auto=update` bilan o'zi qo'shiladi):

```powershell
# jonli v2 ni to'xtatish
.\build\run\stop-v2.ps1
# yangi jar
.\gradlew.bat bootJar
# qayta yoqish (token application-local.properties dan o'qiladi)
.\build\run\start-v2.ps1
# frontend
cd frontend; npm run build   # yoki npm run dev
```

Demo ma'lumot kerak bo'lsa — profil `local,dev` (batafsil: `docs/bot-v3-demo.md`).
Prod da `dev` profilni yoqmang.
