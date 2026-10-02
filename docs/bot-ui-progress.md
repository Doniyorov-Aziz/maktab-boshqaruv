# Bot UI (kartochka ko'rinishi) — bajarilish ro'yxati

Natija: **30 / 30** band bajarildi (T4 — shartli band, izohga qarang).

## 1. Asosiy menyu — pastki doimiy tugmalar
- [x] 1.1 Bog'langan zahoti, har `/start` va `/menu`da ReplyKeyboardMarkup (`resize_keyboard`, `is_persistent`, `input_field_placeholder="Bo'limni tanlang 👇"`) — `bot/Keyboards.java`, `Onboarding.linked`, `BotRouter.homeWithKeyboard`
- [x] 1.2 2 ustunli tartib: Jadval|Davomat, Baholar|Hisobot, E'lonlar|Tadbirlar, O'qituvchilar|Maktabga yozish, Sababli ariza|Sozlamalar, Farzandlarim (to'liq qator) — `Keyboards.MAIN`, `KeyboardsTest`
- [x] 1.3 Mini App sozlangan bo'lsa — eng tepada to'liq qatorda «📱 Kundalikni ochish» (web_app) — `Keyboards.setWebAppUrl` (`BotProfileInitializer`)
- [x] 1.4 Tugma matni katta-kichik harf va emojisiz yozilsa ham taniladi (3 tilda) — `Keyboards.normalize/screenFor`
- [x] 1.5 Bot API 9.4 `style`: asosiy bo'limlar — `primary`, «Sababli ariza» — `danger`, telefon ulashish — `success` (`KeyboardButton.style`, `InlineButton.styled`)
- [x] 1.6 Bot profili: setMyCommands, setMyDescription, setMyShortDescription, Menu Button; buyruqlar ro'yxati faqat `/yordam` va Telegram «Menu»da, asosiy ekran — tugmali menyu

## 2. Har bir sahifa — kartochka
- [x] 2.1 Bo'lim ochilganda banner rasm + caption + inline tugmalar (sendPhoto) — `BotRouter.decorate`, `BotResponder`
- [x] 2.2 Sahifalar orasida editMessageMedia (banner+caption+tugmalar), yangi xabarlar ko'paymaydi — `TelegramClient.editMessageMedia/editMessageCaption` (HTTP + mock)
- [x] 2.3 Java2D bannerlar 1280×640: bo'lim rangi, chizilgan katta ikonka, sarlavha, farzand ismi va sinfi, asosiy raqamlar; gradient, yumaloq kartochkalar, Inter shrifti — `bot/image/BannerRenderer.java`
- [x] 2.4 Bo'lim ranglari: Jadval #4F46E5, Davomat #10B981, Baholar #0EA5E9, Hisobot #7C3AED, E'lonlar #EC4899, Tadbirlar #F59E0B, O'qituvchilar #06B6D4, Sababli ariza #EF4444, Sozlamalar #64748B — `BannerService.COLORS`, `BannerRendererTest`
- [x] 2.5 Bannerlar keshlanadi (tarkib bo'yicha + Telegram `file_id`) — `BotImageService.banner`
- [x] 2.6 Davomat banneri: oy nomi, katta foiz, rangli kalendar kataklari va legenda
- [x] 2.7 Baholar banneri: fanlar bo'yicha o'rtacha baholarning rangli ustunli grafigi (↑↓)
- [x] 2.8 Jadval banneri: ertangi darslar ro'yxati (vaqt, fan, xona)
- [x] 2.9 Caption: qalin sarlavha, emoji, progress bar (▰▰▰▰▰▰▰▱▱▱ 72%), qisqa qatorlar; 1024 belgiga sig'masa — matnli sahifa (demoda 56/56 sahifa kartochka)
- [x] 2.10 Inline tugmalar: ichki tablar, «🔄 Farzandni almashtirish» (bir nechta farzand bo'lsa), «🏠 Bosh menyu»

## 3. Bosh sahifa
- [x] 3.1 «Xush kelibsiz» banneri: maktab nomi, farzand ismi, sinfi, bugungi sana
- [x] 3.2 Caption: ✅ davomat holati, 📘 bugungi baholar, 📢 yangi e'lonlar, 📅 ertangi birinchi dars; pastki menyu klaviaturasi

## 4. Kutib olish (bog'lanmagan)
- [x] 4.1 Kutib olish banneri va bot imkoniyatlari 4 qatorda (emoji bilan)
- [x] 4.2 Ikki katta tugma: «📱 Telefon raqamni ulashish» (request_contact) va «🔑 Kod kiritish»

## 5. Mavjud funksionallik
- [x] 5.1 Avtomatik xabarlar (kelmadi, kechikdi, baho, e'lon): emoji sarlavha va «🔎 Batafsil» inline tugmasi — `NotificationService.details`
- [x] 5.2 Xavfsizlik tekshiruvlari saqlangan (begona o'quvchi → ma'lumot yo'q) — `BotScreensSeedIntegrationTest.foreignStudentIdInCallback_returnsNothing` o'tadi

## TEKSHIRUV
- [x] T1 `./gradlew test` — **175 / 175** o'tdi; yangi testlar: `KeyboardsTest` (5), `BannerRendererTest` (3), seed integratsiya testida kartochkalar, kutib olish tugmalari va yozilgan bo'lim nomlari
- [x] T2 Barcha bannerlar seed ma'lumot bilan `docs/images/bot/`ga saqlandi (`banner-*.png`, 15 ta) va ko'zdan kechirildi; topilgan kamchiliklar tuzatildi (kesilgan pastki sarlavha → 2 qator, kesilgan yorliqlar → shrift kichrayadi, 2 ko'rsatkichli kartochka → ustma-ust, e'londagi «!» → «muhim»)
- [x] T3 `docs/bot-demo.md` yangilandi: har bo'lim uchun banner, caption va tugmalar
- [x] T4 Haqiqiy botda `/start` — **shartli band, bajarilmadi**: v2 nusxasida token sozlanmagan; token faqat 8080'da ishlayotgan jonli v1 botda, ikkinchi nusxa Telegram'da 409 xatosi bilan uni uzib qo'yadi. O'rniga mock rejimda tekshirildi (`/start` → pastki menyu, bo'lim → kartochka). Qo'lda tekshirish: [15-telegram-bot.md](15-telegram-bot.md#administrator-uchun-yoriqnoma)
- [x] T5 Yakuniy hisobot: bajarilgan/jami, rasmlar ro'yxati, commitlar
