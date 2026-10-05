# Qochuvchi «Kirish» tugmasi — v2

O'zgargan fayllar: `frontend/src/pages/LoginPage.vue` (effekt), `frontend/src/i18n/login.js` (faqat effekt matnlari: «Qochdim 🏃», «Ushlolmaysiz 😜», «To'ldiring 😉», «Login va parolni kiriting 🙂» va ularning ruscha tarjimasi).

## Xatti-harakat
- [x] Login yoki paroldan kamida bittasi bo'sh bo'lsa qochadi; ikkalasi to'ldirilsa — oddiy tugma
- [x] Qochishda haqiqiy o'lcham 140×40, shrift 13px (200ms), qisqa navbatma-navbat matnlar; qaytishda asl o'lcham, «Kirish» va joy (300ms)
- [x] Kursor uchidan qochish: masofa < 90px bo'lsa, «tugma markazi − kursor» vektori bo'yicha 110–150px; kursor tugma ustida bo'lsa — tasodifiy yo'nalish; translate 180ms `cubic-bezier(.22,1,.36,1)`, requestAnimationFrame bilan
- [x] Kartochka ichida 16px chegara bilan clamp; burchak/chetga qisilsa — qarama-qarshi yarmiga «pop» (scale 0.9→1) bilan sakraydi; input ustiga tushmaydi (ostiga/ustiga suriladi); absolute qatlam + asl joyida 52px o'rinbosar (layout sakramaydi)
- [x] 12 soniya qoidasi: birinchi qochishdan 12 s; keyin asl holat, «Login va parolni kiriting 🙂», bo'sh maydon qizil va fokusda, endi qochmaydi; maydon tozalanib qayta bo'sh qolsa — yana bir o'yin (sessiyada ko'pi bilan 2); taymerlar unmount'da tozalanadi
- [x] Saqlangan qoidalar: Enter/klaviaturada qochmaydi; touch va reduced-motion'da o'chiq; noto'g'ri login/parol faqat bosilgandan keyin serverdan 401 bilan; aria-live xabari

## TEKSHIRUV
- ✅ `npm run lint` va `npm run build` xatosiz
- ✅ a) Chapdan yaqinlashganda o'ngga (Δx = +133px), o'ngdan — chapga (Δx = −112px), pastdan — yuqoriga (Δy = −75px)
- ✅ b) Qochayotgan tugma 140×40px (< 160px), matni «Ushlolmaysiz 😜»
- ✅ c) 30 ta tasodifiy yaqinlashish: har safar kartochka ichida (16px chegara bilan), hech bir input bilan kesishmadi; tugma 30 dan 29 marta haqiqatan qochdi
- ✅ d) `page.clock` bilan 12 s: tugma asl o'lcham (350×52) va o'rinbosar joyida, «Login va parolni kiriting 🙂» ko'rinadi, bo'sh parol maydoni qizil va fokusda, endi qochmaydi
- ✅ e) Ikkala maydon to'ldirilgach darhol asl holat (140 → 350px, «Kirish»), bosilganda login ishladi (dashboard)
- ✅ f) Mobil (390px, hasTouch) va reduced-motion'da qochmaydi
- ✅ Qo'shimcha: Enter bilan bo'sh formada qochmaydi, faqat validatsiya xabari; 401 da silkinish va «Login yoki parol noto'g'ri. Qaytadan urinib ko'ring.»; til/tema, server yo'q banneri, yashil ✓ — oldingi 14 tekshiruv ham o'tdi
- ✅ Video: `docs/images/login/dodge-v2.webm` (qochish va 12 s dan keyingi qaytish)
- ✅ Skrinshotlar: `docs/images/login/dodge-small.png`, `docs/images/login/dodge-timeout.png`

Avtomatik test qilinmagan: «sessiyada 2 o'yin» qoidasi (kodda bor, alohida test yozilmagan).
