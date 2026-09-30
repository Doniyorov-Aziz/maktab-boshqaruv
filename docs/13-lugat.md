# 13 — Lug'at

[← 12 — Yangi modul qo'shish](12-yangi-modul-qoshish.md) · Keyingisi: [14 — Kelajak rejalari →](14-kelajak-rejalari.md)

Barcha atamalar oddiy tilda, alifbo tartibida (lotin harflari bo'yicha).

## Mundarija
A · B · C · D · E · F · G · H · I · J · L · M · N · O · P · R · S · T

---

### Annotatsiya (annotation)

Java kodida `@` belgisi bilan boshlanadigan, klass/metod/maydonga "yorliq" qo'yish usuli (masalan `@Entity`, `@GetMapping`). O'zi hech narsa bajarmaydi, lekin Spring kabi freymvorklarga "bu klass/metod bilan nima qilish kerakligini" aytadi.

### API (Application Programming Interface)

Ikki dastur bir-biri bilan qanday "gaplashishi" kerakligini belgilaydigan qoidalar to'plami. Bu loyihada — backend'ning frontend'ga taqdim etadigan barcha `/api/...` yo'llari.

### Bean

Spring tomonidan boshqariladigan (yaratiladigan, hayot siklini kuzatadigan) obyekt. `@Service`, `@Repository`, `@Component` bilan belgilangan har bir klass — Bean.

### BCrypt

Parolni xavfsiz saqlash algoritmi: parolni qaytarib bo'lmaydigan (bir tomonlama) "xesh"ga aylantiradi, va har safar tasodifiy "tuz" qo'shadi. Batafsil: [06-backend.md — Parol xeshlash](06-backend.md#parol-xeshlash-bcrypt).

### Build

Loyiha manba kodini ishga tayyor holatga keltirish jarayoni (backend'da: kompilyatsiya + `.jar` yig'ish, `./gradlew build`; frontend'da: JS/CSS'ni optimallashtirib, statik fayllarga aylantirish, `npm run build`).

### Component (komponent)

Vue'da: mustaqil, qayta ishlatiladigan UI bo'lagi (bitta `.vue` fayl). Batafsil: [08-frontend.md — Vue asoslari](08-frontend.md#vue-asoslari-loyihadagi-misollar-bilan).

### Controller

HTTP so'rovlarni qabul qiluvchi qatlam (`@RestController`). Batafsil: [06-backend.md — Controller](06-backend.md#controller).

### CORS (Cross-Origin Resource Sharing)

Brauzer xavfsizlik siyosati: turli manzil (port/domen)dagi sayt/server orasidagi so'rovlarni server aniq ruxsat bermaguncha bloklaydi. Batafsil: [06-backend.md — CORS](06-backend.md#cors).

### Dependency Injection (DI, "bog'liqlikni tashqaridan berish")

Klass o'ziga kerakli obyektni (masalan Repository'ni) o'zi `new` bilan yaratmasdan, Spring tomonidan "tashqaridan" avtomatik berilishi (`@Autowired`). Bu — IoC tamoyilining amaliy ko'rinishi.

### DTO (Data Transfer Object)

Ma'lumotni bir qatlamdan ikkinchisiga (masalan Service'dan Controller'ga, keyin JSON'ga) tashish uchun mo'ljallangan, oddiy Java klassi. Entity emas — nega alohida kerakligi: [06-backend.md — Entity, DTO va Mapper](06-backend.md#entity-dto-va-mapper).

### Endpoint

API'ning bitta aniq "eshigi": bitta HTTP metod + bitta URL kombinatsiyasi (masalan `GET /api/rooms/{id}`).

### Entity

Baza jadvalining bevosita Java aksi (`@Entity` bilan belgilangan klass). Bitta Entity obyekti = bazadagi bitta qator.

### Filter

Har bir HTTP so'rov Controller'ga yetib borishidan **oldin** ishga tushadigan kod bo'lagi (masalan `JwtAuthFilter` — tokenni tekshiradi). Interceptor'dan farqi: Filter — servlet konteyneri (Tomcat) darajasida, freymvorkdan mustaqil ishlaydi.

### Guard

Vue Router'da: sahifaga o'tishdan oldin ishga tushadigan, "ruxsat bormi" tekshiruvchi funksiya (`Router.beforeEach`). Batafsil: [08-frontend.md — Router va himoyalangan sahifalar](08-frontend.md#router-va-himoyalangan-sahifalar).

### Hibernate

JPA standartining eng ko'p ishlatiladigan implementatsiyasi — Java obyektlarini haqiqiy SQL so'rovlariga aylantiruvchi kutubxona.

### HTTP metodlari

Brauzer/klient serverga "nima qilmoqchiligini" bildiruvchi so'rov turlari: **GET** (ma'lumot olish), **POST** (yangi narsa yaratish), **PUT** (mavjudni to'liq yangilash), **DELETE** (o'chirish).

### Idempotent

Bir xil amalni bir necha marta takrorlash bir marta bajarishdan farq qilmaydigan xususiyat. Masalan `DELETE /api/rooms/5` — birinchi chaqiriqda o'chiradi, ikkinchisida ham natija bir xil ("bu xona yo'q"). Loyihadagi seederlar ham idempotent qilib yozilgan — qayta ishga tushirilsa, ma'lumotni takrorlamaydi.

### Interceptor

axios'da: har bir chiqayotgan so'rov yoki kirayotgan javobni "ushlab qolib" o'zgartirish imkoni (masalan token qo'shish, 401'da chiqarish). Batafsil: [08-frontend.md — axios sozlamalari va interceptor'lar](08-frontend.md#axios-sozlamalari-va-interceptorlar).

### IoC (Inversion of Control, "boshqaruvni teskari aylantirish")

Odatiy dasturlashda klass o'ziga kerakli obyektlarni o'zi yaratadi; IoC'da esa freymvork (Spring) buni klass o'rniga bajaradi va tayyor obyektni beradi. Dependency Injection — shu tamoyilning amalga oshirilishi.

### JPA (Java Persistence API)

Java obyektlarini baza jadvaliga moslashtirish uchun standart interfeys (spetsifikatsiya, aniq kutubxona emas).

### JPQL (Java Persistence Query Language)

JPA'ning o'z so'rov tili, SQL'ga o'xshash, lekin jadval/ustun nomlari o'rniga Entity klass/maydon nomlari ishlatiladi. Misollar: [05-malumotlar-bazasi.md — JPA metodlari qanday SQL'ga aylanadi](05-malumotlar-bazasi.md#jpa-metodlari-qanday-sqlga-aylanadi).

### JSON (JavaScript Object Notation)

Matn shaklida ma'lumot almashish formati (`{"key": "value"}`), API so'rov/javoblarining standart tanasi.

### JWT (JSON Web Token)

Foydalanuvchi haqidagi ma'lumotni o'zida tashuvchi, raqamli imzo bilan tasdiqlangan token. Batafsil: [03-texnologiyalar.md — JWT](03-texnologiyalar.md#jwt-jjwt-kutubxonasi).

### Lazy/Eager loading

Bog'liq ma'lumot **qachon** yuklanishi: **eager** — darhol, asosiy so'rov bilan birga; **lazy** — faqat aynan shu maydonga murojaat qilingandagina, alohida qo'shimcha so'rov bilan. Bu loyihada barcha `@ManyToOne` bog'lanishlar standart bo'yicha **eager**.

### LazyInitializationException

Hibernate'ning "lazy" (kechiktirilgan) yuklanadigan maydoniga, baza ulanishi (tranzaksiya/sessiya) allaqachon yopilgandan keyin murojaat qilinsa chiqadigan xato. Oddiy tilda: siz "keyinroq olib kelaman" deb va'da qilingan ma'lumotni so'rayapsiz, lekin uni olib keladigan "xat tashuvchi" (baza ulanishi) allaqachon ketib qolgan. Bu loyihada kamdan-kam uchraydi, chunki barcha `@ManyToOne` bog'lanishlar eager (darhol yuklanadi), va `spring.jpa.open-in-view=false` tufayli, agar shunday xato bo'lsa ham, u yashirin qolmay, **darhol** ko'rinadi. Batafsil: [06-backend.md — application.properties](06-backend.md#applicationproperties) va [11-muammolar-va-yechimlar.md](11-muammolar-va-yechimlar.md#lazyinitializationexception).

### Migratsiya

Baza sxemasini (jadval/ustun qo'shish, o'zgartirish) versiyalab, har bir o'zgarishni alohida, tartiblangan fayl sifatida boshqarish usuli (Flyway/Liquibase kabi vositalar bilan). Bu loyihada ishlatilmaydi — [05-malumotlar-bazasi.md](05-malumotlar-bazasi.md#migratsiyalar-bu-loyihada-qanday-ishlaydi)ga qarang.

### N+1 muammosi

Bitta so'rov o'rniga, ro'yxatdagi har bir element uchun **qo'shimcha** alohida so'rov yuborilib ketishi (masalan 20 ta xonani olish uchun 1 ta so'rov, keyin har birining binosi uchun yana 20 ta so'rov — jami 21 ta, "N+1"). Sabab — lazy loading'ni e'tiborsiz ishlatish. Bu loyihada barcha bog'lanishlar eager bo'lgani va Repository darajasida aniq JOIN so'rovlar yozilgani uchun bu muammo kamdan-kam uchraydi.

### ORM (Object-Relational Mapping)

Java obyektlari (ob'ekt dunyosi) va baza jadvallari (relyatsion dunyo) orasidagi "tarjimon" yondashuvining umumiy nomi. Hibernate — ORM'ning bitta implementatsiyasi.

### Pagination (sahifalash)

Katta ro'yxatni bir vaqtda to'liq emas, kichik "sahifalar" (masalan 20 tadan) qilib qaytarish. Loyihada `Pageable`/`Page<T>` orqali amalga oshiriladi.

### Props

Vue'da: ota-komponentdan bola-komponentga uzatiladigan ma'lumot (bir tomonlama, yuqoridan pastga). Batafsil: [08-frontend.md — Vue asoslari](08-frontend.md#vue-asoslari-loyihadagi-misollar-bilan).

### Repository

Bazaga to'g'ridan-to'g'ri murojaat qiluvchi qatlam, Spring Data JPA interfeysi orqali. Batafsil: [06-backend.md — Repository](06-backend.md#repository).

### REST (Representational State Transfer)

API qurish uslubi: har bir resurs (masalan "o'quvchi") o'z URL'iga ega, va standart HTTP metodlari (GET/POST/PUT/DELETE) orqali boshqariladi.

### Router

Vue'da: URL manzil bilan qaysi sahifa (komponent) ko'rsatilishini bog'lovchi tizim.

### Seed (ma'lumot urug'i)

Ilova birinchi marta ishga tushirilganda avtomatik yaratiladigan namunaviy/boshlang'ich ma'lumot (masalan dastlabki ADMIN foydalanuvchi). Batafsil: [05-malumotlar-bazasi.md — Seed ma'lumotlar](05-malumotlar-bazasi.md#seed-malumotlar).

### Service

Biznes mantiq qatlami: qoidalarni tekshiradi, bir nechta Repository'ni birlashtiradi. Batafsil: [06-backend.md — Service va @Transactional](06-backend.md#service-va-transactional).

### SPA (Single Page Application, "bitta sahifali ilova")

Brauzer bitta HTML sahifani yuklaydi, keyingi barcha navigatsiya sahifa qayta yuklanmasdan, JavaScript orqali bo'ladigan veb-ilova turi.

### Store

Vue ilovasida bir nechta komponent orasida umumiy holatni (masalan "kim tizimga kirgan") saqlovchi joy (bu loyihada — Pinia).

### Tranzaksiya (transaction)

Bir nechta baza amalini "bitta bo'lak" qilib bog'lash: yoki hammasi muvaffaqiyatli bajariladi, yoki (xato chiqsa) hammasi bekor qilinadi. Spring'da `@Transactional` annotatsiyasi orqali. Batafsil: [06-backend.md — Service va @Transactional](06-backend.md#service-va-transactional).

---
Keyingisi: [14 — Kelajak rejalari →](14-kelajak-rejalari.md)
