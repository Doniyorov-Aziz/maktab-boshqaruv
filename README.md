# Maktab Boshqaruv

Maktab boshqaruv tizimi uchun REST API. Spring Boot + PostgreSQL asosida qurilgan, JWT orqali autentifikatsiya va rol-based avtorizatsiyani qo'llab-quvvatlaydi.

## Texnologiyalar

- Java 21
- Spring Boot 4.1 (Web, Data JPA, Security, Validation)
- PostgreSQL
- JWT (jjwt)
- Gradle
- JUnit 5 + Mockito

## Domen modeli

`School` → `Building` → `Room`, `School` → `AcademicYear` → `SchoolClass` → `Student`, `Subject`, `Employee`/`Position`, va bularning barchasini bog'lovchi `LessonSlot` (dars jadvali). Foydalanuvchilar `User` (rollar: `ADMIN`, `EDITOR`, `VIEWER`) orqali boshqariladi.

## Ishga tushirish (lokal/o'rganish uchun)

Hech qanday sozlash shart emas — standart qiymatlar bilan darhol ishga tushadi:

```bash
./gradlew bootRun
```

### Backend va frontend'ni birga ishga tushirish

Ikkalasini alohida terminalda ishga tushirish shart emas — loyiha ildizida:

```bash
./start.sh        # Git Bash / macOS / Linux — Ctrl+C ikkalasini ham to'xtatadi
start.bat         # Windows — ikkita alohida oynada ochadi
```

Backend `http://localhost:8080`, frontend `http://localhost:9000` da ko'tariladi. Backend tayyorligini `GET /api/health` orqali tekshirish mumkin (autentifikatsiyasiz) — login sahifasi ham shu endpoint orqali backend ishlab turganini avtomatik tekshiradi va aks holda ogohlantirish ko'rsatadi.

Birinchi marta ishga tushirilganda, bazada hech qanday foydalanuvchi bo'lmasa, dastlabki ADMIN akkaunt avtomatik yaratiladi: **`admin` / `admin123`**. Shundan keyin `/api/users` orqali qo'shimcha foydalanuvchilar (EDITOR/VIEWER) yaratish mumkin.

Testlarni ishga tushirish:
```bash
./gradlew test
```

### Sozlamalarni o'zgartirish kerak bo'lsa

Barcha qiymatlar `src/main/resources/application.properties`da, standart bilan birga yozilgan (`${VAR:standart_qiymat}` shaklida). Boshqa qiymat berish uchun shunchaki mos environment variable'ni sozlang:

| Nomi | Standart qiymati |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/maktab_db` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `Aziz0708.` |
| `JWT_SECRET` | (loyihada tayyor qiymat bor) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin123` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:9000` |

> **Eslatma:** bu standart qiymatlar faqat lokal/o'rganish maqsadida qulaylik uchun. Loyiha real serverga (production) chiqariladigan bo'lsa, bularni albatta maxfiy, faqat shu muhitga xos qiymatlar bilan almashtirish kerak (environment variable orqali, kodga yozmasdan).

## Autentifikatsiya

```
POST /api/auth/login
{
  "username": "admin",
  "password": "..."
}
```

Javobda qaytgan JWT tokenni keyingi so'rovlarda `Authorization: Bearer <token>` header orqali yuborish kerak.

## Rollar

- **ADMIN** — barcha amallar, jumladan o'chirish va foydalanuvchilarni boshqarish
- **EDITOR** — ko'rish, yaratish, yangilash (o'chira olmaydi)
- **VIEWER** — faqat ko'rish

## API modullari

`/api/schools`, `/api/buildings`, `/api/rooms`, `/api/academic-years`, `/api/school-classes`, `/api/students`, `/api/subjects`, `/api/employees`, `/api/positions`, `/api/lesson-slots`, `/api/users` — barchasi bir xil CRUD patternga ega.

Ro'yxat endpoint'lari (`GET` ko'plik) pagination'ni qo'llab-quvvatlaydi: `?page=0&size=20&sort=name,asc`.

## Frontend

`frontend/` papkasida Quasar (Vue 3) admin panel joylashgan — login sahifasi va barcha 11 modul uchun jadval/forma CRUD ekranlari, bitta universal komponent (`src/pages/CrudPage.vue`) orqali `src/config/modules.js` konfiguratsiyasidan generatsiya qilinadi.

### Ishga tushirish

```bash
cd frontend
npm install
cp .env.example .env   # kerak bo'lsa QCLI_API_BASE_URL'ni o'zgartiring
npm run dev
```

Standart holatda `http://localhost:9000` da ochiladi va backend'ga `http://localhost:8080` orqali ulanadi. Backend'ning `CORS_ALLOWED_ORIGINS` shu manzilga mos bo'lishi kerak (default qiymat mos keladi).
