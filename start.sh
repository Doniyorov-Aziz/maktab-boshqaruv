#!/usr/bin/env bash
# Starts the backend (Spring Boot, :8080) and frontend (Quasar dev server,
# :9000) together, with sane local defaults. Ctrl+C stops both.
set -e
cd "$(dirname "$0")"

# DB paroli va JWT secret src/main/resources/application-local.properties da
# (gitignore'da) turadi va faqat "local" profilda yuklanadi.
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
if [ "$SPRING_PROFILES_ACTIVE" = "local" ] && [ ! -f src/main/resources/application-local.properties ]; then
  echo "src/main/resources/application-local.properties topilmadi." >&2
  echo "application-local.properties.example dan nusxa olib, qiymatlarni to'ldiring." >&2
  exit 1
fi

echo "Backend ishga tushirilmoqda (http://localhost:8080, profil: $SPRING_PROFILES_ACTIVE)..."
./gradlew bootRun &
BACKEND_PID=$!

cd frontend
echo "Frontend ishga tushirilmoqda (http://localhost:9000)..."
npm run dev &
FRONTEND_PID=$!

trap 'kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null' EXIT INT TERM
wait
