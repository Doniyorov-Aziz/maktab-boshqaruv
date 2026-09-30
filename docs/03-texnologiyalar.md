# 03 — Texnologiyalar

[← 02 — Arxitektura](02-arxitektura.md) · Keyingisi: [04 — Papkalar tuzilishi →](04-papkalar-tuzilishi.md)

## Mundarija
- [Texnologiyalar jadvali](#texnologiyalar-jadvali)
- [Backend texnologiyalari batafsil](#backend-texnologiyalari-batafsil)
- [Frontend texnologiyalari batafsil](#frontend-texnologiyalari-batafsil)
- [Baza](#baza)
- [Dev vositalari](#dev-vositalari)

## Texnologiyalar jadvali

Versiyalar `build.gradle` va `frontend/package.json`dan olingan (aniq versiya ko'rsatilmagan joyda Spring Boot BOM — **B**ill **O**f **M**aterials, ya'ni Spring Boot o'zi mos versiyani tanlaydi — orqali boshqarilishi bildiriladi).

### Backend

| Nomi | Versiyasi | Vazifasi |
|---|---|---|
| Java | 21 | Dasturlash tili (`build.gradle`, `toolchain.languageVersion`) |
| Spring Boot | 4.1.0 | Asosiy freymvork (`build.gradle`, plugin versiyasi) |
| Spring Web (`spring-boot-starter-webmvc`) | Spring Boot bilan boshqariladi | REST API qatlami |
| Spring Data JPA | Spring Boot bilan boshqariladi | Baza bilan ishlash (Hibernate ustida) |
| Spring Security | Spring Boot bilan boshqariladi | Autentifikatsiya/avtorizatsiya |
| Spring Boot Validation | Spring Boot bilan boshqariladi | `@Valid`, Bean Validation |
| jjwt (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) | 0.12.5 | JWT token yaratish/tekshirish |
| Lombok | Spring Boot bilan boshqariladi (`compileOnly`) | Boilerplate kodni qisqartirish (loyihada minimal ishlatilgan) |
| PostgreSQL JDBC drayveri | Spring Boot BOM bilan boshqariladi (`runtimeOnly`) | Bazaga ulanish |
| Gradle | 9.5.1 (wrapper) | Build vositasi |
| JUnit 5 + Mockito | `spring-boot-starter-test` orqali | Backend testlari |
| SLF4J + Logback | Spring Boot bilan birga keladi (alohida qo'shilmagan) | Loglash |

**Loyihada YO'Q, shuning uchun tilga olinmaydi:** MapStruct, Flyway, Liquibase, springdoc/Swagger, Maven. (Bular haqida nega yo'qligi va kelajakda qo'shish mumkinligi: [14-kelajak-rejalari.md](14-kelajak-rejalari.md).)

### Frontend

| Nomi | Versiyasi | Vazifasi |
|---|---|---|
| Vue | 3.5.22 | UI freymvork |
| Quasar | 2.32.3 | Komponentlar kutubxonasi + build tizimi |
| @quasar/app-vite | 3.8.4 | Quasar'ning Vite asosidagi build vositasi |
| Vue Router | 5.0.6 | Sahifalar orasida navigatsiya |
| Pinia | 4.0.3 | Global holat (state) boshqaruvi |
| axios | 1.20.0 | HTTP so'rovlar |
| chart.js + vue-chartjs | 4.5.1 / 5.3.4 | Grafiklar (chiziqli, ustunli) |
| @fontsource/inter | 5.3.0 | Inter shrifti (o'zi joylashtirilgan, tashqi CDN'ga bog'liq emas) |
| @quasar/extras | 2.1.0 | Material Icons shrift-ikonkalari |

**Loyihada YO'Q:** TypeScript, vue-i18n (bitta til — o'zbekcha — qattiq yozilgan), PWA plagini, Vuex (Pinia ishlatiladi).

### Baza

| Nomi | Vazifasi |
|---|---|
| PostgreSQL | Asosiy (va yagona) ma'lumotlar bazasi (`application.properties`dagi `spring.datasource.url`dan aniqlandi) |

### Dev vositalari

| Nomi | Versiyasi | Vazifasi |
|---|---|---|
| oxfmt | ^0.x | Frontend kod formatlash (`npm run lint`) |
| oxlint | ^1.x | Frontend statik tahlil (linter) |
| Playwright | (loyiha `package.json`ida yo'q — tashqi/global vosita) | Ishlab chiqish jarayonida UI'ni qo'lda tekshirish uchun ishlatiladi, loyihaning rasmiy test freymvorki emas |

**Muhim eslatma:** frontend'da ESLint/Prettier o'rniga **oxlint/oxfmt** ishlatiladi — bu ancha yangi, Rust'da yozilgan, tezroq muqobil vositalar. Frontend'da rasmiy avtomatlashtirilgan test fayllari (`.spec.js`/`.test.js`) mavjud emas — sifat nazorati lint + `quasar build` (Vite orqali kompilyatsiya xatolarini ushlaydi) + qo'lda/Playwright orqali tekshirish bilan ta'minlanadi.

---

## Backend texnologiyalari batafsil

### Java 21

**Bu nima?** Java — 1995-yildan buyon ishlatilib kelinayotgan, "bir marta yoz, hamma joyda ishlat" tamoyilidagi dasturlash tili. 21-versiya — 2023-yil sentyabrida chiqqan, uzoq muddat qo'llab-quvvatlanadigan (LTS — **L**ong **T**erm **S**upport) versiya.

**Nega aynan shu tanlangan?** Spring Boot 4.x'ning minimal talabi Java 17+, lekin Java 21 LTS bo'lgani va yangi til imkoniyatlarini (masalan, `record`, pattern matching) berganligi uchun tanlangan — muqobili Java 17 (oldingi LTS) bo'lardi, lekin yangi loyiha uchun eng so'nggi LTS tanlash odatiy amaliyot.

**Loyihada qayerda?** `build.gradle`:
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

### Spring Boot 4.1.0

**Bu nima?** Spring Boot — Java'da veb-ilova yozishni osonlashtiruvchi "konfiguratsiyadan ko'ra konventsiya" (convention over configuration) freymvorki. Oddiy tilda: agar siz "standart" qoidalarga rioya qilsangiz (masalan, klassni `@RestController` deb belgilasangiz), Spring Boot avtomatik hamma narsani (server, routing, dependency injection) sozlab qo'yadi — sizga faqat biznes mantiqni yozish qoladi.

**Nega aynan shu tanlangan?** Java ekotizimida veb-API yozish uchun eng keng tarqalgan, eng ko'p hujjatlashtirilgan freymvork. Muqobillari: Quarkus, Micronaut (ikkalasi ham tezroq ishga tushadi, lekin Spring'ga qaraganda kichikroq community va kamroq o'quv materiali bor — o'rganayotgan dasturchi uchun Spring Boot afzalroq).

**Loyihada qayerda?** Har bir `@RestController`, `@Service`, `@Repository`, `@Entity` — bularning barchasi Spring Boot'ning "component scanning" (avtomatik topib, boshqarish) mexanizmi orqali ishlaydi. Boshlanish nuqtasi:
```java
// MaktabBoshqaruvApplication.java
@SpringBootApplication
@EnableJpaAuditing
public class MaktabBoshqaruvApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaktabBoshqaruvApplication.class, args);
    }
}
```

### Spring Web (REST)

**Bu nima?** Spring'ning HTTP so'rovlarni qabul qilish/javob qaytarish qismi. `@RestController`, `@GetMapping`, `@RequestParam` kabi annotatsiyalar shu moduldan.

**Nega aynan shu?** Spring Boot ekotizimining tabiiy qismi — alohida tanlov emas, Spring Boot'ni tanlash bilan birga keladi.

**Loyihada qayerda?** Har bir `controller/*.java` fayli. Masalan:
```java
// BuildingController.java
@GetMapping("/{id}")
public ResponseEntity<BuildingResponseDto> getBuildingById(@PathVariable Long id) {
    return ResponseEntity.ok(buildingService.getBuildingById(id));
}
```

### Spring Data JPA + Hibernate

**Bu nima?** JPA — Java obyektlari va baza jadvallarini bog'lash uchun standart interfeys (spetsifikatsiya). Hibernate — shu standartning eng mashhur implementatsiyasi (haqiqiy SQL generatsiya qiluvchi kutubxona). Spring Data JPA esa Hibernate ustiga qo'shimcha qatlam: repository interfeyslarini yozsangiz bo'ldi, metod tanasini (SQL so'rovini) o'zi generatsiya qiladi.

**Hayotiy o'xshatish:** JPA — bu "tarjimon". Siz Java tilida gapirasiz ("bu Student obyektini saqla"), Hibernate buni SQL tiliga tarjima qilib bazaga yetkazadi.

**Nega aynan shu?** Spring ekotizimida bazaviy standart, katta community. Muqobili: MyBatis (SQL'ni qo'lda yozishga ko'proq erkinlik beradi, lekin ko'proq boilerplate) yoki jOOQ. SQL'ni yaxshi biladigan dasturchi uchun muhim farq: JPA/Hibernate'da siz ko'pincha SQL yozmaysiz, buning o'rniga Java metod nomlari yoki JPQL (Java Persistence Query Language — SQL'ga o'xshash, lekin jadval emas, Entity klass nomlari bilan yoziladigan so'rov tili) yozasiz.

**Loyihada qayerda?** Har bir `repository/*.java` interfeysi:
```java
// AttendanceRepository.java
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByLessonSlotIdAndRecordDate(Long lessonSlotId, LocalDate recordDate);
}
```
Bu bitta qator kod ortida to'liq SQL turadi — batafsil: [05-malumotlar-bazasi.md](05-malumotlar-bazasi.md).

### Spring Security

**Bu nima?** Kim tizimga kira olishi (autentifikatsiya) va kirgandan keyin nima qila olishi (avtorizatsiya) ni boshqaruvchi modul.

**Nega aynan shu?** Spring ekotizimida standart tanlov, JWT bilan ham, sessiya bilan ham ishlay oladi. Bu loyihada **stateless** (holatsiz — server hech qanday sessiya saqlamaydi, har bir so'rov o'zi bilan token olib keladi) rejimda, JWT bilan ishlatilgan.

**Loyihada qayerda?** `config/SecurityConfig.java`, `config/JwtAuthFilter.java`. Batafsil: [06-backend.md — Xavfsizlik](06-backend.md#xavfsizlik).

### JWT (jjwt kutubxonasi)

**Bu nima?** JWT (**J**SON **W**eb **T**oken) — foydalanuvchi haqidagi ma'lumotni (kim, qaysi rol) o'z ichiga olgan, raqamli imzo bilan "muhrlangan" matn bo'lagi. Server bu tokenni har safar qayta tekshiradi, lekin uni **saqlamaydi** — shuning uchun "stateless".

**Hayotiy o'xshatish:** Bu — muhrlangan konsert chiptasiga o'xshaydi. Chipta o'zida "kim", "qaysi joy" degan ma'lumotni tashiydi, va nazoratchi faqat muhrni (imzoni) tekshiradi — qaysidir ro'yxatga qarab tekshirmaydi.

**Nega aynan shu?** Muqobili — server-side sessiya (session-based auth), lekin bu holda server har bir foydalanuvchi uchun holatni saqlashi kerak bo'ladi (ko'proq xotira, gorizontal masshtablashni qiyinlashtiradi). JWT — zamonaviy SPA + REST API arxitekturasi uchun standart yechim.

**Loyihada qayerda?** `util/JwtUtil.java` — token yaratadi va tekshiradi; `config/JwtAuthFilter.java` — har bir so'rovda `Authorization` headerini o'qiydi. Batafsil: [06-backend.md](06-backend.md#jwt-yaratish-va-tekshirish).

### Bean Validation (`@Valid`, `@NotBlank`, `@Min`/`@Max`)

**Bu nima?** Java klassining maydonlariga (masalan DTO'ga) "qoida" yozib qo'yish imkonini beruvchi standart (masalan `@NotBlank` — bo'sh bo'lmasin, `@Min(2)` — 2 dan kichik bo'lmasin). Controller'da `@Valid` deb belgilansa, Spring bu qoidalarni so'rov kelishi bilan avtomatik tekshiradi.

**Nega aynan shu?** Har bir Service metodida qo'lda `if (name == null) throw ...` yozishdan ko'ra, deklarativ (qoidani to'g'ridan-to'g'ri maydon ustida yozish) usul — kamroq kod, kamroq xato.

**Loyihada qayerda?** Masalan, `GradeRequestDto.java`:
```java
@Min(value = 2, message = "Baho 2 dan kam bo'lmasligi kerak")
@Max(value = 5, message = "Baho 5 dan katta bo'lmasligi kerak")
private Integer score;
```

### Lombok

**Bu nima?** Getter/setter/konstruktor kabi "takrorlanuvchi" kodni annotatsiya orqali avtomatik generatsiya qiluvchi kutubxona (kompilyatsiya vaqtida).

**Nega aynan shu?** `build.gradle`da bor, lekin loyiha kodida Entity/DTO klasslarining aksariyati getter/setter'larni **qo'lda** yozgan (Lombok'siz) — shuning uchun bu "mavjud, lekin minimal ishlatilgan" kutubxona sifatida hisoblanadi. Kelajakda yangi DTO/Entity yozganda `@Getter`/`@Setter`/`@Data` bilan kodni qisqartirish mumkin.

### Gradle

**Bu nima?** Loyihani build qilish (kompilyatsiya, testlarni ishga tushirish, jar yig'ish) va kutubxona bog'liqliklarini (dependency) boshqarish vositasi.

**Nega aynan shu?** Muqobili — Maven (XML asosida, ko'proq "boilerplate"). Gradle Groovy/Kotlin DSL asosida, qisqaroq va moslashuvchan konfiguratsiya beradi. Loyiha Gradle Wrapper (`gradlew`/`gradlew.bat`) bilan birga keladi — Gradle'ni alohida o'rnatish shart emas.

---

## Frontend texnologiyalari batafsil

### Vue 3

**Bu nima?** Vue — foydalanuvchi interfeysini "komponent"lar (mustaqil, qayta ishlatiladigan UI bo'laklari) orqali quruvchi JavaScript freymvorki. Vue 3'da `<script setup>` sintaksisi ishlatiladi — bu Composition API'ning eng qisqa yozilishi.

**Hayotiy o'xshatish:** Har bir Vue komponenti — LEGO bo'lagiga o'xshaydi: o'zining ko'rinishi (`<template>`), mantig'i (`<script>`) va stili (`<style>`) bor, va ularni birlashtirib katta sahifa yasaysiz.

**Nega aynan shu?** Muqobillari React va Angular. Vue — o'rganish egri chizig'i (learning curve) pastroq, HTML'ga yaqinroq sintaksis (React'ning JSX'iga qaraganda), va Quasar aynan Vue ustida qurilgan bo'lgani uchun tabiiy tanlov.

**Loyihada qayerda?** Har bir `.vue` fayl. Masalan, `frontend/src/pages/DashboardPage.vue`da `<script setup>`:
```vue
<script setup>
import { ref, computed, onMounted } from 'vue'
const loading = ref(true)          // reaktiv o'zgaruvchi
const summary = computed(() => ...) // hisoblanadigan qiymat
onMounted(loadAll)                  // komponent ekranga chiqqanda ishga tushadi
</script>
```
Vue asoslari (component, props, emit, ref/computed, lifecycle) — [08-frontend.md](08-frontend.md)da loyihadagi haqiqiy misollar bilan tushuntirilgan.

### Quasar 2

**Bu nima?** Vue ustiga qurilgan, tayyor UI komponentlar to'plami (tugma, jadval, forma, dialog va h.k. — barchasi Material Design uslubida) + build tizimi (`@quasar/app-vite`).

**Hayotiy o'xshatish:** Agar Vue — g'isht bo'lsa, Quasar — tayyor devor bloklari to'plami: tugma, jadval, dialog kabi elementlarni noldan yasash shart emas.

**Nega aynan shu?** Muqobillari — Vuetify, PrimeVue. Quasar tanlangan sabab: bitta kod bazasidan SPA, PWA, mobil va desktop ilova yasash imkonini beradi (hozircha loyihada faqat SPA rejimi ishlatiladi), va o'zining Vite asosidagi build vositasi tez.

**Loyihada qayerda?** `quasar.config.js` — asosiy sozlama (routing rejimi `hash`, plaginlar `Notify`/`Dialog`/`LoadingBar`, ikonka to'plami `material-icons`). Har bir sahifada `q-card`, `q-table`, `q-btn`, `q-dialog` kabi komponentlar.

### Vue Router 5

**Bu nima?** URL manzil (`/attendance`, `/app/rooms`) bilan qaysi Vue komponent ko'rsatilishini bog'lovchi kutubxona.

**Nega aynan shu?** Vue ekotizimining rasmiy routerchisi — alternativasi yo'q darajada standart tanlov.

**Loyihada qayerda?** `frontend/src/router/routes.js` — barcha yo'llar ro'yxati; `frontend/src/router/index.js` — himoyalangan sahifalar uchun **global guard** (`Router.beforeEach`):
```js
Router.beforeEach(to => {
  const authStore = useAuthStore()
  if (!authStore.isAuthenticated && to.path !== '/login') {
    return '/login'   // token yo'q — login sahifasiga qaytaradi
  }
  ...
})
```
Batafsil: [08-frontend.md — Router va himoyalangan sahifalar](08-frontend.md#router-va-himoyalangan-sahifalar).

### Pinia

**Bu nima?** Vue ilovasi uchun global holat (state) ombori — turli komponentlar orasida umumiy ma'lumotni (masalan, "kim tizimga kirgan", "qaysi maktab tanlangan") saqlash uchun.

**Hayotiy o'xshatish:** Agar har bir komponent — alohida xona bo'lsa, Pinia store — barcha xonalar kira oladigan umumiy shkaf.

**Nega aynan shu?** Vue 3'ning rasmiy tavsiya qilingan store kutubxonasi (eski Vuex'ning o'rnini bosgan) — qisqaroq sintaksis, TypeScript bilan yaxshiroq ishlaydi (bu loyihada TS ishlatilmasa ham).

**Loyihada qayerda?** `frontend/src/stores/auth.js` (token, username, role, employeeId) va `frontend/src/stores/school.js` (tanlangan maktab). Batafsil: [08-frontend.md — Store](08-frontend.md#store-pinia).

### axios

**Bu nima?** Brauzerdan HTTP so'rov yuborish uchun kutubxona (brauzerning o'ziga xos `fetch()` funksiyasiga muqobil, lekin qulayroq API bilan).

**Nega aynan shu?** `fetch()`ga nisbatan afzalligi: so'rov/javobni avtomatik JSON qilib beradi, va **interceptor** (har bir so'rov/javobni "ushlab qolib" o'zgartirish imkoni) mexanizmi tayyor holda bor — bu loyihada token qo'shish va 401 xatoni ushlash uchun aynan shu ishlatilgan.

**Loyihada qayerda?** `frontend/src/boot/axios.js` — batafsil: [08-frontend.md — axios va interceptor'lar](08-frontend.md#axios-sozlamalari-va-interceptorlar).

### Chart.js + vue-chartjs

**Bu nima?** Chart.js — HTML `<canvas>` ustida grafik chizuvchi JavaScript kutubxonasi; vue-chartjs — uni Vue komponenti sifatida ishlatish uchun "o'rovchi" (wrapper).

**Nega aynan shu?** Yengil, keng tarqalgan, ko'p turdagi grafiklarni (chiziqli, ustunli) qo'llab-quvvatlaydi. Dashboard'dagi "Oxirgi 30 kunlik davomat" (chiziqli) va "Fanlar bo'yicha o'rtacha baho" (gorizontal ustunli) grafiklari shu orqali chizilgan.

**Loyihada qayerda?** `frontend/src/pages/DashboardPage.vue`:
```js
import { Bar, Line } from 'vue-chartjs'
import { Chart as ChartJS, ... } from 'chart.js'
```

---

## Baza

### PostgreSQL

**Bu nima?** Ochiq manbali (open-source), relyatsion (jadval-ustun-qator asosidagi) ma'lumotlar bazasi boshqaruv tizimi.

**Nega aynan shu?** Muqobillari MySQL, MariaDB. PostgreSQL tanlangan sabab (kod/config'dan ko'rinmaydi, lekin umumiy amaliyot): kuchli JOIN va agregatsiya imkoniyatlari, standart SQL'ga yaqinroq rioya qilishi — bu loyihada ko'p qatlamli JOIN so'rovlar (masalan `attendance` → `lesson_slot` → `school_class` → `academic_year` → `school`) ko'p ishlatilgani uchun muhim.

**Loyihada qayerda?** `application.properties`:
```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/maktab_db}
```

---
Keyingisi: [04 — Papkalar tuzilishi →](04-papkalar-tuzilishi.md)
