# 📚 Kitob mutolaasi — bot simulyatsiyasi (Telegram API mock)

Avtomatik yozilgan: `LibraryBotFlowIntegrationTest` (`LIBRARY_DEMO_OUT=docs/library-bot-demo.md`).
Bot haqiqiy router orqali ishladi, Telegram API o'rnida `MockTelegramClient` — har bir chiqish shu yerda.
Kitoblar test uchun generatsiya qilingan namunaviy PDF lar (haqiqiy kitob emas). `**qalin**`, `_kursiv_` —
Telegram HTML formatlash; `[ tugma ]` — inline tugmalar qatorlari; `<sub>` — callback_data (faqat ID lar).

## 0. Ota-ona botga ulanadi

✍️ Ota-ona yozadi: «/start ••••••••»

**🤖 Bot — 💬 sendMessage**

> ✅ **Tabriklaymiz!** Endi **Bobur Tojiboyev** (1-A sinf) haqida hamma narsa shu yerda.
> 
> **3 qadamda tanishib oling:**
> 1️⃣ Pastdagi tugmalar — asosiy bo'limlar: jadval, davomat, baholar va boshqalar
> 2️⃣ Muhim voqealar — kelmaslik, yangi baho, e'lon — o'zi xabar bo'lib keladi
> 3️⃣ «⚙️ Sozlamalar»da qaysi xabarlar va qachon kelishini tanlaysiz

**🤖 Bot — 🖼 photo (muqova / banner, caption bilan)**

> 🏠 **Bosh menyu** · 13-oktabr, seshanba
> 
> 👤 **Bobur Tojiboyev** · 1-A sinf
> 👩‍🏫 Sinf rahbari: Zebo Ne'matova
> 
> **Bugun:**
> 📚 5 ta dars · 08:30–13:25
> ⏳ Davomat hali belgilanmagan
> 📘 Bugun baho hali yo'q
> 📢 Yangi e'lonlar: **21**
> 🗓 Yaqin tadbir: **Ota-onalar yig'ilishi (demo)** — 14-oktabr
> 📅 Ertaga 1-dars: **08:30** · 📖 O'zbek adabiyoti
> 
> Bo'limni tanlang 👇

`[ 📅 Dars jadvali ] [ ✅ Davomat ]`  
`[ 📘 Baholar ] [ 📊 Hisobot ]`  
`[ ⭐ Xulq ] [ 📢 E'lonlar ]`  
`[ 🗓 Tadbirlar ] [ 👩‍🏫 O'qituvchilar ]`  
`[ ✉️ Ma'muriyatga xat ] [ 🤒 Sababli ariza ]`  
`[ 🏫 Maktab haqida ] [ ⚙️ Sozlamalar ]`  
`[ 📚 Kitob mutolaasi ] [ 👨‍👩‍👧 Farzandlarim ]`  

## 1. Menyudagi «📚 Kitob mutolaasi» tugmasi

✍️ Ota-ona yozadi: «📚 Kitob mutolaasi»

**🤖 Bot — 💬 sendMessage**

> 🏠 › 📚 Kutubxona
> 
> 📚 **Maktab kutubxonasi** — 7 ta kitob
> Kitob nomini yoki muallifni yozing, yoki bo'limni tanlang 👇

`[ 🔍 Qidirish ] [ ⭐ Tavsiyalar ]`  
`[ 🕵️ Detektiv D2837 · 6 ]`  
`[ 🔥 Eng ko'p o'qilganlar ] [ 🆕 Yangi kitoblar ]`  
`[ ❤️ Saqlanganlarim ] [ 📥 Yuklab olganlarim ]`  
`[ 🏠 Bosh menyu ]`  

## 2. Bo'lim

👆 Ota-ona bosadi: [🕵️ Detektiv D2837]  <sub>callback_data=lib:v:c:c:7</sub>

**🤖 Bot — ✏️ editMessageText**

> 🏠 › 📚 Kutubxona › 🕵️ Detektiv D2837
> 
> **🕵️ Detektiv D2837** — 6 ta kitob
> 
> 1. 📕 **Namuna detektiv 1 D2837** — Namuna muallif A · 2 bet
> 2. 📕 **Namuna detektiv 2 D2837** — Namuna muallif B · 2 bet
> 3. 📕 **Namuna detektiv 3 D2837** — Namuna muallif A · 2 bet
> 4. 📕 **Namuna detektiv 4 D2837** — Namuna muallif B · 2 bet
> 5. 📕 **Namuna detektiv 5 D2837** — Namuna muallif C · 2 bet
> 
> Kitobni tanlang 👇

`[ 📕 Namuna detektiv 1 D2837 — Namuna muallif A · 2 bet ]`  
`[ 📕 Namuna detektiv 2 D2837 — Namuna muallif B · 2 bet ]`  
`[ 📕 Namuna detektiv 3 D2837 — Namuna muallif A · 2 bet ]`  
`[ 📕 Namuna detektiv 4 D2837 — Namuna muallif B · 2 bet ]`  
`[ 📕 Namuna detektiv 5 D2837 — Namuna muallif C · 2 bet ]`  
`[ · ] [ 1/2 ] [ ▶️ ]`  
`[ 📚 Kutubxona ] [ 🏠 Bosh menyu ]`  

## 3. Kitob kartochkasi

👆 Ota-ona bosadi: [📕 Namuna detektiv 1 D2837]  <sub>callback_data=lib:v:b:b:39:k:c7.0</sub>

**🤖 Bot — 🖼 photo (muqova / banner, caption bilan)**

> 🏠 › 📚 Kutubxona
> 
> 📕 **Namuna detektiv 1 D2837**
> ✍️ Namuna muallif A
> 
> 🗂 Bo'lim: 🕵️ Detektiv D2837
> 🌐 Til: o'zbekcha
> 📄 2 bet
> 👥 Kattalar uchun
> 📅 2024-yil
> 
> _Generatsiya qilingan namunaviy PDF — haqiqiy kitob emas._
> 
> 📥 10 marta yuklangan · 👍 3 · 👎 0

`[ 📥 PDF yuklab olish ] [ ❤️ Saqlash ]`  
`[ 👍 3 ] [ 👎 0 ]`  
`[ ⬅️ Orqaga ] [ 📚 Kutubxona ]`  

## 4. «📥 PDF yuklab olish» (toast: 📥 Kitob yuborilmoqda…)

👆 Ota-ona bosadi: [📥 PDF yuklab olish]  <sub>callback_data=lib:a:dl:b:39:k:c7.0</sub>

**🤖 Bot — 🖼 photo (muqova / banner, caption bilan)**

> 🏠 › 📚 Kutubxona
> 
> 📕 **Namuna detektiv 1 D2837**
> ✍️ Namuna muallif A
> 
> 🗂 Bo'lim: 🕵️ Detektiv D2837
> 🌐 Til: o'zbekcha
> 📄 2 bet
> 👥 Kattalar uchun
> 📅 2024-yil
> 
> _Generatsiya qilingan namunaviy PDF — haqiqiy kitob emas._
> 
> 📥 11 marta yuklangan · 👍 3 · 👎 0

`[ 📥 PDF yuklab olish ] [ ❤️ Saqlash ]`  
`[ 👍 3 ] [ 👎 0 ]`  
`[ ⬅️ Orqaga ] [ 📚 Kutubxona ]`  

## 5. PDF ota-onaga yuboriladi

📤 Outbox yuboradi (NotificationSender)

**🤖 Bot — 📎 sendDocument (PDF, file_id=mock-document-3)**

> 📖 **Namuna detektiv 1 D2837**
> Namuna muallif A
> 
> Yoqimli mutolaa! 📖

## 6. Qidiruv (kirillcha, boshqa apostrof)

✍️ Ota-ona yozadi: «ўткан кунлар»

**🤖 Bot — 💬 sendMessage**

> 🏠 › 📚 Kutubxona › 🔍 Qidirish
> 
> 🔍 «ўткан кунлар» — 1 ta kitob topildi
> 
> 1. 📕 **O‘tkan kunlar (namuna) D2837** — Namuna muallif D · 2 bet
> 
> Kitobni tanlang 👇

`[ 📕 O‘tkan kunlar (namuna) D2837 — Namuna muallif D · 2 bet ]`  
`[ 📚 Kutubxona ] [ 🏠 Bosh menyu ]`  

## 7. Topilmagan qidiruv

✍️ Ota-ona yozadi: «mavjud bo'lmagan kitob xyz»

**🤖 Bot — 💬 sendMessage**

> 🏠 › 📚 Kutubxona › 🔍 Qidirish
> 
> 😔 «mavjud bo'lmagan kitob xyz» bo'yicha hech narsa topilmadi.
> Shu bo'limlarga qarab ko'ring:

`[ 🕵️ Detektiv D2837 · 6 ]`  
`[ 📚 Kutubxona ] [ 🏠 Bosh menyu ]`  

## 8. «⭐ Tavsiyalar»

👆 Ota-ona bosadi: [⭐ Tavsiyalar]  <sub>callback_data=lib:v:r</sub>

**🤖 Bot — ✏️ editMessageText**

> 🏠 › 📚 Kutubxona › ⭐ Tavsiyalar
> 
> ⭐ **Tavsiyalar**
> 
> 👤 **Siz uchun**
> 1. Namuna detektiv 2 D2837 — Namuna muallif B
> 2. Namuna detektiv 3 D2837 — Namuna muallif A
> 3. O‘tkan kunlar (namuna) D2837 — Namuna muallif D
> 
> 🔥 **Bu hafta mashhur**
> 1. Namuna detektiv 1 D2837 — Namuna muallif A
> 
> 🏫 **Maktab tavsiya qiladi**
> 1. Namuna detektiv 1 D2837 — Namuna muallif A
> 2. Namuna ertaklar D2837 — Namuna muallif C
> 
> 🧒 **Farzandingiz bilan o'qing**
> 1. Namuna ertaklar D2837 — Namuna muallif C

`[ 📕 Namuna detektiv 2 D2837 ] [ 📕 Namuna detektiv 3 D2837 ]`  
`[ 📕 O‘tkan kunlar (namuna) D2837 ] [ 📕 Namuna detektiv 1 D2837 ]`  
`[ 📕 Namuna ertaklar D2837 ]`  
`[ 📚 Kutubxona ] [ 🏠 Bosh menyu ]`  

## 9. Boshqa menyu tugmasi qidiruv rejimini yopadi

✍️ Ota-ona yozadi: «📅 Dars jadvali»

**🤖 Bot — 🖼 photo (muqova / banner, caption bilan)**

> 🏠 › 📅 Jadval › Bugun
> 
> **13-oktabr, seshanba**
> 
> **1.** 08:30–09:15 · 🎨 **Tasviriy san'at**
>       👩‍🏫 Bahodir Xasanov · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **2.** 09:25–10:10 · 🤝 **Tarbiya**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **3.** 10:20–11:05 · 🇬🇧 **Ingliz tili**
>       👩‍🏫 Aziz Nazarov · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **4.** 11:15–12:00 · 🏃 **Jismoniy tarbiya**
>       👩‍🏫 Zebo Usmonova · 🚪 Katta sport zali-xona
>       ☕ _Tanaffus 40 daqiqa_
> **5.** 12:40–13:25 · 📝 **Ona tili**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona

`[ • Bugun • ] [ Ertaga ] [ Hafta ]`  
`[ 🏠 Bosh menyu ]`  

## 10. Endi oddiy matn — qidiruv emas

✍️ Ota-ona yozadi: «detektiv»

**🤖 Bot — 💬 sendMessage**

> 🤔 Tushunmadim. Pastdagi tugmalardan foydalaning yoki /yordam yozing.

## 11. Juma 19:00 — haftalik tavsiya xabari

📤 Outbox yuboradi (NotificationSender)

**🤖 Bot — 💬 sendMessage**

> 📚 **Dam olish kunlari uchun 3 ta kitob**
> 
> 1. **Namuna detektiv 2 D2837** — Namuna muallif B
> 2. **Namuna detektiv 3 D2837** — Namuna muallif A
> 3. **O‘tkan kunlar (namuna) D2837** — Namuna muallif D
> 
> Kitobni tanlang — PDF shu yerga keladi 👇
> 
> 🏫 _1-maktab_

`[ 📕 Namuna detektiv 2 D2837 ]`  
`[ 📕 Namuna detektiv 3 D2837 ]`  
`[ 📕 O‘tkan kunlar (namuna) D2837 ]`  
`[ 📚 Kutubxona ]`  
