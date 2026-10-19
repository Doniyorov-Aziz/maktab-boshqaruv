# Davomat olish va Baholar jurnali — v2

Branch: `feature/attendance-journal` (worktree `.claude/worktrees/attendance-journal`, `main` fb3ac3c dan). Push qilinmagan.
Sessiya uzilsa — quyidagi ❌ bandlardan davom eting. **Backend testlari faqat `DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test` bilan.**

## Reja
1. Audit va «oldin» rasmlari (`docs/images/attendance-journal/before-*.png`).
2. Xatolar 1.1–1.5.
3. Backend: `/api/pages/attendance/*` (kun, ro'yxat + tarix, saqlash), `/api/pages/journal/*` (o'quvchi, ustun davomati, eksport), testlar.
4. Davomat sahifasi.
5. Jurnal sahifasi.
6. Playwright, «keyin» rasmlari, hisobot.

## Holat
- ✅ Audit: 1.1 sabab — `q-btn-toggle` tugmalari orasida bo'shliq va chegara yo'q; 1.2 sabab — «Dars» ro'yxati sinfning BUTUN haftasini ko'rsatardi (hafta kuni bo'yicha alifbo tartibida, shuning uchun dushanbada «Chorshanba» birinchi); 1.4 sabab — parallel tugmalari `AppButton` (primary = to'ldirilgan) edi; 1.5 sabab — fan faqat o'qituvchining joriy darsidan olinardi, admin uchun bo'sh qolardi.
- ✅ «Oldin» rasmlari: 8 ta (davomat va jurnal × 1920 dark/light, 1366, 390)
