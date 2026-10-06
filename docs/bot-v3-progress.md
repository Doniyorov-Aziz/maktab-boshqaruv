# Ota-onalar boti v3 — premium

Branch: `feature/bot-v3` (`main` dan). Sessiya uzilsa — shu fayldan davom eting.

## 0. Audit (v2 holati, kod yozishdan oldin)

| # | Band | v2 holati | Reja |
|---|---|---|---|
| 1.1 | Menyuda Mini App tugmasi (WebAppInfo) | QISMAN — bor, nomi «📱 Kundalikni ochish» | «📱 Ilovani ochish» |
| 1.2 | Alohida yengil Vue sahifa, 360–430px | BOR — `/#/webapp`, `WebAppLayout` | — |
| 1.3 | initData HMAC-SHA256 + auth_date ≤ 24 soat, faqat o'z farzandi, aks holda 403 | BOR — `TelegramInitDataValidator`, `ParentApiController` | API darajasida 403 testi qo'shiladi |
| 1.4 | themeParams, haptic, expand | BOR | — |
| 1.5 | BackButton, MainButton | YO'Q | qo'shiladi |
| 1.6 | a) Bosh: «Hozir: 3-dars …» jonli progress bar, davomat badge, oxirgi 3 baho | QISMAN — «▶ hozir» belgisi, bugungi baholar | jonli kartochka + oxirgi 3 baho |
| 1.7 | b) Jadval: hafta kunlari tablari, bugun ajratilgan, hozirgi dars, fan rang/emoji | QISMAN — Bugun/Ertaga/Hafta | kun tablari, fan ranglari |
| 1.8 | c) Baholar: o'rtacha kartochkalar, 30 kunlik chiziq, so'nggi baholar, sinf o'rtachasi | QISMAN — o'rtacha bar, so'nggi baholar | 30 kunlik dinamika, sinf o'rtachasi bilan taqqoslash |
| 1.9 | d) Davomat: oylik kalendar issiqlik xaritasi, foiz | BOR | — |
| 1.10 | e) E'lonlar va tadbirlar | QISMAN — faqat e'lonlar | tadbirlar qo'shiladi |
| 1.11 | Skeleton, bo'sh holat, xato holati, pull-to-refresh | QISMAN — skeleton, xato | bo'sh holat illustratsiyasi, pull-to-refresh |
| 1.12 | Bir nechta farzand: avatar-switcher | QISMAN — matnli chiplar | avatarlar |
| 2.1 | Ertalabki digest 07:30 (ish kunlari, yakshanba/bayramda emas) | YO'Q | yangi xabar turi + jadval |
| 2.2 | Kechki jadval + «ertaga kerakli narsalar» | QISMAN — tadbirlar bor | muddatli e'lonlar qo'shiladi |
| 2.3 | Haftalik hisobot rasm-kartochka 1080×1350 + «📱 Batafsil» | QISMAN — matnli hisobot | Java2D rasm, Mini App tugmasi |
| 2.4 | Real-time: «📱 Batafsil», «💬 Sinf rahbariga yozish»; past baho 2–3 yumshoq | QISMAN — «🔎 Batafsil», chegara 2 | ikki tugma, chegara 3 |
| 2.5 | Outbox, AFTER_COMMIT, retry, 429 | BOR | — |
| 3.1 | Xabar turlarini yoqish/o'chirish (digest bilan) | QISMAN — digest yo'q | digest tugmasi |
| 3.2 | Tinch soatlar, tongda bitta jamlangan xabar | QISMAN — har xabar alohida | jamlangan xabar |
| 3.3 | Til uz/ru (bot va Mini App, messages fayllari) | BOR — uz/cy/ru | yangi matnlar ham fayllarda |
| 3.4 | Farzandni tanlash va «Botdan uzish» | BOR | — |
| 4.1 | setMyCommands (uz/ru), tavsif, menu button = Mini App | BOR | menu button nomi yangilanadi |
| 4.2 | Bo'lim = bitta «sahifa» (editMessageMedia/Text) | BOR | — |
| 4.3 | Noma'lum matn: do'stona javob + menyu | BOR | — |
| 4.4 | Rate limit: 1 s da 3 tadan ortiq — yumshoq ogohlantirish | QISMAN — 2/s | 3/s |
| 5.1 | Faqat dev profilda demo ota-ona va 1 hafta ma'lumot | YO'Q | `@Profile("dev")` seeder |
| 5.2 | docs/bot-v3-demo.md (5 daqiqalik ssenariy) | YO'Q | yoziladi |

## Bajarilish
(ish davomida to'ldiriladi)
