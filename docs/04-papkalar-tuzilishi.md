# 04 — Papkalar tuzilishi

[← 03 — Texnologiyalar](03-texnologiyalar.md) · Keyingisi: [05 — Ma'lumotlar bazasi →](05-malumotlar-bazasi.md)

## Mundarija
- [Backend papka daraxti](#backend-papka-daraxti)
- [Frontend papka daraxti](#frontend-papka-daraxti)

## Backend papka daraxti

```
src/
├── main/
│   ├── java/uz/azizbek/maktabboshqaruv/
│   │   ├── MaktabBoshqaruvApplication.java   # kirish nuqtasi (main metod)
│   │   ├── config/            (5 fayl)       # Spring konfiguratsiyasi + seed data
│   │   ├── controller/        (20 fayl)      # HTTP endpoint'lar
│   │   ├── dto/                (54 fayl)     # Request/Response obyektlari
│   │   ├── entity/             (25 fayl)     # 17 @Entity klass + 8 enum
│   │   ├── exception/           (1 fayl)     # Global xato ushlagich
│   │   ├── repository/        (17 fayl)      # Spring Data JPA interfeyslari
│   │   ├── service/            (19 fayl)     # Biznes mantiq
│   │   └── util/                (1 fayl)     # JwtUtil
│   └── resources/
│       ├── application.properties            # Asosiy sozlamalar (maxfiy qiymatlarsiz)
│       ├── application-local.properties.example  # Lokal maxfiy sozlamalar shabloni
│       └── application-local.properties      # (gitignore'da) DB paroli, JWT secret — "local" profil
└── test/
    └── java/uz/azizbek/maktabboshqaruv/
        ├── MaktabBoshqaruvApplicationTests.java  # kontekst yuklanish testi
        ├── service/            (10 fayl)         # Service-qatlam testlari (Mockito)
        └── util/                (1 fayl)         # JwtUtilTest
```

### `config/` — nega alohida papka

Odatda Spring loyihalarida xavfsizlik sozlamalari `security/` papkasida bo'ladi, lekin bu loyihada hammasi `config/`ga jamlangan — kichik loyiha uchun ortiqcha bo'linishdan qochish maqsadida:

| Fayl | Vazifasi |
|---|---|
| `SecurityConfig.java` | Spring Security filter zanjiri, CORS, parol hash algoritmi |
| `JwtAuthFilter.java` | Har bir so'rovda JWT tokenni tekshiruvchi filter |
| `AdminSeeder.java` | Birinchi ishga tushirishda ADMIN foydalanuvchi yaratadi (`@Order(1)`) |
| `DataSeeder.java` | Namunaviy maktab/bino/xona/xodim/sinf/o'quvchi/jadval ma'lumotlarini yaratadi (`@Order(2)`) |
| `OperationalDataSeeder.java` | Davomat/baho/e'lon/tadbir kabi "operatsion" namunaviy ma'lumotlarni yaratadi, `ustoz1`/`ustoz2` akkauntlarini o'qituvchiga bog'laydi (`@Order(3)`) |

### `controller/` — har biri bitta resursga mos

20 ta controller, deyarli har biri bitta Entity guruhiga mos keladi (`SchoolController`, `BuildingController`, `RoomController`, ...). Bundan tashqari maxsus vazifali controller'lar:

| Fayl | Vazifasi |
|---|---|
| `AuthController.java` | Login, parol hash olish (`/api/auth/**`) |
| `DashboardController.java` | Bosh sahifa uchun statistik/agregatsiya endpoint'lari |
| `ProfileController.java` | O'quvchi/o'qituvchi/sinf profil sahifalari uchun to'plangan ma'lumot |
| `HealthController.java` | Autentifikatsiyasiz "tirikmi" tekshiruvi |

### `dto/` — nega 54 ta fayl

Har bir Entity uchun odatda ikkita DTO bor: `XxxRequestDto` (yaratish/yangilash uchun kiruvchi ma'lumot) va `XxxResponseDto` (chiquvchi, tekis/flat qilingan ma'lumot). Bundan tashqari, Dashboard va profil sahifalari uchun maxsus, bir nechta Entity'dan yig'ilgan "agregat" DTO'lar bor (`DashboardOverviewDto`, `ClassProfileDto`, `TodayLessonDto` va h.k.). Nega Entity to'g'ridan-to'g'ri qaytarilmasligi: [06-backend.md — Entity, DTO va Mapper](06-backend.md#entity-dto-va-mapper).

### `entity/` — 17 klass + 8 enum

To'liq ro'yxat va bog'lanishlar uchun: [05-malumotlar-bazasi.md](05-malumotlar-bazasi.md).

## Frontend papka daraxti

```
frontend/
├── src/
│   ├── App.vue                # ildiz komponent — dark mode'ni ishga tushiradi
│   ├── boot/                  # Quasar "boot fayllari" — ilova ishga tushishidan oldin bajariladi
│   │   ├── axios.js           # axios instance + interceptor'lar
│   │   ├── lang.js            # o'zbekcha tilni Quasar'ga o'rnatadi
│   │   └── pinia.js           # Pinia'ni ilovaga ulaydi
│   ├── components/            # qayta ishlatiladigan, sahifaga bog'liq bo'lmagan komponentlar
│   │   ├── ClassRankingCard.vue
│   │   ├── DateField.vue
│   │   └── PageLayout.vue
│   ├── config/
│   │   └── modules.js         # barcha CRUD modullarning markazlashgan konfiguratsiyasi
│   ├── css/
│   │   ├── app.scss           # global CSS token'lar (light/dark)
│   │   └── quasar.variables.scss  # Quasar rang palitrasi
│   ├── lang/
│   │   └── uz.js              # Quasar komponentlari uchun o'zbekcha tarjima
│   ├── layouts/
│   │   └── MainLayout.vue     # header + chap sidebar + sahifa konteyneri
│   ├── pages/                 # har bir marshrutga mos sahifa komponenti (12 ta)
│   ├── router/
│   │   ├── index.js           # Router obyekti + autentifikatsiya guard'i
│   │   └── routes.js          # barcha yo'llar ro'yxati
│   ├── stores/                # Pinia store'lar
│   │   ├── auth.js            # token, username, role, employeeId
│   │   └── school.js          # tanlangan maktab
│   └── utils/
│       ├── date.js            # sana formatlash yordamchilari
│       └── jwt.js             # JWT'ni brauzerda dekodlash (imzoni TEKSHIRMAYDI, faqat o'qiydi)
├── quasar.config.js            # Quasar/Vite build sozlamalari
├── package.json
└── .env / .env.example         # QCLI_API_BASE_URL (backend manzili)
```

### `pages/` — har bir sahifa

| Fayl | Marshrut | Vazifasi |
|---|---|---|
| `LoginPage.vue` | `/login` | Kirish shakli |
| `DashboardPage.vue` | `/` | Bosh sahifa: KPI kartalar, grafiklar, reyting, e'tibor talab qiladigan holatlar |
| `CrudPage.vue` | `/app/:moduleKey` | **Universal** CRUD sahifa — 14 ta modulning barchasi shu bitta komponent orqali ko'rsatiladi |
| `AttendancePage.vue` | `/attendance` | Kundalik davomat olish |
| `GradebookPage.vue` | `/gradebook` | Baholar jurnali |
| `TimetablePage.vue` | `/timetable` | Dars jadvali (jonli vaqt chizig'i, PDF) |
| `CalendarPage.vue` | `/calendar` | Tadbirlar taqvimi |
| `StudentProfilePage.vue` | `/profiles/student/:id` | Bitta o'quvchi profili |
| `TeacherProfilePage.vue` | `/profiles/teacher/:id` | Bitta o'qituvchi profili |
| `ClassProfilePage.vue` | `/profiles/class/:id` | Bitta sinf profili |
| `ErrorNotFound.vue` | `/:catchAll(.*)*` | 404 sahifa |

`CrudPage.vue`ning "universal" bo'lishi — bitta komponent `config/modules.js`dagi konfiguratsiyaga qarab 14 xil modulni (Maktablar, Binolar, Xonalar, ...) ko'rsata olishi — bu loyihaning eng muhim frontend arxitektura qarori. Batafsil: [08-frontend.md — CrudPage: universal komponent](08-frontend.md#crudpage-universal-komponent) va [12-yangi-modul-qoshish.md](12-yangi-modul-qoshish.md).

---
Keyingisi: [05 — Ma'lumotlar bazasi →](05-malumotlar-bazasi.md)
