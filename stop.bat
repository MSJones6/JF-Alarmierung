@echo off
setlocal
cd /d "%~dp0"
docker compose down
echo Alle Dienste wurden gestoppt.
pause
