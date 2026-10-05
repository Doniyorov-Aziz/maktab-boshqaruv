# Login sahifasi — «Katakli daftar» qayta dizayni

Natija: **32 / 32** band bajarildi. Fayllar: `frontend/src/pages/LoginPage.vue`, `frontend/src/css/app.scss` (yangi tokenlar), `frontend/src/boot/axios.js` (401 matni «Login yoki parol noto'g'ri»).

## Hozirgi muammolar
- [x] M1 Chap panel bo'sh gradient emas — sarlavha, «shisha» kartochkalar, imkoniyatlar va pastki qator panelni to'ldiradi; ≥1600px'da sarlavha 56px va kompozitsiya kattalashadi
- [x] M2 Shablon ko'rinish (binafsha gradient + doiralar) olib tashlandi — katakli daftar motivi
- [x] M3 «16 Modul · 3 Rol darajasi · 100% Nazorat» olib tashlandi
- [x] M4 O'ng tomonda alohida fon va soyali kartochka; «Username» → «Login» (maydon va xato xabari)

## Chap panel (~55%)
- [x] C1 `#1E1B4B → #312E81` gradient + 24px katakli naqsh (7%) + qizil-pushti hoshiya chizig'i (28%), faqat CSS
- [x] C2 Logotip (oq yumaloq kvadratda bitiruvchi qalpoqcha) + «Maktab Boshqaruv»
- [x] C3 «Maktabingiz — / bir qarashda» (52px, keng ekranda 56px) + 18px lavanda tavsif
- [x] C4 4 ta qiyshaygan «shisha» kartochka: davomat halqasi 94%, hozirgi dars (Matematika · 5-A · 201-xona, progress), yangi baho «5», Telegram ✓✓ — statik namuna
- [x] C5 Har biri 6.5–8 s davomli, turli fazadagi `translateY` «suzish» animatsiyasi
- [x] C6 Davomat · Baholar · Dars jadvali · Telegram xabarnomalar
- [x] C7 «© 2026 Maktab Boshqaruv · v0.0.1» (versiya `package.json`dan import qilinadi)

## O'ng panel (~45%) — forma
- [x] F1 Fon `#F7F7FB` / `#0F0F1A`; kartochka oq / `#17172A`, 20px radius, soya, 40px ichki bo'shliq, 420px
- [x] F2 Salomlashuv ikonkasi + «Xush kelibsiz» (28px) + «Davom etish uchun hisobingizga kiring»
- [x] F3 «Login» va «Parol» (ko'z tugmasi `aria-pressed` bilan), 52px, indigo fokus halqasi, xatoda qizil chegara + halqa
- [x] F4 «⇪ Caps Lock yoqilgan» (`getModifierState`)
- [x] F5 «Eslab qolish» + «Parolni unutdingizmi?» → oyna «Parolni tiklash uchun maktab administratoriga murojaat qiling»
- [x] F6 Indigo gradient «Kirish» tugmasi, 52px, hover'da 2px ko'tariladi, spinner + «Kirilmoqda...», yuklanishda o'chadi va `onSubmit` ikki marta yuborishdan himoyalangan
- [x] F7 Sariq banner «Server bilan bog'lanib bo'lmadi» + «Qayta tekshirish» (o'sha `/api/health` tekshiruvi); bu holatda qizil xabar takrorlanmaydi
- [x] F8 Noto'g'ri parolda 300ms silkinish + xato xabari
- [x] F9 O'ng yuqorida ☀/☾ almashtirgich, `localStorage.darkMode`da saqlanadi (ilova bilan bir xil kalit)
- [x] F10 Rozilik matni kartochka ostida

## Umumiy
- [x] U1 Inter shrifti, barcha matn o'zbekcha (lotin)
- [x] U2 Ranglar/o'lchamlar `--login-*`, `--text-title`, `--text-lead`, `--text-display`, `--radius-xl` tokenlari orqali (`app.scss`, light va dark)
- [x] U3 400ms kirish animatsiyasi; `prefers-reduced-motion: reduce`da barcha animatsiyalar `none` (Playwright bilan tekshirildi)
- [x] U4 Login'ga avtofokus; Tab: Parol → ko'z → Eslab qolish → Parolni unutdingizmi? → Kirish; Enter yuboradi; `:focus-visible` halqasi; aria-label'lar; oq matn to'q indigo fonda va to'q matn oq kartochkada (AA)
- [x] U5 ≥1280px: 55/45 yonma-yon
- [x] U6 768–1279px: 50/50, sarlavha 44px, kompozitsiya 0.82 (1100px'dan tor — 0.68)
- [x] U7 <768px: ixcham sarlavha bloki (logo + sarlavha + naqsh), kartochkalarsiz, forma to'liq kenglikda, gorizontal scroll 0px
- [x] U8 Login mantiqi saqlangan: o'sha `POST /api/auth/login`, `authStore.setToken`, «eslab qolish», `/api/health`, `friendlyMessage`

## TEKSHIRUV
- [x] T1 `npm run lint` va `npm run build` xatosiz
- [x] T2 6 ta screenshot ko'zdan kechirildi (va qo'shimcha 1024×768):
  `login-1920x1080-light.png`, `login-1920x1080-dark.png`, `login-1366x768-light.png`, `login-1366x768-dark.png`, `login-390x844-light.png`, `login-390x844-dark.png`, `login-1024x768-light.png`.
  Topilib tuzatilganlar: 1920'da bo'sh qolgan chap panel, telefonda matnga yopishgan tema tugmasi, 1024'da siqilgan kartochka, server xatosining ikki marta chiqishi, oyna ochilganda bo'sh maydonning qizarishi
- [x] T3 Xato holatlari: `login-xato-parol.png`, `login-server-yoq.png`, `login-caps-lock.png` (+ `login-parolni-unutdim.png`)
- [x] T4 Playwright: Enter bilan va tugma bilan kirish → dashboard (`#/`, token saqlandi); noto'g'ri parol → xato, silkinish, qizil maydonlar, spinner to'xtadi; backend o'chiq → sariq banner (ochilganda ham, kirishda ham); konsolda xato yo'q; 30/30 tekshiruv o'tdi
- [x] T5 Hisobot: 32/32, screenshot'lar ro'yxati yuqorida
