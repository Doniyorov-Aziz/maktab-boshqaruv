# Baholar jurnali (katakli jurnal) — progress

Branch: `feature/journal` (`main` = 557dd55 dan). Worktree: `.claude/worktrees/v4`. Push yo'q.
Sessiya uzilsa — shu fayldan davom eting.

## 0. Audit

| Narsa | Qayerda | Holat |
|---|---|---|
| Baholar sahifasi | `frontend/src/pages/GradebookPage.vue` — sinf/fan select, Dan/Gacha, sanalar bo'yicha jadval, bitta baho dialogi | qayta quriladi |
| Baho | `Grade` (student, subject, gradeDate, score 2–5, type CURRENT/EXAM/QUARTERLY, comment, createdDate) | `createdBy` YO'Q → qo'shiladi |
| Baho validatsiyasi | `GradeService.validateGradeDate` — yakshanba/kelajak 409, `@Min(2) @Max(5)` → 400 | yakshanba matni spec bo'yicha o'zgartiriladi |
| Bahoni o'chirish | `DELETE /api/grades/{id}` faqat ADMIN | EDITOR+ ga ochiladi (spec 7) |
| Sinf rahbari | `SchoolClass.classTeacher` (Employee) | BOR |
| Xodim telefoni | `Employee.phone` | BOR |
| Dars jadvali | `LessonSlot` (class, subject, employee, room, weekday, startTime, endTime) | BOR |
| Qo'ng'iroq jadvali (dars raqami) | alohida jadval yo'q — sinfning turli boshlanish vaqtlari tartibi = dars raqami (PDF ham shunday) | shu qoida ishlatiladi |
| Bayramlar | `CalendarEvent` HOLIDAY / VACATION (`AttendanceCalendarService.holidays`) | BOR |
| Vaqt | `Clock` bean, Asia/Tashkent (`TelegramConfig`) | BOR — testda soxta soat |
| Sinf tanlash | `AttendanceCalendarPage.vue` ichida (parallel tugmalari + harflar) | umumiy `ClassPicker.vue` ga chiqariladi |
| O'quvchisi bor sinflar | `SchoolClassResponseDto.studentCount` | BOR |

## Reja va holat

| # | Band | Holat |
|---|---|---|
| 0 | Audit + shu fayl | ✅ |
| 1.1 | Umumiy `ClassPicker` (davomat va jurnal), bo'sh sinflar xira, URL `?class&subject&month` | ⏳ |
| 1.2 | Fan select (sinfda o'tiladigan faol fanlar) + «O'qituvchi: …» | ⏳ |
| 1.3 | Oy almashtirgich + «Bugun», Dan/Gacha olib tashlanadi | ⏳ |
| 2 | Sinf paneli: rahbar + telefon (tel:, nusxalash), hozirgi dars / tanaffus / boshlanmagan / tugadi / dam olish, progress, «Shu darsga baho qo'yish», bugungi jadval chizig'i, o'qituvchi uchun avto tanlov | ⏳ |
| 3 | Jurnal jadvali: kunlar ustunlari, sticky, bugun, chiplar, doim ko'rinadigan «+», bitta popover, optimistik saqlash, o'chirish + undo, klaviatura | ⏳ |
| 4 | Yakshanba / bayram / kelajak: kulrang, «+» yo'q, xabar; backend 409/400 | ⏳ |
| 5 | Pastki qator (sinf o'rtachasi), statistika chiplari, bo'sh holat, butun sinfga tez baho dialogi | ⏳ |
| 6 | `GET /api/grades/journal`, `GET /api/classes/{id}/overview`, `GET /api/me/current-lesson` (1–3 SQL) | ⏳ |
| 7 | Ruxsatlar: ko'rish VIEWER+, baho EDITOR+, school_id | ⏳ |
| 8 | Dizayn: tokenlar, dark/light, 1366/1920, mobil | ⏳ |
| T | Tekshiruv 1–5 | ⏳ |
