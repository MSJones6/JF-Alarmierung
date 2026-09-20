@echo off
setlocal
cd /d "%~dp0"

where docker >nul 2>&1
if errorlevel 1 (
	echo Docker wurde nicht gefunden.
	echo Bitte Docker Desktop installieren und danach start.bat erneut starten:
	echo https://www.docker.com/products/docker-desktop/
	pause
	exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
	echo Docker laeuft nicht. Bitte Docker Desktop starten und es erneut versuchen.
	pause
	exit /b 1
)

echo Baue und starte alle Dienste (Frontend, Backend, Mosquitto) ...
docker compose up --build -d
if errorlevel 1 (
	echo Start fehlgeschlagen. Siehe Meldung oben.
	pause
	exit /b 1
)

echo.
echo Fertig.
echo Weboberflaeche:  http://localhost
echo Datenbank-UI:    http://localhost:8081  (optional, Adminer)
echo MQTT:            Port 1883
echo.
echo Zum Beenden: stop.bat doppelklicken.
start "" "http://localhost"
pause
