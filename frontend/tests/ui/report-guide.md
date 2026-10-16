## 5. Tokenlar — yagona manba

Hamma raqamlar `src/css/tokens.scss` da. Boshqa joyda `px` bilan radius / balandlik / max-width yozilmaydi (`tests/ui/hardcoded.js` tekshiradi; media query breakpoint'lari bundan mustasno — ularda CSS o'zgaruvchisi ishlamaydi).

| Token                                            | Qiymat                 | Qayerda                                                                 |
| ------------------------------------------------ | ---------------------- | ----------------------------------------------------------------------- |
| `--radius-sm`                                    | 8px                    | chip, badge, kichik tugma, ikonka-kvadrat 32/40                         |
| `--radius-md`                                    | 10px                   | input, select, sana, vaqt, tugma — hammasi bir xil; 48px ikonka-kvadrat |
| `--radius-lg`                                    | 16px                   | kartochka, panel, dialog                                                |
| `--radius-pill`                                  | 999px                  | faqat doira: avatar, nuqta, progress-bar                                |
| `--control-h`                                    | 40px                   | har bir input / select / sana / vaqt / tugma                            |
| `--control-h-sm`                                 | 32px                   | jadval ichidagi ixcham tugma, chip                                      |
| `--space-1..8`                                   | 4/8/12/16/24/32/40/48  | bo'shliqlar                                                             |
| `--border`, `--border-control`, `--border-focus` | 1px / 1px / 2px        | kartochka chegarasi, maydon chegarasi, fokus                            |
| `--page-pad`                                     | 12 / 16 / 24 / 32 / 40 | sahifa yon padding'i (<600 / 600–1023 / 1024–1439 / 1440–1919 / ≥1920)  |
| `--page-gap`                                     | 32px (`--space-6`)     | header ↔ sahifa sarlavhasi ↔ kontent ↔ pastki chet                      |
| `--chart-h`                                      | 240 / 300 / 360        | grafik balandligi (<600 / 600–1439 / ≥1440)                             |
| `--tile-sm/md/lg`                                | 32 / 40 / 48           | ikonka-kvadratlar                                                       |
| `--grid-min` / `--grid-min-sm`                   | 320 / 220              | kartochka grid'idagi eng tor ustun                                      |
| `--form-max`                                     | 960px                  | uzun forma ichki ustuni (Bot sozlamalari)                               |

Quasar'ning o'z SASS o'zgaruvchilari ham shu qiymatlarga o'rnatilgan (`quasar.variables.scss`: `$generic-border-radius`, `$button-border-radius`, `$button-font-weight`, `$chip-height`, `$table-border-radius`), shuning uchun Quasar o'zi chizadigan menyu, jadval, tooltip ham mos.

Rang tokenlari (`--c-border`, `--border` …) `body` da e'lon qilingan: dark mavzu `body.body--dark` ga qo'yiladi va o'zgaruvchilar o'sha yerda hisoblanadi — `:root` da e'lon qilinsa, dark mavzuda light ranglar qolib ketardi.

## 6. Komponentlar (`src/components/app/`)

Sahifalarda `q-input`, `q-select`, `q-btn` to'g'ridan-to'g'ri ishlatilmaydi — lint xato beradi. Hamma Quasar prop'lari, hodisalari va slot'lari o'tadi.

### AppInput

```vue
<AppInput v-model="form.name" label="Nomi" :rules="[v => !!v || 'Kiriting']" />
<AppInput v-model="q" placeholder="Qidirish..." clearable>
  <template #prepend><q-icon name="search" /></template>
</AppInput>
<AppInput v-model="text" type="textarea" autogrow label="Izoh" />
<!-- ko'p qatorli: balandligi tabiiy -->
```

### AppSelect

```vue
<AppSelect
  v-model="classId"
  :options="classes"
  label="Sinf"
  emit-value
  map-options
  clearable
/>
```

### AppDateInput — `dd.MM.yyyy` ko'rinadi, v-model `YYYY-MM-DD`

```vue
<AppDateInput v-model="form.birthDate" label="Tug'ilgan sana" no-future />
<AppDateInput
  v-model="day"
  label="Sana"
  no-sundays
  min="2026-09-01"
  max="2027-05-31"
  clearable
/>
```

Maydonning istalgan joyini bosish kalendarni ochadi, ikonka o'ngda; faqat-o'qish holatida ham chegara oddiy (punktir emas).

### AppTimeInput — 24 soatli `HH:mm`

```vue
<AppTimeInput v-model="form.reportTime" label="Hisobot vaqti" />
```

### AppButton — `variant`: primary / secondary / ghost / danger; `size`: md / sm

```vue
<AppButton label="Saqlash" icon="save" @click="save" />
<AppButton variant="secondary" label="Bekor qilish" v-close-popup />
<AppButton variant="ghost" icon="edit" size="sm" aria-label="Tahrirlash" />
<!-- faqat ikonka: kvadrat -->
<AppButton variant="danger" label="O'chirish" :loading="busy" />
<AppButton variant="ghost" color="positive" icon="check" label="Keldi" />
<!-- boshqa rang, shakl o'sha -->
<AppButton
  variant="ghost"
  square
><q-avatar size="var(--avatar-sm)">A</q-avatar></AppButton>
```

Balandlik 40 (sm: 32), radius 10 (sm: 8), ikonka va matn orasi 8px, bosh harf yo'q, og'irlik 600. Jadval qatoridagi tugmalar — `size="sm"`.

### IconTile — rangli kvadrat ichida ikonka; o'lcham faqat 32 / 40 / 48

```vue
<IconTile icon="school" size="48" />
<!-- brend gradient -->
<IconTile icon="event" size="32" :color="typeColor(ev.type)" />
<IconTile
  icon="groups"
  size="40"
  color="rgba(79, 70, 229, 0.12)"
  text-color="var(--q-primary)"
/>
```

### PageLayout — har sahifaning ramkasi

```vue
<page-layout icon="groups" title="Sinflar" subtitle="1-maktab">
  <template #actions><AppButton icon="add" label="Qo'shish" /></template>
  ...
</page-layout>
<page-layout icon="grid_on" title="Baholar jurnali" fill>   <!-- jadval ekran balandligida, ichida scroll -->
```

`#subtitle` slot'i ham bor (bir necha qatorli izoh uchun). `max-width` yo'q — kontent sidebar'dan qolgan butun kenglikni oladi.

## 7. Layout yordamchilari (`src/css/controls.scss`)

| Sinf                       | Nima qiladi                                                                                                         |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------- |
| `.app-grid`                | `repeat(auto-fill, minmax(320px, 1fr))` — kartochkalar: katta ekranda ko'proq ustun                                 |
| `.app-toolbar`             | filtr qatori: bir xil balandlik, pastki chiziqlar bir tekisda (`align-items: flex-end`)                             |
| `.app-chart`               | grafik konteyneri: kenglik 100%, balandlik `--chart-h` (Chart.js: `responsive: true`, `maintainAspectRatio: false`) |
| `.app-form-column`         | uzun forma ustuni ≤ 960px                                                                                           |
| `.app-fill`                | ekran balandligidagi blok, ichida scroll                                                                            |
| `.app-dialog--sm/md/lg/xl` | dialog kartochkasi kengligi (420 / 560 / 760 / 960, telefonda to'liq)                                               |
| `.app-toggle--sm`          | jadval/kartochka burchagidagi ixcham segment almashtirgich                                                          |

Sahifalar bo'yicha: Dashboard — 12 ustunli grid (ikki mustaqil ustun, kartochkalar o'z balandligida, teshik yo'q), KPI — `auto-fit`; CRUD kartochkalari va kutubxona — `auto-fill` grid; Davomat kalendari va Baholar jurnali — `fill` (≥1024px da ekran balandligida, jadval ichida scroll; telefonda sahifa odatdagidek scroll bo'ladi). Sidebar ≥1024px da doim ochiq (yig'ish mumkin), tor ekranda drawer — avvaldan shunday edi, saqlandi.

## 8. Tanlangan usullar

- **Yorliq maydon ichida** (Quasar'ning suzuvchi yorlig'i) — loyihada eng ko'p ishlatilgan usul; login sahifasidagi tashqi yorliqlar ham shunga o'tkazildi.
- **Balandlik** `--control-h` dan; ichkarida Quasar'ning `dense` rejimi ishlatiladi (u aynan 40px beradi va suzuvchi yorliqni to'g'ri joylaydi), balandlikni esa `.app-field` qoidasi tokenga bog'laydi.
- **Telegram Mini App** (`/webapp`) — Telegram mavzusidagi alohida mobil ilova, Quasar boshqaruv elementlari yo'q; ko'rinishi saqlandi, o'lchamlari tokenlarga o'tkazildi (`--wa-*`).
- `DateField.vue` o'chirildi — o'rniga `AppDateInput`.

## 9. Tekshiruv qanday ishga tushiriladi

```bash
cd frontend
npm run lint            # oxfmt + oxlint + ESLint
npm run build
# UI testlari: backend ishlab turgan bo'lishi va admin login kerak
QCLI_API_BASE_URL=http://localhost:8081 npx quasar build      # (CSP connect-src ham shu portga)
UI_API=http://localhost:8081 UI_USER=... UI_PASS=... npm test   # 1440×900 o'lchamlar + 1920/2560/390 layout
npm run ui:audit        # after/ skrinshotlar + audit-after.json
node tests/ui/report-md.mjs   # shu hisobot
```

Test sahifa va elementni aytib yiqiladi, masalan: `crud-students: AppButton "Excel" height 36 ≠ 40`.

Lint qoidasi isboti (vaqtincha `ErrorNotFound.vue` ga `<q-input>` va `import { QBtn } from 'quasar'` qo'shilgan, keyin olib tashlangan):

```text
src\pages\ErrorNotFound.vue
  11:9   error  Use the design-system component from src/components/app/ (AppInput / AppDateInput)  vue/no-restricted-html-elements
  20:10  error  'QBtn' import from 'quasar' is restricted. Use the design-system component from src/components/app/  no-restricted-imports
✖ 2 problems (2 errors, 0 warnings)
```

## 10. Yo'l-yo'lakay topilgan, tuzatilmagan (backend)

- `GET /api/buildings` — bir xonaga bir vaqtda ikkita dars qo'yilgan bo'lsa, `BuildingService.toResponseDto` (`BuildingService.java:111`) «hozir dars bormi» so'rovida bitta natija kutadi va 500 qaytaradi (`IncorrectResultSizeDataAccessException`). Test bazasida shunday dars bor, shuning uchun Binolar sahifasi ma'lum soatlarda xato ko'rsatadi. Frontend vazifasiga kirmaydi.
