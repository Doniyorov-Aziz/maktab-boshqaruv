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
- ✅ Server tomonda qidiruv `q`: o'quvchi (ism-familiya), xodim (ism-familiya, telefon), fan, lavozim, o'quv yili. Saralash: ko'rinadigan ustun → entity maydonlari (`fullName` → familiya, ism; `className` → sinf raqami, harfi; `positionTitle` → lavozim nomi); noma'lum ustun avvalgidek 400 «Saralash …» (mavjud `ErrorResponsesIntegrationTest` qoidasi) — `util/PageSort`.
- ✅ Filtr chiplari soni: `GET /api/students/facets` (sinf bo'yicha, Telegram bor/yo'q), `GET /api/employees/facets` (holat, lavozim), `GET /api/subjects/facets` (faol/nofaol).
- ✅ Lavozimni xodimi bilan o'chirish — **409** «Bu lavozimda N ta xodim bor — o'chirib bo'lmaydi».
- ✅ Eksport: `POST /api/export/table?format=xlsx|pdf` — sahifa ko'rsatgan jadvalni (ustunlar, qatorlar) fayl qilib qaytaradi; telefon ustuni `+998 90 111 22 33`.
- ✅ Telefon formatlash: `util/PhoneFormat` (frontenddagi `formatPhone` bilan bir qoida).
- ✅ Indekslar (`config/ListIndexMigration`, ishga tushishda `create index if not exists`).
- ✅ Hammasi `schoolId` bilan cheklangan; ruxsatlar avvalgidek (ro'yxat VIEWER+, o'zgartirish EDITOR+, o'chirish ADMIN).
- ✅ Testlar: `CrudListIntegrationTest` (521 o'quvchi ≤ 3 SQL; boshqa maktab aralashmaydi — ro'yxat, qidiruv, sinf filtri, facet, lavozim soni; lavozim 409; saralash; eksport fayllari), `PhoneFormatTest`.

### 1. Umumiy CrudPage (`pages/CrudPage.vue` + `components/crud/`)
- ✅ 1.1 Sarlavha: PageLayout (IconTile, nom, «1-maktab · 511 ta»); o'ngda sahifa ichidagi qidiruv (**«/»** fokus beradi), **Eksport ▾ Excel / PDF** (`POST /api/export/table`, hozirgi qidiruv va filtrning hamma qatorlari), **Yangi qo'shish**.
- ✅ 1.2 Hero KPI imkoniyati: `module.hero` (endpoint + KpiCard lar). ⚠️ Hech bir modulda hali **yoqilmagan** — KPI lar 4- va 6-bo'limlarga tegishli (keyingi qism).
- ✅ 1.3 Filtrlar select o'rniga **chiplar**, yonida soni (facet endpointlari): «Lavozim: Direktor 1 · Fan o'qituvchisi 21…», «Holat: Ishda 37». Sinf filtri **ClassPicker** («Barchasi» tugmasi ClassPicker ning o'ziga qo'shildi: `allow-all`). Toggle lar ham chip. Faol filtr bo'lsa «× tozalash».
- ✅ 1.4 **Jadval | Kartochkalar** (SegTabs, modulda `cards` bo'lsa). Ko'rinish, qidiruv, filtrlar, saralash, sahifa va hajm **URL da** (`?view=cards&positionId=5&page=2…`); orqaga/oldinga ishlaydi. Telefonda kartochkasi bor modul kartochkalarda ochiladi.
- ✅ 1.5 Jadval (`CrudTable` + `CrudCell`):
  - ustun turlari: `person` (avatar + ism + ostida izoh), `chip`, `badge`, `date` (dd.MM.yyyy), `age`, `phone` (+998 90 123 45 67, `tel:`, hover da nusxalash), `percent` (mini bar + raqam), `grade` (rangli pill + trend), `subjects` (max 3 + «+N»), `list` (3 tagacha chip, ko'p bo'lsa «18 ta sinf» + popover), `spark`, `avatars`, `bool`;
  - amallar ustuni o'ngda sticky, **eni 88px** (tahrirlash + o'chirish; modulning qo'shimcha amallari bo'lsa «⋯» menyu), tooltip bilan, doim ko'rinadi; eski keng bo'sh ustun yo'q;
  - qator bosilsa — o'ngdan **SideDrawer** (batafsil), «Tahrirlash» shu panelning o'zida (modal emas); «Profil» tugmasi;
  - zichlik **Qulay / Ixcham**, ustunlarni yashirish/ko'rsatish menyusi — ikkalasi localStorage da;
  - checkbox (Shift bilan oraliq) → pastda sticky panel: Excel, PDF, o'chirish, modulning `bulkActions`;
  - sahifalash: default **25**, 10/25/50/100, «1–25 / 511»;
  - sarlavha sticky, hover yoritiladi, juda yengil zebra.
- ✅ 1.6 Kartochkalar: `auto-fill, minmax(280px, 1fr)`, dashboard uslubi (`dash-card`). Maktab, bino, sinf kartochkalari endi config dan (ilgari CrudPage ichida qattiq yozilgan edi).
- ✅ 1.7 Forma: o'ngdan **560px** panel (SideDrawer `wide`), **bo'limlar** (`field.section`), 2 ustun (`field.full` — butun qator), inline validatsiya, Saqlash/Bekor pastda sticky, saqlanmagan o'zgarishda ogohlantirish (panelni yopish, sahifadan chiqish), telefon **niqob bilan** (+998 __ ___ __ __), serverga `+998901112233` bo'lib ketadi.
- ✅ 1.8 O'chirish: tasdiqda bog'liqlik (`deleteImpact`): «Bu lavozimda 21 ta xodim bor — o'chirib bo'lmaydi» (backend ham 409). Xodimda o'chirish o'rniga **«Ishdan ketgan deb belgilash»** taklif qilinadi (`deactivate`). Fan — nofaol qilish (avvalgidek). Backend 409 matni dialogda ko'rsatiladi.
- ✅ 1.9 Holatlar: skeleton, bo'sh («+ Qo'shish»), natija yo'q («Filtrni tozalash»), xato («Qayta urinish»).
- ✅ 1.10 Light va dark: ranglar faqat tokenlardan (`--crud-*` light/dark uchun alohida), dark da checkbox lar ham qorong'i.
- ✅ Boshqa 9 modul (maktablar, binolar, xonalar, sinflar, dars jadvali ro'yxati, e'lonlar, tadbirlar, xulq, foydalanuvchilar) yangi CrudPage da ishlaydi.

## Tekshiruv (bu qism)
- ✅ `./gradlew build` (test bazasi): **315 test, 0 xato**, 3 o'tkazilgan (fixture dumplar).
- ✅ `npm run lint`, `npm run lint:check`, `npm run build` — xatosiz.
- ✅ Playwright (fixture + mock, test bazasidan): `crud-logic` 7/7, `crud.spec` **26/26** — amallar ustuni ≤ 88px (1920 light/dark, 1366, 390); xodimda >3 sinf → «N ta sinf» chip + popover; telefon `+998 90 111 22 33` va `tel:`; 25 qator, «1–25 / N», sahifa/hajm URL da; server saralash; «/» → qidiruv; natija yo'q / xato holatlari; ClassPicker filtri (son facet bilan teng, «Barchasi»); chip filtr soni va tozalash; ko'rinish va filtr URL da (reload dan keyin ham); zichlik va ustunlar saqlanadi; qator → panel, oxirigacha scroll; tahrirlash (560px, bo'limlar, niqob, inline xato, saqlanmagan ogohlantirish); lavozim 409; xodim «Ishdan ketgan»; bulk Excel; Eksport PDF; VIEWER; boshqa modullar; telefonda kartochkalar.
- ✅ Umumiy komponentlarga tegilgani uchun: `aj-logic` + `aj` + `pages-v2-logic` + `pages-v2` — **94/94**.
- ✅ «Keyin» rasmlari `docs/images/crud-v2/after-*.png` (5 sahifa × 1920 light, 1920 dark, 390 + `crud-detail-*`, `crud-form-*`, `crud-cards-dark`, `crud-bulk-dark`), har biri ko'rib chiqildi. Topilgan va tuzatilgan: panelda Ism/Familiya/Lavozim takrorlanardi; panel sarlavhasida ism ikki marta; filtrsiz sahifada bo'sh katta kartochka; filtr asboblari alohida qatorga tushardi; dark da oq checkbox lar; telefonda jadval ichki scroll.
- ❌ Haqiqiy backend bilan UI testi — `UI_PASS` yo'q.
- ℹ️ «Bosh sahifa darajasidami?» — karkas (sarlavha, chiplar, jadval, panel, forma, kartochkalar) dashboard tilida. Hali **oddiy** ko'rinadigani — mazmun: o'quvchi/xodim qatorlarida davomat %, baho, xavf, yuklama yo'q; o'quv yillari, fanlar, lavozimlar oddiy jadval. Bular 2–6 bo'limlar.

## CrudPage konfiguratsiyasi — qisqa qo'llanma (`config/modules.js`)
Modul: `serverSearch`, `facetsEndpoint`, `filters: [{ key, label, options | optionsEndpoint…, picker: 'class' }]`, `toggles`, `cards: { title, subtitle?, avatar?, icon?, color?, badge?, text?, stats? (≤4), footer?, footerIcon? }`, `viewType: 'cards'`, `hero: { endpoint, cards: [{ key, label, icon, color, value, format?, delta?, spark? }] }`, `detail: [{ title, items: [{ key, label, value?, col? }] }]`, `titleOf`, `detailSubtitle`, `profileLink`, `deleteImpact(row) → { blocked, message }`, `deactivate: { label, applies, payload }`, `softDelete`, `rowActions`, `bulkActions`.

Ustun: `type`, `sortable`, `sortKey`, `hidden`, `detailOnly`, `width`, `sub(row)`, `link`, `badgeColors/badgeLabels/badgeHint`, `listLabel` + `max`, `thresholds`, `trend(row)`, `format`.

Maydon: `section`, `full`, `type: 'phone'`, `rules`, `showIf`, `parentKey/parentValue`, `requiredOnCreateOnly`.

## O'zgargan / yangi fayllar
Backend: `service/pages/ListStatsService.java`, `service/pages/TableExportService.java`, `controller/ExportController.java`, `util/PageSort.java`, `util/PhoneFormat.java`, `config/ListIndexMigration.java` (yangi); Student/Employee/Subject/Position/AcademicYear — Controller, Service, Repository, ResponseDto; testlar `CrudListIntegrationTest`, `PhoneFormatTest`, `CrudFixtureDump`.

Frontend: `pages/CrudPage.vue` (qayta yozildi); `components/crud/*` (yangi); `ClassPicker.vue` (`allow-all`), `SideDrawer.vue` (`wide`), `config/modules.js`, `css/tokens.scss`, `utils/tashkent.js`; testlar `tests/ui/crud-mock.js`, `crud-logic.spec.js`, `crud.spec.js`, `crud-shots.spec.js`.

## Keyingi qism (hali QILINMAGAN — 2–6 bo'limlar)
- **2. O'quv yillari** — yil kartochkalari (holat, choraklar/ta'tillar progress, «Bugun» markeri, statistikalar), Quarter entity, «Joriy yil», «Keyingi yilni yaratish», kesishma 409, bitta joriy yil (+ testlar).
- **3. Fanlar** — kartochkalar (Subject.icon, o'qituvchilar, soat, sifat + DeltaPill, sparkline), Faol | Nofaol tablar, drawer, rang/ikonka tanlash.
- **4. O'quvchilar** — hero KPI, chiplar (xavf, Telegram, tug'ilgan kun), yangi ustunlar, **manzil jadvaldan olib tashlanadi**, boy panel, ommaviy ko'chirish/xabar. Backend maydonlari (`attendance30`, `average30`, `averagePrev`, `risk`, `telegram`) va facet lar **tayyor**.
- **5. Lavozimlar** — kartochkalar (ikonka, rang, xodimlar + avatar stack, `Position.defaultRole`), kartochka → Xodimlar filtrlangan. 409 va `employeeCount` **tayyor**.
- **6. Xodimlar** — hero KPI, «Sinf rahbarlari» chipi, yuklama bar, jurnal to'liqligi, kartochka, boy panel. `weeklyLessons`, `journal7`, facet lar **tayyor**.
- Shu bo'limlarning Playwright tekshiruvlari va qo'shimcha rasmlar (student-drawer, employee-drawer, subject-cards, academic-years).
