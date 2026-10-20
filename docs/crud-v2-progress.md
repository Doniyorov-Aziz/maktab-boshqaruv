# CRUD sahifalari v2 — 1-qism: umumiy CrudPage + backend asoslari

Branch: `feature/crud-v2` (worktree `.claude/worktrees/crud-v2`, `main` 7971c5f dan). **Push qilinmagan.**
Bu qismda faqat vazifaning **0, 1 va 7-bo'limlari**. 2–6 bo'limlar — keyingi qism (pastda).
Sessiya uzilsa — ❌ bandlardan davom eting. **Backend testlari faqat `DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test` bilan.**

## Reja
1. Audit, «oldin» rasmlari (fixture + mock API, test bazasidan).
2. Backend: qidiruv `q`, hisoblangan maydonlar, facet (filtr soni) endpointlari, lavozim o'chirish 409, eksport endpointi, telefon formatlash util, testlar.
3. Frontend: `components/crud/` (jadval kataklari, filtrlar, kartochkalar, drawer, forma), yangi `CrudPage.vue`, `modules.js` ga ustun turlari.
4. Playwright, «keyin» rasmlari, hisobot.

## Holat

### 0. Audit
- ✅ O'qildi: `pages/CrudPage.vue` (1250 qator, q-table + modal o'rniga SideDrawer forma, select filtrlar, qidiruv uchun 3000 ta qatorni yuklab brauzerda filtrlash), `config/modules.js` (14 modul), 5 modulning controller/service/repository/DTO lari.
- ✅ Topilganlar: o'quvchilar ro'yxatida SchoolClass ning EAGER bog'lanishlari (o'quv yili, sinf rahbari) har sinf uchun alohida SQL; xodimlarda lavozim/maktab har biri alohida; `sort=fullName` serverda xato (bunday maydon yo'q); xodim/fan/lavozim/yilda server qidiruvi yo'q; lavozimni xodimi bilan o'chirish FK xatosi (500); indekslar yo'q: attendance(student_id, record_date), students(class_id), lesson_slot(employee_id/class_id/subject_id), employee(school_id, position_id).
- ✅ «Oldin» rasmlari: `docs/images/crud-v2/before-*.png` (5 sahifa × 1920 light, 1920 dark, 390 = 15 ta). Mock API (`tests/ui/crud-mock.js`) test bazasidan yozilgan fixture (`CrudFixtureDump`) ni server kabi sahifalaydi, saralaydi, qidiradi va filtrlaydi.

### 7. Backend
- ✅ Hisoblangan maydonlar (`service/pages/ListStatsService`), sahifa uchun bitta SQL:
  - O'quvchi: `attendance30` (%, 30 kun, sababli hisobga olinmaydi), `average30`, `averagePrev` (oldingi 30 kun), `risk` (40+ — xavf ro'yxatidan, keshdan; pastida null), `telegram`.
  - Xodim: `weeklyLessons` (haftalik dars soni), `journal7` (oxirgi 7 ish kuni jurnal to'liqligi %, keshdan).
  - Fan: `teacherCount`, `classCount`, `weeklyLessons`. Lavozim: `employeeCount` (maktab bo'yicha; `schoolId` siz — hamma maktab). O'quv yili: `classCount`, `studentCount`.
- ✅ Kesh: xavf ro'yxati va jurnal to'liqligi — mavjud `DashboardCache` (90 s, davomat/baho saqlanganda tozalanadi).
- ✅ SQL soni: o'quvchilar sahifasi **3 ta** (sahifa + count + raqamlar) — 521 o'quvchi bilan Hibernate statistics testi (xavf keshi isitilgan holda). Xodimlar: 5 ta (sahifa, count, nimani o'qitadi, sinf rahbarligi, haftalik dars) — qator soniga bog'liq emas. Fan/lavozim/yil: sahifa + count + 1 guruhlangan SQL.
- ✅ Server tomonda qidiruv `q`: o'quvchi (ism-familiya), xodim (ism-familiya, telefon), fan, lavozim, o'quv yili. Saralash: ko'rinadigan ustun → entity maydonlari (`fullName` → familiya, ism; `className` → sinf raqami, harfi; `positionTitle` → lavozim nomi); noma'lum ustun tashlab yuboriladi (500 emas) — `util/PageSort`.
- ✅ Filtr chiplari soni: `GET /api/students/facets` (sinf bo'yicha, Telegram bor/yo'q), `GET /api/employees/facets` (holat, lavozim), `GET /api/subjects/facets` (faol/nofaol).
- ✅ Lavozimni xodimi bilan o'chirish — **409** «Bu lavozimda N ta xodim bor — o'chirib bo'lmaydi».
- ✅ Eksport: `POST /api/export/table?format=xlsx|pdf` — sahifa ko'rsatgan jadvalni (ustunlar, qatorlar) fayl qilib qaytaradi; telefon ustuni `+998 90 111 22 33`.
- ✅ Telefon formatlash: `util/PhoneFormat` (frontenddagi `formatPhone` bilan bir qoida).
- ✅ Indekslar (`config/ListIndexMigration`, ishga tushishda `create index if not exists`).
- ✅ Hammasi `schoolId` bilan cheklangan; ruxsatlar avvalgidek (ro'yxat VIEWER+, o'zgartirish EDITOR+, o'chirish ADMIN).
- ✅ Testlar: `CrudListIntegrationTest` (521 o'quvchi ≤ 3 SQL; boshqa maktab aralashmaydi — ro'yxat, qidiruv, sinf filtri, facet, lavozim soni; lavozim 409; saralash; eksport fayllari), `PhoneFormatTest`.

### 1. Umumiy CrudPage
(ish davomida)
