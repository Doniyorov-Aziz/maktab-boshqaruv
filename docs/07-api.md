# 07 — API endpoint'lar

[← 06 — Backend](06-backend.md) · Keyingisi: [08 — Frontend →](08-frontend.md)

## Mundarija
- [Umumiy qoidalar](#umumiy-qoidalar)
- [Auth](#auth-apiauth)
- [Dashboard](#dashboard-apidashboard)
- [Profillar](#profillar-apiprofiles)
- [Ta'lim tuzilmasi: Schools / Buildings / Rooms](#talim-tuzilmasi-schools-buildings-rooms)
- [O'quv jarayoni: Academic Years / School Classes / Subjects / Lesson Slots](#oquv-jarayoni-academic-years-school-classes-subjects-lesson-slots)
- [Davomat](#davomat-apiattendance)
- [Baholar](#baholar-apigrades)
- [Odamlar: Students / Positions / Employees](#odamlar-students-positions-employees)
- [Kundalik hayot: Announcements / Calendar Events / Behavior Records](#kundalik-hayot-announcements-calendar-events-behavior-records)
- [Telegram va xabarnomalar](#telegram-va-xabarnomalar-apitelegram-apinotifications)
- [Foydalanuvchilar](#foydalanuvchilar-apiusers)
- [Health](#health-apihealth)
- [Swagger/OpenAPI](#swaggeropenapi)

## Umumiy qoidalar

- **Bazaviy manzil**: lokal ishga tushirishda `http://localhost:8080`.
- **Autentifikatsiya**: `/api/auth/**` va `/api/health` dan tashqari, **barcha** endpoint `Authorization: Bearer <token>` headerini talab qiladi.
- **Ro'yxat endpoint'lari** (`GET`, ko'plik) deyarli barchasi sahifalashni (`Pageable`) qo'llab-quvvatlaydi: `?page=0&size=20&sort=name,asc`. Javob shakli:
  ```json
  { "content": [...], "totalElements": 42, "totalPages": 3, "number": 0, "size": 20 }
  ```
- **Ko'p maktabli modullar** (`schoolScoped`) — `schoolId` query-parametri **majburiy**.
- **Xatolar**: barcha xato javoblari oddiy matn (`Content-Type: text/plain`) tanasi bilan qaytadi, JSON emas (`GlobalExceptionHandler`, batafsil: [06-backend.md — Xatolarni qayta ishlash](06-backend.md#xatolarni-qayta-ishlash)).
- **Rol ustunidagi qisqartmalar**: A = `ADMIN`, E = `EDITOR`, V = `VIEWER`.

## Auth — `/api/auth`

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| POST | `/api/auth/login` | Login, JWT token qaytaradi | ochiq |
| GET | `/api/auth/hash/{raw}` | Berilgan matnni BCrypt bilan xeshlaydi (qo'lda foydalanuvchi yaratish uchun yordamchi) | A |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# Javob: "eyJhbGciOiJIUzI1NiJ9...." (JWT string)
```

## Dashboard — `/api/dashboard`

Barchasi `@RequestParam Long schoolId` talab qiladi, hammasi `ADMIN`/`EDITOR`/`VIEWER`ga ochiq.

| Metod | URL | Vazifa | Qo'shimcha parametrlar |
|---|---|---|---|
| GET | `/api/dashboard/overview` | Bosh sahifaning **barcha** bloklarini bitta so'rovda qaytaradi (summary, jonli darslar, davomat trendi, sinflar reytingi, fanlar o'rtachasi, e'tibor, faoliyat) | `trendDays` (standart 30), `activityLimit` (standart 8) |
| GET | `/api/dashboard/summary` | Faqat KPI kartalar uchun ma'lumot | — |
| GET | `/api/dashboard/live-lessons` | Hozir/keyingi/o'tgan darslar ro'yxati | — |
| GET | `/api/dashboard/live-lessons-summary` | Joriy davr haqida qisqa matn | — |
| GET | `/api/dashboard/attendance-trend` | Kunlik davomat foizi tarixi | `days` (standart 30) |
| GET | `/api/dashboard/class-rankings` | Sinflar reytingi (davomat/baho/umumiy ball, 30 kunlik) | — |
| GET | `/api/dashboard/subject-averages` | Fan bo'yicha o'rtacha baho | — |
| GET | `/api/dashboard/absentees-today` | Bugun kelmagan o'quvchilar ro'yxati | — |
| GET | `/api/dashboard/attention` | "E'tibor talab qiladi" ro'yxati | — |
| GET | `/api/dashboard/activity` | So'nggi faoliyat lentasi | `limit` (standart 8) |

```bash
curl http://localhost:8080/api/dashboard/overview?schoolId=1 \
  -H "Authorization: Bearer $TOKEN"
```

Ko'rsatkichlar qanday hisoblanishi (formulalari bilan): [09-asosiy-jarayonlar.md](09-asosiy-jarayonlar.md).

## Profillar — `/api/profiles`

Klass darajasida `ADMIN`/`EDITOR`/`VIEWER`ga ochiq.

| Metod | URL | Vazifa |
|---|---|---|
| GET | `/api/profiles/students/{id}` | Bitta o'quvchining to'liq profili (davomat, baholar, xulq yozuvlari birga) |
| GET | `/api/profiles/teachers/{id}` | Bitta o'qituvchining profili (dars jadvali, bog'liq sinflar) |
| GET | `/api/profiles/classes/{id}` | Bitta sinfning profili (o'quvchilar ro'yxati, dars jadvali) |

## Ta'lim tuzilmasi: Schools / Buildings / Rooms

### `/api/schools` (`schoolScoped: false`)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/schools` | Ro'yxat (pagination) | A/E/V |
| GET | `/api/schools/{id}` | Bitta maktab | A/E/V |
| POST | `/api/schools` | Yaratish | A/E |
| PUT | `/api/schools/{id}` | Yangilash | A/E |
| DELETE | `/api/schools/{id}` | O'chirish | A |

### `/api/buildings` (`schoolId` majburiy)

Xuddi shu 5 ta endpoint (GET ro'yxat, GET id, POST, PUT, DELETE), xuddi shu rol taqsimoti (A/E/V — A/E/V — A/E — A/E — A).

### `/api/rooms` (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol | Qo'shimcha parametrlar |
|---|---|---|---|---|
| GET | `/api/rooms` | Ro'yxat, filtrlar bilan | A/E/V | `buildingId` (ixtiyoriy), `type` (ixtiyoriy, `RoomType` qiymati) |
| GET | `/api/rooms/{id}` | Bitta xona | A/E/V | — |
| GET | `/api/rooms/{id}/occupancy` | Shu xonaning **bugungi to'liq kunlik band-bo'sh jadvali** | A/E/V | — |
| POST | `/api/rooms` | Yaratish | A/E | — |
| PUT | `/api/rooms/{id}` | Yangilash | A/E | — |
| DELETE | `/api/rooms/{id}` | O'chirish | A | — |

```bash
curl "http://localhost:8080/api/rooms?schoolId=1&type=GYM&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

## O'quv jarayoni: Academic Years / School Classes / Subjects / Lesson Slots

`/api/academic-years`, `/api/school-classes`, `/api/subjects` — barchasi (`schoolId` majburiy) standart 5 ta CRUD endpoint, A/E/V — A/E/V — A/E — A/E — A rol taqsimoti bilan.

### `/api/lesson-slots` (dars jadvali yozuvlari, `schoolId` majburiy)

| Metod | URL | Vazifa | Rol | Qo'shimcha parametrlar |
|---|---|---|---|---|
| GET | `/api/lesson-slots` | Ro'yxat (pagination) | A/E/V | — |
| GET | `/api/lesson-slots/timetable` | Haftalik jadval ko'rinishi (kun/vaqt bo'yicha guruhlangan) | A/E/V | `schoolClassId`, `employeeId`, `roomId` — barchasi ixtiyoriy, biri tanlanadi |
| GET | `/api/lesson-slots/{id}` | Bitta yozuv | A/E/V | — |
| POST | `/api/lesson-slots` | Yaratish (xona/o'qituvchi ziddiyati tekshiriladi) | A/E | — |
| PUT | `/api/lesson-slots/{id}` | Yangilash | A/E | — |
| DELETE | `/api/lesson-slots/{id}` | O'chirish | A | — |

```bash
curl "http://localhost:8080/api/lesson-slots/timetable?schoolId=1&schoolClassId=5" \
  -H "Authorization: Bearer $TOKEN"
```

## Davomat — `/api/attendance`

| Metod | URL | Vazifa | Rol | Parametrlar / body |
|---|---|---|---|---|
| GET | `/api/attendance` | Ro'yxat (pagination) | A/E/V | `schoolId` |
| GET | `/api/attendance/today-lessons` | Bugungi barcha darslar + har biri uchun davomat olinganmi | A/E/V | `schoolId`, `employeeId` (ixtiyoriy — berilsa, faqat o'sha o'qituvchining darslari) |
| GET | `/api/attendance/roster` | Bitta dars uchun sinf ro'yxati + mavjud davomat holati | A/E/V | `lessonSlotId`, `date` (ISO, `yyyy-MM-dd`) |
| POST | `/api/attendance/bulk` | Butun sinf uchun davomatni **bir vaqtda** saqlash | A/E | body: `{lessonSlotId, recordDate, entries: [{studentId, status, comment}]}` |
| POST | `/api/attendance` | Bitta o'quvchi uchun bitta yozuv qo'shish | A/E | body: `AttendanceRequestDto` |
| PUT | `/api/attendance/{id}` | Bitta yozuvni yangilash | A/E | — |
| DELETE | `/api/attendance/{id}` | O'chirish | A | — |

```bash
curl -X POST http://localhost:8080/api/attendance/bulk \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"lessonSlotId":12,"recordDate":"2026-09-29","entries":[{"studentId":3,"status":"PRESENT"},{"studentId":4,"status":"ABSENT"}]}'
```

To'liq oqim (frontend tugmasidan bazagacha): [02-arxitektura.md](02-arxitektura.md#bitta-sorovning-toliq-yoli-davomatni-saqlash).

## Baholar — `/api/grades`

| Metod | URL | Vazifa | Rol | Parametrlar |
|---|---|---|---|---|
| GET | `/api/grades` | Ro'yxat (pagination) | A/E/V | `schoolId` |
| GET | `/api/grades/gradebook` | Bitta sinf+fan uchun davr ichidagi baholar jadvali (o'quvchilar × sanalar) | A/E/V | `schoolClassId`, `subjectId`, `from`, `to` (ISO sana) |
| POST | `/api/grades` | Yangi baho qo'yish (2–5 oralig'ida) | A/E | body: `GradeRequestDto` |
| PUT | `/api/grades/{id}` | Bahoni yangilash | A/E | — |
| DELETE | `/api/grades/{id}` | O'chirish | A | — |

```bash
curl "http://localhost:8080/api/grades/gradebook?schoolClassId=5&subjectId=2&from=2026-09-01&to=2026-09-29" \
  -H "Authorization: Bearer $TOKEN"
```

## Odamlar: Students / Positions / Employees

### `/api/students` (`schoolId` majburiy)

Standart 5 ta CRUD endpoint (A/E/V — A/E/V — A/E — A/E — A).

### `/api/positions` (`schoolScoped: false`)

Standart 5 ta CRUD endpoint, `schoolId` talab qilinmaydi.

### `/api/employees` (`schoolId` majburiy)

Standart 5 ta CRUD endpoint.

```bash
curl "http://localhost:8080/api/students?schoolId=1&page=0&size=20&sort=lastName,asc" \
  -H "Authorization: Bearer $TOKEN"
```

## Kundalik hayot: Announcements / Calendar Events / Behavior Records

### `/api/announcements` (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/announcements` | Ro'yxat (pagination) | A/E/V |
| GET | `/api/announcements/latest` | So'nggi N ta (`limit`, standart 5) | A/E/V |
| GET | `/api/announcements/{id}` | Bitta e'lon | A/E/V |
| POST | `/api/announcements` | Yaratish | A/E |
| PUT | `/api/announcements/{id}` | Yangilash | A/E |
| DELETE | `/api/announcements/{id}` | O'chirish | A |

### `/api/calendar-events` (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/calendar-events` | Ro'yxat (pagination) | A/E/V |
| GET | `/api/calendar-events/range` | Berilgan sana oralig'idagi tadbirlar (`from`, `to`) | A/E/V |
| GET | `/api/calendar-events/upcoming` | Yaqinlashayotgan tadbirlar (`limit`, standart 5) | A/E/V |
| POST | `/api/calendar-events` | Yaratish | A/E |
| PUT | `/api/calendar-events/{id}` | Yangilash | A/E |
| DELETE | `/api/calendar-events/{id}` | O'chirish | A |

### `/api/behavior-records` (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/behavior-records` | Ro'yxat (pagination) | A/E/V |
| POST | `/api/behavior-records` | Yaratish | A/E |
| PUT | `/api/behavior-records/{id}` | Yangilash | A/E |
| DELETE | `/api/behavior-records/{id}` | O'chirish | A |

```bash
curl "http://localhost:8080/api/calendar-events/range?schoolId=1&from=2026-09-01&to=2026-09-30" \
  -H "Authorization: Bearer $TOKEN"
```

## Telegram va xabarnomalar — `/api/telegram`, `/api/notifications`

Ota-onalar uchun Telegram bot (batafsil: [15-telegram-bot.md](15-telegram-bot.md)). Bog'lash kodlari bolaning ma'lumotiga obuna bo'lish imkonini beradi, shuning uchun ular `VIEWER`ga ochilmagan.

### `/api/telegram`

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/telegram/status` | Bot holati: `mode` (`LIVE`/`MOCK`/`DISABLED`/`NO_TOKEN`), `active`, `tokenConfigured`, `botUsername`, `lastError`. Tokenning o'zi hech qachon qaytarilmaydi | A/E/V |
| GET | `/api/telegram/students/{studentId}` | O'quvchining bog'lash kodi, deep link, ulangan ota-onalar ro'yxati (`links[]`: `id`, `firstName`, `telegramUsername`, `linkedAt`) | A/E |
| POST | `/api/telegram/students/{studentId}/regenerate-code` | Yangi kod yaratadi (eskisi bekor bo'ladi, bog'lanishlar saqlanadi) | A/E |
| DELETE | `/api/telegram/links/{linkId}` | Ota-onani uzish (`active=false`) | A/E |
| GET | `/api/telegram/classes/{schoolClassId}/codes` | Sinfning barcha o'quvchilari uchun kod + deep link (QR varaq uchun) | A/E |
| GET | `/api/telegram/files/{fileId}` | Ota-ona yuborgan rasmni backend orqali beradi (token brauzerga chiqmaydi) | A/E |
| POST | `/api/telegram/mock/updates` | **Faqat `telegram.mock=true`**: ota-onaning harakatini simulyatsiya qiladi, botning javoblarini (yuborish/tahrirlash/rasm, tugmalari bilan) qaytaradi. Body: `chatId` + bittasi: `text`, `contactPhone`, `photoBase64`, `callbackData` (+ `messageId`); ixtiyoriy `firstName`, `username`, `contactUserId`, `languageCode` | A |
| GET | `/api/telegram/mock/messages?chatId=&from=` | **Mock**: shu chatga yuborilgan xabarlar, `from` indeksidan boshlab (`next` — keyingi indeks) | A |
| GET | `/api/telegram/mock/photos/{fileId}` | **Mock**: bot yuborgan PNG | A |

### `/api/parent-messages` — ota-onalar murojaatlari (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/parent-messages` | Ro'yxat, yangilari birinchi, pagination. Ixtiyoriy `status` (`NEW`/`ANSWERED`) | A/E |
| GET | `/api/parent-messages/count-new` | Javob kutayotganlar soni (sidebar belgisi) | A/E |
| GET | `/api/parent-messages/{id}/photo` | Murojaatga ilova qilingan rasm | A/E |
| POST | `/api/parent-messages/{id}/reply` | Javob yozish. Body: `{"text": "..."}`. Javob ota-onaga botda (`MESSAGE_REPLY`) yuboriladi | A/E |

### `/api/absence-requests` — sababli arizalar (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/absence-requests` | Ro'yxat, pagination. Ixtiyoriy `status` (`PENDING`/`APPROVED`/`REJECTED`) | A/E |
| GET | `/api/absence-requests/count-pending` | Kutilayotganlar soni | A/E |
| GET | `/api/absence-requests/{id}/photo` | Ma'lumotnoma rasmi | A/E |
| POST | `/api/absence-requests/{id}/approve` | Tasdiqlash: arizadagi kunlarning barcha darslari davomatda `EXCUSED` (bayram/ta'til kunlari bundan mustasno), javobda `excusedLessons`; ota-onaga xabar. `PENDING` bo'lmasa — 409 | A/E |
| POST | `/api/absence-requests/{id}/reject` | Rad etish. Body: `{"text": "sabab"}` (majburiy); ota-onaga sababi bilan xabar | A/E |

### `/api/broadcasts` — ota-onalarga umumiy xabar (faqat ADMIN)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/broadcasts?schoolId=` | Yuborilganlar tarixi va natijasi (yuborildi / jami) | A |
| POST | `/api/broadcasts/preview` | Ota-ona ko'radigan matn va qabul qiluvchilar soni, hech narsa yubormaydi | A |
| POST | `/api/broadcasts` | Yuborish (outbox orqali, `BROADCAST`). Body: `schoolId`, `text`, `audience` (`ALL`/`CLASSES`), `classIds` | A |

### `/api/bot` — bot statistikasi va sozlamalari (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/bot/stats` | Ulangan ota-onalar foizi, faol (7/30 kun), 30 kunlik yuborilgan xabarlar, eng ko'p ochilgan bo'limlar, sinflar bo'yicha qamrov | A/E |
| GET | `/api/bot/settings` | Bot sozlamalari (yozuv bo'lmasa — standart qiymatlar) | A/E |
| PUT | `/api/bot/settings` | Saqlash: `phone`, `directorName`, `receptionHours`, `bellScheduleNote`, `showTeacherPhones`, `tomorrowScheduleTime`, `weeklyReportDay`, `weeklyReportTime`, `eventReminderTime`, `lowGradeThreshold` (2 yoki 3) | A |
| POST | `/api/bot/mock/run-jobs` | **Mock**: ertangi jadval, haftalik hisobot, tadbir eslatmasini hoziroq ishga tushiradi | A |

### `/api/parent` — Mini App (JWT emas, Telegram imzosi)

Login talab qilinmaydi (`SecurityConfig`da ochiq), lekin har so'rovda `X-Telegram-Init-Data` sarlavhasi bo'lishi shart. Imzo HMAC-SHA256 bilan tekshiriladi, `auth_date` 24 soatdan eski bo'lmasligi kerak — aks holda **401**. Chat bog'lanmagan o'quvchi id'si — **403**. Barcha endpointlar faqat o'qish uchun.

| Metod | URL | Vazifa |
|---|---|---|
| GET | `/api/parent/me` | Ota-ona ismi, tili, bog'langan farzandlar, tanlangan farzand |
| GET | `/api/parent/students/{id}/today` | «Bugun»: holat, darslar, bugungi baholar, yangi e'lonlar, yaqin tadbir |
| GET | `/api/parent/students/{id}/schedule?day=today\|tomorrow\|week` | Dars jadvali |
| GET | `/api/parent/students/{id}/attendance?month=YYYY-MM` | Oylik xulosa, kunlar kalendari, kelmagan/kechikkan darslar, fanlar bo'yicha |
| GET | `/api/parent/students/{id}/grades` | So'nggi baholar va fanlar bo'yicha o'rtacha (tendensiya bilan) |
| GET | `/api/parent/students/{id}/grades/{subjectId}` | Fan bo'yicha barcha baholar va o'qituvchi |
| GET | `/api/parent/students/{id}/announcements` | Ota-onalarga mo'ljallangan e'lonlar |

### `/api/notifications` (`schoolId` majburiy)

| Metod | URL | Vazifa | Rol | Qo'shimcha parametrlar |
|---|---|---|---|---|
| GET | `/api/notifications` | Xabarnomalar jurnali (outbox), yangilari birinchi, pagination | A/E | `type` (`ATTENDANCE_ABSENT`, `ATTENDANCE_LATE`, `GRADE_NEW`, `GRADE_UPDATED`, `GRADE_LOW`, `ANNOUNCEMENT`, `TOMORROW_SCHEDULE`, `WEEKLY_REPORT`, `EVENT_REMINDER`, `MESSAGE_REPLY`, `ABSENCE_DECISION`, `BROADCAST`), `status` (`PENDING`, `SENT`, `FAILED`, `SKIPPED`), `from`, `to` (`YYYY-MM-DD`, `createdAt` bo'yicha) — barchasi ixtiyoriy |
| GET | `/api/notifications/stats` | `sentToday`, `failedToday`, `failedTotal`, `pending`, `skippedToday`, `linkedStudents`, `totalStudents`, `linkedPercent`, `parentCount` | A/E | — |
| POST | `/api/notifications/{id}/retry` | `FAILED` xabarni qayta navbatga qo'yadi (`attempts=0`). Boshqa holatda — 409 | A/E | — |
| GET | `/api/notifications/settings` | Maktab sozlamalari (yozuv bo'lmasa — standart qiymatlar) | A/E | — |
| PUT | `/api/notifications/settings` | Sozlamalarni saqlash. Body: `attendanceEnabled`, `gradeEnabled`, `announcementEnabled`, `quietHoursEnabled`, `quietHoursStart`, `quietHoursEnd` (`HH:mm`) | A | — |

```bash
curl "http://localhost:8080/api/notifications?schoolId=1&status=FAILED&from=2026-09-01" \
  -H "Authorization: Bearer $TOKEN"

curl -X PUT "http://localhost:8080/api/notifications/settings?schoolId=1" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"attendanceEnabled":true,"gradeEnabled":true,"announcementEnabled":false,"quietHoursEnabled":true,"quietHoursStart":"22:00","quietHoursEnd":"07:00"}'
```

## Foydalanuvchilar — `/api/users`

Klass darajasida `@PreAuthorize("hasRole('ADMIN')")` — **barcha** 5 ta endpoint faqat `ADMIN` uchun (`schoolId` talab qilinmaydi, `schoolScoped: false`).

| Metod | URL | Vazifa |
|---|---|---|
| GET | `/api/users` | Ro'yxat (pagination) |
| GET | `/api/users/{id}` | Bitta foydalanuvchi |
| POST | `/api/users` | Yaratish (`username`, `password`, `role`, ixtiyoriy `employeeId`) |
| PUT | `/api/users/{id}` | Yangilash (`password` bo'sh qoldirilsa, eski parol saqlanadi) |
| DELETE | `/api/users/{id}` | O'chirish |

## Health — `/api/health`

| Metod | URL | Vazifa | Rol |
|---|---|---|---|
| GET | `/api/health` | `{"status": "UP"}` — tirikligini tekshirish | ochiq |

## Swagger/OpenAPI

Loyihada springdoc/Swagger **ulanmagan** — interaktiv API hujjat sahifasi (masalan `/swagger-ui.html`) mavjud emas. Barcha endpoint'lar shu fayldagi jadvallar orqali hujjatlashtirilgan. Kelajakda qo'shish haqida: [14-kelajak-rejalari.md](14-kelajak-rejalari.md).

---
Keyingisi: [08 — Frontend →](08-frontend.md)
