# CRUD sahifalari v2 — 1-qism (umumiy CrudPage + backend) va 2-qism (5 sahifa)

Branch: `feature/crud-v2` (worktree `.claude/worktrees/crud-v2`, `main` 7971c5f dan). **Push qilinmagan.**
1-qism: vazifaning **0, 1 va 7-bo'limlari**. 2-qism: **2–6 bo'limlar** (pastda, «2-qism»).
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
- ✅ SQL soni: o'quvchilar sahifasi **3 ta** (sahifa + count + raqamlar) — 521 o'quvchi bilan Hibernate statistics testi (xavf keshi isitilgan holda). Xodimlar ham **3 ta** (sahifa + count + bitta so'rov: nimani o'qitadi, qaysi sinfga rahbar, haftalik dars; jurnal keshdan) — 3-qismda 5 tadan tushirildi, testi bor. Fan/lavozim/yil: sahifa + count + 1 guruhlangan SQL.
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
- ✅ 1.2 Hero KPI: `module.hero` (endpoint + KpiCard lar) — O'quvchilar va Xodimlar sahifalarida yoqilgan (2-qism).
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
- ℹ️ «Bosh sahifa darajasidami?» — karkas (sarlavha, chiplar, jadval, panel, forma, kartochkalar) dashboard tilida; sahifalarning mazmuni (ko'rsatkichlar, kartochkalar, panellar) 2-qismda qo'shildi.

## CrudPage konfiguratsiyasi — qisqa qo'llanma (`config/modules.js`)
Modul: `serverSearch`, `facetsEndpoint`, `filters: [{ key, label, options | optionsEndpoint…, picker: 'class' }]`, `toggles`, `cards: { title, subtitle?, avatar?, icon?, color?, badge?, text?, stats? (≤4), footer?, footerIcon? }`, `viewType: 'cards'`, `hero: { endpoint, cards: [{ key, label, icon, color, value, format?, delta?, spark? }] }`, `detail: [{ title, items: [{ key, label, value?, col? }] }]`, `titleOf`, `detailSubtitle`, `profileLink`, `deleteImpact(row) → { blocked, message }`, `deactivate: { label, applies, payload }`, `softDelete`, `rowActions`, `bulkActions`.

Ustun: `type`, `sortable`, `sortKey`, `hidden`, `detailOnly`, `width`, `sub(row)`, `link`, `badgeColors/badgeLabels/badgeHint`, `listLabel` + `max`, `thresholds`, `trend(row)`, `format`.

Maydon: `section`, `full`, `type: 'phone'`, `rules`, `showIf`, `parentKey/parentValue`, `requiredOnCreateOnly`.

## O'zgargan / yangi fayllar
Backend: `service/pages/ListStatsService.java`, `service/pages/TableExportService.java`, `controller/ExportController.java`, `util/PageSort.java`, `util/PhoneFormat.java`, `config/ListIndexMigration.java` (yangi); Student/Employee/Subject/Position/AcademicYear — Controller, Service, Repository, ResponseDto; testlar `CrudListIntegrationTest`, `PhoneFormatTest`, `CrudFixtureDump`.

Frontend: `pages/CrudPage.vue` (qayta yozildi); `components/crud/*` (yangi); `ClassPicker.vue` (`allow-all`), `SideDrawer.vue` (`wide`), `config/modules.js`, `css/tokens.scss`, `utils/tashkent.js`; testlar `tests/ui/crud-mock.js`, `crud-logic.spec.js`, `crud.spec.js`, `crud-shots.spec.js`.

## 2-qism (2–6 bo'limlar) — 2026-10-20

### 2. O'quv yillari
- ✅ Jadval o'rniga **yil kartochkalari** (yangisi tepada, `YearsView`): nom, holat (Joriy — brend rangida glow / Davom etmoqda / Yakunlangan / Rejalashtirilgan), sanalar; choraklar va ta'tillar **sanasi bo'yicha segmentli bar** (ta'til chiziqli), o'tgan qism, joriy yilda **«Bugun» markeri**, «21% o'tdi · 158 ish kuni qoldi», «Hozir: 1-chorak»; ish kunlari, o'quvchilar va sinflar, davomat %, sifat % (o'tganlar uchun «yakuniy»).
- ✅ Choraklar va ta'tillar — yangi `AcademicTerm` entity (nom, tur QUARTER/VACATION, sanalar, yil); panelda tahrir (`YearDetail`), «Odatiy taqvim», bir martada saqlash.
- ✅ «Joriy yil qilib belgilash» (`AcademicYear.current`, maktabda bitta), «Keyingi yilni yaratish» (oxirgi yil sanalari va choraklari +1 yil; yil bo'lmasa odatiy taqvim; dialogda o'zgartirish mumkin).
- ✅ Backend: yillar kesishsa **409**; chorak yil tashqarisida yoki ustma-ust bo'lsa **409** (hech narsa saqlanmaydi). Ish kunlari — yakshanba va bayram/ta'til tadbirlarisiz.
- ✅ Vaqt chizig'ida **sudrab o'zgartirish** (3-qism): panelda yil bari, har chorak/ta'tilning boshlanish va tugash chetini sichqoncha bilan sudrash yoki fokus qilib ← → (Shift — 7 kun); yil ichida, qo'shni qismga tegmasdan, o'z boshqa chetidan o'tmasdan (`years.js#moveEdge`), keyin «Choraklarni saqlash».

### 3. Fanlar
- ✅ Default **kartochkalar** (`SubjectCard`): fan rangidagi ikonka (`Subject.icon`, bo'lmasa nomdan taklif: Matematika → calculate, Fizika → science, Ona tili → menu_book …), nom, soat/hafta, sinflar, o'qituvchilar avatar stack, **shu chorak sifati + DeltaPill**, **30 kunlik o'rtacha sparkline**; nofaol — xira + «Nofaol».
- ✅ **Faol | Nofaol** tablar (sonlari bilan) eski toggle o'rniga (`?status=ACTIVE|INACTIVE`, URL da).
- ✅ Panel (`SubjectDetail`): baholar taqsimoti 5/4/3/2, sifat va oldingi davr, o'qituvchilar va yuklama, **har o'qituvchi qaysi sinflarda haftasiga necha soat** (3-qism), sinflar bo'yicha soat va sifat.
- ✅ Formada **12 rangli palitra** va **ikonka tanlash** (taklif qilingani punktir bilan).
- ℹ️ «Chorak» — joriy yilning `AcademicTerm` choraklaridan; ular kiritilmagan bo'lsa oxirgi 60 kun va undan oldingi 60 kun.

### 4. O'quvchilar
- ✅ **Hero KPI**: jami, bugungi davomat (14 kunlik sparkline; bugun hali belgilanmagan bo'lsa — oxirgi ish kuni, izohda sanasi), xavf ostidagilar (bosilsa filtr), Telegramdagi ota-onalar %. ⚠️ O'g'il/qiz nisbati yo'q — jins maydoni yo'q.
- ✅ Filtrlar: ClassPicker («Barchasi») + chiplar **Xavf ostida · Telegram ✓/✗ · 🎂 Bugun** (sonlari bilan, server tomonda).
- ✅ Ustunlar: avatar + ism (ostida «1-A sinf · 7 yosh») · ota-ona (ism, ostida bosiladigan telefon) · davomat % (30 kun, bar) · o'rtacha baho (pill + trend) · **xavf balli har bir o'quvchida** (kichik halqa, 0–100; 3-qismda faqat 40+ emas, hammasi) · Telegram ✓/✗. **Manzil jadvalda ham, ustun menyusida ham yo'q** — faqat panelda.
- ✅ Kartochkalar: avatar, ism, sinf, 3 ko'rsatkich.
- ✅ Panel (`StudentDetail`): sinf, yosh, Telegram; 3 KPI; **30 kunlik davomat nuqtalari**; **xavf sabablari**; fanlar bo'yicha o'rtacha; ota-ona va telefon; tug'ilgan sana, joy, manzil; xulq yozuvlari; [Ota-onaga xabar] [Profil].
- ✅ Ommaviy: **boshqa sinfga ko'chirish** (boshqa maktabga — 409), Excel/PDF, **ota-onalarga xabar** (Xabarlar sahifasi `?students=…` bilan ochiladi va Telegramga ulangan ota-onalarni o'zi belgilaydi).

### 5. Lavozimlar
- ✅ **Kartochkalar** (`PositionCard`): nomga mos ikonka va rang, nom, tavsif (`Position.description` — yangi), xodimlar soni + avatar stack, **rol chipi** (`Position.defaultRole` — yangi; Foydalanuvchilar formasida xodim tanlanganda shu rol taklif qilinadi).
- ✅ Kartochka bosilsa → **Xodimlar shu lavozim filtri bilan**. Xodimi bor lavozimni o'chirish — 409.

### 6. Xodimlar
- ✅ **Hero KPI**: jami, ishda (izohda ta'tildagilar), o'qituvchilarning o'rtacha haftalik yuklamasi, jurnal to'liqligi % (7 ish kuni).
- ✅ Chiplar: lavozimlar (soni), holat, **«Sinf rahbarlari»**.
- ✅ Ustunlar: avatar + ism (ostida lavozim) · holat (ta'tilda — «20-okt gacha») · fanlar (max 3 + «+N») · sinflar («18 ta sinf» + popover) · sinf rahbari · **yuklama** (soat + normaga (18) nisbatan bar; dars bermasa «—») · jurnal % · telefon.
- ✅ Kartochkalar (`EmployeeCard`): avatar, ism, lavozim, fanlar, **yuklama halqasi**, telefon, rahbarlik.
- ✅ Panel (`EmployeeDetail`): kontakt (qo'ng'iroq, nusxa), holat va ta'til sanalari, **jurnal to'liqligi dinamikasi** (10 ish kuni), **yuklama issiqlik xaritasi** (kunlar × darslar), **bu haftalik dars jadvali**, fanlar va sinflar, [Jadvalni ko'rish] [Tahrirlash].

### Backend (2-qism)
- Yangi: `AcademicTerm`, `AcademicYear.current`, `Subject.icon`, `Position.description` / `defaultRole`; `ListPagesService` + `ListPagesController` (`/api/pages/lists/students/hero`, `/students/{id}/extra`, `/students/move`, `/employees/hero`, `/employees/{id}/extra`, `/subjects/{id}/extra`, `/years`); `/api/academic-years/{id}/current`, `/{id}/terms` (GET/PUT), `/next` (GET/POST).
- Filtrlar: o'quvchilar `atRisk`, `telegram`, `birthday`; xodimlar `classTeacher`; fanlar `status`.
- **Topilgan va tuzatilgan mavjud xato:** o'qituvchi bir nechta sinfga rahbar bo'lsa `/api/profiles/teachers/{id}` 500 qaytarardi — endi «5-A, 6-B».
- Ota-onalar tanlovi (`/api/telegram/parents/choices`) ga `studentId` qo'shildi.
- Testlar: `CrudPagesIntegrationTest` (5 test) — yillar kesishmasi 409, bitta joriy yil, choraklar 409 va eski holat saqlanadi, keyingi yil; o'quvchi chiplari, ko'chirish (boshqa maktabga emas), sinf rahbarlari, Faol/Nofaol, lavozim roli (noma'lum → 400), xodim issiqlik xaritasi, fan sinflari, hero lar.

### Tekshiruv (2-qism)
- ✅ `./gradlew build` (test bazasi): **320 test, 0 xato**.
- ✅ `npm run lint`, `lint:check`, `build` — xatosiz.
- ✅ Playwright: `crud-logic` 9/9, `crud.spec` 26/26, **`crud-pages.spec` 12/12**, `crud-shots` 18/18; regressiya `aj` + `pages-v2` **94/94**.
- ✅ Rasmlar ko'rib chiqildi va tuzatildi: fan ikonkalari kichik edi, avatarlar yopishardi, filtr qatorlari bir qatorga sig'masdi, lavozim nomi kesilardi, dars bermaydigan xodimda «0 soat», telefonda hero 4 qator, 13 sinf rahbari uzun qator, bugun davomat bo'lmaganda «—».
- ℹ️ Test bazasida bugungi sanani o'z ichiga olgan o'quv yili yo'q — «Bugun» markeri testi va rasmi uchun mock **sun'iy joriy yil** qo'shadi (`crud-mock.js#syntheticYear`).
- ❌ Haqiqiy backend bilan UI testi — `UI_PASS` yo'q.
- ✅ Foydalanuvchilar formasidagi rol taklifi brauzerda tekshirildi (3-qism, `crud-pages-2`).

## 3-qism — chala qolganlar tugatildi (2026-10-20)
- ✅ **2. O'quv yillari:** chorak/ta'tillarni **vaqt chizig'ida sudrab** o'zgartirish (sichqoncha va klaviatura), qoidalari `years.js#moveEdge`. Panelda chorak qatori ikki qavat (nom va tur, keyin sanalar) — hamma maydon o'qiladi. Topilgan va tuzatilgan: bar qo'shilganda tahrir ro'yxati `v-else` ostida qolib yashirinardi; yuklanish tugamay «Odatiy taqvim» bosilsa kechikkan javob kiritilganni o'chirardi (tugmalar yuklanish tugaguncha o'chiq).
- ✅ **3. Fanlar:** panelda o'qituvchi → sinflar (haftasiga soat) — `/api/pages/lists/subjects/{id}/extra` ga `byTeacher`.
- ✅ **4. O'quvchilar:** xavf balli **har bir o'quvchi** uchun (`HomeDashboardService.riskStudents(..., minScore)` — xavf ro'yxati avvalgidek 40 dan, ro'yxat sahifasi 0 dan; `ListStatsService.allRisk`, kesh 90 s); kartochka va panel ham (xavf 0 bo'lsa «Xavf belgilari yo'q»). ⚠️ O'g'il/qiz nisbati — jins maydoni yo'q, qilinmadi (topshiriqda «agar maydon bo'lsa»).
- ✅ **5. Lavozimlar:** lavozimning roli Foydalanuvchilar formasida taklif qilinishi — brauzer testi.
- ✅ **6. Xodimlar:** ro'yxat **3 ta SQL** (5 dan) — bitta native so'rov (o'qitadi + rahbar + haftalik dars), jurnal keshdan; Hibernate statistics testi.
- ✅ **Har sahifa 4 o'lchamda** (1920 light, 1920 dark, 1366, 390): asosiy element, gorizontal scroll yo'q, panel ekranga sig'adi, telefonda hero 2 ustun, konsolda xato yo'q (`crud-pages-2.spec.js`, 20 test).

### Tekshiruv (3-qism)
- ✅ `./gradlew build` (test bazasi, fixture dumplarsiz): **321 test, 0 xato**, 3 o'tkazilgan. `EmployeeServiceTest` (mock) ga `EntityManager` mock qo'shildi.
- ✅ `npm run lint`, `lint:check`, `build` — xatosiz.
- ✅ Playwright: `crud-logic` **10/10** (sudrash qoidalari), `crud.spec` 26/26, `crud-pages` 12/12, **`crud-pages-2` 24/24**, `crud-shots` 18/18 — jami **90/90**.
- ✅ Yangi rasm `after-year-drawer.png` (bar + ikki qavatli qatorlar), qolganlari qayta olindi va ko'rib chiqildi.
- ⚠️ Regressiya `aj` + `pages-v2`: 89/94 — 5 tasi **sanaga bog'liq** (dushanba darslari, «hozirgi dars», kalendar tadbiri). Sabab: bugun backend build paytida eski fixture fayllar (kechagi sessiya, 19-oktabr dushanba) test bazasidan bugungi sana bilan qayta yozildi (muhit o'zgaruvchilari ochiq qolgan — mening xatoim; faqat test bazasi). **Isbot:** o'sha 5 ta test `main` (7971c5f) build ida ham aynan shu fixture lar bilan yiqiladi — kod emas, ma'lumot.
- ❌ Haqiqiy backend bilan UI testi — `UI_PASS` yo'q.

