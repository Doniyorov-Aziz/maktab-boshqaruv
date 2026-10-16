# Yagona dizayn tizimi — hisobot

Oldin: 2026-10-16 04:34, keyin: 2026-10-16 05:24 (UTC). Skrinshotlar: `before/` va `after/` — 1366×768, 1920×1080, 2560×1440, 390×844 (light) va 1920×1080 (dark).

## 1. Natija qisqacha

- Qattiq yozilgan radius / height / max-width / avatar o'lchami: **367 → 0** (tokenlar fayli bundan mustasno).
- `npm test`: o'lcham testi (har sahifa, 1440×900) va layout testi (1920, 2560, 390) — hammasi o'tadi.
- `npm run lint`: oxlint + ESLint (`q-input` / `q-select` / `q-btn` va `QInput` / `QSelect` / `QBtn` importi `src/components/app/` dan tashqarida — xato).

## 2. Oldin / keyin: elementlar (1366×768)

Belgilar: `40px / r10` = balandlik / burchak radiusi; `×N` — nechta. "Keyin" ustunidagi `33.6px / r50` (tugma) va `40px / r0 / none` (maydon) — jadval pastidagi sahifalash tugmalari va "Sahifada:" tanlovi: ularni Quasar o'zi chizadi, testlar ularni o'tkazib yuboradi. Dashboard'dagi `32px / r10` — reyting kartochkasidagi ixcham segment almashtirgich ichidagi tugmalar.

| Sahifa | Maydonlar oldin | Maydonlar keyin | Tugmalar oldin | Tugmalar keyin |
|---|---|---|---|---|
| login | 52px / r12 / solid ×2 | 40px / r10 / solid (dense) ×2 | 40px / r50, 33.6px / r50, 52px / r12 | 40px / r10 ×2, 32px / r8 |
| dashboard | — | — | 33.6px / r50 ×4, 32px / r3 ×4, 28px / r0 ×2, 34px / r50, 28px / r3 | 40px / r10 ×9, 32px / r10 ×3 |
| crud-schools | 40px / r4 / solid (dense) | 40px / r10 / solid (dense) | 33.6px / r50 ×6, 24px / r50 ×4, 32px / r3 ×2, 34px / r50, 36px / r3 | 40px / r10 ×10, 32px / r8 ×4 |
| crud-buildings | 40px / r4 / solid (dense) | 40px / r10 / solid (dense) | 33.6px / r50 ×6, 24px / r50 ×6, 32px / r3 ×2, 34px / r50, 36px / r3 | 40px / r10 ×9, 33.6px / r50 |
| crud-rooms | 40px / r4 / solid (dense) ×3, 40px / r0 / none (dense) | 40px / r10 / solid (dense) ×3, 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×8, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×4 |
| crud-academic-years | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 33.6px / r50 ×4, 24px / r50 ×4, 32px / r3 ×2, 34px / r50, 36px / r3 | 40px / r10 ×8, 32px / r8 ×4 |
| crud-school-classes | 40px / r4 / solid (dense) | 40px / r10 / solid (dense) | 24px / r50 ×24, 33.6px / r50 ×6, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×24, 40px / r10 ×10 |
| crud-students | 40px / r4 / solid (dense) ×2, 40px / r0 / none (dense) | 40px / r10 / solid (dense) ×2, 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×8, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×4 |
| crud-subjects | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×8, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×4 |
| crud-positions | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 24px / r50 ×18, 33.6px / r50 ×4, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×18, 40px / r10 ×8 |
| crud-employees | 40px / r4 / solid (dense) ×3, 40px / r0 / none (dense) | 40px / r10 / solid (dense) ×3, 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×8, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×4 |
| crud-lesson-slots | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×8, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×4 |
| crud-announcements | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×8, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×4 |
| crud-calendar-events | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×6, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×2 |
| crud-behavior-records | 40px / r4 / solid (dense) ×2, 40px / r0 / none (dense) | 40px / r10 / solid (dense) ×2, 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×8, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×20, 40px / r10 ×8, 33.6px / r50 ×4 |
| crud-users | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 24px / r50 ×10, 33.6px / r50 ×4, 32px / r3 ×2, 34px / r50, 36px / r3 | 32px / r8 ×10, 40px / r10 ×8 |
| attendance | 40px / r4 / solid (dense) ×2, 40px / r4 / dashed (dense) | 40px / r10 / solid (dense) ×3 | 33.6px / r50 ×4, 32px / r3, 34px / r50, 36px / r3, 56px / r3 | 40px / r10 ×8 |
| attendance-calendar | — | — | 34px / r3 ×11, 33.6px / r50 ×6, 32px / r3, 34px / r50, 36px / r3 | 40px / r10 ×20 |
| gradebook | 40px / r4 / solid (dense) | 40px / r10 / solid (dense) | 34px / r3 ×11, 33.6px / r50 ×6, 32px / r3 ×2, 34px / r50, 36px / r3 | 40px / r10 ×21 |
| timetable | — | — | 33.6px / r50 ×7, 32px / r3 ×2, 32px / r0 ×2, 34px / r50, 36px / r3, 36px / r0 | 40px / r10 ×12, 36px / r10 ×2 |
| calendar | — | — | 33.6px / r50 ×6, 32px / r3 ×3, 34px / r50, 32px / r0, 36px / r3 | 40px / r10 ×12 |
| notifications | 40px / r4 / solid (dense) ×4, 40px / r4 / dashed (dense) ×2, 40px / r0 / none (dense) | 40px / r10 / solid (dense) ×6, 40px / r0 / none (dense) | 24px / r50 ×20, 33.6px / r50 ×9, 36px / r3 ×2, 32px / r3, 34px / r50 | 32px / r8 ×20, 40px / r10 ×9, 33.6px / r50 ×4 |
| telegram-parents | 40px / r4 / solid (dense) | 40px / r10 / solid (dense) | 33.6px / r50 ×4, 36px / r0 ×2, 32px / r3, 34px / r50, 36px / r28 | 40px / r10 ×9 |
| parent-messages | 40px / r4 / solid (dense) ×4, 40px / r4 / dashed (dense) | 40px / r10 / solid (dense) ×5 | 33.6px / r50 ×5, 32px / r3, 34px / r50, 36px / r3 | 40px / r10 ×8 |
| absence-requests | 40px / r0 / none (dense) | 40px / r0 / none (dense) | 33.6px / r50 ×7, 28px / r3 ×4, 36px / r0 ×3, 32px / r3, 34px / r50, 36px / r28 | 40px / r10 ×11, 32px / r8 ×6 |
| broadcasts | 40px / r4 / solid (dense) ×3, 40px / r4 / dashed (dense) ×2, 148px / r4 / solid, 40px / r0 / none (dense) | 40px / r10 / solid (dense) ×5, 136px / r10 / solid (dense), 40px / r0 / none (dense) | 22.8px / r3 ×10, 33.6px / r50 ×6, 36px / r3 ×3, 36px / r0 ×2, 32px / r3, 34px / r50, 36px / r28 | 40px / r10 ×12, 32px / r8 ×10, 33.6px / r50 ×2 |
| bot-stats | — | — | 22.8px / r3 ×22, 33.6px / r50 ×5, 32px / r3, 34px / r50 | 32px / r8 ×22, 40px / r10 ×7 |
| bot-settings | 40px / r4 / solid (dense) ×7, 56px / r4 / solid | 40px / r10 / solid (dense) ×8 | 33.6px / r50 ×4, 32px / r3, 34px / r50, 36px / r28, 36px / r0, 36px / r3 | 40px / r10 ×9 |
| library-books | 40px / r4 / solid (dense) ×5 | 40px / r10 / solid (dense) ×5 | 24px / r50 ×20, 33.6px / r50 ×4, 32px / r0 ×4, 36px / r3 ×3, 32px / r3 ×2, 34px / r50, 32px / r28 | 32px / r8 ×20, 40px / r10 ×11, 38px / r10 ×4 |
| library-categories | — | — | 33.6px / r50 ×40, 32px / r3, 34px / r50, 36px / r3 | 40px / r10 ×43 |
| library-authors | 40px / r4 / solid (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense), 40px / r0 / none (dense) | 33.6px / r50 ×56, 32px / r0 ×3, 32px / r3, 34px / r50, 36px / r3, 32px / r28 | 32px / r8 ×50, 40px / r10 ×7, 38px / r10 ×4, 33.6px / r50 ×2 |
| library-permissions | 40px / r0 / none (dense) | 40px / r0 / none (dense) | 33.6px / r50 ×10, 32px / r3 ×4, 32px / r0 ×4, 36px / r3 ×2, 34px / r50, 32px / r28 | 32px / r8 ×9, 40px / r10 ×8, 38px / r10 ×5 |
| library-stats | 40px / r4 / solid (dense) ×3, 40px / r4 / dashed (dense) ×2, 24px / r0 / none (dense), 40px / r0 / none (dense) | 40px / r10 / solid (dense) ×5, 24px / r0 / none (dense), 40px / r0 / none (dense) | 34px / r3 ×11, 33.6px / r50 ×9, 24px / r50 ×4, 32px / r3, 34px / r50, 36px / r3 | 40px / r10 ×19, 24px / r50 ×4, 33.6px / r50 ×4 |
| student-profile | — | — | 33.6px / r50 ×4, 32px / r3 ×3, 34px / r50, 24px / r50, 36px / r3 | 40px / r10 ×9, 32px / r8 |
| teacher-profile | — | — | 33.6px / r50 ×4, 32px / r3 ×2, 34px / r50 | 40px / r10 ×7 |
| class-profile | — | — | 33.6px / r50 ×4, 32px / r3 ×2, 34px / r50, 36px / r3 | 40px / r10 ×8 |
| class-qr-print | — | — | 32px / r3, 36px / r3 | 40px / r10 ×2 |
| not-found | 52px / r12 / solid ×2 | 40px / r10 / solid (dense) ×2 | 40px / r50, 33.6px / r50, 52px / r12 | 40px / r10 ×2, 32px / r8 |

| Sahifa | Kartochkalar oldin | Kartochkalar keyin | Ikonka-kvadrat / avatar oldin | keyin |
|---|---|---|---|---|
| login | — | — | — | — |
| dashboard | r16 / 1px solid ×12 | r16 / 1px solid ×12 | 34px / r50, 38px / r50, 48px / r50 | 40px / r8 ×5, 32px / r50, 48px / r10 |
| crud-schools | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 40px / r50 ×2, 34px / r50, 38px / r50, 48px / r50 | 40px / r8 ×3, 32px / r50, 48px / r10 |
| crud-buildings | r16 / 1px solid ×3 | r16 / 1px solid | 40px / r50 ×3, 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-rooms | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-academic-years | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-school-classes | r16 / 1px solid ×12 | r16 / 1px solid ×12 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-students | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-subjects | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-positions | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-employees | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-lesson-slots | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-announcements | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-calendar-events | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-behavior-records | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| crud-users | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| attendance | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| attendance-calendar | r16 / 1px solid ×3 | r16 / 1px solid ×3 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| gradebook | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| timetable | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| calendar | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| notifications | r16 / 1px solid ×4, r18 / 1px solid ×4 | r16 / 1px solid ×8 | 34px / r50, 38px / r50, 48px / r50 | 48px / r10 ×5, 32px / r50, 40px / r8 |
| telegram-parents | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| parent-messages | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 40px / r50 ×16, 34px / r50, 38px / r50, 48px / r50 | 40px / r50 ×16, 32px / r50, 40px / r8, 48px / r10 |
| absence-requests | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| broadcasts | r16 / 1px solid ×3 | r16 / 1px solid ×3 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| bot-stats | r18 / 1px solid ×7, r16 / 1px solid ×4 | r16 / 1px solid ×11 | 34px / r50, 38px / r50, 48px / r50 | 48px / r10 ×8, 32px / r50, 40px / r8 |
| bot-settings | r16 / 1px solid ×4 | r16 / 1px solid ×4 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| library-books | r16 / 1px solid ×21 | r16 / 1px solid ×21 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| library-categories | r16 / 1px solid | r16 / 1px solid | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| library-authors | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| library-permissions | r16 / 1px solid ×2 | r16 / 1px solid ×2 | 34px / r50, 38px / r50, 48px / r50 | 32px / r50, 40px / r8, 48px / r10 |
| library-stats | r16 / 1px solid ×6, r18 / 1px solid ×4 | r16 / 1px solid ×10 | 34px / r50, 38px / r50, 48px / r50 | 48px / r10 ×5, 32px / r50, 40px / r8 |
| student-profile | r16 / 1px solid ×7 | r16 / 1px solid ×7 | 34px / r50 ×2, 38px / r50, 72px / r50, 40px / r50 | 32px / r50 ×2, 40px / r8 ×2, 72px / r50 |
| teacher-profile | r16 / 1px solid ×7 | r16 / 1px solid ×7 | 34px / r50, 38px / r50, 72px / r50 | 32px / r50, 40px / r8, 72px / r50 |
| class-profile | r16 / 1px solid ×6 | r16 / 1px solid ×6 | 34px / r50 ×22, 38px / r50, 64px / r50 | 32px / r50 ×22, 40px / r8, 48px / r10 |
| class-qr-print | — | — | — | — |
| not-found | — | — | — | — |

## 3. Oldin / keyin: kenglik (kontent / sidebar'dan qolgan joy, px)

| Sahifa | 1366 oldin | 1366 keyin | 1920 oldin | 1920 keyin | 2560 oldin | 2560 keyin | 390 scroll keyin |
|---|---|---|---|---|---|---|---|
| login | 751/1366 | 751/1366 | 1152/1920 | 1152/1920 | 1536/2560 | 1536/2560 | yo'q |
| dashboard | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-schools | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-buildings | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-rooms | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-academic-years | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-school-classes | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-students | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-subjects | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-positions | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-employees | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-lesson-slots | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-announcements | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-calendar-events | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-behavior-records | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| crud-users | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| attendance | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| attendance-calendar | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| gradebook | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| timetable | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| calendar | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| notifications | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| telegram-parents | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| parent-messages | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| absence-requests | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| broadcasts | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| bot-stats | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| bot-settings | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| library-books | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| library-categories | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| library-authors | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| library-permissions | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| library-stats | 1110/1126 | 1078/1126 | 1584/1680 | 1600/1680 | 1584/2320 | 2240/2320 | yo'q |
| student-profile | 87/1126 | 1078/1126 | 87/1680 | 1600/1680 | 87/2320 | 2240/2320 | yo'q |
| teacher-profile | 87/1126 | 1078/1126 | 87/1680 | 1600/1680 | 87/2320 | 2240/2320 | yo'q |
| class-profile | 87/1126 | 1078/1126 | 87/1680 | 1600/1680 | 87/2320 | 2240/2320 | yo'q |
| class-qr-print | null/undefined | null/undefined | null/undefined | null/undefined | null/undefined | null/undefined | yo'q |
| not-found | 751/1366 | 751/1366 | 1152/1920 | 1152/1920 | 1536/2560 | 1536/2560 | yo'q |

## 4. O'zgargan fayllar (main bilan solishtirganda)

| Holat | Fayl |
|---|---|
| o'zgardi | `.gitignore` |
| qo'shildi | `eslint.config.js` |
| o'zgardi | `package-lock.json` |
| o'zgardi | `package.json` |
| qo'shildi | `playwright.config.js` |
| o'zgardi | `src/components/AuthImageDialog.vue` |
| o'zgardi | `src/components/BookCover.vue` |
| o'zgardi | `src/components/ClassOverviewPanel.vue` |
| o'zgardi | `src/components/ClassPicker.vue` |
| o'zgardi | `src/components/ClassRankingCard.vue` |
| o'chirildi | `src/components/DateField.vue` |
| o'zgardi | `src/components/PageLayout.vue` |
| o'zgardi | `src/components/TelegramStudentCard.vue` |
| qo'shildi | `src/components/app/AppButton.vue` |
| qo'shildi | `src/components/app/AppDateInput.vue` |
| qo'shildi | `src/components/app/AppInput.vue` |
| qo'shildi | `src/components/app/AppSelect.vue` |
| qo'shildi | `src/components/app/AppTimeInput.vue` |
| qo'shildi | `src/components/app/IconTile.vue` |
| o'zgardi | `src/components/webapp/WaState.vue` |
| o'zgardi | `src/css/app.scss` |
| qo'shildi | `src/css/controls.scss` |
| o'zgardi | `src/css/quasar.variables.scss` |
| qo'shildi | `src/css/tokens.scss` |
| o'zgardi | `src/layouts/MainLayout.vue` |
| o'zgardi | `src/layouts/WebAppLayout.vue` |
| o'zgardi | `src/pages/AbsenceRequestsPage.vue` |
| o'zgardi | `src/pages/AppealsPage.vue` |
| o'zgardi | `src/pages/AttendanceCalendarPage.vue` |
| o'zgardi | `src/pages/AttendancePage.vue` |
| o'zgardi | `src/pages/BotSettingsPage.vue` |
| o'zgardi | `src/pages/BotStatsPage.vue` |
| o'zgardi | `src/pages/BroadcastPage.vue` |
| o'zgardi | `src/pages/CalendarPage.vue` |
| o'zgardi | `src/pages/ClassProfilePage.vue` |
| o'zgardi | `src/pages/ClassQrPrintPage.vue` |
| o'zgardi | `src/pages/CrudPage.vue` |
| o'zgardi | `src/pages/DashboardPage.vue` |
| o'zgardi | `src/pages/ErrorNotFound.vue` |
| o'zgardi | `src/pages/GradebookPage.vue` |
| o'zgardi | `src/pages/LibraryAuthorsPage.vue` |
| o'zgardi | `src/pages/LibraryBooksPage.vue` |
| o'zgardi | `src/pages/LibraryCategoriesPage.vue` |
| o'zgardi | `src/pages/LibraryPermissionsPage.vue` |
| o'zgardi | `src/pages/LibraryStatsPage.vue` |
| o'zgardi | `src/pages/LoginPage.vue` |
| o'zgardi | `src/pages/NotificationsPage.vue` |
| o'zgardi | `src/pages/StudentProfilePage.vue` |
| o'zgardi | `src/pages/TeacherProfilePage.vue` |
| o'zgardi | `src/pages/TelegramParentsPage.vue` |
| o'zgardi | `src/pages/TimetablePage.vue` |
| o'zgardi | `src/pages/webapp/WebAnnouncementsPage.vue` |
| o'zgardi | `src/pages/webapp/WebAttendancePage.vue` |
| o'zgardi | `src/pages/webapp/WebGradesPage.vue` |
| o'zgardi | `src/pages/webapp/WebSchedulePage.vue` |
| o'zgardi | `src/pages/webapp/WebTodayPage.vue` |
| qo'shildi | `tests/ui/audit-md.mjs` |
| qo'shildi | `tests/ui/audit.mjs` |
| qo'shildi | `tests/ui/design-system.spec.js` |
| qo'shildi | `tests/ui/hardcoded.js` |
| qo'shildi | `tests/ui/layout.spec.js` |
| qo'shildi | `tests/ui/measure.js` |
| qo'shildi | `tests/ui/pages.js` |
| qo'shildi | `tests/ui/report-guide.md` |
| qo'shildi | `tests/ui/report-md.mjs` |
| qo'shildi | `tests/ui/session.js` |
| qo'shildi | `tests/ui/shot.mjs` |
| qo'shildi | `tests/ui/summarize.js` |

## 5. Tokenlar — yagona manba

Hamma raqamlar `src/css/tokens.scss` da. Boshqa joyda `px` bilan radius / balandlik / max-width yozilmaydi (`tests/ui/hardcoded.js` tekshiradi; media query breakpoint'lari bundan mustasno — ularda CSS o'zgaruvchisi ishlamaydi).

| Token | Qiymat | Qayerda |
|---|---|---|
| `--radius-sm` | 8px | chip, badge, kichik tugma, ikonka-kvadrat 32/40 |
| `--radius-md` | 10px | input, select, sana, vaqt, tugma — hammasi bir xil; 48px ikonka-kvadrat |
| `--radius-lg` | 16px | kartochka, panel, dialog |
| `--radius-pill` | 999px | faqat doira: avatar, nuqta, progress-bar |
| `--control-h` | 40px | har bir input / select / sana / vaqt / tugma |
| `--control-h-sm` | 32px | jadval ichidagi ixcham tugma, chip |
| `--space-1..8` | 4/8/12/16/24/32/40/48 | bo'shliqlar |
| `--border`, `--border-control`, `--border-focus` | 1px / 1px / 2px | kartochka chegarasi, maydon chegarasi, fokus |
| `--page-pad` | 12 / 16 / 24 / 32 / 40 | sahifa yon padding'i (<600 / 600–1023 / 1024–1439 / 1440–1919 / ≥1920) |
| `--page-gap` | 32px (`--space-6`) | header ↔ sahifa sarlavhasi ↔ kontent ↔ pastki chet |
| `--chart-h` | 240 / 300 / 360 | grafik balandligi (<600 / 600–1439 / ≥1440) |
| `--tile-sm/md/lg` | 32 / 40 / 48 | ikonka-kvadratlar |
| `--grid-min` / `--grid-min-sm` | 320 / 220 | kartochka grid'idagi eng tor ustun |
| `--form-max` | 960px | uzun forma ichki ustuni (Bot sozlamalari) |

Quasar'ning o'z SASS o'zgaruvchilari ham shu qiymatlarga o'rnatilgan (`quasar.variables.scss`: `$generic-border-radius`, `$button-border-radius`, `$button-font-weight`, `$chip-height`, `$table-border-radius`), shuning uchun Quasar o'zi chizadigan menyu, jadval, tooltip ham mos.

Rang tokenlari (`--c-border`, `--border` …) `body` da e'lon qilingan: dark mavzu `body.body--dark` ga qo'yiladi va o'zgaruvchilar o'sha yerda hisoblanadi — `:root` da e'lon qilinsa, dark mavzuda light ranglar qolib ketardi.

## 6. Komponentlar (`src/components/app/`)

Sahifalarda `q-input`, `q-select`, `q-btn` to'g'ridan-to'g'ri ishlatilmaydi — lint xato beradi. Hamma Quasar prop'lari, hodisalari va slot'lari o'tadi.

### AppInput
```vue
<AppInput v-model="form.name" label="Nomi" :rules="[v => !!v || 'Kiriting']" />
<AppInput v-model="q" placeholder="Qidirish..." clearable>
  <template #prepend><q-icon name="search" /></template>
</AppInput>
<AppInput v-model="text" type="textarea" autogrow label="Izoh" />   <!-- ko'p qatorli: balandligi tabiiy -->
```

### AppSelect
```vue
<AppSelect v-model="classId" :options="classes" label="Sinf" emit-value map-options clearable />
```

### AppDateInput — `dd.MM.yyyy` ko'rinadi, v-model `YYYY-MM-DD`
```vue
<AppDateInput v-model="form.birthDate" label="Tug'ilgan sana" no-future />
<AppDateInput v-model="day" label="Sana" no-sundays min="2026-09-01" max="2027-05-31" clearable />
```
Maydonning istalgan joyini bosish kalendarni ochadi, ikonka o'ngda; faqat-o'qish holatida ham chegara oddiy (punktir emas).

### AppTimeInput — 24 soatli `HH:mm`
```vue
<AppTimeInput v-model="form.reportTime" label="Hisobot vaqti" />
```

### AppButton — `variant`: primary / secondary / ghost / danger; `size`: md / sm
```vue
<AppButton label="Saqlash" icon="save" @click="save" />
<AppButton variant="secondary" label="Bekor qilish" v-close-popup />
<AppButton variant="ghost" icon="edit" size="sm" aria-label="Tahrirlash" />   <!-- faqat ikonka: kvadrat -->
<AppButton variant="danger" label="O'chirish" :loading="busy" />
<AppButton variant="ghost" color="positive" icon="check" label="Keldi" />      <!-- boshqa rang, shakl o'sha -->
<AppButton variant="ghost" square><q-avatar size="var(--avatar-sm)">A</q-avatar></AppButton>
```
Balandlik 40 (sm: 32), radius 10 (sm: 8), ikonka va matn orasi 8px, bosh harf yo'q, og'irlik 600. Jadval qatoridagi tugmalar — `size="sm"`.

### IconTile — rangli kvadrat ichida ikonka; o'lcham faqat 32 / 40 / 48
```vue
<IconTile icon="school" size="48" />                                          <!-- brend gradient -->
<IconTile icon="event" size="32" :color="typeColor(ev.type)" />
<IconTile icon="groups" size="40" color="rgba(79, 70, 229, 0.12)" text-color="var(--q-primary)" />
```

### PageLayout — har sahifaning ramkasi
```vue
<page-layout icon="groups" title="Sinflar" subtitle="1-maktab">
  <template #actions><AppButton icon="add" label="Qo'shish" /></template>
  ...
</page-layout>
<page-layout icon="grid_on" title="Baholar jurnali" fill>   <!-- jadval ekran balandligida, ichida scroll -->
```
`#subtitle` slot'i ham bor (bir necha qatorli izoh uchun). `max-width` yo'q — kontent sidebar'dan qolgan butun kenglikni oladi.

## 7. Layout yordamchilari (`src/css/controls.scss`)

| Sinf | Nima qiladi |
|---|---|
| `.app-grid` | `repeat(auto-fill, minmax(320px, 1fr))` — kartochkalar: katta ekranda ko'proq ustun |
| `.app-toolbar` | filtr qatori: bir xil balandlik, pastki chiziqlar bir tekisda (`align-items: flex-end`) |
| `.app-chart` | grafik konteyneri: kenglik 100%, balandlik `--chart-h` (Chart.js: `responsive: true`, `maintainAspectRatio: false`) |
| `.app-form-column` | uzun forma ustuni ≤ 960px |
| `.app-fill` | ekran balandligidagi blok, ichida scroll |
| `.app-dialog--sm/md/lg/xl` | dialog kartochkasi kengligi (420 / 560 / 760 / 960, telefonda to'liq) |
| `.app-toggle--sm` | jadval/kartochka burchagidagi ixcham segment almashtirgich |

Sahifalar bo'yicha: Dashboard — 12 ustunli grid (ikki mustaqil ustun, kartochkalar o'z balandligida, teshik yo'q), KPI — `auto-fit`; CRUD kartochkalari va kutubxona — `auto-fill` grid; Davomat kalendari va Baholar jurnali — `fill` (≥1024px da ekran balandligida, jadval ichida scroll; telefonda sahifa odatdagidek scroll bo'ladi). Sidebar ≥1024px da doim ochiq (yig'ish mumkin), tor ekranda drawer — avvaldan shunday edi, saqlandi.

## 8. Tanlangan usullar

- **Yorliq maydon ichida** (Quasar'ning suzuvchi yorlig'i) — loyihada eng ko'p ishlatilgan usul; login sahifasidagi tashqi yorliqlar ham shunga o'tkazildi.
- **Balandlik** `--control-h` dan; ichkarida Quasar'ning `dense` rejimi ishlatiladi (u aynan 40px beradi va suzuvchi yorliqni to'g'ri joylaydi), balandlikni esa `.app-field` qoidasi tokenga bog'laydi.
- **Telegram Mini App** (`/webapp`) — Telegram mavzusidagi alohida mobil ilova, Quasar boshqaruv elementlari yo'q; ko'rinishi saqlandi, o'lchamlari tokenlarga o'tkazildi (`--wa-*`).
- `DateField.vue` o'chirildi — o'rniga `AppDateInput`.

## 9. Tekshiruv qanday ishga tushiriladi

```bash
cd frontend
npm run lint            # oxfmt + oxlint + ESLint
npm run build
# UI testlari: backend ishlab turgan bo'lishi va admin login kerak
QCLI_API_BASE_URL=http://localhost:8081 npx quasar build      # (CSP connect-src ham shu portga)
UI_API=http://localhost:8081 UI_USER=... UI_PASS=... npm test   # 1440×900 o'lchamlar + 1920/2560/390 layout
npm run ui:audit        # after/ skrinshotlar + audit-after.json
node tests/ui/report-md.mjs   # shu hisobot
```
Test sahifa va elementni aytib yiqiladi, masalan: `crud-students: AppButton "Excel" height 36 ≠ 40`.

Lint qoidasi isboti (vaqtincha `ErrorNotFound.vue` ga `<q-input>` va `import { QBtn } from 'quasar'` qo'shilgan, keyin olib tashlangan):
```text
src\pages\ErrorNotFound.vue
  11:9   error  Use the design-system component from src/components/app/ (AppInput / AppDateInput)  vue/no-restricted-html-elements
  20:10  error  'QBtn' import from 'quasar' is restricted. Use the design-system component from src/components/app/  no-restricted-imports
✖ 2 problems (2 errors, 0 warnings)
```

## 10. Yo'l-yo'lakay topilgan, tuzatilmagan (backend)

- `GET /api/buildings` — bir xonaga bir vaqtda ikkita dars qo'yilgan bo'lsa, `BuildingService.toResponseDto` (`BuildingService.java:111`) «hozir dars bormi» so'rovida bitta natija kutadi va 500 qaytaradi (`IncorrectResultSizeDataAccessException`). Test bazasida shunday dars bor, shuning uchun Binolar sahifasi ma'lum soatlarda xato ko'rsatadi. Frontend vazifasiga kirmaydi.
