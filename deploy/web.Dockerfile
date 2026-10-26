# Frontend (Quasar) + Caddy: statik sayt, /api -> backend, avtomatik HTTPS (Let's Encrypt).
# Build konteksti - repo ildizi (deploy/docker-compose.yml: context: ..).

FROM node:22-bookworm-slim AS build
WORKDIR /web
COPY frontend/ ./
# API manzili: "/" - sayt va /api bitta domenda (Caddy). Boshqa manzil kerak bo'lsa:
#   docker compose build --build-arg API_BASE_URL=https://api.example.uz web
ARG API_BASE_URL=/
RUN npm ci --no-audit --no-fund \
    && printf 'QCLI_API_BASE_URL=%s\n' "$API_BASE_URL" > .env \
    && npx quasar build

FROM caddy:2-alpine
COPY deploy/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /web/dist/spa /srv
