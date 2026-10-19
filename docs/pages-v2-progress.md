# Sahifalar v2: Dars jadvali · Taqvim · Sinflar · Xonalar

Branch: `feature/pages-v2` (worktree `.claude/worktrees/pages-v2`, `main` 40eb701 dan). Push qilinmagan.
Sessiya uzilsa — shu fayldagi ❌ bandlardan davom eting.

## Reja (bosqichlar)

1. Backend asos: Subject.color, Room maydonlari, RoomBooking, LessonSubstitution, CalendarEvent maydonlari, kalendar obuna tokeni.
2. Backend endpointlar: `/api/pages/timetable/*`, `/api/pages/calendar/*`, `/api/pages/classes/*`, `/api/pages/rooms/*`, ICS.
3. Backend testlar.
4. Frontend umumiy komponentlar va fan ranglari.
5. Sahifalar: Dars jadvali → Taqvim → Sinflar (+ /classes/:id) → Xonalar.
6. Bosh sahifa mayda xatolari.
7. Playwright testlar, skrinshotlar, hisobot.

## Holat

(quyida har band ✅ / ❌ — ish davomida yangilanadi)

### Backend
- ✅ Subject.color (+ DTO, fanlar sahifasida tanlash uchun), 12 rangli palitra `SubjectColors` (PDF ham shu rangda)
- ✅ Room: type (+ Faollar zali, Boshqa), status FAOL/TA'MIRDA, equipment teglari, kompyuterlar soni; demo rejimda bo'sh xonalarga realistik qiymat (`RoomDetailsDemoFill`)
- ✅ RoomBooking (bir martalik band qilish, konflikt 409), LessonSubstitution (faqat bitta sana), CalendarFeedToken (.ics obuna havolasi)
- ✅ CalendarEvent: vaqt, xona (avtomatik band qilinadi, ko'chirilganda qayta tekshiriladi), joy, sinflar, "ota-onalarga xabar", takrorlanish (har hafta/oy)
- ✅ Jadval konflikti aniq xabar bilan: "Karimova D. bu vaqtda 7-B da", "204-xona bu vaqtda band (7-B, Fizika)", "5-A sinfida bu vaqtda dars bor (…)"
- ✅ `/api/pages/timetable/*`, `/api/pages/calendar/*`, `/api/pages/classes/*`, `/api/pages/rooms/*`, `/api/public/calendar/{token}.ics`
- ✅ Yangi tadbir → Telegram outbox (demo rejimda faqat log); 1 kun oldingi eslatma tanlangan sinflarga
- ✅ Backend testlar: `PagesV2IntegrationTest` (8 ta) — konfliktlar 409, o'rinbosar bitta sana, bo'sh xona qidiruvi, tadbir ko'chirilganda xona, .ics RFC 5545, outbox/demo, boshqa maktab, sinf kartochkasi formulalari
