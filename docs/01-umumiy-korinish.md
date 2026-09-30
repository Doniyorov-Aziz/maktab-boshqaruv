# 01 — Umumiy ko'rinish

[← Hujjatlar ro'yxatiga](../README.md) · Keyingisi: [02 — Arxitektura →](02-arxitektura.md)

## Mundarija
- [Loyiha nima qiladi](#loyiha-nima-qiladi)
- [Kimlar uchun](#kimlar-uchun)
- [Foydalanuvchi rollari](#foydalanuvchi-rollari)
- [Ko'p maktabli (multi-school) tizim](#kop-maktabli-multi-school-tizim)
- [Modullar ro'yxati](#modullar-royxati)

## Loyiha nima qiladi

**Maktab Boshqaruv** — maktab (yoki bir nechta maktab) uchun kundalik boshqaruv ishlarini bitta joyda yig'adigan veb-ilova: sinflar va o'quvchilar ro'yxatidan tortib, dars jadvali, kundalik davomat, baholar jurnali, xulq-atvor yozuvlari, e'lonlar va tadbirlar taqvimigacha. Tizim ikki qismdan iborat: **backend** (Spring Boot REST API + PostgreSQL baza) va **frontend** (Quasar/Vue admin panel), ular bir-biridan mustaqil ishga tushadigan alohida loyihalar sifatida saqlanadi (`/` — backend, `/frontend` — frontend).

## Kimlar uchun

Tizimdan uchta turdagi foydalanuvchi foydalanadi:
- **Maktab ma'muriyati (admin/direktor)** — barcha ma'lumotlarni boshqaradi: sinflar, xodimlar, o'quvchilar, dars jadvali tuzish, foydalanuvchi akkauntlari.
- **O'qituvchilar** — davomat oladi, baho qo'yadi. Ularning akkaunti (`EDITOR` roli) boshqa editorlar kabi **barcha** ma'lumotga kira oladi; faqat Davomat sahifasida, `User` `Employee`ga bog'langan bo'lsa, sukut bo'yicha "Mening darslarim" filtri yoqilgan bo'ladi (uni o'chirib, barcha darslarni ko'rish mumkin). Bu ruxsat cheklovi emas, faqat qulaylik filtri.
- **Kuzatuvchilar (viewer)** — faqat ko'rish huquqi bilan hisobot va statistikani kuzatadi.

## Foydalanuvchi rollari

Tizimda uchta rol bor (`Role` enum, `src/main/java/uz/azizbek/maktabboshqaruv/entity/Role.java`):

| Rol | Huquqi |
|---|---|
| `ADMIN` | Hammasi: ko'rish, yaratish, yangilash, **o'chirish**, foydalanuvchilarni boshqarish |
| `EDITOR` | Ko'rish, yaratish, yangilash (o'chira olmaydi) |
| `VIEWER` | Faqat ko'rish |

Rollar qanday tekshirilishi va har bir endpoint qaysi rolga ochiqligi haqida batafsil: [06-backend.md — Xavfsizlik](06-backend.md#xavfsizlik).

## Ko'p maktabli (multi-school) tizim

Bitta joylashtirilgan (deploy qilingan) tizim bir nechta maktabni bir vaqtda xizmat qila oladi — har bir `School` yozuvi o'z binolari, sinflari, xodimlari, o'quvchilari bilan mustaqil. Frontend'da yuqori panelda joriy maktab tanlanadi (`schoolStore.activeSchoolId`), va deyarli har bir so'rovga `schoolId` parametri sifatida qo'shib yuboriladi. Batafsil: [02-arxitektura.md — Ko'p maktablilik](02-arxitektura.md#kop-maktablilik-multi-school-qanday-ishlaydi).

## Modullar ro'yxati

Chap menyudagi barcha modullar (guruhlar bo'yicha, `frontend/src/config/modules.js`dagi tartibda):

**Kundalik ish** (statik sahifalar, alohida Vue komponentlari):
- **Davomat olish** — sinf va dars tanlab, o'quvchilarni PRESENT/ABSENT/LATE/EXCUSED deb belgilash.
- **Baholar jurnali** — sinf va fan bo'yicha davr ichidagi baholarni jadval ko'rinishida kiritish.
- **Dars jadvali** — haftalik dars jadvali (sinf/o'qituvchi/xona bo'yicha ko'rish), jonli vaqt chizig'i, PDF chop etish.
- **Taqvim** — bayramlar, imtihonlar, ota-onalar yig'ilishi va boshqa tadbirlar oylik/haftalik ko'rinishda.

**Ta'lim tuzilmasi** (`school`, `building`, `room`):
- **Maktablar** — tizimdagi har bir maktab yozuvi (nomi, manzili).
- **Binolar** — maktabga tegishli binolar.
- **Xonalar** — binoga tegishli xonalar (turi: sinfxona/laboratoriya/kompyuter xonasi/sport zali/kutubxona), kunlik band-bo'sh jadvali bilan.

**O'quv jarayoni** (`academicYear`, `schoolClass`, `subject`, `lessonSlot`):
- **O'quv yillari** — maktabning o'quv yillari (boshlanish/tugash sanasi).
- **Sinflar** — sinf (masalan "5-A"), sinf rahbari, maksimal o'quvchi soni.
- **Fanlar** — o'qitiladigan fanlar ro'yxati.
- **Dars jadvali (ro'yxat)** — har bir individual dars vaqti yozuvi (sinf+fan+o'qituvchi+xona+kun+vaqt); menyuda yashirin (`hidden: true`), Dars jadvali sahifasi orqali boshqariladi.

**Odamlar** (`student`, `employee`, `position`):
- **O'quvchilar** — F.I.Sh., tug'ilgan sana, sinf, ota-ona ma'lumotlari.
- **Lavozimlar** — xodim lavozimlari ro'yxati (Direktor, Fan o'qituvchisi va h.k.).
- **Xodimlar** — o'qituvchi va boshqa xodimlar, lavozimga bog'langan.

**Kundalik hayot** (`announcement`, `calendarEvent`, `behaviorRecord`):
- **E'lonlar** — hammaga/o'qituvchilarga/bitta sinfga qaratilgan e'lon, muhimlik darajasi bilan.
- **Tadbirlar** — taqvimdagi voqealar (bayram, imtihon, ota-onalar yig'ilishi, ta'til, boshqa).
- **Xulq yozuvlari** — o'quvchiga nisbatan rag'batlantirish yoki ogohlantirish yozuvi.

**Boshqaruv** (`user`):
- **Foydalanuvchilar** — tizimga kirish akkauntlari, faqat `ADMIN` uchun ko'rinadi (`adminOnly: true`).

Har bir modulning aniq ustunlari, forma maydonlari va API endpoint'i uchun: [07-api.md](07-api.md) va [08-frontend.md](08-frontend.md).

---
Keyingisi: [02 — Arxitektura →](02-arxitektura.md)
