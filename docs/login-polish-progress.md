# Login sahifasi — sayqal (v2)

## 1. Chap panel kompozitsiyasi
- [ ] 1.1 Yaxlit, vertikal markazlashgan blok: logo tepada 32px chekkada, o'rtada sarlavha + tavsif + kartochkalar, pastda imkoniyatlar va copyright; katta bo'shliqlar yo'q
- [ ] 1.2 ≥1440px: ikki ustun — chapda matn (max 520px), o'ngda kartochkalar; 1280–1439px: kartochkalar matn ostida
- [ ] 1.3 «Sahna»: 5 kartochka turli o'lcham va chuqurlikda (orqadagilar kichikroq va xiraroq), ortida indigo-binafsha nur (radial-gradient, blur 80px)
- [ ] 1.4 Qiyalik ≤ ±1.5°, chuqurlik o'lcham/soya/shaffoflik bilan; matn tiniq (translateZ(0), will-change, butun piksel)
- [ ] 1.5 Kartochkalar: davomat (94%, «512 o'quvchidan 481 nafari darsda»), hozirgi dars (oq matnli to'q indigo chip ≥4.5:1, sinf, xona, vaqt, progress), yangi baho «5», Telegram ✓✓, mini haftalik jadval (5 kun × 3 rangli katak)
- [ ] 1.6 Suzish 4–6px, 7–9 s, turli fazada; sichqoncha parallaksi ≤8px faqat desktop'da; reduced-motion'da hammasi o'chadi
- [ ] 1.7 Daftar naqshi ~5%, hoshiya chizig'i saqlangan, chetlarda vinyetka
- [ ] 1.8 Tavsif chiroyli bo'linadi (`text-wrap: balance`), «bitta tizimda» yolg'iz qolmaydi
- [ ] 1.9 4 ta imkoniyat «chip»i (ikonka + so'z, 14px, yarim shaffof, yumaloq), ostida copyright va versiya (12px, xira)

## 2. O'ng panel
- [ ] 2.1 Dark: #0E0E1A + forma ortida yumshoq indigo nur; panellar chegarasi yumshoq (1px yorug' chiziq + 40px gradient o'tish)
- [ ] 2.2 Kartochka 440px, ichki bo'shliq 44px, ingichka chegara (oq 8%), inset highlight
- [ ] 2.3 Qo'l o'rniga indigo gradientli 48px logotip + «Administrator paneli» yorlig'i
- [ ] 2.4 «Xush kelibsiz 👋» sarlavhasi, ostida tavsif
- [ ] 2.5 Label doim maydon tepasida, placeholder misol bilan («masalan: direktor»), 52px, fokusda indigo halqa + yorug'lik
- [ ] 2.6 Kartochka ostida 3 ishonch belgisi: Xavfsiz ulanish · Rollar bo'yicha kirish · Telegram orqali ota-onalar (13px, xira)
- [ ] 2.7 Eng pastda markazda «Yordam kerakmi? Maktab administratoriga murojaat qiling.»

## 3. Yuqori o'ng boshqaruv
- [ ] 3.1 Tema tugmasi 24px ichkarida, 40px yumaloq, tooltip, kesilmaydi
- [ ] 3.2 «O'Z | RU» til almashtirgich: tanlov saqlanadi, RU'da sahifa matnlari ruscha (faqat shu sahifa lug'ati)

## 4. Light rejim
- [ ] 4.1 Chap panel indigo; o'ng #F6F7FB + lavanda nur; oq kartochka chiroyli soya bilan; WCAG AA

## 5. Mikro-tafsilotlar
- [ ] 5.1 Tugma: hover'da gradient siljiydi + 1px ko'tariladi, bosilganda 0.98, yuklanishda spinner + «Kirilmoqda...»
- [ ] 5.2 Muvaffaqiyatli kirishda 300ms yashil «✓», keyin dashboard
- [ ] 5.3 Barcha o'tishlar 150–250ms, bitta easing `cubic-bezier(.2,.8,.2,1)`
- [ ] 5.4 Favicon va sarlavha «Kirish · Maktab Boshqaruv»

## 6. Saqlanadigan narsalar
- [ ] 6.1 Login mantiqi (API, token, eslab qolish, health, xato xabarlari, Caps Lock, silkinish, «Parolni unutdingizmi?») buzilmagan

## TEKSHIRUV
- [ ] T1 `npm run lint` va `npm run build` xatosiz
- [ ] T2 8 ta screenshot (1920×1080, 1440×900, 1366×768, 390×844 × light/dark) → docs/images/login/v2/
- [ ] T3 Har biri ko'zdan kechirildi: 120px'dan katta bo'sh teshik yo'q, hech narsa kesilmagan, matn tiniq, chip o'qiladi, chegara keskin emas, telefonda gorizontal scroll yo'q
- [ ] T4 Funksional: Enter va tugma bilan kirish, noto'g'ri parolda xato + silkinish, backend o'chiq → sariq banner, til va tema yangilangandan keyin saqlanadi
- [ ] T5 Hisobot: bajarilgan/jami, screenshot'lar, avvalgi versiyadan farqlar
