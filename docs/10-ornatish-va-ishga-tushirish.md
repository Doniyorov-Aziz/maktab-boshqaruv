# 10 — O'rnatish va ishga tushirish

[← 09 — Asosiy jarayonlar](09-asosiy-jarayonlar.md) · Keyingisi: [11 — Muammolar va yechimlar →](11-muammolar-va-yechimlar.md)

## Mundarija
- [Talablar](#talablar)
- [1-qadam: repozitoriyni olish](#1-qadam-repozitoriyni-olish)
- [2-qadam: bazani tayyorlash](#2-qadam-bazani-tayyorlash)
- [3-qadam: sozlamalar (maxfiy qiymatlar va environment variable'lar)](#3-qadam-sozlamalar-maxfiy-qiymatlar-va-environment-variablelar)
- [4-qadam: backend'ni ishga tushirish](#4-qadam-backendni-ishga-tushirish)
- [5-qadam: frontend'ni ishga tushirish](#5-qadam-frontendni-ishga-tushirish)
- [Ikkalasini birga ishga tushirish](#ikkalasini-birga-ishga-tushirish)
- [Test foydalanuvchilar](#test-foydalanuvchilar)
- [Testlarni ishga tushirish](#testlarni-ishga-tushirish)

## Talablar

| Vosita | Versiya | Izoh |
|---|---|---|
| JDK | 21 | Gradle Wrapper avtomatik shu versiyani talab qiladi (`build.gradle`, toolchain) |
| Gradle | alohida o'rnatish **shart emas** | Loyihada Gradle Wrapper (`gradlew`/`gradlew.bat`, versiya 9.5.1) bor |
| PostgreSQL | islagan versiya (masalan 14+) | Mahalliy ishga tushirilgan bo'lishi kerak, `localhost:5432` |
| Node.js | `>= 26` yoki `^24` yoki `^22.12` | `frontend/package.json`, `engines` maydoni |
| npm | Node bilan birga keladi | Frontend paketlarini o'rnatish uchun |

## 1-qadam: repozitoriyni olish

```bash
git clone <repozitoriy-manzili>
cd maktab-boshqaruv
```

## 2-qadam: bazani tayyorlash

PostgreSQL'da `maktab_db` nomli bo'sh baza yaratiladi (jadvallarni Hibernate o'zi yaratadi — [05-malumotlar-bazasi.md](05-malumotlar-bazasi.md#migratsiyalar-bu-loyihada-qanday-ishlaydi)):

```sql
CREATE DATABASE maktab_db;
```

Baza foydalanuvchisi standart holatda `postgres`; uning parolini (`<parolingiz>`) 3-qadamda lokal sozlamalar fayliga yozasiz. Parol kodda ham, hujjatlarda ham saqlanmaydi.

## 3-qadam: sozlamalar (maxfiy qiymatlar va environment variable'lar)

Sozlamalar `src/main/resources/application.properties`da (`${VAR:standart_qiymat}`). Maxfiy qiymatlar — **`DB_PASSWORD` va `JWT_SECRET` — standartsiz**: ular berilmasa backend ishga tushmaydi (`Could not resolve placeholder 'JWT_SECRET'`).

**Lokal ishlab chiqish uchun** ularni `local` profil fayliga yozing:

1. Shablondan nusxa oling (fayl `.gitignore`da — hech qachon commit qilinmaydi):
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```
2. `o'zgartiring` yozuvlarini haqiqiy qiymatlar bilan almashtiring:
   ```properties
   spring.datasource.password=<parolingiz>
   jwt.secret=<base64-jwt-secret>
   ```
3. JWT secret uchun yangi tasodifiy qiymat generatsiya qiling (kamida 32 bayt, Base64):
   ```bash
   openssl rand -base64 64 | tr -d '\n'
   ```
   ```powershell
   [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
   ```
4. Backend'ni `local` profil bilan ishga tushiring — `start.sh`/`start.bat` va IntelliJ'dagi `MaktabBoshqaruvApplication (local)` konfiguratsiyasi buni avtomatik qiladi (4-qadamga qarang).

**Server/production'da** `application-local.properties` ishlatilmaydi — qiymatlar environment variable orqali beriladi:

| O'zgaruvchi | Standart qiymati | Ma'nosi |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/maktab_db` | Baza manzili |
| `DB_USERNAME` | `postgres` | Baza foydalanuvchisi |
| `DB_PASSWORD` | **yo'q (majburiy)** | Baza paroli |
| `JWT_SECRET` | **yo'q (majburiy)** | JWT imzolash kaliti (Base64, kamida 32 bayt) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin123` | Birinchi ADMIN akkaunt |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:9000` | Qaysi frontend manzilidan so'rov qabul qilinishi |

> **Diqqat:** `ADMIN_PASSWORD`ning standart qiymati (`admin123`) ham faqat lokal/o'rganish uchun — real serverda uni ham albatta almashtiring. `JWT_SECRET` o'zgarsa, avval berilgan barcha tokenlar yaroqsiz bo'lib qoladi (foydalanuvchilar qayta kiradi).

Frontend uchun `frontend/.env` (`frontend/.env.example`dan nusxa):
```
QCLI_API_BASE_URL=http://localhost:8080
```

## 4-qadam: backend'ni ishga tushirish

### Terminal orqali

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun                   # macOS/Linux/Git Bash
```
```powershell
$env:SPRING_PROFILES_ACTIVE="local"; .\gradlew.bat bootRun        # Windows PowerShell
```

Birinchi ishga tushirishda Gradle kerakli versiyani (9.5.1) avtomatik yuklab oladi. Backend `http://localhost:8080` da ko'tariladi; tayyorligini tekshirish:
```bash
curl http://localhost:8080/api/health
# {"status":"UP"}
```

### IntelliJ IDEA'da

1. Loyihani papka sifatida oching (`Open` → loyiha ildizi) — IntelliJ Gradle loyihasini avtomatik taniydi.
2. Yuqoridagi run konfiguratsiyalar ro'yxatidan **`MaktabBoshqaruvApplication (local)`**ni tanlab ▶ (Run) bosing. Bu konfiguratsiya repozitoriyda (`.run/` papkasi) saqlangan va `local` profilni yoqadi. (`MaktabBoshqaruvApplication.java` ichidagi yashil strelka profilsiz konfiguratsiya yaratadi — undan foydalansangiz, `Edit Configurations → Active profiles` maydoniga `local` yozing.)
3. **Run** va **Debug** rejimlari orasidagi farq: `Run` (Shift+F10) — oddiy ishga tushirish, breakpoint'larga e'tibor bermaydi. `Debug` (Shift+F9) — agar kodda breakpoint (qizil nuqta) qo'yilgan bo'lsa yoki "Exception breakpoint"lar yoqilgan bo'lsa, o'sha joyda **to'xtaydi** va butun ilova muzlaganday ko'rinadi. Login cheksiz aylanib, hech qanday javob kelmasa — avval shu narsani tekshiring: [11-muammolar-va-yechimlar.md](11-muammolar-va-yechimlar.md#login-bosilganda-intellij-debuggerda-toxtab-qoladi).

## 5-qadam: frontend'ni ishga tushirish

```bash
cd frontend
npm install
cp .env.example .env      # kerak bo'lsa QCLI_API_BASE_URL'ni o'zgartiring
npm run dev
```

`http://localhost:9000` da ochiladi (`devServer.open: true` tufayli brauzer avtomatik ochiladi). Backend'ning `CORS_ALLOWED_ORIGINS` shu manzilga mos bo'lishi kerak (standart qiymat allaqachon mos).

## Ikkalasini birga ishga tushirish

Loyiha ildizida tayyor skriptlar bor:

```bash
./start.sh        # Git Bash / macOS / Linux — ikkalasini ham fon jarayoni sifatida ishga tushiradi, Ctrl+C ikkalasini ham to'xtatadi
start.bat          # Windows — ikkitasini alohida oynada ochadi
```

Ikkala skript ham `SPRING_PROFILES_ACTIVE` berilmagan bo'lsa `local` profilni yoqadi va `application-local.properties` topilmasa, tushunarli xabar bilan to'xtaydi.

## Test foydalanuvchilar

Birinchi ishga tushirishda (bo'sh baza bilan) seederlar avtomatik quyidagi akkauntlarni yaratadi ([05-malumotlar-bazasi.md — Seed ma'lumotlar](05-malumotlar-bazasi.md#seed-malumotlar)):

| Username | Parol | Rol | Izoh |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | To'liq huquq |
| `direktor` | `direktor123` | `EDITOR` | |
| `ustoz1` | `ustoz123` | `EDITOR` | Bitta `Employee`ga bog'langan — "faqat mening darslarim" filtri ishlaydi |
| `ustoz2` | `ustoz123` | `EDITOR` | Xuddi shunday, boshqa xodimga bog'langan |

## Testlarni ishga tushirish

```bash
./gradlew test
```

13 ta test fayli, jami ~65 ta test (`@SpringBootTest` kontekst-yuklash testi — `local` profil bilan ishlaydi, ya'ni `application-local.properties` va ishlab turgan PostgreSQL talab qiladi + har bir Service uchun Mockito bilan yozilgan unit testlar). Natija: `build/reports/tests/test/index.html`.

Frontend'da rasmiy test fayllari yo'q — sifat `npm run lint` (oxfmt + oxlint) va `npm run build` (Vite kompilyatsiyasi, xato bo'lsa build to'xtaydi) orqali tekshiriladi:

```bash
cd frontend
npm run lint
npm run build
```

---
Keyingisi: [11 — Muammolar va yechimlar →](11-muammolar-va-yechimlar.md)
