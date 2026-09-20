#!/usr/bin/env bash
# Hält alle Docker-Compose-Dienste an.
set -euo pipefail
cd "$(dirname "$0")"
docker compose down
echo "Alle Dienste wurden gestoppt."
