@echo off
REM Starts backend (Spring Boot, :8080) and frontend (Quasar dev server,
REM :9000) in separate windows, with sane local defaults.
cd /d "%~dp0"

start "Maktab Boshqaruv - Backend" cmd /k "gradlew.bat bootRun"
start "Maktab Boshqaruv - Frontend" cmd /k "cd frontend && npm run dev"
