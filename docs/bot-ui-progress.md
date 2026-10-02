# Bot UI (kartochka ko'rinishi) — bajarilish ro'yxati

## 1. Asosiy menyu — pastki doimiy tugmalar
- [ ] 1.1 Bog'langan zahoti, har `/start` va `/menu`da ReplyKeyboardMarkup (`resize_keyboard`, `is_persistent`, `input_field_placeholder="Bo'limni tanlang 👇"`)
- [ ] 1.2 2 ustunli tartib: Jadval|Davomat, Baholar|Hisobot, E'lonlar|Tadbirlar, O'qituvchilar|Maktabga yozish, Sababli ariza|Sozlamalar, Farzandlarim (to'liq qator)
- [ ] 1.3 Mini App sozlangan bo'lsa — eng tepada to'liq qatorda «📱 Kundalikni ochish» (web_app)
- [ ] 1.4 Tugma matni katta-kichik harf va emojisiz yozilsa ham taniladi
- [ ] 1.5 Bot API `style` maydoni (9.4): asosiy bo'limlar — `primary`, «Sababli ariza» — `danger`, tasdiqlovchi tugmalar — `success`
- [ ] 1.6 Bot profili: setMyCommands, setMyDescription, setMyShortDescription, Menu Button; buyruqlar ro'yxati faqat yordam sahifasida

## 2. Har bir sahifa — kartochka
- [ ] 2.1 Bo'lim ochilganda banner rasm + caption + inline tugmalar (sendPhoto)
- [ ] 2.2 Sahifalar orasida editMessageMedia / editMessageCaption, yangi xabarlar ko'paymaydi
- [ ] 2.3 Java2D bannerlar 1280×640: bo'lim rangi, katta ikonka, sarlavha, farzand ismi va sinfi, asosiy raqamlar; gradient, yumaloq kartochkalar, Inter shrifti
- [ ] 2.4 Bo'lim ranglari: Jadval #4F46E5, Davomat #10B981, Baholar #0EA5E9, Hisobot #7C3AED, E'lonlar #EC4899, Tadbirlar #F59E0B, O'qituvchilar #06B6D4, Sababli ariza #EF4444, Sozlamalar #64748B
- [ ] 2.5 Bannerlar keshlanadi
- [ ] 2.6 Davomat banneri: oy nomi, katta foiz, rangli kalendar kataklari va legenda
- [ ] 2.7 Baholar banneri: fanlar bo'yicha o'rtacha baholarning rangli ustunli grafigi
- [ ] 2.8 Jadval banneri: ertangi darslar ro'yxati (vaqt, fan, xona) jadval ko'rinishida
- [ ] 2.9 Caption: qalin sarlavha, emoji, progress bar (▰▰▰▰▰▰▰▱▱▱ 72%), qisqa qatorlar
- [ ] 2.10 Inline tugmalar: ichki tablar, «🔄 Farzandni almashtirish» (bir nechta farzand bo'lsa), «🏠 Bosh menyu»

## 3. Bosh sahifa
- [ ] 3.1 «Xush kelibsiz» banneri: maktab nomi, farzand ismi, sinfi, bugungi sana
- [ ] 3.2 Caption: ✅ davomat holati, 📘 bugungi baholar, 📢 yangi e'lonlar, 📅 ertangi birinchi dars; pastki menyu klaviaturasi

## 4. Kutib olish (bog'lanmagan)
- [ ] 4.1 Kutib olish banneri va bot imkoniyatlari 4 qatorda (emoji bilan)
- [ ] 4.2 Ikki katta tugma: «📱 Telefon raqamni ulashish» (request_contact) va «🔑 Kod kiritish»

## 5. Mavjud funksionallik
- [ ] 5.1 Avtomatik xabarlar (kelmadi, kechikdi, baho, e'lon): emoji sarlavha va «Batafsil» inline tugmasi
- [ ] 5.2 Xavfsizlik tekshiruvlari saqlangan (begona o'quvchi → ma'lumot yo'q)

## TEKSHIRUV
- [ ] T1 `./gradlew test` — barcha testlar; yangi testlar: menyu matnini tanish, har bo'lim bannerini yaratish (o'lcham 1280×640)
- [ ] T2 Barcha bannerlar seed ma'lumot bilan `docs/images/bot/`ga saqlandi va ko'zdan kechirildi
- [ ] T3 `docs/bot-demo.md` yangilandi: har bo'lim uchun banner, caption, tugmalar
- [ ] T4 Haqiqiy botda `/start` → pastki menyu va bitta bo'lim (token sozlangan bo'lsa)
- [ ] T5 Yakuniy hisobot: bajarilgan/jami, rasmlar ro'yxati, commitlar
