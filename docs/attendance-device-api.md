# Qurilma orqali davomat (turniket, QR, Face ID)

Maktabga kirishdagi qurilma o'quvchi kelganini tizimga yuboradi va davomat o'zi qo'yiladi.
Hozircha qurilma ulanmagan — bu interfeys kelajak uchun tayyor turibdi.

## 1. API kalit

ADMIN «Maktab sozlamalari» sahifasida qurilma uchun kalit yaratadi (masalan «Asosiy kirish turniketi»).
Kalit **faqat bir marta** ko'rsatiladi; bazada uning SHA-256 xeshi saqlanadi, o'zi hech qayerda yozilmaydi.
Kalitni qurilma sozlamasiga kiriting. Kerak bo'lmay qolsa — «Bekor qilish»: shu zahoti ishlamay qoladi.

| Amal | So'rov | Kim |
|---|---|---|
| Ro'yxat | `GET /api/device-keys?schoolId=1` | ADMIN |
| Yaratish | `POST /api/device-keys` `{"schoolId": 1, "name": "Turniket"}` → `{"key": {...}, "apiKey": "mk_…"}` | ADMIN |
| Bekor qilish | `DELETE /api/device-keys/{id}` | ADMIN |

## 2. Kelishni yuborish

```http
POST /api/attendance/checkin
X-Api-Key: <qurilma kaliti>
Content-Type: application/json

{
  "studentExternalId": "123",
  "cardId": null,
  "timestamp": "2026-10-16T08:47:00+05:00",
  "deviceId": "turniket-1"
}
```

| Maydon | Ma'nosi |
|---|---|
| `studentExternalId` | o'quvchining tizimdagi raqami (id) |
| `cardId` | yoki karta / bilaguzuk raqami (o'quvchida `card_id` saqlangan bo'lsa); ikkalasidan biri kerak |
| `timestamp` | ISO-8601; offset bo'lmasa Toshkent vaqti deb olinadi; bo'sh bo'lsa — hozir |
| `deviceId` | qaysi qurilma (izohda ko'rinadi), ixtiyoriy |

Javob:

```json
{"studentId": 123, "studentName": "Ali Valiyev", "status": "LATE", "lateMinutes": 17,
 "lessonsMarked": 4, "lessonsKeptManual": 1}
```

## 3. Qoidalar

- Sinfning shu kungi **birinchi darsi boshlanishigacha** kelgan → `Keldi` (PRESENT).
- Undan keyin kelgan → o'quvchi kirgan dars `Kechikdi` (LATE), `lateMinutes` = birinchi dars boshlanishidan beri o'tgan daqiqa; keyingi darslar `Keldi`.
- Kelishdan oldin tugagan darslarga hech narsa yozilmaydi.
- Manba: `DEVICE`. Qurilma avtomatik belgilarni (`AUTO_CARRY`, `DEMO_SIM`) almashtiradi, lekin **xodim qo'ygan (`MANUAL`) belgiga tegmaydi** — bunday darslar `lessonsKeptManual` da sanaladi.
- `DEVICE` belgisi haqiqiy hisoblanadi: ota-onaga «kechikdi» xabari boradi, o'qituvchi intizomida «olingan» deb sanaladi.
- Yakshanba, bayram yoki ta'til kuni → 409 «dam olish kuni».

## 4. Xatolar

| Kod | Qachon |
|---|---|
| 401 | kalit yo'q, noto'g'ri yoki bekor qilingan |
| 404 | o'quvchi topilmadi yoki boshqa maktabniki |
| 400 | `studentExternalId` ham `cardId` ham yo'q, `timestamp` formati noto'g'ri |
| 409 | dam olish kuni yoki sinfning shu kunda darsi yo'q |

## 5. Sinab ko'rish

```bash
curl -X POST http://localhost:8080/api/attendance/checkin \
  -H "X-Api-Key: <kalit>" -H "Content-Type: application/json" \
  -d '{"studentExternalId":"123","timestamp":"2026-10-16T08:47:00+05:00","deviceId":"T1"}'
```

Test: `AttendanceAutomationIntegrationTest.checkin_needsAValidKey_andMarksLateWithMinutes`.
