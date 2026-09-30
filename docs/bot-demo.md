# Ota-onalar boti — to'liq demo

> Bu fayl `scripts/bot-demo.mjs` tomonidan avtomatik yaratildi (30/09/2026, 15:33:59),
> backend `telegram.mock=true` rejimida, seed ma'lumot bilan. Har bir 👤 qadam — ota-onaning harakati,
> 🤖 — bot javobi, `[...]` — inline tugmalar, ✏️ — xabar yangisi yuborilmay, o'rnida tahrirlangani.
> Botni Telegram'da ochmasdan turib barcha sahifalarni shu yerda ko'rish mumkin.

**Mundarija:** 
[1. Kutib olish](#1-kutib-olish-va-ulanish) · [2. Bosh menyu](#2-bosh-menyu--bugun) · [3. Dars jadvali](#3-dars-jadvali) · [4. Davomat](#4-davomat) · [5. Baholar](#5-baholar) · [6. Hisobot](#6-hisobot) · [7. Xulq](#7-xulq) · [8. E'lonlar](#8-elonlar) · [9. Tadbirlar](#9-tadbirlar) · [10. O'qituvchilar](#10-oqituvchilar) · [11. Maktabga yozish](#11-maktabga-yozish) · [12. Sababli ariza](#12-sababli-ariza) · [13. Maktab haqida](#13-maktab-haqida) · [14. Sozlamalar](#14-sozlamalar-3-til) · [15. Farzandlarim](#15-farzandlarim) · [16. Xavfsizlik](#16-xavfsizlik) · [17. Avtomatik xabarlar](#17-avtomatik-xabarlar)

## 1. Kutib olish va ulanish

### 1.1. Birinchi /start (hali ulanmagan)

👤 **Ota-ona:** `/start`

🤖 **Bot:**

> 👋 **Assalomu alaykum!**
>
> Men — **Maktab Boshqaruv** boti, farzandingizning maktabdagi yordamchisiman.
>
> 📅 Dars jadvali va ertangi darslar
> ✅ Davomat — kelmasa yoki kechiksa, darhol xabar
> 📘 Baholar va haftalik hisobot
> 📢 Maktab e'lonlari va tadbirlar
> 💬 Maktabga to'g'ridan-to'g'ri yozish
>
> **Qanday ulanaman?**
> 1️⃣ Sinf rahbari bergan **QR kodni** telefon kamerasi bilan skanerlang
> 2️⃣ Yoki maktab yuborgan **taklif havolasini** oching
> 3️⃣ Yoki pastdagi **«📱 Raqamni ulashish»** tugmasini bosing — maktabda yozilgan raqamingiz bo'yicha farzandingizni topaman
>
> ⌨️ _Pastki klaviatura:_
> `📱 Raqamni ulashish`

### 1.2. Sinf rahbari bergan QR kod / havola orqali ulanish

👤 **Ota-ona:** QR kodni skanerladi → Telegram `/start QAkeaSgRhP` yubordi

🤖 **Bot:**

> ✅ **Tabriklaymiz!** Endi **Elyor Berdiyev** (1-A sinf) haqida hamma narsa shu yerda.
>
> **3 qadamda tanishib oling:**
> 1️⃣ Pastdagi tugmalar — asosiy bo'limlar: jadval, davomat, baholar va boshqalar
> 2️⃣ Muhim voqealar — kelmaslik, yangi baho, e'lon — o'zi xabar bo'lib keladi
> 3️⃣ «⚙️ Sozlamalar»da qaysi xabarlar va qachon kelishini tanlaysiz
>
> ⌨️ _Pastki klaviatura:_
> `📅 Dars jadvali` `✅ Davomat`
> `📘 Baholar` `📊 Hisobot`
> `📢 E'lonlar` `🗓 Tadbirlar`
> `👩‍🏫 O'qituvchilar` `💬 Maktabga yozish`
> `⚙️ Sozlamalar` `👨‍👩‍👧 Farzandlarim`

🤖 **Bot:**

> 🏠 **Bosh menyu** · 30-sentabr, chorshanba
>
> 👤 **Elyor Berdiyev** · 1-A sinf
> 👩‍🏫 Sinf rahbari: Zebo Ne'matova
>
> **Bugun:**
> 📚 5 ta dars · 08:30–13:25
> 🔴 2 ta darsga kelmadi
> 📘 Bugungi baholar: 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🟢 **5** Ona tili
> 📢 Yangi e'lonlar: **7**
> 🗓 Yaqin tadbir: **O'qituvchilar va murabbiylar kuni** — 1-oktabr
>
> Bo'limni tanlang 👇
>
> `[📱 Kundalikni ochish ↗]`
> `[📅 Dars jadvali]` `[✅ Davomat]`
> `[📘 Baholar]` `[📊 Hisobot]`
> `[⭐ Xulq]` `[📢 E'lonlar]`
> `[🗓 Tadbirlar]` `[👩‍🏫 O'qituvchilar]`
> `[💬 Maktabga yozish]` `[🤒 Sababli ariza]`
> `[🏫 Maktab haqida]` `[⚙️ Sozlamalar]`
> `[👨‍👩‍👧 Farzandlarim]`

### 1.3. Noto'g'ri kod

👤 **Ota-ona:** `/start NotoGriKod1`

🤖 **Bot:**

> ❌ Kod topilmadi. Havolani sinf rahbaringizdan qayta so'rang yoki /start bosib raqamingizni ulashing.
>
> ⌨️ _Pastki klaviatura:_
> `📅 Dars jadvali` `✅ Davomat`
> `📘 Baholar` `📊 Hisobot`
> `📢 E'lonlar` `🗓 Tadbirlar`
> `👩‍🏫 O'qituvchilar` `💬 Maktabga yozish`
> `⚙️ Sozlamalar` `👨‍👩‍👧 Farzandlarim`

## 2. Bosh menyu — "Bugun"

### 2.1. Bosh menyu

👤 **Ota-ona:** tugmani bosdi `[🏠 Bosh menyu]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 **Bosh menyu** · 30-sentabr, chorshanba
>
> 👤 **Elyor Berdiyev** · 1-A sinf
> 👩‍🏫 Sinf rahbari: Zebo Ne'matova
>
> **Bugun:**
> 📚 5 ta dars · 08:30–13:25
> 🔴 2 ta darsga kelmadi
> 📘 Bugungi baholar: 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🟢 **5** Ona tili
> 📢 Yangi e'lonlar: **7**
> 🗓 Yaqin tadbir: **O'qituvchilar va murabbiylar kuni** — 1-oktabr
>
> Bo'limni tanlang 👇
>
> `[📱 Kundalikni ochish ↗]`
> `[📅 Dars jadvali]` `[✅ Davomat]`
> `[📘 Baholar]` `[📊 Hisobot]`
> `[⭐ Xulq]` `[📢 E'lonlar]`
> `[🗓 Tadbirlar]` `[👩‍🏫 O'qituvchilar]`
> `[💬 Maktabga yozish]` `[🤒 Sababli ariza]`
> `[🏫 Maktab haqida]` `[⚙️ Sozlamalar]`
> `[👨‍👩‍👧 Farzandlarim]`

## 3. Dars jadvali

### 3.1. Bugun

👤 **Ota-ona:** tugmani bosdi `[📅 Dars jadvali]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📅 Jadval › Bugun
>
> **30-sentabr, chorshanba**
>
> **1.** 08:30–09:15 · 📖 **O'zbek adabiyoti**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **2.** 09:25–10:10 · 🎵 **Musiqa**
>       👩‍🏫 Bekzod Islomov · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **3.** 10:20–11:05 · 📝 **Ona tili**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **4.** 11:15–12:00 · 📝 **Ona tili**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>       ☕ _Tanaffus 40 daqiqa_
> **5.** 12:40–13:25 · 📝 **Ona tili**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>
> `[• Bugun •]` `[Ertaga]` `[Hafta]`
> `[🏠 Bosh menyu]`

### 3.2. Ertaga

👤 **Ota-ona:** tugmani bosdi `[Ertaga]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📅 Jadval › Ertaga
>
> **1-oktabr, payshanba**
>
> 🎉 Ertaga O'qituvchilar va murabbiylar kuni — dars yo'q
>
> `[Bugun]` `[• Ertaga •]` `[Hafta]`
> `[🏠 Bosh menyu]`

### 3.3. Hafta

👤 **Ota-ona:** tugmani bosdi `[Hafta]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📅 Jadval › Hafta
>
> **Dushanba, 28-sentabr** · 5 ta dars · 08:30–13:25
>    1. 🔢 Matematika
>    2. 🏃 Jismoniy tarbiya
>    3. 📖 O'zbek adabiyoti
>    4. 🇬🇧 Ingliz tili
>    5. 🎵 Musiqa
>
> **Seshanba, 29-sentabr** · 5 ta dars · 08:30–13:25
>    1. 🎨 Tasviriy san'at
>    2. 🤝 Tarbiya
>    3. 🇬🇧 Ingliz tili
>    4. 🏃 Jismoniy tarbiya
>    5. 📝 Ona tili
>
> **Chorshanba, 30-sentabr** · 5 ta dars · 08:30–13:25
>    1. 📖 O'zbek adabiyoti
>    2. 🎵 Musiqa
>    3. 📝 Ona tili
>    4. 📝 Ona tili
>    5. 📝 Ona tili
>
> **Payshanba, 1-oktabr**
> 🎉 O'qituvchilar va murabbiylar kuni — dars yo'q
>
> **Juma, 2-oktabr** · 5 ta dars · 08:30–13:25
>    1. 🔢 Matematika
>    2. 🔢 Matematika
>    3. 🔢 Matematika
>    4. 🛠 Texnologiya
>    5. 📖 O'zbek adabiyoti
>
> **Shanba, 3-oktabr** · 4 ta dars · 08:30–12:00
>    1. 🔢 Matematika
>    2. 🤝 Tarbiya
>    3. 🛠 Texnologiya
>    4. 🔢 Matematika
>
> `[Bugun]` `[Ertaga]` `[• Hafta •]`
> `[🏠 Bosh menyu]`

### 3.4. Pastki klaviatura orqali ochish

👤 **Ota-ona:** «📅 Dars jadvali»

🤖 **Bot:**

> 🏠 › 📅 Jadval › Bugun
>
> **30-sentabr, chorshanba**
>
> **1.** 08:30–09:15 · 📖 **O'zbek adabiyoti**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **2.** 09:25–10:10 · 🎵 **Musiqa**
>       👩‍🏫 Bekzod Islomov · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **3.** 10:20–11:05 · 📝 **Ona tili**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>       ☕ _Tanaffus 10 daqiqa_
> **4.** 11:15–12:00 · 📝 **Ona tili**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>       ☕ _Tanaffus 40 daqiqa_
> **5.** 12:40–13:25 · 📝 **Ona tili**
>       👩‍🏫 Zebo Ne'matova · 🚪 201-xona
>
> `[• Bugun •]` `[Ertaga]` `[Hafta]`
> `[🏠 Bosh menyu]`

## 4. Davomat

### 4.1. Oylik xulosa

👤 **Ota-ona:** tugmani bosdi `[✅ Davomat]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Davomat › Sentabr 2026
>
> **Sentabr 2026**
>
> 📚 Jami darslar: **28**
> ✅ Kelgan: **21**
> 🟡 Kechikkan: **2**
> 🔴 Kelmagan: **4**
> 🔵 Sababli: **1**
>
> ▰▰▰▰▰▰▰▰▱▱ **82.1%**
>> Sinf o'rtachasi: 94.2% — o'rtachadan pastroq, e'tibor bering
>
> `[◀️ Avgust]` `[Sentabr 2026]` `[·]`
> `[🗓 Kalendar]` `[📋 Batafsil]`
> `[📚 Fanlar bo'yicha]` `[🖼 Rasm ko'rinishida]`
> `[• 🗓 Oy •]` `[🏷 Chorak]` `[🎓 O'quv yili]`
> `[🏠 Bosh menyu]`

### 4.2. Oldingi oy

👤 **Ota-ona:** tugmani bosdi `[◀️ Avgust]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Davomat › Avgust 2026
>
> **Avgust 2026**
>
> 📚 Jami darslar: **25**
> ✅ Kelgan: **20**
> 🟡 Kechikkan: **0**
> 🔴 Kelmagan: **3**
> 🔵 Sababli: **2**
>
> ▰▰▰▰▰▰▰▰▱▱ **80%**
>> Sinf o'rtachasi: 91.8% — o'rtachadan pastroq, e'tibor bering
>
> `[◀️ Iyul]` `[Avgust 2026]` `[Sentabr ▶️]`
> `[🗓 Kalendar]` `[📋 Batafsil]`
> `[📚 Fanlar bo'yicha]` `[🖼 Rasm ko'rinishida]`
> `[• 🗓 Oy •]` `[🏷 Chorak]` `[🎓 O'quv yili]`
> `[🏠 Bosh menyu]`

### 4.3. Kalendar

👤 **Ota-ona:** tugmani bosdi `[🗓 Kalendar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Davomat › Sentabr 2026 › 🗓 Kalendar
>
> **Sentabr 2026** · kalendar
>
> `01–06` ▫️🟩🟩🟩🟩🟩⬜
> `07–13` 🟩🟦🟩🟥🟩🟩⬜
> `14–20` 🟨🟩🟥🟩🟩🟩⬜
> `21–27` 🟩🟩🟩🟩🟩🟩⬜
> `28–30` 🟩🟩🟥
>
> 🟩 keldi · 🟨 kechikdi · 🟥 kelmadi · 🟦 sababli · ⬜ dars yo'q
>
> `[🖼 Rasm ko'rinishida]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 4.4. Batafsil — kelmagan va kechikkan darslar

👤 **Ota-ona:** tugmani bosdi `[📋 Batafsil]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Davomat › Sentabr 2026 › 📋 Batafsil
>
> **Kelmagan va kechikkan darslar** · Sentabr 2026
>
> 🔴 **30-sentabr, chorshanba** · 1-dars · O'zbek adabiyoti
> 🟡 **30-sentabr, chorshanba** · 2-dars · Musiqa
> 🔴 **30-sentabr, chorshanba** · 3-dars · Ona tili
> 🔴 **16-sentabr, chorshanba** · 1-dars · O'zbek adabiyoti
> 🟡 **14-sentabr, dushanba** · 1-dars · Matematika
> 🔴 **10-sentabr, payshanba** · 1-dars · Tasviriy san'at
>
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 4.5. Fanlar bo'yicha

👤 **Ota-ona:** tugmani bosdi `[📚 Fanlar bo'yicha]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Davomat › Sentabr 2026 › 📚 Fanlar bo'yicha
>
> **Fanlar bo'yicha** · Sentabr 2026
>
> 📖 O'zbek adabiyoti: 🔴 2 · 🟡 0
> 📝 Ona tili: 🔴 1 · 🟡 0
> 🎨 Tasviriy san'at: 🔴 1 · 🟡 0
> 🔢 Matematika: 🔴 0 · 🟡 1
> 🎵 Musiqa: 🔴 0 · 🟡 1
>
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 4.6. Kalendar rasm ko'rinishida

👤 **Ota-ona:** tugmani bosdi `[🖼 Rasm ko'rinishida]`

🤖 **Bot:**

> ![rasm](images/bot/01-davomat-kalendar.png)
>
> 🗓 **Elyor Berdiyev** · Sentabr 2026
>
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 4.7. Chorak bo'yicha

👤 **Ota-ona:** tugmani bosdi `[🏷 Chorak]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Davomat › 1-chorak
>
> **1-chorak**
>
> 📚 Jami darslar: **38**
> ✅ Kelgan: **21**
> 🟡 Kechikkan: **2**
> 🔴 Kelmagan: **4**
> 🔵 Sababli: **11**
>
> ▰▰▰▰▰▰▱▱▱▱ **60.5%**
>> Sinf o'rtachasi: 92.5% — o'rtachadan pastroq, e'tibor bering
>
> `[·]` `[1-chorak]` `[2-chorak ▶️]`
> `[🗓 Kalendar]` `[📋 Batafsil]`
> `[📚 Fanlar bo'yicha]`
> `[🗓 Oy]` `[• 🏷 Chorak •]` `[🎓 O'quv yili]`
> `[🏠 Bosh menyu]`

### 4.8. O'quv yili bo'yicha

👤 **Ota-ona:** tugmani bosdi `[🎓 O'quv yili]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Davomat › 2026–2027 o'quv yili
>
> **2026–2027 o'quv yili**
>
> 📚 Jami darslar: **38**
> ✅ Kelgan: **21**
> 🟡 Kechikkan: **2**
> 🔴 Kelmagan: **4**
> 🔵 Sababli: **11**
>
> ▰▰▰▰▰▰▱▱▱▱ **60.5%**
>> Sinf o'rtachasi: 92.5% — o'rtachadan pastroq, e'tibor bering
>
> `[🗓 Kalendar]` `[📋 Batafsil]`
> `[📚 Fanlar bo'yicha]`
> `[🗓 Oy]` `[🏷 Chorak]` `[• 🎓 O'quv yili •]`
> `[🏠 Bosh menyu]`

## 5. Baholar

### 5.1. So'nggi baholar

👤 **Ota-ona:** tugmani bosdi `[📘 Baholar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📘 Baholar › 🕘 So'nggi
>
> **So'nggi baholar**
>
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🔴 **2** · O'zbek adabiyoti · 30-sentabr
>       _joriy_ — «Uy vazifasi bajarilmagan»
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🔴 **2** · O'zbek adabiyoti · 30-sentabr
>       _joriy_ — «Uy vazifasi bajarilmagan»
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🔴 **2** · O'zbek adabiyoti · 30-sentabr
>       _joriy_ — «Uy vazifasi bajarilmagan»
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🟢 **5** · Tasviriy san'at · 29-sentabr
>       _joriy_
>
> `[• 🕘 So'nggi •]` `[📚 Fanlar]` `[🏅 Chorak]`
> `[·]` `[1/4]` `[▶️]`
> `[🏠 Bosh menyu]`

### 5.2. Fanlar bo'yicha o'rtacha (tendensiya bilan)

👤 **Ota-ona:** tugmani bosdi `[📚 Fanlar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📘 Baholar › 📚 Fanlar
>
> **Fanlar bo'yicha o'rtacha**
>
> 📘 Ona tili **4.4** ▰▰▰▰▱ ↑
> 📘 Tasviriy san'at **4.3** ▰▰▰▰▱ ↓
> 📙 Musiqa **3.0** ▰▰▰▱▱ ↓
> 📙 O'zbek adabiyoti **2.6** ▰▰▰▱▱ ↓
>
>> ↑ ↓ — o'tgan oyga nisbatan o'zgarish. Fanni bosing — barcha baholar ochiladi.
>
> `[🕘 So'nggi]` `[• 📚 Fanlar •]` `[🏅 Chorak]`
> `[📝 Ona tili]` `[🎨 Tasviriy san'at]`
> `[🎵 Musiqa]` `[📖 O'zbek adabiyoti]`
> `[📈 Grafik]`
> `[🏠 Bosh menyu]`

### 5.3. Fan tafsiloti — barcha baholar va o'qituvchi

👤 **Ota-ona:** tugmani bosdi `[📝 Ona tili]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📘 Baholar › Ona tili
>
> 📝 **Ona tili**
> 👩‍🏫 O'qituvchi: Zebo Ne'matova
> 📈 O'rtacha: **4.43** · 7 ta baho
>
> 🟢 **5** · 30-sentabr, chorshanba · _joriy_
> 🟢 **5** · 30-sentabr, chorshanba · _joriy_
> 🟢 **5** · 30-sentabr, chorshanba · _joriy_
> 🟢 **5** · 30-sentabr, chorshanba · _joriy_
> 🔴 **2** · 19-sentabr, shanba · _imtihon_
> 🔵 **4** · 8-sentabr, seshanba · _imtihon_
> 🟢 **5** · 6-sentabr, yakshanba · _imtihon_
>
> `[💬 Maktabga yozish]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 5.4. Chorak baholari

👤 **Ota-ona:** tugmani bosdi `[🏅 Chorak]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📘 Baholar › 🏅 Chorak
>
> **Chorak baholari**
>
> Chorak baholari hali qo'yilmagan 🙂
>
> `[🕘 So'nggi]` `[📚 Fanlar]` `[• 🏅 Chorak •]`
> `[🏠 Bosh menyu]`

### 5.5. Grafik (rasm)

👤 **Ota-ona:** tugmani bosdi `[📈 Grafik]`

🤖 **Bot:**

> ![rasm](images/bot/02-baholar-grafik.png)
>
> 📈 **Elyor Berdiyev** · fanlar bo'yicha o'rtacha baholar
>
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

## 6. Hisobot

### 6.1. Hafta

👤 **Ota-ona:** tugmani bosdi `[📊 Hisobot]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📊 Hisobot › 🗓 Hafta
>
> **28-sentabr – 30-sentabr**
>
> ✅ **Davomat:** 60% (3/5) · 🔴 2 · 🟡 1
> 📘 **Baholar:** 8 ta, o'rtacha **3.88**
> ⭐ **Xulq:** 🌟 0 rag'bat · ⚠️ 0 ogohlantirish
>
> 💪 **Kuchli fanlar:** 📝 Ona tili, 🎨 Tasviriy san'at
> 🎯 **E'tibor talab qiladi:** 📖 O'zbek adabiyoti
>
> `[• 🗓 Hafta •]` `[📆 Oy]`
> `[🖼 Rasm ko'rinishida]`
> `[🏠 Bosh menyu]`

### 6.2. Oy

👤 **Ota-ona:** tugmani bosdi `[Oy]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📊 Hisobot › 📆 Oy
>
> **Sentabr 2026**
>
> ✅ **Davomat:** 82.1% (23/28) · 🔴 4 · 🟡 2
> 📘 **Baholar:** 16 ta, o'rtacha **3.75**
> ⭐ **Xulq:** 🌟 0 rag'bat · ⚠️ 0 ogohlantirish
>
> 💪 **Kuchli fanlar:** —
> 🎯 **E'tibor talab qiladi:** 🎵 Musiqa, 📖 O'zbek adabiyoti
>
> `[🗓 Hafta]` `[• 📆 Oy •]`
> `[🖼 Rasm ko'rinishida]`
> `[🏠 Bosh menyu]`

### 6.3. Hisobot kartochkasi (rasm)

👤 **Ota-ona:** tugmani bosdi `[🖼 Rasm ko'rinishida]`

🤖 **Bot:**

> ![rasm](images/bot/03-hisobot-kartochka.png)
>
> 📊 **Elyor Berdiyev** · Sentabr 2026
>
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

## 7. Xulq

### 7.1. Joriy oy

👤 **Ota-ona:** tugmani bosdi `[⭐ Xulq]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⭐ Xulq › Sentabr 2026
>
> **Sentabr 2026**: 🌟 0 ta rag'bat · ⚠️ 0 ta ogohlantirish
>
> Hozircha xulq yozuvlari yo'q 🙂
>
> `[◀️ Avgust]` `[Sentabr 2026]` `[·]`
> `[🏠 Bosh menyu]`

### 7.2. Oldingi oy

👤 **Ota-ona:** tugmani bosdi `[◀️ Avgust]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⭐ Xulq › Avgust 2026
>
> **Avgust 2026**: 🌟 0 ta rag'bat · ⚠️ 0 ta ogohlantirish
>
> Hozircha xulq yozuvlari yo'q 🙂
>
> `[◀️ Iyul]` `[Avgust 2026]` `[Sentabr ▶️]`
> `[🏠 Bosh menyu]`

## 8. E'lonlar

### 8.1. Ro'yxat

👤 **Ota-ona:** tugmani bosdi `[📢 E'lonlar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📢 E'lonlar
>
> **E'lonlar** · 10 ta
>
> 🆕 🔴 **Kuzgi sport musobaqasi** · 30-sentabr
> 🔴 **Kuzgi sport musobaqasi** · 30-sentabr
> 🔴 **Kuzgi sport musobaqasi** · 30-sentabr
> **Ota-onalar yigilishi** · 30-sentabr
> 🆕 **Iqtidorli o'quvchilar uchun to'garaklar** · 30-sentabr
> 🆕 **Maktab oshxonasi menyusi yangilandi** · 30-sentabr
>
>> 🆕 — yangi · 🔴 — muhim
>
> `[🆕 🔴 Kuzgi sport musobaqasi]`
> `[🔴 Kuzgi sport musobaqasi]`
> `[🔴 Kuzgi sport musobaqasi]`
> `[Ota-onalar yigilishi]`
> `[🆕 Iqtidorli o'quvchilar uchun to'garaklar]`
> `[🆕 Maktab oshxonasi menyusi yangilandi]`
> `[·]` `[1/2]` `[▶️]`
> `[🏠 Bosh menyu]`

### 8.2. To'liq matn

👤 **Ota-ona:** tugmani bosdi `[🆕 🔴 Kuzgi sport musobaqasi]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📢 E'lonlar › Kuzgi sport musobaqasi
>
> 🔴 **Kuzgi sport musobaqasi**
> _30-sentabr, chorshanba_
>
> Shanba kuni soat 10:00 da maktab stadionida «Sog'lom avlod» sport musobaqasi bo'lib o'tadi. Farzandingizga sport kiyimini olib kelishni unutmang!
>
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

## 9. Tadbirlar

### 9.1. Yaqinlashayotgan tadbirlar

👤 **Ota-ona:** tugmani bosdi `[🗓 Tadbirlar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 🗓 Tadbirlar
>
> **Yaqinlashayotgan tadbirlar**
>
> 🎉 **O'qituvchilar va murabbiylar kuni**
>       1-oktabr, payshanba · _ertaga_
> 👨‍👩‍👧 **Ota-onalar yig'ilishi**
>       6-oktabr, seshanba · _6 kundan keyin_
> 📝 **I chorak yakuniy nazorat ishlari**
>       26-oktabr – 30-oktabr · _26 kundan keyin_
> 🏖 **Kuzgi ta'til**
>       2-noyabr – 9-noyabr · _33 kundan keyin_
>
>> 🔔 Har bir tadbirdan 1 kun oldin eslatib qo'yaman
>
> `[🏠 Bosh menyu]`

## 10. O'qituvchilar

### 10.1. Sinf rahbari va fan o'qituvchilari

👤 **Ota-ona:** tugmani bosdi `[👩‍🏫 O'qituvchilar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 👩‍🏫 O'qituvchilar
>
> **O'qituvchilar** · 1-A sinf
>
> ⭐ **Sinf rahbari:** Zebo Ne'matova
>
> 🇬🇧 **Ingliz tili** — Aziz Nazarov
> 🏃 **Jismoniy tarbiya** — Zebo Usmonova
> 🔢 **Matematika** — Zebo Ne'matova
> 🎵 **Musiqa** — Bekzod Islomov
> 📖 **O'zbek adabiyoti** — Zebo Ne'matova
> 📝 **Ona tili** — Zebo Ne'matova
> 🤝 **Tarbiya** — Zebo Ne'matova
> 🎨 **Tasviriy san'at** — Bahodir Xasanov
> 🛠 **Texnologiya** — Robiya Yoqubova
>
>> 📞 O'qituvchi bilan bog'lanish uchun «💬 Maktabga yozish» bo'limidan foydalaning
>
> `[💬 Maktabga yozish]`
> `[🏠 Bosh menyu]`

## 11. Maktabga yozish

### 11.1. Bo'lim

👤 **Ota-ona:** tugmani bosdi `[💬 Maktabga yozish]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 💬 Murojaat
>
> **Maktabga yozish**
> Savol, taklif yoki muammo bo'lsa — yozing, javob shu yerga keladi.
>
> **Kimga yozasiz?**
>
> **Oxirgi murojaatlar**
> ✅ 30-sentabr · 👩‍🏫 Sinf rahbariga
> _«Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin oli…»_
> ↪️ **Javob:** Va alaykum assalom! Albatta, 3-darsdan so'ng olib ketishingiz mumkin. Sinf rahbari.
> ✅ 30-sentabr · 👩‍🏫 Sinf rahbariga
> _«Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin oli…»_
> ↪️ **Javob:** Va alaykum assalom! Albatta, 3-darsdan so'ng olib ketishingiz mumkin. Sinf rahbari.
> ✅ 30-sentabr · 👩‍🏫 Sinf rahbariga
> _«Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin oli…»_
> ↪️ **Javob:** Va alaykum assalom! Albatta, 3-darsdan so'ng olib ketishingiz mumkin. Sinf rahbari.
>
> `[👩‍🏫 Sinf rahbariga]` `[🏢 Ma'muriyatga]`
> `[🏠 Bosh menyu]`

### 11.2. Sinf rahbariga yozish

👤 **Ota-ona:** tugmani bosdi `[👩‍🏫 Sinf rahbariga]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 💬 Murojaat › 👩‍🏫 Sinf rahbariga
>
> ✍️ **👩‍🏫 Sinf rahbariga** xabaringizni yozing.
> Xohlasangiz, rasm ham yuborishingiz mumkin (izoh bilan).
>
> `[✖️ Bekor qilish]`

👤 **Ota-ona:** «Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin olib ketsam bo'ladimi?»

🤖 **Bot:**

> ✅ Xabaringiz yuborildi! Javob kelishi bilan shu yerda xabar beraman.
>
> 🏠 › 💬 Murojaat
>
> **Maktabga yozish**
> Savol, taklif yoki muammo bo'lsa — yozing, javob shu yerga keladi.
>
> **Kimga yozasiz?**
>
> **Oxirgi murojaatlar**
> ⏳ 30-sentabr · 👩‍🏫 Sinf rahbariga
> _«Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin oli…»_
> ✅ 30-sentabr · 👩‍🏫 Sinf rahbariga
> _«Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin oli…»_
> ↪️ **Javob:** Va alaykum assalom! Albatta, 3-darsdan so'ng olib ketishingiz mumkin. Sinf rahbari.
> ✅ 30-sentabr · 👩‍🏫 Sinf rahbariga
> _«Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin oli…»_
> ↪️ **Javob:** Va alaykum assalom! Albatta, 3-darsdan so'ng olib ketishingiz mumkin. Sinf rahbari.
> ✅ 30-sentabr · 👩‍🏫 Sinf rahbariga
> _«Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin oli…»_
> ↪️ **Javob:** Va alaykum assalom! Albatta, 3-darsdan so'ng olib ketishingiz mumkin. Sinf rahbari.
>
> `[👩‍🏫 Sinf rahbariga]` `[🏢 Ma'muriyatga]`
> `[🏠 Bosh menyu]`

### 11.3. Maktab javob berdi

🏫 **Maktab (admin panel):** «Murojaatlar» sahifasida javob yozdi

_(bir necha soniyadan so'ng botdan avtomatik xabar keldi)_

🤖 **Bot:**

> 💬 **Maktabdan javob**
>> Assalomu alaykum! Ertaga farzandim tish shifokoriga boradi, 3-darsdan keyin olib ketsam bo'ladimi?
> Va alaykum assalom! Albatta, 3-darsdan so'ng olib ketishingiz mumkin. Sinf rahbari.
>
> 🏫 _1-maktab_

## 12. Sababli ariza

### 12.1. Bo'lim — sana tanlash

👤 **Ota-ona:** tugmani bosdi `[🤒 Sababli ariza]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 🤒 Sababli ariza
>
> **Sababli ariza**
> Farzandingiz maktabga kela olmasa, oldindan xabar bering.
>
> **Qaysi kundan boshlab?**
>
> **Oldingi arizalar**
> ✅ Qabul qilindi · 1-oktabr – 2-oktabr · 🤒 Kasallik
> ✅ Qabul qilindi · 1-oktabr – 2-oktabr · 🤒 Kasallik
> ✅ Qabul qilindi · 1-oktabr – 2-oktabr · 🤒 Kasallik
>
> `[Bugun]` `[Ertaga]`
> `[2-oktabr, Ju]` `[3-oktabr, Sh]`
> `[5-oktabr, Du]` `[6-oktabr, Se]`
> `[🏠 Bosh menyu]`

### 12.2. Necha kun?

👤 **Ota-ona:** tugmani bosdi `[Ertaga]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 🤒 Sababli ariza
>
> **Necha kun?**
> 📅 Boshlanish: 1-oktabr, payshanba
>
> `[1 kun]` `[2 kun]` `[3 kun]`
> `[5 kun]` `[7 kun]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 12.3. Sabab

👤 **Ota-ona:** tugmani bosdi `[2 kun]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 🤒 Sababli ariza
>
> **Sabab nima?**
>
> `[🤒 Kasallik]`
> `[👨‍👩‍👧 Oilaviy sabab]`
> `[📝 Boshqa]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 12.4. Izoh

👤 **Ota-ona:** tugmani bosdi `[🤒 Kasallik]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 🤒 Sababli ariza
>
> ✍️ Qisqa izoh yozing (masalan: «isitmasi bor»), yoki «O'tkazib yuborish»ni bosing.
>
> `[⏭ O'tkazib yuborish]`
> `[✖️ Bekor qilish]`

👤 **Ota-ona:** «Isitmasi bor, shifokor 2 kun uyda dam olishni buyurdi»

🤖 **Bot:**

> 🏠 › 🤒 Sababli ariza
>
> 📎 Shifokor ma'lumotnomasi bo'lsa, rasmini yuboring. Bo'lmasa — «O'tkazib yuborish».
>
> `[⏭ O'tkazib yuborish]`
> `[✖️ Bekor qilish]`

### 12.5. Ma'lumotnoma rasmi

👤 **Ota-ona:** shifokor ma'lumotnomasining rasmini yubordi 📎

🤖 **Bot:**

> 🏠 › 🤒 Sababli ariza
>
> **Arizani tekshiring:**
> 👤 Elyor Berdiyev
> 📅 1-oktabr, payshanba – 2-oktabr, juma (2 kun)
> 📝 🤒 Kasallik
> 💬 Isitmasi bor, shifokor 2 kun uyda dam olishni buyurdi
> 📎 Rasm biriktirildi
>
> Yuboraymi?
>
> `[📨 Yuborish]`
> `[✖️ Bekor qilish]`

### 12.6. Tasdiqlash va yuborish

👤 **Ota-ona:** tugmani bosdi `[📨 Yuborish]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> ✅ Ariza yuborildi! Sinf rahbari ko'rib chiqishi bilan xabar beraman.
>
> 🏠 › 🤒 Sababli ariza
>
> **Sababli ariza**
> Farzandingiz maktabga kela olmasa, oldindan xabar bering.
>
> **Qaysi kundan boshlab?**
>
> **Oldingi arizalar**
> ⏳ Ko'rib chiqilmoqda · 1-oktabr – 2-oktabr · 🤒 Kasallik
> ✅ Qabul qilindi · 1-oktabr – 2-oktabr · 🤒 Kasallik
> ✅ Qabul qilindi · 1-oktabr – 2-oktabr · 🤒 Kasallik
> ✅ Qabul qilindi · 1-oktabr – 2-oktabr · 🤒 Kasallik
>
> `[Bugun]` `[Ertaga]`
> `[2-oktabr, Ju]` `[3-oktabr, Sh]`
> `[5-oktabr, Du]` `[6-oktabr, Se]`
> `[🏠 Bosh menyu]`

### 12.7. Sinf rahbari arizani tasdiqladi

🏫 **Maktab (admin panel):** «Sababli arizalar» sahifasida «Tasdiqlash»ni bosdi — o'sha kunlarning davomati avtomatik «sababli» bo'ldi

> _Admin panel: 5 ta dars «sababli» (EXCUSED) deb belgilandi._

_(botdan avtomatik xabar)_

🤖 **Bot:**

> ✅ **Arizangiz qabul qilindi**
> 👤 Elyor Berdiyev · 01.10.2026 – 02.10.2026
> Bu kunlardagi darslar sababli deb belgilandi. Tezroq sog'ayib keting! 🌷
>
> 🏫 _1-maktab_

## 13. Maktab haqida

### 13.1. Kontaktlar va qo'ng'iroq jadvali

👤 **Ota-ona:** tugmani bosdi `[🏫 Maktab haqida]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 🏫 Maktab
>
> 🏫 **1-maktab**
> 📍 Toshkent sh., Chilonzor tumani, Bunyodkor ko'chasi 12
>
> 🔔 **Qo'ng'iroq jadvali**
> 1-dars · 08:30–09:15
> 2-dars · 09:25–10:10
> 3-dars · 10:20–11:05
> 4-dars · 11:15–12:00
> 5-dars · 12:40–13:25
> 6-dars · 13:35–14:20
>
> `[💬 Maktabga yozish]`
> `[🏠 Bosh menyu]`

## 14. Sozlamalar (3 til)

### 14.1. Xabarlar

👤 **Ota-ona:** tugmani bosdi `[⚙️ Sozlamalar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⚙️ Sozlamalar
>
> **Sozlamalar**
> Qaysi xabarlar kelsin? Bosib yoqing yoki o'chiring.
>
> ✅ Kelmadi / kechikdi
> ✅ Baholar
> ✅ Past baho
> ✅ E'lonlar
> ✅ Ertangi jadval
> ✅ Haftalik hisobot
> ✅ Tadbir eslatmasi
>
> `[✅ Kelmadi / kechikdi]` `[✅ Baholar]`
> `[✅ Past baho]` `[✅ E'lonlar]`
> `[✅ Ertangi jadval]` `[✅ Haftalik hisobot]`
> `[✅ Tadbir eslatmasi]`
> `[⏰ Vaqtlar]` `[🌐 Til / Язык]`
> `[🏠 Bosh menyu]`

### 14.2. «Haftalik hisobot»ni o'chirish

👤 **Ota-ona:** tugmani bosdi `[✅ Haftalik hisobot]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⚙️ Sozlamalar
>
> **Sozlamalar**
> Qaysi xabarlar kelsin? Bosib yoqing yoki o'chiring.
>
> ✅ Kelmadi / kechikdi
> ✅ Baholar
> ✅ Past baho
> ✅ E'lonlar
> ✅ Ertangi jadval
> ▫️ Haftalik hisobot
> ✅ Tadbir eslatmasi
>
> `[✅ Kelmadi / kechikdi]` `[✅ Baholar]`
> `[✅ Past baho]` `[✅ E'lonlar]`
> `[✅ Ertangi jadval]` `[▫️ Haftalik hisobot]`
> `[✅ Tadbir eslatmasi]`
> `[⏰ Vaqtlar]` `[🌐 Til / Язык]`
> `[🏠 Bosh menyu]`

👤 **Ota-ona:** tugmani bosdi `[▫️ Haftalik hisobot]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⚙️ Sozlamalar
>
> **Sozlamalar**
> Qaysi xabarlar kelsin? Bosib yoqing yoki o'chiring.
>
> ✅ Kelmadi / kechikdi
> ✅ Baholar
> ✅ Past baho
> ✅ E'lonlar
> ✅ Ertangi jadval
> ✅ Haftalik hisobot
> ✅ Tadbir eslatmasi
>
> `[✅ Kelmadi / kechikdi]` `[✅ Baholar]`
> `[✅ Past baho]` `[✅ E'lonlar]`
> `[✅ Ertangi jadval]` `[✅ Haftalik hisobot]`
> `[✅ Tadbir eslatmasi]`
> `[⏰ Vaqtlar]` `[🌐 Til / Язык]`
> `[🏠 Bosh menyu]`

### 14.3. Vaqtlar

👤 **Ota-ona:** tugmani bosdi `[⏰ Vaqtlar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⚙️ Sozlamalar › ⏰ Vaqtlar
>
> **Vaqtlar**
> 📅 Ertangi jadval: **19:00** da
> 🌙 Tinch soatlar: **maktab bo'yicha (22:00–07:00)**
>> Tinch soatlarda yuzaga kelgan xabarlar ertalab yuboriladi
>
> `[📅 Ertangi jadval vaqti:]`
> `[18:00]` `[• 19:00 •]` `[20:00]` `[21:00]`
> `[🌙 Tinch soatlar:]`
> `[21:00–07:00]` `[22:00–07:00]` `[23:00–07:00]`
> `[🏫 Maktabniki]` `[🔔 O'chirish]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

👤 **Ota-ona:** tugmani bosdi `[20:00]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⚙️ Sozlamalar › ⏰ Vaqtlar
>
> **Vaqtlar**
> 📅 Ertangi jadval: **20:00** da
> 🌙 Tinch soatlar: **maktab bo'yicha (22:00–07:00)**
>> Tinch soatlarda yuzaga kelgan xabarlar ertalab yuboriladi
>
> `[📅 Ertangi jadval vaqti:]`
> `[18:00]` `[19:00]` `[• 20:00 •]` `[21:00]`
> `[🌙 Tinch soatlar:]`
> `[21:00–07:00]` `[22:00–07:00]` `[23:00–07:00]`
> `[🏫 Maktabniki]` `[🔔 O'chirish]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

👤 **Ota-ona:** tugmani bosdi `[19:00]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⚙️ Sozlamalar › ⏰ Vaqtlar
>
> **Vaqtlar**
> 📅 Ertangi jadval: **19:00** da
> 🌙 Tinch soatlar: **maktab bo'yicha (22:00–07:00)**
>> Tinch soatlarda yuzaga kelgan xabarlar ertalab yuboriladi
>
> `[📅 Ertangi jadval vaqti:]`
> `[18:00]` `[• 19:00 •]` `[20:00]` `[21:00]`
> `[🌙 Tinch soatlar:]`
> `[21:00–07:00]` `[22:00–07:00]` `[23:00–07:00]`
> `[🏫 Maktabniki]` `[🔔 O'chirish]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 14.4. Til

👤 **Ota-ona:** tugmani bosdi `[🌐 Til / Язык]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ⚙️ Sozlamalar › 🌐 Til / Язык
>
> **Tilni tanlang** · Тилни танланг · Выберите язык
>
> `[✅ 🇺🇿 O'zbekcha]`
> `[🇺🇿 Ўзбекча]`
> `[🇷🇺 Русский]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 14.5. Ўзбекча (кирилл)

👤 **Ota-ona:** tugmani bosdi `[🇺🇿 Ўзбекча]`

🤖 **Bot:**

> ✅ Тил ўзгартирилди
>
> ⌨️ _Pastki klaviatura:_
> `📅 Дарс жадвали` `✅ Давомат`
> `📘 Баҳолар` `📊 Ҳисобот`
> `📢 Эълонлар` `🗓 Тадбирлар`
> `👩‍🏫 Ўқитувчилар` `💬 Мактабга ёзиш`
> `⚙️ Созламалар` `👨‍👩‍👧 Фарзандларим`

🤖 **Bot:**

> 🏠 › ⚙️ Созламалар
>
> **Созламалар**
> Қайси хабарлар келсин? Босиб ёқинг ёки ўчиринг.
>
> ✅ Келмади / кечикди
> ✅ Баҳолар
> ✅ Паст баҳо
> ✅ Эълонлар
> ✅ Эртанги жадвал
> ✅ Ҳафталик ҳисобот
> ✅ Тадбир эслатмаси
>
> `[✅ Келмади / кечикди]` `[✅ Баҳолар]`
> `[✅ Паст баҳо]` `[✅ Эълонлар]`
> `[✅ Эртанги жадвал]` `[✅ Ҳафталик ҳисобот]`
> `[✅ Тадбир эслатмаси]`
> `[⏰ Вақтлар]` `[🌐 Тил / Язык]`
> `[🏠 Бош меню]`

👤 **Ota-ona:** tugmani bosdi `[🏠 Бош меню]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 **Бош меню** · 30-сентябр, чоршанба
>
> 👤 **Elyor Berdiyev** · 1-A синф
> 👩‍🏫 Синф раҳбари: Zebo Ne'matova
>
> **Бугун:**
> 📚 5 та дарс · 08:30–13:25
> 🔴 2 та дарсга келмади
> 📘 Бугунги баҳолар: 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🟢 **5** Ona tili
> 📢 Янги эълонлар: **6**
> 🗓 Яқин тадбир: **O'qituvchilar va murabbiylar kuni** — 1-октябр
>
> Бўлимни танланг 👇
>
> `[📱 Кундаликни очиш ↗]`
> `[📅 Дарс жадвали]` `[✅ Давомат]`
> `[📘 Баҳолар]` `[📊 Ҳисобот]`
> `[⭐ Хулқ]` `[📢 Эълонлар]`
> `[🗓 Тадбирлар]` `[👩‍🏫 Ўқитувчилар]`
> `[💬 Мактабга ёзиш]` `[🤒 Сабабли ариза]`
> `[🏫 Мактаб ҳақида]` `[⚙️ Созламалар]`
> `[👨‍👩‍👧 Фарзандларим]`

### 14.6. Русский

👤 **Ota-ona:** tugmani bosdi `[🇷🇺 Русский]`

🤖 **Bot:**

> ✅ Язык изменён
>
> ⌨️ _Pastki klaviatura:_
> `📅 Расписание` `✅ Посещаемость`
> `📘 Оценки` `📊 Отчёт`
> `📢 Объявления` `🗓 События`
> `👩‍🏫 Учителя` `💬 Написать в школу`
> `⚙️ Настройки` `👨‍👩‍👧 Мои дети`

🤖 **Bot:**

> 🏠 › ⚙️ Настройки
>
> **Настройки**
> Какие сообщения получать? Нажмите, чтобы включить или выключить.
>
> ✅ Пропуск / опоздание
> ✅ Оценки
> ✅ Низкая оценка
> ✅ Объявления
> ✅ Расписание на завтра
> ✅ Еженедельный отчёт
> ✅ Напоминание о событиях
>
> `[✅ Пропуск / опоздание]` `[✅ Оценки]`
> `[✅ Низкая оценка]` `[✅ Объявления]`
> `[✅ Расписание на завтра]` `[✅ Еженедельный отчёт]`
> `[✅ Напоминание о событиях]`
> `[⏰ Время]` `[🌐 Til / Язык]`
> `[🏠 Главное меню]`

👤 **Ota-ona:** tugmani bosdi `[✅ Посещаемость]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › ✅ Посещаемость › Сентябрь 2026
>
> **Сентябрь 2026**
>
> 📚 Всего уроков: **28**
> ✅ Присутствовал(а): **21**
> 🟡 Опоздания: **2**
> 🔴 Пропуски: **4**
> 🔵 По уважительной: **1**
>
> ▰▰▰▰▰▰▰▰▱▱ **82.1%**
>> Среднее по классу: 94.2% — ниже среднего, обратите внимание
>
> `[◀️ Август]` `[Сентябрь 2026]` `[·]`
> `[🗓 Календарь]` `[📋 Подробно]`
> `[📚 По предметам]` `[🖼 В виде картинки]`
> `[• 🗓 Месяц •]` `[🏷 Четверть]` `[🎓 Учебный год]`
> `[🏠 Главное меню]`

### 14.7. O'zbekcha (lotin) ga qaytish

👤 **Ota-ona:** tugmani bosdi `[🇺🇿 O'zbekcha]`

🤖 **Bot:**

> ✅ Til o'zgartirildi
>
> ⌨️ _Pastki klaviatura:_
> `📅 Dars jadvali` `✅ Davomat`
> `📘 Baholar` `📊 Hisobot`
> `📢 E'lonlar` `🗓 Tadbirlar`
> `👩‍🏫 O'qituvchilar` `💬 Maktabga yozish`
> `⚙️ Sozlamalar` `👨‍👩‍👧 Farzandlarim`

🤖 **Bot:**

> 🏠 › ⚙️ Sozlamalar
>
> **Sozlamalar**
> Qaysi xabarlar kelsin? Bosib yoqing yoki o'chiring.
>
> ✅ Kelmadi / kechikdi
> ✅ Baholar
> ✅ Past baho
> ✅ E'lonlar
> ✅ Ertangi jadval
> ✅ Haftalik hisobot
> ✅ Tadbir eslatmasi
>
> `[✅ Kelmadi / kechikdi]` `[✅ Baholar]`
> `[✅ Past baho]` `[✅ E'lonlar]`
> `[✅ Ertangi jadval]` `[✅ Haftalik hisobot]`
> `[✅ Tadbir eslatmasi]`
> `[⏰ Vaqtlar]` `[🌐 Til / Язык]`
> `[🏠 Bosh menyu]`

## 15. Farzandlarim

### 15.1. Ikkinchi farzandni qo'shish

👤 **Ota-ona:** tugmani bosdi `[👨‍👩‍👧 Farzandlarim]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 👨‍👩‍👧 Farzandlarim
>
> **Farzandlarim**
>
> ✅ **Elyor Berdiyev** — 1-A sinf
>
>> Farzandni tanlang — barcha bo'limlar shu farzand bo'yicha ko'rsatiladi
>
> `[✅ Elyor · 1-A]` `[🔗 Uzish]`
> `[➕ Farzand qo'shish]`
> `[🏠 Bosh menyu]`

👤 **Ota-ona:** tugmani bosdi `[➕ Farzand qo'shish]`

🤖 **Bot:**

> ➕ Farzand qo'shish
>
> ⌨️ _Pastki klaviatura:_
> `📱 Raqamni ulashish`

🤖 **Bot:**

> 🏠 › 👨‍👩‍👧 Farzandlarim › ➕ Farzand qo'shish
>
> **Farzand qo'shish**
> Sinf rahbari bergan **kodni** shu yerga yozing yoki QR kodni skanerlang.
> Raqamingiz maktabda yozilgan bo'lsa — «📱 Raqamni ulashish» tugmasini bosing.
>
> `[✖️ Bekor qilish]`

👤 **Ota-ona:** «zvNWFkt8qu»

🤖 **Bot:**

> ✅ **Tabriklaymiz!** Endi **Jasur Berdiyev** (1-A sinf) haqida hamma narsa shu yerda.
>
> **3 qadamda tanishib oling:**
> 1️⃣ Pastdagi tugmalar — asosiy bo'limlar: jadval, davomat, baholar va boshqalar
> 2️⃣ Muhim voqealar — kelmaslik, yangi baho, e'lon — o'zi xabar bo'lib keladi
> 3️⃣ «⚙️ Sozlamalar»da qaysi xabarlar va qachon kelishini tanlaysiz
>
> ⌨️ _Pastki klaviatura:_
> `📅 Dars jadvali` `✅ Davomat`
> `📘 Baholar` `📊 Hisobot`
> `📢 E'lonlar` `🗓 Tadbirlar`
> `👩‍🏫 O'qituvchilar` `💬 Maktabga yozish`
> `⚙️ Sozlamalar` `👨‍👩‍👧 Farzandlarim`

🤖 **Bot:**

> 🏠 **Bosh menyu** · 30-sentabr, chorshanba
>
> 👤 **Jasur Berdiyev** · 1-A sinf
> 👩‍🏫 Sinf rahbari: Zebo Ne'matova
>
> **Bugun:**
> 📚 5 ta dars · 08:30–13:25
> 🟡 2 ta darsga kechikdi
> 📘 Bugungi baholar: 🟢 **5** Tarbiya, 🔵 **4** Musiqa
> 📢 Yangi e'lonlar: **6**
> 🗓 Yaqin tadbir: **O'qituvchilar va murabbiylar kuni** — 1-oktabr
>
> Bo'limni tanlang 👇
>
> `[📱 Kundalikni ochish ↗]`
> `[📅 Dars jadvali]` `[✅ Davomat]`
> `[📘 Baholar]` `[📊 Hisobot]`
> `[⭐ Xulq]` `[📢 E'lonlar]`
> `[🗓 Tadbirlar]` `[👩‍🏫 O'qituvchilar]`
> `[💬 Maktabga yozish]` `[🤒 Sababli ariza]`
> `[🏫 Maktab haqida]` `[⚙️ Sozlamalar]`
> `[👨‍👩‍👧 Farzandlarim]`
> `[🔄 Farzandni almashtirish]`

### 15.2. Ro'yxat va farzandni almashtirish

👤 **Ota-ona:** tugmani bosdi `[👨‍👩‍👧 Farzandlarim]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 👨‍👩‍👧 Farzandlarim
>
> **Farzandlarim**
>
> **Elyor Berdiyev** — 1-A sinf
> ✅ **Jasur Berdiyev** — 1-A sinf
>
>> Farzandni tanlang — barcha bo'limlar shu farzand bo'yicha ko'rsatiladi
>
> `[👤 Elyor · 1-A]` `[🔗 Uzish]`
> `[✅ Jasur · 1-A]` `[🔗 Uzish]`
> `[➕ Farzand qo'shish]`
> `[🏠 Bosh menyu]`

👤 **Ota-ona:** tugmani bosdi `[👤 Elyor · 1-A]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 **Bosh menyu** · 30-sentabr, chorshanba
>
> 👤 **Elyor Berdiyev** · 1-A sinf
> 👩‍🏫 Sinf rahbari: Zebo Ne'matova
>
> **Bugun:**
> 📚 5 ta dars · 08:30–13:25
> 🔴 2 ta darsga kelmadi
> 📘 Bugungi baholar: 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🔴 **2** O'zbek adabiyoti, 🟢 **5** Ona tili, 🟢 **5** Ona tili
> 📢 Yangi e'lonlar: **6**
> 🗓 Yaqin tadbir: **O'qituvchilar va murabbiylar kuni** — 1-oktabr
>
> Bo'limni tanlang 👇
>
> `[📱 Kundalikni ochish ↗]`
> `[📅 Dars jadvali]` `[✅ Davomat]`
> `[📘 Baholar]` `[📊 Hisobot]`
> `[⭐ Xulq]` `[📢 E'lonlar]`
> `[🗓 Tadbirlar]` `[👩‍🏫 O'qituvchilar]`
> `[💬 Maktabga yozish]` `[🤒 Sababli ariza]`
> `[🏫 Maktab haqida]` `[⚙️ Sozlamalar]`
> `[👨‍👩‍👧 Farzandlarim]`
> `[🔄 Farzandni almashtirish]`

### 15.3. Har sahifada tanlangan farzand va «🔄 Farzandni almashtirish»

👤 **Ota-ona:** tugmani bosdi `[📘 Baholar]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 📘 Baholar › 🕘 So'nggi
> 👤 **Elyor Berdiyev** · 1-A sinf
>
> **So'nggi baholar**
>
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🔴 **2** · O'zbek adabiyoti · 30-sentabr
>       _joriy_ — «Uy vazifasi bajarilmagan»
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🔴 **2** · O'zbek adabiyoti · 30-sentabr
>       _joriy_ — «Uy vazifasi bajarilmagan»
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🔴 **2** · O'zbek adabiyoti · 30-sentabr
>       _joriy_ — «Uy vazifasi bajarilmagan»
> 🟢 **5** · Ona tili · 30-sentabr
>       _joriy_
> 🟢 **5** · Tasviriy san'at · 29-sentabr
>       _joriy_
>
> `[• 🕘 So'nggi •]` `[📚 Fanlar]` `[🏅 Chorak]`
> `[·]` `[1/4]` `[▶️]`
> `[🔄 Farzandni almashtirish]`
> `[🏠 Bosh menyu]`

👤 **Ota-ona:** tugmani bosdi `[🔄 Farzandni almashtirish]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 👨‍👩‍👧 Farzandlarim
>
> **Farzandlarim**
>
> ✅ **Elyor Berdiyev** — 1-A sinf
> **Jasur Berdiyev** — 1-A sinf
>
>> Farzandni tanlang — barcha bo'limlar shu farzand bo'yicha ko'rsatiladi
>
> `[✅ Elyor · 1-A]` `[🔗 Uzish]`
> `[👤 Jasur · 1-A]` `[🔗 Uzish]`
> `[➕ Farzand qo'shish]`
> `[⬅️ Orqaga]` `[🏠 Bosh menyu]`

### 15.4. Farzandni uzish (tasdiqlash bilan)

👤 **Ota-ona:** tugmani bosdi `[🔗 Uzish]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 👨‍👩‍👧 Farzandlarim
>
> **Jasur Berdiyev**ni uzasizmi?
> Bu farzand haqida xabarlar kelmay qoladi.
>
> `[✅ Ha]` `[✖️ Yo'q]`

👤 **Ota-ona:** tugmani bosdi `[✅ Ha]`

🤖 **Bot:**

> ✏️ _(shu xabar tahrirlandi)_
>
> 🏠 › 👨‍👩‍👧 Farzandlarim
>
> **Farzandlarim**
>
> ✅ **Elyor Berdiyev** — 1-A sinf
>
>> Farzandni tanlang — barcha bo'limlar shu farzand bo'yicha ko'rsatiladi
>
> `[✅ Elyor · 1-A]` `[🔗 Uzish]`
> `[➕ Farzand qo'shish]`
> `[🏠 Bosh menyu]`

## 16. Xavfsizlik

### 16.1. Boshqa o'quvchining ID'si bilan soxta callback

👤 **Ota-ona:** qo'lda yasalgan callback yubordi: `att:s:27` (o'ziga bog'lanmagan o'quvchi)

> 🔒 _Bot hech qanday ma'lumot qaytarmadi — faqat «🔒 Bu ma'lumot sizga ochiq emas» ogohlantirishi (answerCallbackQuery) ko'rsatildi._

### 16.2. Tushunarsiz xabar va /yordam

👤 **Ota-ona:** «salom»

🤖 **Bot:**

> 🤔 Tushunmadim. Pastdagi tugmalardan foydalaning yoki /yordam yozing.
>
> ⌨️ _Pastki klaviatura:_
> `📅 Dars jadvali` `✅ Davomat`
> `📘 Baholar` `📊 Hisobot`
> `📢 E'lonlar` `🗓 Tadbirlar`
> `👩‍🏫 O'qituvchilar` `💬 Maktabga yozish`
> `⚙️ Sozlamalar` `👨‍👩‍👧 Farzandlarim`

👤 **Ota-ona:** `/yordam`

🤖 **Bot:**

> ℹ️ **Yordam**
> /menu — bosh menyu
> /jadval — dars jadvali
> /davomat — davomat
> /baholar — baholar
> /stop — xabarlarni to'xtatish
>
> Pastdagi tugmalar orqali istalgan bo'limga o'ting 👇
>
> ⌨️ _Pastki klaviatura:_
> `📅 Dars jadvali` `✅ Davomat`
> `📘 Baholar` `📊 Hisobot`
> `📢 E'lonlar` `🗓 Tadbirlar`
> `👩‍🏫 O'qituvchilar` `💬 Maktabga yozish`
> `⚙️ Sozlamalar` `👨‍👩‍👧 Farzandlarim`

## 17. Avtomatik xabarlar

Quyidagi xabarlarni ota-ona hech narsa bosmasdan oladi — ular outbox (NotificationLog) orqali navbatga qo'yilib, yuboruvchi tomonidan jo'natiladi.

### 17.1. Farzand darsga kelmadi

🏫 **Maktab (admin panel):** o'qituvchi O'zbek adabiyoti darsida davomat oldi: Elyor Berdiyev — «Kelmadi»

### 17.2. Farzand kechikdi

🏫 **Maktab (admin panel):** Musiqa darsida — «Kechikdi»

### 17.3. Yangi baho

🏫 **Maktab (admin panel):** Ona tili fanidan 5 baho qo'ydi

🤖 **Bot:**

> 📘 **Elyor Berdiyev** Ona tili fanidan **5** baho oldi (joriy baho, 30.09.2026).
>
> 🏫 _1-maktab_

### 17.4. Past baho — alohida, mehribon ohangdagi xabar

🏫 **Maktab (admin panel):** O'zbek adabiyoti fanidan 2 baho qo'ydi

🤖 **Bot:**

> 💛 **Elyor Berdiyev** O'zbek adabiyoti fanidan **2** baho oldi (30.09.2026).
>> Xavotir olmang — har bir bola ba'zan qiynaladi. Farzandingiz bilan mehr bilan gaplashib, qaysi mavzu qiyin bo'lganini so'rang. Kerak bo'lsa, o'qituvchi bilan maslahatlashing 🤝
>
> 🏫 _1-maktab_
>
> `[✍️ O'qituvchiga yozish]`

### 17.5. Maktab e'loni

🏫 **Maktab (admin panel):** «Hamma uchun» e'lon joyladi

🤖 **Bot:**

> 📢 **Maktab e'loni**: Kuzgi sport musobaqasi
> Shanba kuni soat 10:00 da maktab stadionida «Sog'lom avlod» sport musobaqasi bo'lib o'tadi. Farzandingizga sport kiyimini olib kelishni unutmang!
>
> 🏫 _1-maktab_

### 17.6. Ertangi dars jadvali, haftalik hisobot va tadbir eslatmasi

🏫 **Maktab (admin panel):** rejalashtirilgan vazifalar ishga tushdi (odatda: har kuni 19:00, shanba 18:00, tadbirdan 1 kun oldin 18:00)

🤖 **Bot:**

> 🔔 **Eslatma:** ertaga — 🎉 **O'qituvchilar va murabbiylar kuni**
> 📅 1-oktabr, payshanba
>
> 🏫 _1-maktab_

### 17.7. Ota-onalarga umumiy xabar (broadcast)

🏫 **Maktab (admin panel):** «Ota-onalarga xabar» sahifasidan butun maktabga xabar yubordi

🤖 **Bot:**

> 📣 **Maktabdan xabar**
>
> Hurmatli ota-onalar! 7-oktabr kuni soat 18:00 da maktab majlislar zalida umumiy ota-onalar yig'ilishi bo'lib o'tadi. Ishtirokingizni so'raymiz.
>
> 🏫 _1-maktab_

