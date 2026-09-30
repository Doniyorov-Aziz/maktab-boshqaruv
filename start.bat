@echo off
REM Starts backend (Spring Boot, :8080) and frontend (Quasar dev server,
REM :9000) in separate windows, with sane local defaults.
cd /d "%~dp0"

REM DB paroli va JWT secret src\main\resources\application-local.properties da
REM (gitignore'da) turadi va faqat "local" profilda yuklanadi.
if "%SPRING_PROFILES_ACTIVE%"=="" set SPRING_PROFILES_ACTIVE=local
if "%SPRING_PROFILES_ACTIVE%"=="local" if not exist "src\main\resources\application-local.properties" (
  echo src\main\resources\application-local.properties topilmadi.
  echo application-local.properties.example dan nusxa olib, qiymatlarni to'ldiring.
  exit /b 1
)

start "Maktab Boshqaruv - Backend" cmd /k "gradlew.bat bootRun"
start "Maktab Boshqaruv - Frontend" cmd /k "cd frontend && npm run dev"
