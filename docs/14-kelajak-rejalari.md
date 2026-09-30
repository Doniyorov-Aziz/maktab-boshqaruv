# 14 — Kelajak rejalari

[← 13 — Lug'at](13-lugat.md) · [Hujjatlar ro'yxatiga →](../README.md)

## Mundarija
- [Hozirgi cheklovlar](#hozirgi-cheklovlar)
- [Rejalashtirilgan takomillashtirishlar](#rejalashtirilgan-takomillashtirishlar)

## Hozirgi cheklovlar

Bu loyiha qasddan **oddiy va o'rganish uchun tushunarli** qilib qurilgan — quyidagilar hozircha yo'q yoki soddalashtirilgan (koddan tasdiqlangan):

- **Migratsiya vositasi yo'q** (Flyway/Liquibase) — sxema `ddl-auto=update` bilan avtomatik boshqariladi, versiyalangan tarix yo'q. Batafsil: [05-malumotlar-bazasi.md](05-malumotlar-bazasi.md#migratsiyalar-bu-loyihada-qanday-ishlaydi).
- **API hujjat sahifasi yo'q** (Swagger/springdoc) — barcha endpoint qo'lda yozilgan `.md` fayllarda ([07-api.md](07-api.md)).
- **Avtomatik Entity↔DTO mapper yo'q** (MapStruct) — har bir Service'da qo'lda yozilgan `toResponseDto(...)`.
- **Frontend'da avtomatlashtirilgan testlar yo'q** — faqat lint + build + qo'lda (Playwright bilan ad-hoc) tekshiruv.
- **Docker/konteynerlashtirish yo'q** — loyiha to'g'ridan-to'g'ri JVM va Node ustida ishga tushiriladi, `Dockerfile`/`docker-compose.yml` mavjud emas.
- **CI/CD yo'q** — `.github/workflows` yoki shunga o'xshash avtomatik build/test quvuri yo'q.
- **JWT yangilanish (refresh token) mexanizmi yo'q** — token 10 soatdan keyin oddiy tugaydi, foydalanuvchi qayta login qilishi kerak.
- **Bitta til** — faqat o'zbekcha, `vue-i18n` kabi ko'p tillilik kutubxonasi ulanmagan.
- **PWA (Progressive Web App) emas** — offline ishlash, mobil qurilmaga "o'rnatish" imkoniyati yo'q (Quasar bu imkoniyatni beradi, lekin loyihada yoqilmagan).
- **Ma'lumotlar bazasida ko'p tenantlilik izolyatsiyasi yo'q** — barcha maktablar bitta bazada, bitta sxemada, faqat `school_id` bo'yicha filtrlash orqali ajratilgan (haqiqiy sxema/baza darajasidagi izolyatsiya emas). Batafsil: [02-arxitektura.md — Ko'p maktablilik](02-arxitektura.md#kop-maktablilik-multi-school-qanday-ishlaydi).
- **Foydalanuvchilar maktabga bog'lanmagan — maktablararo ruxsat izolyatsiyasi yo'q.** `User` entity'sida `School` maydoni yo'q (faqat ixtiyoriy `employee`), `schoolId` esa frontend yuboradigan oddiy so'rov parametri. Natijada istalgan `EDITOR`/`VIEWER` `schoolId`ni almashtirib, **boshqa maktab** ma'lumotlarini ko'ra oladi (`EDITOR` esa o'zgartira ham oladi). Haqiqiy ko'p maktabli foydalanish uchun `User`ni maktabga bog'lash va backend'da har bir so'rovda `schoolId`ni shu bog'lanish bo'yicha tekshirish kerak.
- **N+1 so'rov xavfi** — barcha 23 ta `@ManyToOne` bog'lanish `fetch` ko'rsatilmagan, ya'ni JPA standarti bo'yicha `EAGER`. Ro'yxat qaytaruvchi so'rovlarda Hibernate har bir qatorning bog'langan obyektlarini (masalan `LessonSlot` → sinf, fan, xodim, xona) alohida `SELECT`lar bilan yuklashi mumkin — ma'lumot ko'paygan sari sekinlashadi. Yechim: `@ManyToOne(fetch = FetchType.LAZY)` va kerakli joylarda `JOIN FETCH`/`@EntityGraph`.

## Rejalashtirilgan takomillashtirishlar

### Telegram bot — ota-onalarga bildirishnoma

**Rejalashtirilgan, hali boshlanmagan.** Maqsad: `Student.guardianPhone`ga bog'langan ota-onalarga Telegram orqali avtomatik xabar yuborish. Kelishilgan qamrov — uchala turi ham:
- **Davomat** — bola darsga kelmagan/kech qolganida.
- **Yangi baho** — o'qituvchi baho qo'yganda.
- **E'lonlar va tadbirlar** — yangi e'lon yoki yaqinlashayotgan tadbir haqida.

Texnik yondashuv (taklif): `Student`ga `telegramChatId` va bir martalik ulash kodi (`telegramLinkCode`) maydonlari qo'shiladi; ota-ona botga `/start <kod>` yuborib o'z akkauntini bog'laydi; backend'da yangi `TelegramBotService` (uzun-polling rejimida, alohida ochiq HTTP manzil talab qilmaydi) `AttendanceService`, `GradeService`, `AnnouncementService`/`CalendarEventService`ga ilova qilinadi. Bot tokeni — foydalanuvchi @BotFather orqali o'zi yaratib berishi kerak bo'ladi (bu loyihaga tashqi maxfiy qiymat).

### Boshqa yo'nalishlar

- **Boyroq PDF hisobotlar** — hozirgi print-CSS asosidagi chop etishdan tashqari, haqiqiy `jsPDF`/server-side PDF generatsiyasi, davomat/baho tabellari uchun.
- **Rollarni kengaytirish** — masalan alohida "ota-ona" roli (faqat o'z farzandi ma'lumotlarini ko'radigan), yoki sinf rahbariga maxsus huquqlar.
- **i18n** — rus/ingliz tillarini qo'shish (`vue-i18n` orqali).
- **PWA** — mobil qurilmada "ilova" sifatida o'rnatish, asosiy sahifalarni offline ko'rish.
- **Testlar** — frontend uchun Playwright'ni rasmiy `devDependency` sifatida ulab, CI'da avtomatik ishga tushiriladigan test to'plami yozish; backend uchun integratsion (`@SpringBootTest` + haqiqiy/test baza) testlar qo'shish.
- **Docker bilan deploy** — backend, frontend va PostgreSQL uchun `docker-compose.yml`, production'ga bir buyruqda chiqarish imkoni.
- **Migratsiya vositasiga o'tish** — loyiha kattalashsa, Flyway'ga o'tish (har bir sxema o'zgarishi versiyalangan SQL fayl sifatida saqlanadi, jamoada ishlash osonlashadi).

---
[← 13 — Lug'at](13-lugat.md) · [Hujjatlar ro'yxatiga →](../README.md)
