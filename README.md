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

## Ishga tushirish

### 1. Kerakli environment variable'lar

| Nomi | Majburiymi | Tavsifi |
|---|---|---|
| `DB_URL` | Yo'q (default: `jdbc:postgresql://localhost:5432/maktab_db`) | PostgreSQL ulanish manzili |
| `DB_USERNAME` | Yo'q (default: `postgres`) | DB foydalanuvchi nomi |
| `DB_PASSWORD` | **Ha** | DB paroli |
| `JWT_SECRET` | **Ha** | JWT tokenlarni imzolash uchun Base64 kodlangan, kamida 32 baytlik maxfiy kalit |
| `ADMIN_USERNAME` | Yo'q | Tizimda hech qanday foydalanuvchi bo'lmaganda avtomatik yaratiladigan dastlabki ADMIN uchun login |
| `ADMIN_PASSWORD` | Yo'q | Dastlabki ADMIN uchun parol |

`JWT_SECRET` generatsiya qilish uchun (PowerShell):
```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))
```

### 2. Environment variable'larni sozlash (PowerShell misoli)

```powershell
$env:DB_PASSWORD="sizning_parolingiz"
$env:JWT_SECRET="Base64_kodlangan_maxfiy_kalit"
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="kamida_6_belgi"
```

### 3. Ishga tushirish

```bash
./gradlew bootRun
```

Birinchi marta ishga tushirilganda, agar `ADMIN_USERNAME`/`ADMIN_PASSWORD` berilgan bo'lsa va bazada hech qanday foydalanuvchi bo'lmasa, dastlabki ADMIN akkaunt avtomatik yaratiladi (`AdminSeeder`). Shundan keyin `/api/users` orqali qo'shimcha foydalanuvchilar (EDITOR/VIEWER) yaratish mumkin.

### 4. Testlarni ishga tushirish

```bash
./gradlew test
```

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
