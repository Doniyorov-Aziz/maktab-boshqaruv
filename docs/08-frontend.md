# 08 — Frontend

[← 07 — API](07-api.md) · Keyingisi: [09 — Asosiy jarayonlar →](09-asosiy-jarayonlar.md)

## Mundarija
- [Vue asoslari — loyihadagi misollar bilan](#vue-asoslari-loyihadagi-misollar-bilan)
- [Layout: MainLayout va PageLayout](#layout-mainlayout-va-pagelayout)
- [Router va himoyalangan sahifalar](#router-va-himoyalangan-sahifalar)
- [Store (Pinia)](#store-pinia)
- [axios sozlamalari va interceptor'lar](#axios-sozlamalari-va-interceptorlar)
- [CrudPage: universal komponent](#crudpage-universal-komponent)
- [Har bir sahifa](#har-bir-sahifa)
- [Quasar konfiguratsiyasi](#quasar-konfiguratsiyasi)
- [Tema tizimi (dark/light, dizayn tokenlari)](#tema-tizimi-darklight-dizayn-tokenlari)
- [Ikonkalar va shriftlar](#ikonkalar-va-shriftlar)

## Vue asoslari — loyihadagi misollar bilan

Barcha sahifalar Vue 3'ning **Composition API** uslubida, `<script setup>` sintaksisi bilan yozilgan (bu — eng qisqa, eng zamonaviy Vue yozish usuli).

**Component (komponent)** — mustaqil, qayta ishlatiladigan UI bo'lagi. Har bir `.vue` fayl — bitta komponent, uchta qismdan iborat: `<template>` (HTML ko'rinish), `<script setup>` (mantiq), `<style>` (CSS).

**`ref` va `computed`** — reaktivlik asosi:
```js
// DashboardPage.vue
const loading = ref(true)                    // ref: o'zgaruvchan qiymat, .value orqali o'qiladi/yoziladi
const attendanceAverage = computed(() => {    // computed: boshqa reaktiv qiymatlardan avtomatik hisoblanadi
  const vals = attendanceTrend.value.map(p => p.rate).filter(v => v != null)
  return vals.length ? Math.round((vals.reduce((a, b) => a + b, 0) / vals.length) * 10) / 10 : null
})
```
`attendanceTrend.value` o'zgarsa, `attendanceAverage` **avtomatik** qayta hisoblanadi — ekranni qo'lda yangilash shart emas.

**`props` va `emit`** — komponentlar orasidagi aloqa. Masalan `components/DateField.vue` ota-komponentdan qiymat oladi (`props`) va o'zgarish haqida xabar beradi (`emit`):
```vue
<script setup>
defineProps({ modelValue: String, label: String })
defineEmits(['update:modelValue'])
</script>
```
Bu — Vue'ning `v-model` naqshi: ota-komponent `<date-field v-model="formModel.birthDate" />` deb yozganda, ichkarida avtomatik `:modelValue="formModel.birthDate"` + `@update:modelValue="v => formModel.birthDate = v"`ga aylanadi.

**Lifecycle (hayot sikli) — `onMounted`**: komponent ekranga birinchi marta chiqqanda bir marta ishga tushadigan funksiya:
```js
// DashboardPage.vue
onMounted(loadAll)                              // sahifa ochilganda ma'lumot yuklanadi
watch(() => schoolStore.activeSchoolId, loadAll) // maktab almashtirilsa, qayta yuklanadi
```

## Layout: MainLayout va PageLayout

- **`layouts/MainLayout.vue`** (816 qator) — har bir ichki sahifani o'rab turadigan "qobiq": yuqori panel (qidiruv, bildirishnomalar, maktab tanlash, dark-mode tugmasi, profil menyusi) + chap sidebar (`q-drawer`, modullar ro'yxati bilan). Sidebar ikki qismdan iborat:
  - **Statik** "Kundalik ish" bo'limi — to'rtta qattiq yozilgan havola (`dailyNavItems`): Davomat olish, Baholar jurnali, Dars jadvali, Taqvim.
  - **Dinamik** qolgan bo'limlar — `config/modules.js`dagi `modules` massividan, `group` maydoni bo'yicha guruhlangan (`groupedModules` computed), va faqat foydalanuvchi ko'ra oladigan modullar chiqariladi:
    ```js
    const visibleModules = computed(() =>
      modules.filter(m => !m.hidden && (!m.adminOnly || authStore.isAdmin))
    )
    ```
    Shu sabab yangi modul qo'shilganda (`modules.js`ga yangi obyekt qo'shib), sidebar'ga **qo'lda** hech narsa qo'shish shart emas.
- **`components/PageLayout.vue`** (65 qator) — har bir sahifa ichida ishlatiladigan kichik "qolip": sarlavha qatori (`header` slot) + kontent qismi. Barcha sahifalar shu orqali bir xil sarlavha/bo'shliq uslubiga ega bo'ladi.

## Router va himoyalangan sahifalar

`router/routes.js` — barcha marshrutlar ro'yxati (to'liq ro'yxat: [04-papkalar-tuzilishi.md](04-papkalar-tuzilishi.md#pages-har-bir-sahifa)). `CrudPage.vue`ning `path: 'app/:moduleKey'` — bitta dinamik marshrut orqali barcha 14 modulga xizmat qiladi (`:moduleKey` — masalan `rooms`, `students`).

**Himoyalangan sahifalar** — loyihada har bir marshrutga alohida `meta: { requiresAuth: true }` yozish o'rniga, **bitta global guard** ishlatiladi (`router/index.js`):

```js
Router.beforeEach(to => {
  const authStore = useAuthStore()
  const isLoginRoute = to.path === '/login'

  if (!authStore.isAuthenticated && !isLoginRoute) {
    return '/login'          // token yo'q — login sahifasiga
  }
  if (authStore.isAuthenticated && isLoginRoute) {
    return '/'                // token bor-u, login sahifasiga kirmoqchi — bosh sahifaga qaytaradi
  }
  LoadingBar.start()
  return true
})
```

Bu — "hamma sahifa standart holatda himoyalangan, faqat `/login` ochiq" mantig'i (aksincha, "hamma ochiq, faqat ba'zilari yopiq" emas).

## Store (Pinia)

### `stores/auth.js`

```js
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || null,
    username: localStorage.getItem('username') || null,
    role: localStorage.getItem('role') || null,
    employeeId: localStorage.getItem('employeeId') ? Number(...) : null
  }),
  getters: {
    isAuthenticated: state => !!state.token,
    isAdmin: state => state.role === 'ADMIN',
    isEditor: state => state.role === 'EDITOR' || state.role === 'ADMIN'
  },
  actions: {
    setToken(token) { /* JWT'ni dekodlab, token/username/role/employeeId'ni localStorage'ga ham yozadi */ },
    logout() { /* hammasini tozalaydi */ }
  }
})
```

Sahifa qayta yuklanganda (F5) ham foydalanuvchi tizimdan chiqib qolmasligi uchun, boshlang'ich holat to'g'ridan-to'g'ri `localStorage`dan o'qiladi.

### `stores/school.js`

Joriy tanlangan maktabni (`activeSchoolId`, `activeSchoolName`) saqlaydi, shuningdek `localStorage`da. `fetchSchools(api)` — barcha maktablarni yuklaydi va agar avval tanlangan maktab endi mavjud bo'lmasa (o'chirilgan bo'lsa), avtomatik birinchisini tanlaydi.

## axios sozlamalari va interceptor'lar

`boot/axios.js` — Quasar "boot fayli" (ilova ishga tushishidan oldin, bir marta bajariladi):

```js
const api = axios.create({
  baseURL: import.meta.env.QCLI_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    error.friendlyMessage = friendlyMessage(error)   // texnik xatoni o'zbekcha, tushunarli matnga aylantiradi
    if (error.response?.status === 401) {
      localStorage.removeItem('token'); localStorage.removeItem('username'); localStorage.removeItem('role')
      if (router.currentRoute.value.path !== '/login') router.push('/login')
    }
    return Promise.reject(error)
  }
)
```

- **Request interceptor** — har bir chiqayotgan so'rovga avtomatik `Authorization` header qo'shadi, shu sabab har bir sahifada bu qatorni qo'lda yozish shart emas.
- **Response interceptor** — ikkita vazifa: (1) har qanday xatoni `error.friendlyMessage`ga o'zbekcha, tushunarli matnga aylantiradi (masalan tarmoq uzilishi, timeout, 401, 5xx uchun turlicha xabar), (2) `401 Unauthorized` kelsa (token muddati tugagan yoki noto'g'ri) — foydalanuvchini avtomatik chiqarib, login sahifasiga qaytaradi.
- **`timeout: 15000`** — 15 soniyadan uzoq javob kelmasa, so'rov avtomatik bekor qilinadi (`ECONNABORTED`), foydalanuvchi cheksiz kutmaydi.

## CrudPage: universal komponent

Loyihaning eng muhim frontend qarori: 14 ta modulning (Maktablar, Binolar, Xonalar, O'quv yillari, Sinflar, O'quvchilar, Fanlar, Lavozimlar, Xodimlar, E'lonlar, Tadbirlar, Xulq yozuvlari, Foydalanuvchilar, va menyuda yashirin Dars jadvali-ro'yxat) barchasi **bitta** `pages/CrudPage.vue` komponenti orqali ko'rsatiladi — har biriga alohida sahifa yozish o'rniga.

**Qanday ishlaydi?** Marshrut `/app/:moduleKey` — `moduleKey` (masalan `rooms`) orqali `config/modules.js`dan mos konfiguratsiya topiladi:

```js
// config/getModule
export function getModule(key) {
  return modules.find(m => m.key === key)
}
```

Har bir modul konfiguratsiyasi shu maydonlarni belgilaydi:

| Maydon | Vazifasi |
|---|---|
| `key`, `title`, `icon`, `color`, `group` | Sidebar va sahifa sarlavhasi uchun |
| `endpoint` | Backend URL (masalan `/api/rooms`) |
| `schoolScoped` | `true` bo'lsa, har bir so'rovga `schoolId` avtomatik qo'shiladi |
| `viewType` | `'cards'` (kartochka-jadval, masalan Xonalar/Maktablar) yoki standart jadval ko'rinishi |
| `columns` | Jadval/kartochkada ko'rsatiladigan ustunlar (`badgeColors`/`badgeLabels` bilan rangli belgi, `link` bilan profilga havola, `format` bilan sana formatlash) |
| `fields` | Yaratish/tahrirlash formasidagi maydonlar (`type`: `text`/`number`/`date`/`time`/`select`/`textarea`/`password`; `select` uchun `optionsEndpoint` — variantlar boshqa API'dan yuklanadi) |
| `filters` | Ro'yxat ustidagi filtr tugmalari (masalan Xonalar uchun Bino va Turi) |
| `hidden` | `true` bo'lsa, sidebar'da ko'rinmaydi (Dars jadvali-ro'yxat) |
| `adminOnly` | `true` bo'lsa, faqat `ADMIN` ko'radi (Foydalanuvchilar) |
| `rowLink` | Qatorni bosganda qaysi profil sahifasiga o'tish (masalan Sinflar → `class`) |

`CrudPage.vue` (1089 qator) shu konfiguratsiyaga qarab: ro'yxatni yuklaydi (server-side sahifalash + filtrlash), qidiruv (matn bo'yicha, alohida to'liq-yuklab-filtrlash mexanizmi bilan), yaratish/tahrirlash formasini (`q-dialog` yon panel) dinamik quradi, o'chirishni tasdiqlaydi.

**Nega bu foydali?** Yangi modul qo'shish uchun yangi `.vue` fayl yozish shart emas — faqat `modules.js`ga konfiguratsiya qo'shiladi. To'liq amaliy misol: [12-yangi-modul-qoshish.md](12-yangi-modul-qoshish.md).

## Har bir sahifa

| Sahifa | Nimani ko'rsatadi | Qaysi API'larni chaqiradi |
|---|---|---|
| `LoginPage.vue` | Login shakli; ochilishda backend tirikligini tekshiradi | `GET /api/health`, `POST /api/auth/login` |
| `DashboardPage.vue` | KPI kartalar, jonli darslar, davomat grafigi, sinflar reytingi (`ClassRankingCard`), e'tibor ro'yxati, fanlar o'rtachasi, tadbirlar, e'lonlar, faoliyat lentasi | `GET /api/dashboard/overview`, `/api/dashboard/absentees-today`, `GET /api/calendar-events/upcoming`, `GET /api/announcements/latest` |
| `CrudPage.vue` | 14 modulning har biri uchun jadval/kartochka ro'yxati + yaratish/tahrirlash formasi | modul konfiguratsiyasidagi `endpoint` (dinamik) |
| `AttendancePage.vue` | Bugungi darslar kartalari (o'qituvchi uchun "faqat mening darslarim" filtri bilan), sinf+dars+sana tanlab davomat belgilash | `GET /api/attendance/today-lessons`, `GET /api/school-classes`, `GET /api/lesson-slots/timetable`, `GET /api/attendance/roster`, `POST /api/attendance/bulk` |
| `GradebookPage.vue` | Sinf+fan+davr tanlab, o'quvchilar × sanalar jadvalida baho kiritish | `GET /api/school-classes`, `GET /api/subjects`, `GET /api/grades/gradebook`, `POST /api/grades` |
| `TimetablePage.vue` | Haftalik dars jadvali (sinf/o'qituvchi/xona bo'yicha), jonli vaqt chizig'i, PDF chop etish (joriy yoki barcha sinflar) | `GET /api/school-classes`, `GET /api/employees`, `GET /api/rooms`, `GET /api/lesson-slots/timetable` |
| `CalendarPage.vue` | Oylik/haftalik taqvim, tadbir turi bo'yicha rangli belgilar | `GET /api/calendar-events/range`, `POST /api/calendar-events` |
| `StudentProfilePage.vue` | Bitta o'quvchining to'liq profili | `GET /api/profiles/students/{id}` |
| `TeacherProfilePage.vue` | Bitta o'qituvchining profili | `GET /api/profiles/teachers/{id}` |
| `ClassProfilePage.vue` | Bitta sinfning profili (o'quvchilar + jadval) | `GET /api/profiles/classes/{id}` |
| `ErrorNotFound.vue` | 404 sahifa | — |

## Quasar konfiguratsiyasi

`quasar.config.js` — asosiy sozlamalar:

```js
boot: ['lang', 'pinia', 'axios'],        // ishga tushishda tartib bilan bajariladigan fayllar
extras: ['roboto-font', 'material-icons'],
build: { vueRouterMode: 'hash' },        // URL'da # ishlatiladi (masalan /#/attendance)
devServer: { open: true },
framework: {
  plugins: ['Notify', 'Dialog', 'LoadingBar']
}
```

**`vueRouterMode: 'hash'`** — nega `history` emas: hash rejimida (`/#/attendance`) server hech qanday qo'shimcha sozlamasiz statik fayllarni bera oladi (har qanday URL `index.html`ga tushadi, brauzer qolganini o'zi hal qiladi); `history` rejimi esa server tomonida "har qanday noma'lum yo'lni `index.html`ga yo'naltir" degan qo'shimcha sozlamani talab qiladi. Kichik/oddiy joylashtirish uchun `hash` qulayroq.

## Tema tizimi (dark/light, dizayn tokenlari)

Rang palitrasi ikki qatlamda aniqlanadi:

1. **`css/quasar.variables.scss`** — Quasar'ning o'z ranglari (`$primary`, `$dark`, `$positive` va h.k.) + loyihaga xos qo'shimcha o'zgaruvchilar (`$brand-page-bg`, `$brand-card-bg`, ...), **har biri light va dark versiyasi bilan** (masalan `$brand-card-bg: #ffffff` / `$brand-card-bg-dark: #1e1f2b`).
2. **`css/app.scss`** — shu SCSS o'zgaruvchilarni CSS custom property'larga (`--card-bg`, `--brand-border`, ...) aylantiradi, `:root`da light qiymatlar bilan, `body.body--dark`da dark qiymatlar bilan:
   ```scss
   :root {
     --card-bg: #{$brand-card-bg};
     --brand-border: #{$brand-border};
   }
   body.body--dark {
     --card-bg: #{$brand-card-bg-dark};
     --brand-border: #{$brand-border-dark};
   }
   ```

Har bir komponent CSS'da to'g'ridan-to'g'ri rang (`#fff`, `color: black`) yozish o'rniga, doim shu `var(--card-bg)` kabi token orqali yozadi — shu sabab dark mode almashtirilganda hech qanday qo'shimcha JS kodsiz, faqat `body`ga `body--dark` klassi qo'shilishi bilan **butun ilova** rangini almashtiradi.

**Dark mode qanday yoqiladi?** OS sozlamasi (`prefers-color-scheme`) emas — foydalanuvchining o'z tanlovi, `localStorage.darkMode`da saqlanadi va `App.vue`da ilova ishga tushganda o'qiladi:
```js
// App.vue
onMounted(() => {
  const saved = localStorage.getItem('darkMode')
  if (saved !== null) $q.dark.set(saved === 'true')
})
```

## Ikonkalar va shriftlar

- **Ikonkalar**: Material Icons (`@quasar/extras`, `quasar.config.js`da `extras: ['material-icons']`). Har bir `q-icon`da `name="fact_check"` kabi Material Icons nomi ishlatiladi — agar shrift yuklanmasa, shu matn (`fact_check`) oddiy so'z sifatida ko'rinib qoladi (bu holat: [11-muammolar-va-yechimlar.md](11-muammolar-va-yechimlar.md)).
- **Shrift**: Inter (`@fontsource/inter`) — loyihaning o'zida joylashtirilgan (`node_modules` orqali build vaqtida qo'shiladi), tashqi Google Fonts CDN'ga bog'liq emas, shu sabab internet aloqasi bo'lmasa ham to'g'ri ko'rinadi.
- **Til**: bitta til — o'zbekcha. `lang/uz.js` — faqat Quasar'ning o'z ichki komponentlari (masalan sana tanlagich, sahifalash) uchun tarjima; ilovaning o'z matnlari (tugma nomlari, sarlavhalar) har bir `.vue` faylda to'g'ridan-to'g'ri o'zbekcha yozilgan — alohida i18n (`vue-i18n`) kutubxonasi ishlatilmagan.

---
Keyingisi: [09 — Asosiy jarayonlar →](09-asosiy-jarayonlar.md)
