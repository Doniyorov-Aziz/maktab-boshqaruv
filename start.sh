#!/usr/bin/env bash
# Starts the backend (Spring Boot, :8080) and frontend (Quasar dev server,
# :9000) together, with sane local defaults. Ctrl+C stops both.
set -e
cd "$(dirname "$0")"

echo "Backend ishga tushirilmoqda (http://localhost:8080)..."
./gradlew bootRun &
BACKEND_PID=$!

cd frontend
echo "Frontend ishga tushirilmoqda (http://localhost:9000)..."
npm run dev &
FRONTEND_PID=$!

trap 'kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null' EXIT INT TERM
wait
