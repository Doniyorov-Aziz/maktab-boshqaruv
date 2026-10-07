# v4 â€” progress

Branch: `feature/v4` (`main` = v3 dan, 36a67e1). Worktree: `.claude/worktrees/v4`. Har bo'lim alohida
commit, push qilinmagan. Parol, JWT secret va bot token kodga, hujjatga, logga va commitlarga tushmagan
(token faqat `application-local.properties` da, u `.gitignore` da).

## Bajarilish

| Bo'lim | Holat | Commit |
|---|---|---|
| 1. Login animatsiyasiz + xodim holati (ishda / ta'tilda / ishdan ketgan) | âœ… | 97dcd73 |
| 2. Dars jadvali bitta ekranda, sinf tablari, A4 yotiq PDF, Ctrl+P | âœ… | 4a347f2 |
| 3. CRUD: sticky amallar, kartochkalar, filtrlar, xodim ma'lumoti, nofaol fanlar | âœ… | 8c2beed |
| 4. Davomat oylik kalendari | âœ… | d738ff4 |
| 5. Yakshanba / kelajak sanaga baho = 409 | âœ… | 7f6222d |
| 6.1 Xulq yozuvlari: sinf, ota-ona, muallif | âœ… | ad00b5f |
| 6.2 Murojaatni qo'lda kiritish (sana + vaqt) | âœ… | cfbd7ef |
| 7. Global loading bar, skeleton, tugma loading | âœ… | 5314824 |
| 8. Telegram admin: ota-onalar, media bilan xabar, tarix, statistika | âœ… | 338740c |
| 9. Â«âœ‰ï¸ Ma'muriyatga xatÂ»: bot oqimi, fayllarni saqlash, chat sahifa | âœ… | cfbd7ef, 72a8846 |
| 10. Bot unumdorligi (Java2D yo'q, file_id, pool, kesh, yuklama testi) | âœ… | a5289a3, f192fe4 |
| Ruxsatlar: hamma so'rov o'z maktabi bilan cheklangan (403) | âœ… | 3660073 |
| Yakuniy tekshiruv (pastda) | âœ… | shu commit |

## 0. Audit (ishdan oldingi holat)

| # | Band | Oldin | Endi |
|---|---|---|---|
| 1.1 | Login: Â«Muvaffaqiyatli âœ“Â» animatsiyasi | BOR (olib tashlanishi kerak edi) | âœ… olib tashlandi |
| 1.2 | Xodim holati, login 403, JWT da 401, avto-qaytish, oxirgi admin | YO'Q | âœ… |
| 2.1 | Jadval bitta ekranda, scrollsiz | QISMAN â€” scroll bor edi | âœ… |
| 2.2 | Sinf tablari, `?class=` | QISMAN â€” select | âœ… |
| 2.3 | PDF A4 yotiq, 1 sahifa; Ctrl+P | YO'Q | âœ… |
| 3.1 | Amallar doim ko'rinadi, sticky | YO'Q â€” faqat hover da | âœ… |
| 3.2 | Maktab kartochkasi bosiladi | YO'Q | âœ… |
| 3.3 | O'quvchilar: sinf filtri + qidiruv | QISMAN | âœ… |
| 3.4 | Sinflar kartochkalari qidiruvi | QISMAN | âœ… |
| 3.5 | Xodim: fanlar, sinflar, sinf rahbari; filtrlar | YO'Q | âœ… |
| 3.6 | Fanlar nofaol qilinadi | YO'Q â€” DELETE o'chirardi | âœ… |
| 4 | Davomat oylik kalendari | YO'Q | âœ… |
| 5 | Yakshanba / kelajak sanaga baho = 409 | YO'Q | âœ… |
| 6.1 | Xulq: ota-ona, sinf, muallif | QISMAN | âœ… |
| 6.2 | Murojaat sana/vaqtini qo'lda tanlash | YO'Q | âœ… |
| 7 | Global LoadingBar, skeleton, tugma loading | QISMAN â€” interceptor yo'q edi | âœ… |
| 8.1 | Sinf bo'yicha ota-onalar + Telegram holati | QISMAN | âœ… |
| 8.2 | Xabar: rasm/video/albom, preview, file_id | QISMAN â€” faqat matn | âœ… |
| 8.3 | Yuborilgan xabarlar tarixi + oluvchilar | QISMAN | âœ… |
| 8.4 | Bot statistikasi | QISMAN | âœ… |
| 9 | Â«âœ‰ï¸ Ma'muriyatga xatÂ» â€” har xil media, saqlash, chat | QISMAN â€” matn + 1 rasm | âœ… |
| 10.1 | Runtime Java2D | BOR (olib tashlanishi kerak edi) | âœ… olib tashlandi |
| 10.2 | Thread pool, rate limit, kesh | QISMAN | âœ… |
| 10.3 | 3000 foydalanuvchi yuklama testi | YO'Q | âœ… |
| R | Maktab bo'yicha cheklov (403) | YO'Q â€” faqat `schoolId` parametri | âœ… |

## Bo'limlar qisqacha va o'zgargan fayllar

Yo'llar: `be/` = `src/main/java/uz/azizbek/maktabboshqaruv/`, `fe/` = `frontend/src/`, `test/` = `src/test/java/â€¦/`.

**1. Login va xodim holati.** Login darhol bosh sahifaga o'tadi (animatsiya yo'q). `Employee.status`
(ACTIVE / ON_LEAVE / DISMISSED) + `leaveFrom/leaveTo`. Ta'tildagi yoki ishdan ketgan xodim login qilsa 403;
ishlab turgan paytda bloklansa keyingi so'rov 401 (`JwtAuthFilter`, 1 daqiqalik kesh), sahifa
`/login?blocked=1` ga qaytadi va sababini yozadi. Har kuni 00:05 da ta'tili tugaganlar avtomatik qaytadi.
Admin o'zini va oxirgi faol adminni bloklay olmaydi. Xodimlar ro'yxatida holat badge va filtr.
Fayllar: `be/entity/{Employee,EmployeeStatus}`, `be/service/{AccountStatusService,EmployeeService}`,
`be/config/{JwtAuthFilter,SecurityConfig}`, `be/controller/{AuthController,EmployeeController}`,
`fe/pages/LoginPage.vue`, `fe/boot/axios.js`, `fe/config/modules.js`, `fe/pages/CrudPage.vue`; testlar
`AccountBlockingIntegrationTest`, `AccountStatusServiceTest`.

**2. Dars jadvali.** Yotiq to'r 1366Ã—768 va 1920Ã—1080 da scrollsiz sig'adi; sinf tablari va `?class=`;
backend PDF (OpenPDF, Inter shrifti ichida) â€” A4 yotiq, 1 sinf = 1 sahifa; `@page landscape` bilan Ctrl+P.
Fayllar: `be/service/TimetablePdfService`, `be/controller/LessonSlotController`, `fe/pages/TimetablePage.vue`,
`resources/fonts/*`; test `TimetablePdfServiceTest`.

**3. CRUD.** Amallar ustuni doim ko'rinadi va sticky; maktab va sinf kartochkalari bosiladi; o'quvchilar
sinf filtri + qidiruv; sinflar kartochkalari qidiruvi; xodimda fanlar, sinflar, sinf rahbari;
fanlar o'chirilmaydi â€” nofaol bo'ladi (eski baholarda nomi qoladi, yangi bahoda tanlanmaydi).
Fayllar: `be/service/{SubjectService,StudentService,GradeService,â€¦}`, `be/entity/Subject`,
`fe/pages/CrudPage.vue`, `fe/config/modules.js`, `fe/pages/GradebookPage.vue`; test `SubjectLifecycleIntegrationTest`.

**4. Davomat oylik kalendari.** `/attendance/calendar`: parallel â†’ sinf, oy; kun ustuni highlight va xulosa
paneli; katakni bosib holatni o'zgartirish (optimistik, darhol saqlanadi); legenda va oylik foiz.
Fayllar: `be/service/AttendanceCalendarService`, `be/controller/AttendanceController`,
`fe/pages/AttendanceCalendarPage.vue`; test `AttendanceCalendarIntegrationTest`.

**5. Baho sanasi.** Yakshanba yoki kelajak sanaga baho = 409; sana tanlagichda bu kunlar yopiq.
Fayllar: `be/service/GradeService`, `fe/components/DateField.vue`; test `GradeDateIntegrationTest`.

**6.1 Xulq yozuvlari** â€” sinf, ota-ona (ism, telefon), kim yozgan; sinf filtri (fetch join, N+1 yo'q).
**6.2** â€” Â«Murojaat qo'shishÂ»: telefon yoki shaxsan kelgan murojaat, haqiqiy sana va vaqt bilan.

**7. Yuklanish.** Yuqorida ingichka LoadingBar (axios interceptor, fon so'rovlari `background:true` bilan
ko'rinmaydi), birinchi yuklanishda skeleton, saqlash tugmasi ikki marta bosilsa bitta yozuv.

**8. Telegram admin.**
- 8.1 Â«Ota-onalar (Telegram)Â» (`/telegram-parents`): sinf â†’ o'quvchi, ota-ona, telefon, Telegram holati
  (âœ… ulangan + sana / âŒ ulanmagan); filtr Barchasi / Botga ulanganlar / Ulanmaganlar; chat_id faqat ADMIN
  ga va qisman (`12****89`); taklif havolasi (deep link) faqat EDITOR+ ga â€” bittalab yoki hammasi birga.
- 8.2 Â«Ota-onalarga xabarÂ»: butun maktab / sinflar / tanlangan ota-onalar (ro'yxatdan, link id bilan â€”
  chat_id brauzerga chiqmaydi); matn + 10 tagacha rasm/video/fayl (oq ro'yxat, 20 MB, 10 MB dan katta
  rasm hujjat sifatida); Telegram ko'rinishidagi preview; outbox orqali yuboriladi, albom `sendMediaGroup`,
  fayl Telegramga bir marta yuklanadi va `file_id` saqlanib qolganlarga shu id bilan ketadi; 429 da
  `retry_after`; sahifa Â«1250/3000 yuborildiÂ» progressini ko'rsatadi.
- 8.3 Tarix: sinf, holat, sana oralig'i va matn bo'yicha filtr; har xabar uchun oluvchilar (ism, sinf,
  ota-ona, holat, vaqt) sinf / holat / ism filtri bilan.
- 8.4 Statistika: 30 kunlik navbatga / yetkazildi / xato grafigi, sinf bo'yicha ulanish % (past sinflar
  birinchi), murojaatlar soni va o'rtacha birinchi javob vaqti.
- Fayllar: `be/service/{BroadcastService,BroadcastMedia,TelegramParentsService,BotStatsService}`,
  `be/controller/{BroadcastController,TelegramParentsController}`, `be/entity/{BroadcastAttachment,Broadcast,BroadcastAudience}`,
  `be/repository/{BroadcastAttachmentRepository,BroadcastRepository,NotificationLogRepository,ParentTelegramLinkRepository}`,
  `be/config/TelegramSchemaMigration` (eski `broadcast_audience_check` cheklovi olib tashlanadi),
  `fe/pages/{TelegramParentsPage,BroadcastPage,BotStatsPage}.vue`; test `TelegramAdminIntegrationTest`.

**9. Ota-onadan maktabga xat.**
- Bot: Â«âœ‰ï¸ Ma'muriyatga xatÂ» (eski Â«ðŸ’¬ Maktabga yozishÂ» shu oqimga aylandi) â†’ kimga (Ma'muriyat / Sinf
  rahbari) â†’ bir nechta farzand bo'lsa farzand â†’ matn, rasm, video, ovozli, audio, hujjat, dumaloq video,
  albom (albomga bitta âœ“) â†’ Â«âœ… YuborishÂ» â†’ Â«Murojaat raqami: #NÂ». 30 daqiqa harakatsizlikda yig'ilgani
  avtomatik yuboriladi. Kuniga 10 ta murojaat, bittasida 20 ta xabar, fayl 20 MB; oshsa do'stona javob.
  Â«â†©ï¸ Javob yozishÂ» shu murojaatni davom ettiradi (yopilgani qayta ochiladi). Albom (bir lahzada ko'p update)
  chat tezlik chegarasidan o'tkazib yuboriladi â€” birorta rasm yo'qolmaydi.
- Saqlash: fayl kelishi bilan alohida pool da yuklab olinadi va `app.storage.path/telegram/{schoolId}/{yyyy-MM}/{uuid}.ext`
  ga yoziladi; DB da file_id, file_unique_id, tur, mime, hajm, davomiylik, yo'l, asl nom. Telegram fayl
  havolasi (token bilan) DB, log va API ga tushmaydi. Oq ro'yxat; exe/bat/js/html/svg va Â«.pdfÂ» deb nomlangan
  skript rad etiladi.
- Platforma (`/parent-messages`): chapda ro'yxat (ota-ona, farzand, sinf, kimga, oxirgi xabar, vaqt,
  o'qilmaganlar, holat), filtr holat / sinf / kimga / sana, qidiruv ism yoki #raqam; o'ngda chat â€” rasm
  (lightbox), video, ovozli xabar (davomiylik + yuklab olish), fayl kartasi, albom to'ri; javob qutisi
  (matn + fayllar) â†’ outbox â†’ Â«ðŸ« Maktabdan javob (#N)Â»; Â«YopishÂ»; sidebar badge (30 s polling) + toast.
  Fayllar 30 daqiqalik imzolangan havola bilan, HTTP Range qo'llab-quvvatlanadi. Sinf rahbariga yozilganini
  o'sha sinf rahbari va ADMIN ko'radi.
- Eski `parent_message` yozuvlari bir marta `appeal` ga ko'chiriladi (`AppealMigration`).
- Fayllar: `be/entity/{Appeal,AppealMessage,AppealEnums}`, `be/service/appeal/*`, `be/service/OutboxMedia`,
  `be/controller/AppealController`, `be/bot/screens/MessageScreen`, `be/bot/BotRouter`,
  `be/telegram/{TelegramModels,TelegramClient,HttpTelegramClient,MockTelegramClient}`,
  `fe/pages/AppealsPage.vue`, `fe/layouts/MainLayout.vue`, i18n; test `AppealFlowIntegrationTest`.

**10. Bot unumdorligi.** Java2D butunlay olib tashlandi; bo'lim bannerlari statik PNG
(`resources/bot/banners/`), Telegramga bir marta yuklanib file_id bilan yuboriladi (`bot_asset`);
haftalik hisobot matnli + Â«ðŸ“± BatafsilÂ»; update lar alohida pool da (8â€“16 oqim, navbat 2000, chat bo'yicha
tartib); outbox ~25/s global, 1/s chat; Caffeine kesh (farzandlar, jadval, tadbirlar) + invalidatsiya;
fetch join, statistikaga JDBC batch.

**Ruxsatlar.** Yangi endpointlar `@PreAuthorize`: ko'rish VIEWER+, o'zgartirish EDITOR+, ommaviy xabar ADMIN.
Maktabga bog'langan foydalanuvchi boshqa maktab yozuviga â€” yo'lda (`/api/students/12`, `/api/profiles/class/5`, â€¦),
parametrda (`schoolId`, `schoolClassId`, `classId`, `studentId`, â€¦) yoki JSON tanada â€” 403 oladi
(`be/config/SchoolScopeConfig`, `be/service/SchoolOwnership` â€” keshlangan bitta kichik so'rov);
maktablar va foydalanuvchilar ro'yxatida faqat o'z maktabi. Maktabsiz super admin cheklanmaydi.
Test `SchoolScopeIntegrationTest`.

## Ishga tushirish

1. **Backend qayta ishga tushirilishi shart** (yangi jadvallar va ustunlar `ddl-auto=update` bilan
   avtomatik qo'shiladi: `appeal`, `appeal_message`, `broadcast_attachment`, `bot_asset`,
   `employee.status/leave_from/leave_to`, `subject.active`, `behavior_record.created_by`,
   `broadcast.chat_ids/media_count`, `notification_log.media`). Birinchi ishga tushishda:
   eski `broadcast_audience_check` cheklovi olib tashlanadi va `parent_message` yozuvlari murojaatlarga ko'chiriladi.
2. **Fayllar papkasi** â€” `app.storage.path` (standart `./storage`, backend ishga tushgan papkaga nisbatan).
   Prod da doimiy disk bering: muhit o'zgaruvchisi `APP_STORAGE_PATH=/var/lib/maktab/storage`
   (yoki `application-local.properties` da `app.storage.path=...`). Papka zaxira nusxaga qo'shilsin; gitga tushmaydi.
3. Yangi sozlamalar (ixtiyoriy, standartlari bor): `spring.servlet.multipart.max-file-size=20MB`,
   `max-request-size=210MB`; `TELEGRAM_UPDATE_THREADS=8`, `TELEGRAM_UPDATE_MAX_THREADS=16`, `TELEGRAM_UPDATE_QUEUE=2000`.
4. Frontend: `npm run build` (yoki `quasar dev`). Agar API boshqa domenda bo'lsa, `frontend/index.html`
   dagi CSP ga shu manzilni `img-src`/`media-src` ga qo'shing (murojaat rasmlari va videolari uchun).
5. Yuklama testi: `./gradlew loadTest` (oddiy `test` ga kirmaydi). Testlar: `DB_URL=jdbc:postgresql://â€¦/maktab_tg_test ./gradlew build`.
   Testlar paytida shu test bazasiga ulangan boshqa backend ishlamasin â€” uning outbox yuboruvchisi testning
   xabarlarini Â«o'g'irlabÂ» yuboradi va mock tekshiruvlari bo'sh qoladi.

## Yakuniy tekshiruv (TEKSHIRUV 1â€“9)

| # | Tekshiruv | Natija |
|---|---|---|
| 1 | `./gradlew build` (testlar bilan), `npm run lint`, `npm run build` | âœ… 215 test, 0 xato; lint va build xatosiz |
| 2 | Backend testlar: ta'tildagi login 403; bloklangan â†’ 401; oxirgi admin; yakshanba 409; nofaol fan; boshqa maktab va murojaat fayli 403; murojaat oqimi (matn, rasm, video, voice, document, albom) + storage; javob â†’ outbox â†’ chat_id; taqiqlangan tur va > 20 MB rad; Â«api.telegram.org/file/botÂ» yo'q | âœ… `AccountBlockingIntegrationTest`, `GradeDateIntegrationTest`, `SubjectLifecycleIntegrationTest`, `AppealFlowIntegrationTest` (5 test), `TelegramAdminIntegrationTest` (4), `SchoolScopeIntegrationTest` (2) |
| 3 | Playwright 1440Ã—900, dark va light â€” login, jadval tablari + PDF (> 0 bayt), davomat kalendari, o'quvchilar sinf filtri, maktab kartochkasi, sticky amallar, ota-onalar 3 filtri, xabar formasi (rasm bilan preview), bot statistikasi, murojaatlar (ro'yxat, chat: matn/rasm/video/ovoz/fayl, javob yuborish, badge) | âœ… 8 skript, ~85 tekshiruv, konsolda xato yo'q |
| 4 | Jadval 1366Ã—768 va 1920Ã—1080 da scroll yo'q; `timetable-1366.png`, `timetable-1920.png`; PDF â†’ rasm: 1 sahifa, yotiq, hamma darslar | âœ… scroll = oyna o'lchami; PDF 842Ã—595 pt, 1 sahifa (`timetable-pdf-1.png`); Â«hamma sinflarÂ» PDF 22 sahifa |
| 5 | Har sahifa skrinshoti `docs/images/v4/` da, ochib tekshirildi | âœ… (ro'yxat pastda) |
| 6 | `BufferedImage`, `Graphics2D`, `ImageIO.write` â€” `src/main` da | âœ… natija bo'sh |
| 7 | Yuklama testi natijasi | âœ… pastda |
| 8 | Token/parol/secret repoda va commitlarda yo'q; `storage/` `.gitignore` da | âœ… `git grep` va `git log -p` bo'sh; `git check-ignore storage/â€¦` âœ“ |
| 9 | Shu fayl: âœ…/âŒ, fayllar, ishga tushirish, `app.storage.path` | âœ… |

DB, backend logi va test chiqishlarida Â«api.telegram.org/fileÂ» qidirildi â€” 0 ta. Telegram fayl yuklashdagi
xato matni ham endi havolani o'z ichiga olmaydi (faqat HTTP kodi).

### 10.3 yuklama testi (`./gradlew loadTest`, Telegram mock, tarmoq vaqtisiz)
```
Foydalanuvchilar: 3000, har raundda 12000 update (/start, jadval, baholar, davomat), 200/s
1-raund (birinchi kirish):   o'rtacha 3.2 ms, p50 3.1 ms, p95 6.1 ms, p99 8.9 ms, max 22.6 ms
                             4.19 DB so'rov/update; heap â‰¤ 203 MB; CPU ~2.1% (28 yadro)
2-raund (qaytgan ota-onalar): o'rtacha 2.9 ms, p50 3.0 ms, p95 4.9 ms, p99 5.9 ms, max 14.1 ms
                             2.78 DB so'rov/update; heap â‰¤ 198 MB; CPU ~1.5%
Maqsad p95 < 300 ms â€” bajarildi. Optimizatsiyadan oldin: 37.4 so'rov/update (eager N+1).
```

### Skrinshotlar (`docs/images/v4/`)
login-light/dark, login-blocked Â· timetable-1366(-dark), timetable-1920(-dark), timetable-mobile, timetable-pdf-1 Â·
employees-light/dark, employees-narrow-sticky, students-filter-light/dark, classes-cards-light/dark,
schools-cards-light/dark, subjects-light/dark Â· attendance-calendar-light/dark, attendance-calendar-day-light Â·
behavior-light/dark Â· loading-skeleton Â· telegram-parents-light/dark, telegram-parents-notlinked Â·
broadcast-preview, broadcast-history, broadcast-recipients Â· bot-stats-light/dark, bot-stats-classes-light/dark Â·
appeals-chat-light/dark, appeals-album-dark, appeals-lightbox, appeals-reply-sent, appeals-manual, appeals-manual-time, appeals-phone.

### Qo'shimcha tuzatishlar (yakuniy tekshiruvdan keyin)
- **Qarindoshlik:** `Student.guardianRelation` (FATHER / MOTHER / OTHER, ixtiyoriy, eski yozuvlar uchun bo'sh).
  O'quvchi formasida Â«Kim bo'ladiÂ» tanlovi; ota-onalar sahifasida Â«Furqat Karimov, otasiÂ», davomat
  kalendarida Â«Ota: Furqat KarimovÂ» / Â«Ona: â€¦Â» (belgilanmagan bo'lsa Â«Ota-ona: â€¦Â»). Mavjud yozuvlar
  taxmin bilan to'ldirilmaydi â€” ma'lumotni maktab kiritadi; demo seeder yangi ota-onaga qarindoshlikni yozadi.
- **8.3 tarix filtrlari:** ro'yxatning o'zida ham sinf (xabar shu sinf ota-onasiga borgan) va holat
  (oluvchilardan kamida biri shu holatda: yetkazilgan / navbatda / xatosi bor) filtri.
- **429 da albom takrorlanmaydi:** media xabar qismlarga bo'linadi (har albom / fayl, keyin matn),
  `notification_log.media_parts` nechta qism yetkazilganini saqlaydi; qayta urinish qolganidan davom etadi
  (`OutboxMediaTest`).
- Testlar: 218 ta, hammasi o'tdi.
