# Serverga qo'yish: ommaviy DEMO (Oracle Cloud Always Free)

Natija: `https://<domen>/#/demo` havolasini ochgan har kim **Mehmon** bo'lib kiradi. U hamma sahifani
ko'radi, lekin hech narsani o'zgartira olmaydi. Siz esa `https://<domen>/#/login` orqali administrator
sifatida kirasiz va hamma narsani qila olasiz.

Serverda uchta konteyner ishlaydi (`deploy/docker-compose.yml`):

| Konteyner | Vazifasi |
|---|---|
| `db` | PostgreSQL 17. Ma'lumotlar `pgdata` volume'ida saqlanadi. |
| `backend` | Spring Boot (Java 21), `server` profili (`src/main/resources/application-server.properties`). Fayllar `storage` volume'ida. |
| `web` | Caddy: frontend (statik sayt), `/api` → backend, avtomatik HTTPS (Let's Encrypt). |

Hamma sirlar (parollar, kalitlar, token) faqat serverdagi `deploy/.env` faylida turadi. Bu fayl gitga
tushmaydi.

> ⚠️ **Telegram bot bir vaqtda faqat bitta joyda ishlay oladi.** Bot o'z xabarlarini Telegram'dan
> o'zi so'rab oladi (getUpdates), ikki nusxa bir-biriga xalaqit beradi va xabarlar yo'qoladi. Serverda
> `TELEGRAM_BOT_ENABLED=true` qilsangiz, kompyuteringizdagi nusxada `TELEGRAM_ENABLED=false`
> qiling (`maktab-live\config\application-local.properties` da `telegram.enabled=false`). Oddiy demo
> uchun botni serverda yoqish shart emas: standart qiymat `false`.

---

## 1. Oracle Cloud akkaunti

1. <https://www.oracle.com/cloud/free/> → **Start for free**.
2. Mamlakat, email va telefonni kiriting. Karta faqat shaxsni tasdiqlash uchun so'raladi: Always Free
   resurslar uchun pul yechilmaydi.
3. **Home Region**ni tanlang (masalan, *Germany Central (Frankfurt)*). Uni keyin o'zgartirib bo'lmaydi.
   Ampere (ARM) VM'lar ba'zi regionlarda tez tugaydi: Frankfurt, Amsterdam yoki Stockholm odatda yaxshi.

## 2. Virtual mashina (VM) yaratish

1. Konsol → **Compute → Instances → Create instance**.
2. **Image**: *Canonical Ubuntu 24.04* (yoki 22.04).
3. **Shape** → *Change shape* → **Ampere** → `VM.Standard.A1.Flex`: **2 OCPU, 12 GB** RAM. Bu Always
   Free chegarasi ichida: jami 4 OCPU / 24 GB bepul.
4. **Networking**: yangi VCN va *public subnet*, **Assign a public IPv4 address** belgilangan bo'lsin.
5. **SSH keys**: *Generate a key pair for me* → ikkala kalitni ham yuklab oling (yoki o'zingizning
   `~/.ssh/id_ed25519.pub` faylingizni yuklang).
6. **Boot volume**: 50 GB yetarli (bepul chegara 200 GB).
7. **Create**. Bir-ikki daqiqada holati *Running* bo'ladi. **Public IP address**ni yozib oling.

> "Out of capacity" xatosi chiqsa, keyinroq qayta urinib ko'ring yoki boshqa *Availability Domain*ni
> tanlang.

IP o'zgarmasligi uchun: **Networking → IP Management → Reserved public IPs** → *Reserve*, so'ng VM'ning
VNIC'iga biriktiring. Bu ham bepul.

Serverga ulanish (Windows PowerShell):

```powershell
ssh -i C:\Users\<siz>\Downloads\ssh-key.key ubuntu@<PUBLIC_IP>
```

## 3. 80 va 443 portlarni ochish

**a) Oracle Security List** (bulut darajasidagi firewall):
Konsol → VM sahifasi → *Subnet* → *Security Lists* → *Default Security List* → **Add Ingress Rules**:

| Source CIDR | IP Protocol | Destination port |
|---|---|---|
| `0.0.0.0/0` | TCP | `80` |
| `0.0.0.0/0` | TCP | `443` |
| `0.0.0.0/0` | UDP | `443` (HTTP/3, ixtiyoriy) |

**b) Ubuntu ichidagi firewall.** Oracle'ning Ubuntu rasmlarida `iptables` qoidalari 22-portdan boshqa
hamma narsani yopib qo'ygan. Ularni oching va saqlang:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p udp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

Agar `ufw` ishlatsangiz (`sudo ufw status` → *active*):

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 443/udp
sudo ufw enable
```

## 4. Docker o'rnatish

```bash
sudo apt update && sudo apt -y upgrade
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu
exit        # chiqib, qayta ssh bilan kiring (docker guruhi kuchga kirishi uchun)
docker compose version   # tekshirish
```

Vaqt zonasi (zaxira cron'i Toshkent vaqtida ishlashi uchun):

```bash
sudo timedatectl set-timezone Asia/Tashkent
```

## 5. Bepul domen (DuckDNS)

1. <https://www.duckdns.org> → GitHub yoki Google bilan kiring.
2. *sub domain* maydoniga nom yozing, masalan `maktab-demo` → **add domain**.
3. **current ip** maydoniga serverning *Public IP*sini yozing → **update ip**.
4. Domeningiz: `maktab-demo.duckdns.org`. Tekshirish: `ping maktab-demo.duckdns.org` serverning IP
   manzilini ko'rsatishi kerak.

O'z domeningiz bo'lsa, DNS'da `A` yozuvini serverning IP manziliga yo'naltiring.

## 6. Loyihani yuklab olish

```bash
cd ~
git clone https://github.com/<github-login>/maktab-boshqaruv.git
cd maktab-boshqaruv/deploy
```

Repo yopiq (private) bo'lsa: GitHub → *Settings → Developer settings → Personal access tokens* →
faqat `repo: read` huquqli token yarating va parol o'rniga shuni kiriting.

## 7. `.env` faylini to'ldirish

```bash
cp .env.example .env
nano .env
```

Majburiy qiymatlar. Tasodifiy qiymatlarni shu buyruqlar bilan yarating va natijani `.env` ga qo'ying:

```bash
openssl rand -base64 24   # DB_PASSWORD
openssl rand -base64 64   # JWT_SECRET  (bir qatorda bo'lsin: | tr -d '\n')
openssl rand -base64 18   # ADMIN_PASSWORD
openssl rand -hex 32      # VISIT_HASH_SALT
```

```dotenv
DOMAIN=maktab-demo.duckdns.org
DB_PASSWORD=...
JWT_SECRET=...
ADMIN_PASSWORD=...
VISIT_HASH_SALT=...
```

Qolganlari ixtiyoriy, bo'sh qolsa standart qiymat olinadi: demo simulyator yoqilgan, mehmon kirishi
yoqilgan, Telegram bot o'chirilgan.

`chmod 600 .env`. Faylni hech kimga yubormang va gitga qo'shmang.

## 8. Ishga tushirish

```bash
docker compose up -d --build
```

Birinchi yig'ish ARM serverda 5–10 daqiqa oladi: Gradle va npm kutubxonalari yuklanadi.
Holatni tekshirish:

```bash
docker compose ps
docker compose logs -f backend    # "Started MaktabBoshqaruvApplication" chiqsa - tayyor (Ctrl+C - chiqish)
```

Birinchi ishga tushishda:

- baza bo'sh, shuning uchun namuna ma'lumotlar yaratiladi: 2 ta maktab, sinflar, o'quvchilar, xodimlar,
  jadval;
- demo simulyator davomat va baholarni o'zi to'ldiradi;
- `ADMIN_USERNAME` / `ADMIN_PASSWORD` bilan administrator yaratiladi. Ommaviy serverda
  `direktor/direktor123` kabi namuna hisoblar **yaratilmaydi**.

Tekshirish:

- `https://<domen>/#/login` → administrator bo'lib kiring;
- `https://<domen>/#/demo?ref=linkedin` → mehmon bo'lib kiring (boshqa brauzerda yoki yashirin oynada);
- administrator panelida **Boshqaruv → Tashriflar**. Bosh sahifada esa
  "👀 Demo tashriflari: N kishi" vidjeti.

Sertifikat chiqmasa (brauzer "xavfsiz emas" desa): `docker compose logs web`. Odatda sabab: domen
hali serverning IP manziliga yo'nalmagan yoki 80/443 portlar yopiq (3-bo'lim).

## 9. Yangilash

```bash
cd ~/maktab-boshqaruv
git pull
cd deploy
./backup.sh                      # avval zaxira
docker compose up -d --build     # faqat o'zgargan konteynerlar qayta yig'iladi
docker image prune -f            # eski rasmlarni tozalash
```

## 10. Zaxira (backup) va tiklash

Kunlik zaxira: `deploy/backup.sh` bazani `deploy/backups/maktab_YYYY-MM-DD_HHMM.sql.gz` ga yozadi va
7 kundan eskilarini o'chiradi.

```bash
chmod +x ~/maktab-boshqaruv/deploy/*.sh
~/maktab-boshqaruv/deploy/backup.sh          # qo'lda sinab ko'ring
crontab -e
```

Cron'ga qo'shing (har kuni 02:30):

```cron
30 2 * * * /home/ubuntu/maktab-boshqaruv/deploy/backup.sh >> /home/ubuntu/maktab-backup.log 2>&1
```

Tiklash (backend vaqtincha to'xtaydi):

```bash
cd ~/maktab-boshqaruv/deploy
./restore.sh backups/maktab_2026-10-26_0230.sql.gz
```

Fayllar (murojaat ilovalari, kutubxona PDF lari) `storage` volume'ida turadi. Ularni ham saqlab
qo'ymoqchi bo'lsangiz:

```bash
docker run --rm -v maktab_storage:/data -v ~/maktab-boshqaruv/deploy/backups:/out alpine \
  tar czf /out/storage_$(date +%F).tar.gz -C /data .
```

Zaxiralarni vaqti-vaqti bilan serverdan tashqariga ham ko'chiring:
`scp ubuntu@<IP>:~/maktab-boshqaruv/deploy/backups/*.gz .`

## 11. Loglar

```bash
docker compose logs -f backend          # backend
docker compose logs -f web              # Caddy (sertifikat, so'rovlar)
docker compose logs --tail 200 db       # PostgreSQL
docker stats                            # xotira va CPU
```

## 12. Boshqarish

| Nima | Qanday |
|---|---|
| Demo havolasini vaqtincha yopish | `.env`: `GUEST_LOGIN_ENABLED=false` → `docker compose up -d`. `#/demo` "Demo hozircha o'chirilgan" deydi, ochiq mehmon tokenlari darhol ishlamay qoladi. |
| Simulyatorni o'chirish | `.env`: `DEMO_ENABLED=false` → `docker compose up -d` |
| Administrator parolini o'zgartirish | Panel → Foydalanuvchilar → admin → tahrirlash |
| To'xtatish / ishga tushirish | `docker compose stop` / `docker compose start` |
| Hammasini o'chirish (ma'lumotlar bilan!) | `docker compose down -v` |

## 13. Xavfsizlik bo'yicha eslatmalar

- Mehmon (GUEST) uchun backend faqat GET/HEAD/OPTIONS so'rovlarini qabul qiladi. Qolganlariga 403 qaytadi:
  qoida filtr darajasida, shuning uchun keyin qo'shiladigan endpointlar ham avtomatik himoyalanadi.
- Mehmon ota-onalar murojaatlari va ularning fayllarini, Telegram xabarlari tarixini, ulash kodlarini,
  API kalitlarini va tashriflar statistikasini ko'ra olmaydi. Telegram chat_id va tokenlar javoblarda
  maskalanadi.
- Kirishga urinishlar cheklangan: login — bitta IP dan 5 daqiqada 10 ta, demo kirish — soatiga 30 ta.
- Tashrifchilarning IP manzili saqlanmaydi, faqat `VISIT_HASH_SALT` bilan olingan SHA-256 xeshi.
- SSH faqat kalit bilan. `PasswordAuthentication no` Ubuntu'da Oracle tomonidan allaqachon o'rnatilgan.
