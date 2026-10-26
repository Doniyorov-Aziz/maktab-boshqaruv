# Ommaviy DEMO: Mehmon roli, tashriflar, server (hisobot)

Branch: `feature/guest-demo` (lokal, push qilinmagan). Sana: 2026-10-26.

## Natija qisqacha

- `https://<domen>/#/demo?ref=linkedin` havolasini ochgan kishi parolsiz **Mehmon** (rol `GUEST`) bo'lib
  kiradi. Token 8 soat amal qiladi.
- Mehmon admin ko'radigan hamma sahifani ko'radi, lekin hech narsani o'zgartira olmaydi. Ota-onalar
  yozishmalari, Telegram tarixi va shunga o'xshash bo'limlar yopiq.
- Administrator (`ADMIN`) huquqlari o'zgarmagan. Admin bosh sahifada "👀 Demo tashriflari" vidjetini va
  **Boshqaruv → Tashriflar** sahifasini ko'radi.
- Serverga qo'yish uchun quyidagilar tayyor: `Dockerfile`, `deploy/` (docker-compose, Caddy, zaxira
  skriptlari), `application-server.properties`, `docs/DEPLOY.md`, `docs/DEMO-SHARE.md`.

## A. Mehmon (GUEST)

### Qanday ishlaydi

| Qatlam | Nima qiladi |
|---|---|
| `POST /api/auth/guest-login` | `GUEST_LOGIN_ENABLED=true` bo'lsa umumiy `mehmon` hisobiga (rol `GUEST`, xodimga bog'lanmagan, paroli tasodifiy va noma'lum) 8 soatlik token beradi. O'chiq bo'lsa 404. Bitta IP dan soatiga 30 ta. |
| `JwtAuthFilter` | `GUEST` tokeniga `ROLE_GUEST` + `ROLE_ADMIN` beradi, shunda barcha GET'lar admin uchun qanday ochilsa, mehmon uchun ham shunday ochiladi. Demo o'chirilsa, eski mehmon tokenlari darhol ishlamay qoladi. |
| `GuestGuardFilter` + `GuestPolicy` | **Global, filtr darajasida**, har qanday controller'dan oldin ishlaydi. Faqat GET/HEAD/OPTIONS o'tadi. Qolganlariga `403 {"message":"Demo rejimida o'zgartirish mumkin emas"}` qaytadi. Yopiq bo'limlar va eksportlar GET'da ham 403. Qoida yo'lga (path) qarab ishlaydi, shuning uchun yangi endpointlar ham avtomatik himoyalanadi. |
| `GuestResponseMasker` | Mehmonga ketadigan har bir JSON javobni daraxt (tree) ko'rinishida ko'rib chiqadi. Maydon nomiga qarab maskalanadi: `chatId`, `telegramUserId`, `token`, `*Token`, `password*`, `secret`, `apiKey`, `linkCode`, `inviteLink`, `feedUrl`, `lastError`, `ipHash`… Matn ichidagi bot tokeni, `mk_…` qurilma kalitlari va `/api/public/calendar/<token>` ham maskalanadi. Ko'rinishi: `99****67`. |
| `/api/auth/login` | Bitta IP dan 5 daqiqada 10 ta urinish (`RateLimiter`). `mehmon` hisobiga parol bilan kirib bo'lmaydi. |

Mehmonga ruxsat berilgan yozish so'rovlari faqat shular: `POST /api/auth/guest-login`, `/api/auth/login`,
`/api/auth/logout`, `/api/demo/visit/ping`.

### GUEST uchun yopilgan endpointlar (GET ham 403, `reason: LOCKED`)

| Bo'lim | Yo'llar |
|---|---|
| "Maktabga xat" / murojaatlar, ularning xabarlari va fayllari | `/api/appeals`, `/api/appeals/**` (ro'yxat, `unread`, `{id}`, `{id}/attachments/{messageId}`), `/api/dashboard/my-appeals` |
| Sababli arizalar (ota-ona matni va rasmi) | `/api/absence-requests`, `/api/absence-requests/**` (`count-pending`, `{id}/photo`) |
| Telegram xabarlari tarixi / outbox (NotificationLog) | `/api/notifications`, `/api/notifications/**` (`stats`, `settings`) |
| Ota-onalarga xabar yuborish va oluvchilar | `/api/broadcasts`, `/api/broadcasts/**` (`{id}`, `{id}/recipients`) |
| Ulash kodlari, ota-ona yuborgan fayllar, mock chat | `/api/telegram/students/**`, `/api/telegram/classes/**` (kodlar), `/api/telegram/files/**`, `/api/telegram/mock/**` |
| Kutubxona: ota-ona kartasi (chat_id bo'yicha), imzolangan ruxsat xatlari, Wikisource | `/api/library/stats/parents/**`, `/api/library/permissions/*/document`, `/api/library/wikisource/**` |
| Sirlar | `/api/device-keys/**` (turniket API kalitlari), `/api/pages/calendar/feeds/**` (.ics obuna tokenlari), `/api/auth/hash/**` |
| Tashriflar statistikasi | `/api/demo/visits`, `/api/demo/visits/**` |

**Eksport (GET 403, `reason: EXPORT`).** Qoida: yo'lda `/export` bo'lsa yoki yo'l `.xlsx .xls .csv .pdf
.docx .ics .zip` bilan tugasa. Hozirgi yo'llar: `/api/attendance/calendar/export`,
`/api/pages/journal/export.xlsx`, `/api/pages/journal/export.pdf`, `/api/dashboard/risk/export`,
`/api/lesson-slots/timetable.pdf`, `/api/library/stats/journal/export`, `/api/pages/calendar/export.ics`,
`/api/library/permissions/template`. `POST /api/export/table` umumiy yozish qoidasi bo'yicha yopiq.

**Bot statistikasi ochiq** (`/api/bot/stats`): unda faqat raqam va grafiklar bor, xabar matnlari yo'q.
**Ota-onalar (Telegram)** ro'yxati ham ochiq: mehmon uni VIEWER ko'radigan shaklda ko'radi, chat_id va
taklif havolalarisiz.

### Frontend

- `#/demo?ref=…` (`DemoPage.vue`): mehmon bo'lib kiradi va bosh sahifaga o'tkazadi. Demo o'chiq bo'lsa
  yoki urinishlar ko'p bo'lsa, tushunarli xabar chiqadi.
- Login sahifasida "👀 Demo sifatida ko'rish" tugmasi bor (server ruxsat bergandagina ko'rinadi).
- Tepada "👀 Demo rejimi — barcha bo'limlarni ko'rishingiz mumkin, o'zgartirish o'chirilgan" banneri va
  "Xodim sifatida kirish" havolasi.
- Yopiq bo'limlar sidebarda o'z joyida qoladi, yonida 🔒 belgisi bor. Bosilsa yoki manzil orqali
  kirilsa, `LockedPage.vue` ochiladi: "Uzr, sizga bu bo'limga kirishga ruxsat yo'q. Ota-onalar
  yozishmalari maxfiy ma'lumot hisoblanadi."
- `AppButton` o'zi aniqlaydi: ikonka yoki matnga qarab (`utils/guest.js`: add, edit, delete, save,
  eksport, yuborish, belgilash…) o'zgartiruvchi tugma ko'rinib turadi, lekin `disabled` bo'ladi va
  "Demo rejimida o'zgartirish o'chirilgan" tooltip'i chiqadi. Kerak bo'lsa qo'lda `guest="block"` yoki
  `guest="allow"` qo'yiladi. Jadvallardagi qalam va savat tugmalari ham o'chiq.
- Tugma bo'lmagan harakatlar ham to'silgan: davomat belgilash, baho tanlagichni ochish, tadbir yaratish,
  dars va tadbirlarni sudrash (drag & drop), chorak chegaralarini va kutubxona bo'limlarini sudrash.
  Bunday urinishda faqat qisqa xabar chiqadi.
- `boot/axios.js`: mehmondan chiqqan POST/PUT/PATCH/DELETE va eksport so'rovlari **brauzerdan umuman
  chiqmaydi** (backend ham baribir rad etadi).
- Mehmon profilini, parolini yoki maktab ma'lumotlarini o'zgartira olmaydi: bular uchun barcha yozish
  so'rovlari yopiq. Maktab tanlagichi faqat qaysi maktabni ko'rishni tanlaydi.

## B. Tashriflar qanday sanaladi

- Brauzer bir yil saqlanadigan tasodifiy `visitorId` (UUID) oladi. U `localStorage`da va cookie'da turadi.
- Mehmon sahifalarni ochganda `POST /api/demo/visit/ping {visitorId, ref, paths[]}` yuboriladi.
  Birinchi so'rov darhol ketadi, keyingilari ko'pi bilan 30 soniyada bir marta, ochilgan sahifalar
  bitta paketda. Vkladka ochiq va ko'rinib turgan bo'lsa, har daqiqada "tirikman" signali ketadi, shunda
  davomiylik to'g'ri hisoblanadi. Server ham 25 soniyadan tez kelgan yozuvni qabul qilmaydi.
- **Noyob tashrifchi** = turli `visitorId` lar soni. **Tashrif (sessiya)** = `demo_visit` dagi bitta
  qator. 30 daqiqa harakatsizlikdan keyin yangi qator boshlanadi. Davomiylik = oxirgi faollik − birinchi
  kirish.
- Hisobga olinmaydi:
  - botlar va crawlerlar (User-Agent'da `bot`, `crawl`, `spider`, `preview`, `headless`, `curl`,
    `python`… bo'lsa);
  - administrator va xodimlar: login paytida brauzer `visitorId`ni yuboradi, u
    `demo_excluded_visitor`ga yoziladi va o'sha brauzerning eski tashriflari o'chiriladi;
  - mehmon bo'lmagan foydalanuvchining ping'i.
- IP manzil xom holda saqlanmaydi, faqat `SHA-256(VISIT_HASH_SALT + ":" + ip)`. Serverda Caddy orqasidagi
  haqiqiy IP `server.forward-headers-strategy=native` orqali olinadi.
- **Tashriflar** sahifasi (`VisitsPage.vue`, faqat haqiqiy ADMIN uchun):
  - KPI'lar: jami noyob ("N kishi"), bugun, shu hafta, o'rtacha ko'rish vaqti;
  - 30 kunlik grafik;
  - manbalar donuti;
  - eng ko'p ko'rilgan sahifalar;
  - qurilma va brauzerlar;
  - oxirgi 50 tashrif (qator bosilsa, sahifalar ketma-ketligi ochiladi);
  - "Ulashish havolalari" bloki: linkedin, telegram, direct havolalari va o'zingiz yozgan manba,
    nusxalash tugmasi bilan.
- Bosh sahifada (faqat ADMIN, demo yoqilgan bo'lsa): "👀 Demo tashriflari: N kishi · bugun M" →
  Tashriflar sahifasiga olib boradi.

## C. Server

- `Dockerfile`: multi-stage, Java 21 (eclipse-temurin, ARM64 va AMD64), `-XX:MaxRAMPercentage=60`,
  root bo'lmagan foydalanuvchi.
- `deploy/web.Dockerfile`: Quasar build (`QCLI_API_BASE_URL=/`, `--build-arg API_BASE_URL` bilan
  o'zgartirsa bo'ladi) va Caddy.
- `deploy/docker-compose.yml`: `db` (PostgreSQL 17, volume), `backend` (`storage` volume, `mem_limit`),
  `web` (Caddy: 80/443, avtomatik HTTPS, `/api` → backend).
- `deploy/.env.example`: faqat bo'sh joylar.
- `application-server.properties`: hamma sir `${ENV}` dan olinadi. `DEMO_ENABLED` va
  `GUEST_LOGIN_ENABLED` standart bo'yicha `true`, `TELEGRAM_BOT_ENABLED` standart bo'yicha `false`.
  `VISIT_HASH_SALT` bor, CORS faqat `https://${DOMAIN}` ga ruxsat beradi. Namuna hisoblar
  (`direktor/direktor123` va boshqalar) serverda yaratilmaydi (`app.seed.demo-users=false`).
- `deploy/backup.sh`: kunlik `pg_dump`, 7 kun saqlanadi, cron namunasi bilan. `deploy/restore.sh`.
- `docs/DEPLOY.md`: Oracle Always Free uchun qadamma-qadam qo'llanma. `docs/DEMO-SHARE.md`: LinkedIn va
  Telegram uchun o'zbek va ingliz tilidagi tayyor matnlar.

Kodda `app.guest.login-enabled` standart bo'yicha `false`. Shuning uchun kompyuterdagi jonli nusxada
(8080) demo havolasi o'zi yoqilmaydi, faqat `server` profili uni yoqadi.

## Tekshiruv

| # | Nima | Natija |
|---|---|---|
| 1 | `gradlew build` (testlar bilan, `DB_URL=…/maktab_tg_test`) | ✅ BUILD SUCCESSFUL: 66 sinf, 340 test, 0 xato (3 tasi avvaldan skip) |
| 1 | `npm run lint`, `npm run build` | ✅ xatosiz |
| 2 | `GuestDemoIntegrationTest` (10 test) | ✅ Ilovaning o'z mapping'laridan **avtomatik yig'ilgan 113 ta** POST/PUT/PATCH/DELETE → 403. Yopiq bo'limlarning 25 ta GET'i → 403 LOCKED. 8 ta eksport → 403 EXPORT. 37 ta sahifa so'rovi (dashboard, davomat, jurnal, jadval, taqvim, sinflar, xonalar, o'quvchilar, xodimlar, kutubxona, bot statistikasi, foydalanuvchilar…) → admin ham, mehmon ham 200. Mehmon javoblarida chat_id, bot tokeni, ulash kodi va parol hashi yo'q, chat_id `99****67` ko'rinishida maskalangan. Admin oldingidek ishlaydi (yopiq bo'limlar, eksport, yozish). Bitta `visitorId` ikki marta kirsa (orada 31 daqiqa): noyob = 1, sessiya = 2. 30 soniya ichidagi ping → THROTTLED. Admin brauzeri va Googlebot hisoblanmaydi. IP faqat 64 belgili xesh. |
| 2 | `GuestDemoLimitsIntegrationTest` (2 test) | ✅ `GUEST_LOGIN_ENABLED=false` bo'lsa guest-login → 404 va eski mehmon tokeni ishlamaydi. 11-login urinishi → 429. |
| 3 | Playwright `tests/ui/guest.spec.js` (9 test) | ✅ `#/demo?ref=linkedin` dashboard'ni banner bilan ochadi. 33 sahifa ochiladi, ochiq sahifalarda birorta ham 403 yo'q. "Murojaatlar" → "ruxsat yo'q". Disabled tugmalar "zo'rlab" bosilganda ham tarmoqqa **birorta ham POST/PUT/PATCH/DELETE ketmadi**. Jadvaldagi qalam bosilganda forma ochilmaydi. Tooltip ishlaydi. Admin Tashriflar sahifasida "1 kishi" va "LinkedIn" ni ko'radi, bosh sahifada "Demo tashriflari: 1 kishi". Login sahifasida demo tugmasi bor. Telefon ko'rinishi ham tekshirildi. |
| 3 | Skrinshotlar | `docs/images/guest/01…10-*.png`, hammasi ko'rib tekshirildi |
| 4 | `docker compose up` | ⚠️ **Bajarilmadi**: bu kompyuterda Docker o'rnatilmagan. O'rniga `server` profili jar bilan alohida bo'sh bazada (8082) sinab ko'rildi: faqat `admin` (ADMIN) va `mehmon` (GUEST) hisoblari yaratildi, `direktor` → 401, admin login → 200, `/api/auth/guest-enabled` → true, guest-login → 200, ping → COUNTED. Mehmon POST qilsa → 403, murojaatlarni ochsa → 403. Saqlangan `ip_hash` aynan `X-Forwarded-For` dagi IP ning xeshi (Caddy orqasida to'g'ri ishlaydi). Docker fayllarining o'zi serverda birinchi `docker compose up -d --build` da tekshiriladi. |

Playwright muhiti: backend jar nusxasi 8081 portda, `maktab_tg_test` bazasida,
`app.guest.login-enabled=true` va `app.rate-limit.enabled=false` bilan ishladi. Frontend
`QCLI_API_BASE_URL=http://localhost:8081` bilan build qilindi, `dist/spa/index.html` CSP'sida 8080 → 8081
almashtirildi. Test admini vaqtincha yaratildi va keyin o'chirildi. Ishga tushirish:

```bash
UI_API=http://localhost:8081 UI_USER=<admin> UI_PASS=<parol> npx playwright test guest.spec.js --workers=1
```

## Topilgan, lekin tegilmagan muammolar (mehmon rejimiga aloqasi yo'q, admin uchun ham bir xil)

- `GET /api/rooms?schoolId=1&page=0&size=25` test bazasida 500 qaytaradi:
  `RoomService.currentStatus` → `IncorrectResultSizeDataAccessException` (bitta xonada bir vaqtda
  ikkita "hozirgi" dars bor).
- Kutubxona muqovasi uchun fayl `storage` da bo'lmasa, `GET /api/library/books/{id}/cover` 404 emas,
  500 qaytaradi.
- `npm run lint` (oxfmt) har safar `YearsView.vue` ni qayta formatlaydi: u oxirgi commit'da formatlanmagan
  holda qolgan. Bu o'zgarish commit'ga kiritilmadi.

## O'zgargan fayllar

Backend, yangi: `config/GuestPolicy.java`, `config/GuestGuardFilter.java`, `config/GuestResponseMasker.java`,
`controller/DemoVisitController.java`, `entity/DemoVisit.java`, `entity/DemoExcludedVisitor.java`,
`repository/DemoVisitRepository.java`, `repository/DemoExcludedVisitorRepository.java`,
`service/DemoVisitService.java`, `service/GuestAccountService.java`, `service/RateLimiter.java`,
`resources/application-server.properties`.

Backend, o'zgargan: `entity/Role.java` (GUEST), `config/JwtAuthFilter.java`, `config/SecurityConfig.java`,
`config/TelegramSchemaMigration.java` (`app_user_role_check`), `config/DataSeeder.java`
(`app.seed.demo-users`), `controller/AuthController.java` (guest-login, guest-enabled, logout, rate limit,
visitorId), `controller/AttendanceAutomationController.java` (`/api/system/info` → `guestLogin`),
`dto/LoginRequest.java`, `util/JwtUtil.java`.

Testlar: `GuestDemoIntegrationTest.java`, `GuestDemoLimitsIntegrationTest.java`, `support/ApiClient.java`
(`withToken`, `header`), `test/resources/config/application.properties` (`app.rate-limit.enabled=false`),
`frontend/tests/ui/guest.spec.js`.

Frontend, yangi: `pages/DemoPage.vue`, `pages/LockedPage.vue`, `pages/VisitsPage.vue`, `utils/guest.js`,
`utils/visitor.js`, `composables/useDemoVisits.js`.

Frontend, o'zgargan: `boot/axios.js`, `stores/auth.js`, `router/index.js`, `router/routes.js`,
`layouts/MainLayout.vue`, `components/app/AppButton.vue`, `components/crud/CrudTable.vue`,
`components/crud/CrudCards.vue`, `components/crud/pages/YearDetail.vue`, `i18n/login.js`, `pages/LoginPage.vue`,
`pages/DashboardPage.vue`, `pages/AttendancePage.vue`, `pages/AttendanceCalendarPage.vue`,
`pages/GradebookPage.vue`, `pages/CalendarPage.vue`, `pages/TimetablePage.vue`,
`pages/LibraryBooksPage.vue`, `pages/LibraryCategoriesPage.vue`, `pages/LibraryPermissionsPage.vue`,
`pages/SchoolSettingsPage.vue`, `pages/StudentProfilePage.vue`.

Server va hujjatlar: `Dockerfile`, `.dockerignore`, `deploy/docker-compose.yml`, `deploy/web.Dockerfile`,
`deploy/Caddyfile`, `deploy/.env.example`, `deploy/backup.sh`, `deploy/restore.sh`, `.gitattributes`
(server fayllari uchun LF), `.gitignore` (`deploy/backups/`), `docs/DEPLOY.md`, `docs/DEMO-SHARE.md`,
`docs/guest-demo-progress.md`, `docs/images/guest/*.png`.
