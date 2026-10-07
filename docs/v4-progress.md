# v4 — progress

Branch: `feature/v4` (`main` = v3 dan, 36a67e1). Worktree: `.claude/worktrees/v4`. Har bo'lim alohida
commit, push qilinmagan. Parol, JWT secret va bot token kodga, hujjatga, logga va commitlarga tushmagan
(token faqat `application-local.properties` da, u `.gitignore` da).

## Bajarilish

| Bo'lim | Holat | Commit |
|---|---|---|
| 1. Login animatsiyasiz + xodim holati (ishda / ta'tilda / ishdan ketgan) | ✅ | 97dcd73 |
| 2. Dars jadvali bitta ekranda, sinf tablari, A4 yotiq PDF, Ctrl+P | ✅ | 4a347f2 |
| 3. CRUD: sticky amallar, kartochkalar, filtrlar, xodim ma'lumoti, nofaol fanlar | ✅ | 8c2beed |
| 4. Davomat oylik kalendari | ✅ | d738ff4 |
| 5. Yakshanba / kelajak sanaga baho = 409 | ✅ | 7f6222d |
| 6.1 Xulq yozuvlari: sinf, ota-ona, muallif | ✅ | ad00b5f |
| 6.2 Murojaatni qo'lda kiritish (sana + vaqt) | ✅ | cfbd7ef |
| 7. Global loading bar, skeleton, tugma loading | ✅ | 5314824 |
| 8. Telegram admin: ota-onalar, media bilan xabar, tarix, statistika | ✅ | 338740c |
| 9. «✉️ Ma'muriyatga xat»: bot oqimi, fayllarni saqlash, chat sahifa | ✅ | cfbd7ef, 72a8846 |
| 10. Bot unumdorligi (Java2D yo'q, file_id, pool, kesh, yuklama testi) | ✅ | a5289a3, f192fe4 |
| Ruxsatlar: hamma so'rov o'z maktabi bilan cheklangan (403) | ✅ | 3660073 |
| Yakuniy tekshiruv (pastda) | ✅ | shu commit |

## 0. Audit (ishdan oldingi holat)

| # | Band | Oldin | Endi |
|---|---|---|---|
| 1.1 | Login: «Muvaffaqiyatli ✓» animatsiyasi | BOR (olib tashlanishi kerak edi) | ✅ olib tashlandi |
| 1.2 | Xodim holati, login 403, JWT da 401, avto-qaytish, oxirgi admin | YO'Q | ✅ |
| 2.1 | Jadval bitta ekranda, scrollsiz | QISMAN — scroll bor edi | ✅ |
| 2.2 | Sinf tablari, `?class=` | QISMAN — select | ✅ |
| 2.3 | PDF A4 yotiq, 1 sahifa; Ctrl+P | YO'Q | ✅ |
| 3.1 | Amallar doim ko'rinadi, sticky | YO'Q — faqat hover da | ✅ |
| 3.2 | Maktab kartochkasi bosiladi | YO'Q | ✅ |
| 3.3 | O'quvchilar: sinf filtri + qidiruv | QISMAN | ✅ |
| 3.4 | Sinflar kartochkalari qidiruvi | QISMAN | ✅ |
| 3.5 | Xodim: fanlar, sinflar, sinf rahbari; filtrlar | YO'Q | ✅ |
| 3.6 | Fanlar nofaol qilinadi | YO'Q — DELETE o'chirardi | ✅ |
| 4 | Davomat oylik kalendari | YO'Q | ✅ |
| 5 | Yakshanba / kelajak sanaga baho = 409 | YO'Q | ✅ |
| 6.1 | Xulq: ota-ona, sinf, muallif | QISMAN | ✅ |
| 6.2 | Murojaat sana/vaqtini qo'lda tanlash | YO'Q | ✅ |
| 7 | Global LoadingBar, skeleton, tugma loading | QISMAN — interceptor yo'q edi | ✅ |
| 8.1 | Sinf bo'yicha ota-onalar + Telegram holati | QISMAN | ✅ |
| 8.2 | Xabar: rasm/video/albom, preview, file_id | QISMAN — faqat matn | ✅ |
| 8.3 | Yuborilgan xabarlar tarixi + oluvchilar | QISMAN | ✅ |
| 8.4 | Bot statistikasi | QISMAN | ✅ |
| 9 | «✉️ Ma'muriyatga xat» — har xil media, saqlash, chat | QISMAN — matn + 1 rasm | ✅ |
| 10.1 | Runtime Java2D | BOR (olib tashlanishi kerak edi) | ✅ olib tashlandi |
| 10.2 | Thread pool, rate limit, kesh | QISMAN | ✅ |
| 10.3 | 3000 foydalanuvchi yuklama testi | YO'Q | ✅ |
| R | Maktab bo'yicha cheklov (403) | YO'Q — faqat `schoolId` parametri | ✅ |

## Bo'limlar qisqacha va o'zgargan fayllar

Yo'llar: `be/` = `src/main/java/uz/azizbek/maktabboshqaruv/`, `fe/` = `frontend/src/`, `test/` = `src/test/java/…/`.

**1. Login va xodim holati.** Login darhol bosh sahifaga o'tadi (animatsiya yo'q). `Employee.status`
(ACTIVE / ON_LEAVE / DISMISSED) + `leaveFrom/leaveTo`. Ta'tildagi yoki ishdan ketgan xodim login qilsa 403;
ishlab turgan paytda bloklansa keyingi so'rov 401 (`JwtAuthFilter`, 1 daqiqalik kesh), sahifa
`/login?blocked=1` ga qaytadi va sababini yozadi. Har kuni 00:05 da ta'tili tugaganlar avtomatik qaytadi.
Admin o'zini va oxirgi faol adminni bloklay olmaydi. Xodimlar ro'yxatida holat badge va filtr.
Fayllar: `be/entity/{Employee,EmployeeStatus}`, `be/service/{AccountStatusService,EmployeeService}`,
`be/config/{JwtAuthFilter,SecurityConfig}`, `be/controller/{AuthController,EmployeeController}`,
`fe/pages/LoginPage.vue`, `fe/boot/axios.js`, `fe/config/modules.js`, `fe/pages/CrudPage.vue`; testlar
`AccountBlockingIntegrationTest`, `AccountStatusServiceTest`.

**2. Dars jadvali.** Yotiq to'r 1366×768 va 1920×1080 da scrollsiz sig'adi; sinf tablari va `?class=`;
backend PDF (OpenPDF, Inter shrifti ichida) — A4 yotiq, 1 sinf = 1 sahifa; `@page landscape` bilan Ctrl+P.
Fayllar: `be/service/TimetablePdfService`, `be/controller/LessonSlotController`, `fe/pages/TimetablePage.vue`,
`resources/fonts/*`; test `TimetablePdfServiceTest`.

**3. CRUD.** Amallar ustuni doim ko'rinadi va sticky; maktab va sinf kartochkalari bosiladi; o'quvchilar
sinf filtri + qidiruv; sinflar kartochkalari qidiruvi; xodimda fanlar, sinflar, sinf rahbari;
fanlar o'chirilmaydi — nofaol bo'ladi (eski baholarda nomi qoladi, yangi bahoda tanlanmaydi).
Fayllar: `be/service/{SubjectService,StudentService,GradeService,…}`, `be/entity/Subject`,
`fe/pages/CrudPage.vue`, `fe/config/modules.js`, `fe/pages/GradebookPage.vue`; test `SubjectLifecycleIntegrationTest`.

**4. Davomat oylik kalendari.** `/attendance/calendar`: parallel → sinf, oy; kun ustuni highlight va xulosa
paneli; katakni bosib holatni o'zgartirish (optimistik, darhol saqlanadi); legenda va oylik foiz.
Fayllar: `be/service/AttendanceCalendarService`, `be/controller/AttendanceController`,
`fe/pages/AttendanceCalendarPage.vue`; test `AttendanceCalendarIntegrationTest`.

**5. Baho sanasi.** Yakshanba yoki kelajak sanaga baho = 409; sana tanlagichda bu kunlar yopiq.
Fayllar: `be/service/GradeService`, `fe/components/DateField.vue`; test `GradeDateIntegrationTest`.

**6.1 Xulq yozuvlari** — sinf, ota-ona (ism, telefon), kim yozgan; sinf filtri (fetch join, N+1 yo'q).
**6.2** — «Murojaat qo'shish»: telefon yoki shaxsan kelgan murojaat, haqiqiy sana va vaqt bilan.

**7. Yuklanish.** Yuqorida ingichka LoadingBar (axios interceptor, fon so'rovlari `background:true` bilan
ko'rinmaydi), birinchi yuklanishda skeleton, saqlash tugmasi ikki marta bosilsa bitta yozuv.

**8. Telegram admin.**
- 8.1 «Ota-onalar (Telegram)» (`/telegram-parents`): sinf → o'quvchi, ota-ona, telefon, Telegram holati
  (✅ ulangan + sana / ❌ ulanmagan); filtr Barchasi / Botga ulanganlar / Ulanmaganlar; chat_id faqat ADMIN
  ga va qisman (`12****89`); taklif havolasi (deep link) faqat EDITOR+ ga — bittalab yoki hammasi birga.
- 8.2 «Ota-onalarga xabar»: butun maktab / sinflar / tanlangan ota-onalar (ro'yxatdan, link id bilan —
  chat_id brauzerga chiqmaydi); matn + 10 tagacha rasm/video/fayl (oq ro'yxat, 20 MB, 10 MB dan katta
  rasm hujjat sifatida); Telegram ko'rinishidagi preview; outbox orqali yuboriladi, albom `sendMediaGroup`,
  fayl Telegramga bir marta yuklanadi va `file_id` saqlanib qolganlarga shu id bilan ketadi; 429 da
  `retry_after`; sahifa «1250/3000 yuborildi» progressini ko'rsatadi.
- 8.3 Tarix: sana oralig'i va matn qidiruvi; har xabar uchun oluvchilar (ism, sinf, ota-ona, holat, vaqt)
  sinf / holat / ism filtri bilan.
- 8.4 Statistika: 30 kunlik navbatga / yetkazildi / xato grafigi, sinf bo'yicha ulanish % (past sinflar
  birinchi), murojaatlar soni va o'rtacha birinchi javob vaqti.
- Fayllar: `be/service/{BroadcastService,BroadcastMedia,TelegramParentsService,BotStatsService}`,
  `be/controller/{BroadcastController,TelegramParentsController}`, `be/entity/{BroadcastAttachment,Broadcast,BroadcastAudience}`,
  `be/repository/{BroadcastAttachmentRepository,BroadcastRepository,NotificationLogRepository,ParentTelegramLinkRepository}`,
  `be/config/TelegramSchemaMigration` (eski `broadcast_audience_check` cheklovi olib tashlanadi),
  `fe/pages/{TelegramParentsPage,BroadcastPage,BotStatsPage}.vue`; test `TelegramAdminIntegrationTest`.

**9. Ota-onadan maktabga xat.**
- Bot: «✉️ Ma'muriyatga xat» (eski «💬 Maktabga yozish» shu oqimga aylandi) → kimga (Ma'muriyat / Sinf
  rahbari) → bir nechta farzand bo'lsa farzand → matn, rasm, video, ovozli, audio, hujjat, dumaloq video,
  albom (albomga bitta ✓) → «✅ Yuborish» → «Murojaat raqami: #N». 30 daqiqa harakatsizlikda yig'ilgani
  avtomatik yuboriladi. Kuniga 10 ta murojaat, bittasida 20 ta xabar, fayl 20 MB; oshsa do'stona javob.
  «↩️ Javob yozish» shu murojaatni davom ettiradi (yopilgani qayta ochiladi). Albom (bir lahzada ko'p update)
  chat tezlik chegarasidan o'tkazib yuboriladi — birorta rasm yo'qolmaydi.
- Saqlash: fayl kelishi bilan alohida pool da yuklab olinadi va `app.storage.path/telegram/{schoolId}/{yyyy-MM}/{uuid}.ext`
  ga yoziladi; DB da file_id, file_unique_id, tur, mime, hajm, davomiylik, yo'l, asl nom. Telegram fayl
  havolasi (token bilan) DB, log va API ga tushmaydi. Oq ro'yxat; exe/bat/js/html/svg va «.pdf» deb nomlangan
  skript rad etiladi.
- Platforma (`/parent-messages`): chapda ro'yxat (ota-ona, farzand, sinf, kimga, oxirgi xabar, vaqt,
  o'qilmaganlar, holat), filtr holat / sinf / kimga / sana, qidiruv ism yoki #raqam; o'ngda chat — rasm
  (lightbox), video, ovozli xabar (davomiylik + yuklab olish), fayl kartasi, albom to'ri; javob qutisi
  (matn + fayllar) → outbox → «🏫 Maktabdan javob (#N)»; «Yopish»; sidebar badge (30 s polling) + toast.
  Fayllar 30 daqiqalik imzolangan havola bilan, HTTP Range qo'llab-quvvatlanadi. Sinf rahbariga yozilganini
  o'sha sinf rahbari va ADMIN ko'radi.
- Eski `parent_message` yozuvlari bir marta `appeal` ga ko'chiriladi (`AppealMigration`).
- Fayllar: `be/entity/{Appeal,AppealMessage,AppealEnums}`, `be/service/appeal/*`, `be/service/OutboxMedia`,
  `be/controller/AppealController`, `be/bot/screens/MessageScreen`, `be/bot/BotRouter`,
  `be/telegram/{TelegramModels,TelegramClient,HttpTelegramClient,MockTelegramClient}`,
  `fe/pages/AppealsPage.vue`, `fe/layouts/MainLayout.vue`, i18n; test `AppealFlowIntegrationTest`.

**10. Bot unumdorligi.** Java2D butunlay olib tashlandi; bo'lim bannerlari statik PNG
(`resources/bot/banners/`), Telegramga bir marta yuklanib file_id bilan yuboriladi (`bot_asset`);
haftalik hisobot matnli + «📱 Batafsil»; update lar alohida pool da (8–16 oqim, navbat 2000, chat bo'yicha
tartib); outbox ~25/s global, 1/s chat; Caffeine kesh (farzandlar, jadval, tadbirlar) + invalidatsiya;
fetch join, statistikaga JDBC batch.

**Ruxsatlar.** Yangi endpointlar `@PreAuthorize`: ko'rish VIEWER+, o'zgartirish EDITOR+, ommaviy xabar ADMIN.
Maktabga bog'langan foydalanuvchi boshqa maktab yozuviga — yo'lda (`/api/students/12`, `/api/profiles/class/5`, …),
parametrda (`schoolId`, `schoolClassId`, `classId`, `studentId`, …) yoki JSON tanada — 403 oladi
(`be/config/SchoolScopeConfig`, `be/service/SchoolOwnership` — keshlangan bitta kichik so'rov);
maktablar va foydalanuvchilar ro'yxatida faqat o'z maktabi. Maktabsiz super admin cheklanmaydi.
Test `SchoolScopeIntegrationTest`.

## Ishga tushirish

1. **Backend qayta ishga tushirilishi shart** (yangi jadvallar va ustunlar `ddl-auto=update` bilan
   avtomatik qo'shiladi: `appeal`, `appeal_message`, `broadcast_attachment`, `bot_asset`,
   `employee.status/leave_from/leave_to`, `subject.active`, `behavior_record.created_by`,
   `broadcast.chat_ids/media_count`, `notification_log.media`). Birinchi ishga tushishda:
   eski `broadcast_audience_check` cheklovi olib tashlanadi va `parent_message` yozuvlari murojaatlarga ko'chiriladi.
2. **Fayllar papkasi** — `app.storage.path` (standart `./storage`, backend ishga tushgan papkaga nisbatan).
   Prod da doimiy disk bering: muhit o'zgaruvchisi `APP_STORAGE_PATH=/var/lib/maktab/storage`
   (yoki `application-local.properties` da `app.storage.path=...`). Papka zaxira nusxaga qo'shilsin; gitga tushmaydi.
3. Yangi sozlamalar (ixtiyoriy, standartlari bor): `spring.servlet.multipart.max-file-size=20MB`,
   `max-request-size=210MB`; `TELEGRAM_UPDATE_THREADS=8`, `TELEGRAM_UPDATE_MAX_THREADS=16`, `TELEGRAM_UPDATE_QUEUE=2000`.
4. Frontend: `npm run build` (yoki `quasar dev`). Agar API boshqa domenda bo'lsa, `frontend/index.html`
   dagi CSP ga shu manzilni `img-src`/`media-src` ga qo'shing (murojaat rasmlari va videolari uchun).
5. Yuklama testi: `./gradlew loadTest` (oddiy `test` ga kirmaydi). Testlar: `DB_URL=jdbc:postgresql://…/maktab_tg_test ./gradlew build`.
   Testlar paytida shu test bazasiga ulangan boshqa backend ishlamasin — uning outbox yuboruvchisi testning
   xabarlarini «o'g'irlab» yuboradi va mock tekshiruvlari bo'sh qoladi.

## Yakuniy tekshiruv (TEKSHIRUV 1–9)

| # | Tekshiruv | Natija |
|---|---|---|
| 1 | `./gradlew build` (testlar bilan), `npm run lint`, `npm run build` | ✅ 215 test, 0 xato; lint va build xatosiz |
| 2 | Backend testlar: ta'tildagi login 403; bloklangan → 401; oxirgi admin; yakshanba 409; nofaol fan; boshqa maktab va murojaat fayli 403; murojaat oqimi (matn, rasm, video, voice, document, albom) + storage; javob → outbox → chat_id; taqiqlangan tur va > 20 MB rad; «api.telegram.org/file/bot» yo'q | ✅ `AccountBlockingIntegrationTest`, `GradeDateIntegrationTest`, `SubjectLifecycleIntegrationTest`, `AppealFlowIntegrationTest` (5 test), `TelegramAdminIntegrationTest` (4), `SchoolScopeIntegrationTest` (2) |
| 3 | Playwright 1440×900, dark va light — login, jadval tablari + PDF (> 0 bayt), davomat kalendari, o'quvchilar sinf filtri, maktab kartochkasi, sticky amallar, ota-onalar 3 filtri, xabar formasi (rasm bilan preview), bot statistikasi, murojaatlar (ro'yxat, chat: matn/rasm/video/ovoz/fayl, javob yuborish, badge) | ✅ 8 skript, ~85 tekshiruv, konsolda xato yo'q |
| 4 | Jadval 1366×768 va 1920×1080 da scroll yo'q; `timetable-1366.png`, `timetable-1920.png`; PDF → rasm: 1 sahifa, yotiq, hamma darslar | ✅ scroll = oyna o'lchami; PDF 842×595 pt, 1 sahifa (`timetable-pdf-1.png`); «hamma sinflar» PDF 22 sahifa |
| 5 | Har sahifa skrinshoti `docs/images/v4/` da, ochib tekshirildi | ✅ (ro'yxat pastda) |
| 6 | `BufferedImage`, `Graphics2D`, `ImageIO.write` — `src/main` da | ✅ natija bo'sh |
| 7 | Yuklama testi natijasi | ✅ pastda |
| 8 | Token/parol/secret repoda va commitlarda yo'q; `storage/` `.gitignore` da | ✅ `git grep` va `git log -p` bo'sh; `git check-ignore storage/…` ✓ |
| 9 | Shu fayl: ✅/❌, fayllar, ishga tushirish, `app.storage.path` | ✅ |

DB, backend logi va test chiqishlarida «api.telegram.org/file» qidirildi — 0 ta. Telegram fayl yuklashdagi
xato matni ham endi havolani o'z ichiga olmaydi (faqat HTTP kodi).

### 10.3 yuklama testi (`./gradlew loadTest`, Telegram mock, tarmoq vaqtisiz)
```
Foydalanuvchilar: 3000, har raundda 12000 update (/start, jadval, baholar, davomat), 200/s
1-raund (birinchi kirish):   o'rtacha 3.2 ms, p50 3.1 ms, p95 6.1 ms, p99 8.9 ms, max 22.6 ms
                             4.19 DB so'rov/update; heap ≤ 203 MB; CPU ~2.1% (28 yadro)
2-raund (qaytgan ota-onalar): o'rtacha 2.9 ms, p50 3.0 ms, p95 4.9 ms, p99 5.9 ms, max 14.1 ms
                             2.78 DB so'rov/update; heap ≤ 198 MB; CPU ~1.5%
Maqsad p95 < 300 ms — bajarildi. Optimizatsiyadan oldin: 37.4 so'rov/update (eager N+1).
```

### Skrinshotlar (`docs/images/v4/`)
login-light/dark, login-blocked · timetable-1366(-dark), timetable-1920(-dark), timetable-mobile, timetable-pdf-1 ·
employees-light/dark, employees-narrow-sticky, students-filter-light/dark, classes-cards-light/dark,
schools-cards-light/dark, subjects-light/dark · attendance-calendar-light/dark, attendance-calendar-day-light ·
behavior-light/dark · loading-skeleton · telegram-parents-light/dark, telegram-parents-notlinked ·
broadcast-preview, broadcast-history, broadcast-recipients · bot-stats-light/dark, bot-stats-classes-light/dark ·
appeals-chat-light/dark, appeals-album-dark, appeals-lightbox, appeals-reply-sent, appeals-manual, appeals-manual-time, appeals-phone.

### Eslatmalar
- Albom va matn bitta outbox qatori: Telegram 429 albomdan keyin, matndan oldin kelsa, qayta urinishda
  albom yana ketadi (fayllar file_id bilan — qayta yuklanmaydi). Kam uchraydi; to'liq yechim — har bir
  qismni alohida qatorga bo'lish.
- Ota-onalar ro'yxatidagi «Furqat, otasi» — `guardianName` va Telegram ismi ko'rsatiladi; qarindoshlik
  turi (otasi/onasi) uchun ma'lumot modelida maydon yo'q.
