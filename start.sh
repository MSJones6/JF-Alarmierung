#!/usr/bin/env bash
# Baut alle Images neu und startet Frontend, API, Datenbank und Mosquitto.
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v docker >/dev/null 2>&1; then
	echo "Docker wurde nicht gefunden."
	echo "Bitte Docker Desktop installieren und danach dieses Skript erneut starten:"
	echo "https://www.docker.com/products/docker-desktop/"
	exit 1
fi

if ! docker info >/dev/null 2>&1; then
	echo "Docker läuft nicht. Bitte Docker Desktop starten und es erneut versuchen."
	exit 1
fi

echo "Baue und starte alle Dienste (Frontend, Backend, Mosquitto) ..."
docker compose up --build -d

echo
echo "Fertig."
echo "Weboberfläche:  http://localhost"
echo "Datenbank-UI:   http://localhost:8081  (optional, Adminer)"
echo "MQTT:           Port 1883"
echo
echo "Zum Beenden: ./stop.sh"
