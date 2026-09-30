# 11 — Muammolar va yechimlar

[← 10 — O'rnatish va ishga tushirish](10-ornatish-va-ishga-tushirish.md) · Keyingisi: [12 — Yangi modul qo'shish →](12-yangi-modul-qoshish.md)

## Mundarija
- [Login cheksiz aylanadi](#login-cheksiz-aylanadi)
- [Login bosilganda IntelliJ debugger'da to'xtab qoladi](#login-bosilganda-intellij-debuggerda-toxtab-qoladi)
- [8080-port band](#8080-port-band)
- [JDK versiyasi mos emas](#jdk-versiyasi-mos-emas)
- [Bazaga ulanib bo'lmaydi](#bazaga-ulanib-bolmaydi)
- [Flyway/Liquibase xatolari](#flywayliquibase-xatolari)
- [CORS xatosi](#cors-xatosi)
- [401/403 xatolari](#401403-xatolari)
- [LazyInitializationException](#lazyinitializationexception)
- [Ikonkalar o'rniga matn chiqadi](#ikonkalar-orniga-matn-chiqadi)
- [Dark mode'da matn ko'rinmaydi](#dark-modeda-matn-korinmaydi)

## Login cheksiz aylanadi

**Alomat**: Login tugmasi bosiladi, "Yuklanmoqda" holatida qoladi yoki hech narsa bo'lmaydi, xato ham chiqmaydi.

**Sabab**: 99% holatda — **backend ishlamayapti** (ishga tushmagan yoki o'chib qolgan). `LoginPage.vue` avval `GET /api/health`ni tekshiradi, lekin agar backend butunlay javob bermasa (portga ulanib bo'lmasa), so'rov `ECONNABORTED`/tarmoq xatosiga uchraydi.

**Tekshirish**:
```bash
curl -v http://localhost:8080/api/health
```
Agar "Connection refused" chiqsa — backend ishlamayapti.

**Yechim**: backend'ni ishga tushiring ([10-ornatish-va-ishga-tushirish.md](10-ornatish-va-ishga-tushirish.md#4-qadam-backendni-ishga-tushirish)) va konsol logini kuzating — "Started MaktabBoshqaruvApplication" degan qator chiqishi kerak.

## Login bosilganda IntelliJ debugger'da to'xtab qoladi

**Alomat**: Backend'ni IntelliJ'da **Debug** rejimida (Shift+F9) ishga tushirgansiz, login tugmasi bosilganda brauzer "muzlab qoladi" — javob kelmaydi, lekin xato ham chiqmaydi.

**Sabab**: IntelliJ'da avvalroq **breakpoint** (qizil nuqta, satr chetida) qo'yilgan bo'lishi mumkin — masalan `AuthController.login()` ichida yoki `JwtAuthFilter`da. Debug rejimida dastur shu qatorga yetganda **to'liq to'xtaydi** va IntelliJ oynasi javob kutayotganini ko'rsatadi (pastdagi "Debug" panelida qizil "to'xtash" belgisi). Bu — xato emas, balki debugger o'z vazifasini bajaryapti; brauzer esa shunchaki javobni kuta beradi.

Yana bir kamdan-kam sabab: agar **"Exception Breakpoint"** (istisno chiqqanda avtomatik to'xtaydigan maxsus breakpoint turi, masalan "har qanday Exception'da to'xta") yoqilgan bo'lsa, `GlobalExceptionHandler` ushlaydigan **oddiy** xatolar (masalan `IllegalStateException` — bu loyihada "topilmadi" holatlari uchun **qasddan** ishlatiladi) ham debugger'ni to'xtatib qo'yadi, garchi bu xato dastur uchun normal oqim bo'lsa ham.

**Yechim**:
1. IntelliJ'ning **Debug** panelida (odatda pastda) barcha breakpoint'larni ko'rib chiqing (`Run → View Breakpoints`, yoki `Ctrl+Shift+F8`), keraksizlarini o'chiring yoki vaqtincha o'chirib qo'ying (checkbox).
2. Agar Exception Breakpoint yoqilgan bo'lsa, uni "faqat ushlanmagan (uncaught) istisnolarda to'xtash" rejimiga o'zgartiring — `IllegalStateException` bu loyihada har doim `GlobalExceptionHandler` tomonidan ushlanadi, shuning uchun "uncaught"ga cheklash debugger'ni keraksiz to'xtatishdan saqlaydi.
3. Oddiy ishlab chiqish/kundalik ishlash uchun **Debug** o'rniga **Run** (Shift+F10) rejimini ishlating — faqat aniq bir xatoni qadam-baqadam kuzatish kerak bo'lgandagina Debug'ga o'ting.

## 8080-port band

**Alomat**: `./gradlew bootRun` ishga tushiriladi, lekin log'da `Port 8080 was already in use` yoki shunga o'xshash xato chiqadi.

**Sabab**: portni allaqachon boshqa jarayon (masalan avvalgi to'xtatilmagan backend nusxasi) band qilib turibdi.

**Yechim (Windows)**:
```powershell
netstat -ano | findstr :8080
taskkill /PID <topilgan_PID> /F
```
**Yechim (macOS/Linux)**:
```bash
lsof -i :8080
kill -9 <topilgan_PID>
```
Muqobil: backend'ni boshqa portda ishga tushirish — `SERVER_PORT=8081 ./gradlew bootRun` (Spring Boot'ning standart `server.port` sozlamasi orqali). Bu holda **frontend'dagi `QCLI_API_BASE_URL`** (`frontend/.env`) yangi portga moslanadi (`http://localhost:8081`) va frontend qayta ishga tushiriladi. `CORS_ALLOWED_ORIGINS`ni o'zgartirish **shart emas** — u backend porti emas, frontend manzilini (`http://localhost:9000`) bildiradi, frontend esa o'z portida qolaveradi.

## JDK versiyasi mos emas

**Alomat**: `./gradlew bootRun` ishga tushirilganda `Unsupported class file major version` yoki `toolchain` bilan bog'liq xato.

**Sabab**: loyiha Java 21'ni talab qiladi (`build.gradle`, `toolchain.languageVersion = JavaLanguageVersion.of(21)`), tizimda eskiroq (masalan Java 17 yoki 11) versiya o'rnatilgan.

**Yechim**: Gradle Toolchain mexanizmi — agar tizimda Java 21 topilmasa, Gradle uni **avtomatik yuklab olishga** harakat qiladi (internet aloqasi kerak). Agar bu ishlamasa, JDK 21'ni qo'lda o'rnating (masalan [Eclipse Temurin](https://adoptium.net)) va `JAVA_HOME`ni shu versiyaga ko'rsating, yoki IntelliJ'da `File → Project Structure → SDK`ni 21'ga o'zgartiring.

## Bazaga ulanib bo'lmaydi

**Alomat**: Backend ishga tushishda `Connection refused` yoki `FATAL: database "maktab_db" does not exist` xatosi bilan to'xtaydi.

**Sabablar va yechimlar**:
- **PostgreSQL ishlamayapti** — xizmatni ishga tushiring (`pg_ctl start`, yoki Windows'da "Services" orqali PostgreSQL xizmatini tekshiring).
- **Baza yaratilmagan** — `CREATE DATABASE maktab_db;` bajarilmagan ([10-ornatish-va-ishga-tushirish.md — 2-qadam](10-ornatish-va-ishga-tushirish.md#2-qadam-bazani-tayyorlash)).
- **Login/parol mos emas** (`password authentication failed`) — lokalda `src/main/resources/application-local.properties`dagi `spring.datasource.password=<parolingiz>` qiymatini PostgreSQL parolingizga moslang (foydalanuvchi nomi standart `postgres`, boshqasi bo'lsa `DB_USERNAME`); serverda esa `DB_USERNAME`/`DB_PASSWORD` environment variable orqali bering. Sozlash: [10-ornatish-va-ishga-tushirish.md — 3-qadam](10-ornatish-va-ishga-tushirish.md#3-qadam-sozlamalar-maxfiy-qiymatlar-va-environment-variablelar).
- **`Could not resolve placeholder 'DB_PASSWORD'` / `'JWT_SECRET'`** — backend `local` profilsiz ishga tushirilgan yoki `application-local.properties` yaratilmagan. `start.sh`/`start.bat` yoki IntelliJ'dagi `MaktabBoshqaruvApplication (local)` konfiguratsiyasidan foydalaning.
- **Boshqa port** — agar PostgreSQL standart `5432` emas, boshqa portda ishlasa, `DB_URL`ni to'liq o'zgartiring.

## Flyway/Liquibase xatolari

**Bu loyihada Flyway ham, Liquibase ham ishlatilmaydi** — shuning uchun "checksum mismatch" yoki "migration failed" kabi xatolar bu loyihada **yuzaga kelmaydi**. Sxema `spring.jpa.hibernate.ddl-auto=update` orqali avtomatik boshqariladi ([05-malumotlar-bazasi.md — Migratsiyalar](05-malumotlar-bazasi.md#migratsiyalar-bu-loyihada-qanday-ishlaydi)).

Agar shunga o'xshash "ustun topilmadi" (`column ... does not exist`) xatosi chiqsa, sabab boshqacha: yangi `@Column(nullable = false)` maydon to'la jadvalga standart qiymatsiz qo'shilgan bo'lishi mumkin — Postgres bunday `ALTER TABLE`ni bajara olmaydi va Hibernate buni jimgina o'tkazib yuboradi. Yechim: o'sha maydonni vaqtincha `nullable` (standart) qilib qo'ying, ilovani ishga tushirib ustunni yaratdiring, kerak bo'lsa keyin qo'lda `NOT NULL` cheklovini (standart qiymat bilan birga) qo'shing.

## CORS xatosi

**Alomat**: brauzer konsolida `has been blocked by CORS policy` degan xato, so'rov Network panelida "failed" deb ko'rinadi (garchi backend ishlab tursa ham).

**Sabab**: frontend boshqa manzildan (masalan `http://localhost:9001` yoki boshqa domen) so'rov yuboryapti, lekin backend'ning `CORS_ALLOWED_ORIGINS` sozlamasi shu manzilni o'z ichiga olmaydi.

**Yechim**: `CORS_ALLOWED_ORIGINS` environment variable'ni frontend qaysi manzilda ishlayotgan bo'lsa, shunga moslang (bir nechtasi vergul bilan: `http://localhost:9000,http://192.168.1.5:9000`). O'zgartirgandan so'ng backend'ni qayta ishga tushirish shart (`application.properties` qayta o'qiladi).

## 401/403 xatolari

| Status | Ma'nosi | Sabab | Yechim |
|---|---|---|---|
| **401 Unauthorized** | Token yo'q, noto'g'ri yoki muddati tugagan | Token 10 soatdan keyin tugaydi (`JwtUtil.expirationMs`); yoki `localStorage`dan qo'lda o'chirilgan | Qayta login qiling. Frontend 401 kelganda avtomatik `/login`ga qaytaradi (`boot/axios.js` interceptor) |
| **403 Forbidden** | Token to'g'ri, lekin rolga ruxsat yo'q | Masalan `VIEWER` roli bilan `POST`/`DELETE` so'ralgan, yoki `EDITOR` bilan biror narsani o'chirishga urinilgan | Kerakli rolga ega akkaunt bilan kiring, yoki [07-api.md](07-api.md)dagi rol jadvalidan qaysi endpoint qaysi rolga ochiqligini tekshiring |

## LazyInitializationException

**Bu xato haqida**: [13-lugat.md — LazyInitializationException](13-lugat.md#lazyinitializationexception)da tushuntirilgan.

**Bu loyihada nega kamdan-kam uchraydi**: ikkita sabab bor:
1. Barcha `@ManyToOne` bog'lanishlar standart holatda **EAGER** (darhol yuklanadi) — loyihada hech qayerda `fetch = FetchType.LAZY` qo'lda yozilmagan.
2. `spring.jpa.open-in-view=false` — bu HTTP so'rov tugagach baza ulanishini ochiq qoldirmaydi, shuning uchun agar biror joyda lazy-yuklanadigan maydonga Controller/JSON-serializatsiya bosqichida (Service tugagandan keyin) murojaat qilinsa, xato **darhol** ko'rinadi (jimgina "ba'zan ishlaydi, ba'zan yo'q" degan holat bo'lmaydi).

**Agar shunday xato chiqsa**: sabab — Service metodi tugagandan keyin (masalan Controller'da) Entity'ning hali yuklanmagan bog'lanishiga murojaat qilingan. Yechim: kerakli ma'lumotni Service ichida, Entity hali "tirik" (tranzaksiya ochiq) paytida DTO'ga o'tkazing — bu loyihaning har bir `toResponseDto(...)` metodi aynan shuni qiladi.

## Ikonkalar o'rniga matn chiqadi

**Alomat**: `q-icon` o'rniga ekranda `fact_check`, `dashboard` kabi so'zlar ko'rinadi (ikonka emas).

**Sabab**: Material Icons shrifti yuklanmagan — odatda internet aloqasi yo'qligi (agar shrift build vaqtida to'g'ri joylashtirilmagan/keshlangan bo'lsa) yoki `quasar.config.js`dagi `extras: ['material-icons']` qatori o'chirilgan/olib tashlangan bo'lsa.

**Yechim**: `frontend/quasar.config.js`da `extras` ro'yxatida `'material-icons'` borligini tekshiring, `npm install`ni qayta bajaring (`@quasar/extras` paketi to'g'ri o'rnatilganini tasdiqlash uchun), va build'ni tozalab qayta ishga tushiring (`rm -rf frontend/dist frontend/.quasar && npm run dev`).

## Dark mode'da matn ko'rinmaydi

**Alomat**: dark rejimga o'tilganda, ba'zi matn/fon kombinatsiyasi o'qib bo'lmaydigan darajada past kontrastli bo'lib qoladi (masalan qora matn qora fonda).

**Sabab**: kimdir CSS'da to'g'ridan-to'g'ri qattiq rang (`color: #000`, `background: white`, yoki Quasar'ning `bg-white`/`text-dark` kabi utility klasslari) yozgan — bular **hech qachon** dark mode'ga moslashmaydi, chunki ular doim bir xil qiymatda qoladi.

**Yechim**: loyihaning qat'iy qoidasi — har qanday rang **faqat** `css/app.scss`da e'lon qilingan CSS custom property (`var(--card-bg)`, `var(--text-primary)`, `var(--brand-border)`, `var(--brand-text-muted)` va h.k.) orqali yozilishi kerak, hech qachon qattiq hex qiymat yoki Quasar'ning `bg-*`/`text-*` fiksatsiya qiluvchi utility klasslari orqali emas. Yangi komponent yozganda mavjud tokenlar ro'yxati uchun: [08-frontend.md — Tema tizimi](08-frontend.md#tema-tizimi-darklight-dizayn-tokenlari).

---
Keyingisi: [12 — Yangi modul qo'shish →](12-yangi-modul-qoshish.md)
