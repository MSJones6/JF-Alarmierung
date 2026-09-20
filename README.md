# JF-Alarmierung

Ein umfassendes Alarmbenachrichtigungssystem, das aus folgenden Hauptkomponenten besteht:
- **AlarmAppServer**: Mosquitto MQTT-Broker und Alarm-API für Routing, Planung und Versand
- **AlarmAppFrontend**: Webbasierte Schnittstelle zum Senden von Alarmmeldungen
- **AlarmAppClient**: Android App zum Empfang von Alarmmeldungen

## Haftungsausschluss
Das komplette Projekt ist ein reines Hobby Projekt.
Die Entwickler können keine Garantie für die Funktionsfähigkeit oder Haftung jedweder Art übernehmen.
Bei Problemen könnt ihr uns jedoch gerne kontaktieren!
Wir sind bemüht, bei Problemen Hilfestellung zu geben.

---

## Beiträge und Feature-Vorschläge

Wir freuen uns über Beiträge, Verbesserungsvorschläge und neue Ideen! 
Das Projekt ist Open-Source und lebt von der Community.

### Wie ihr beitragen könnt:

- **Feature-Vorschläge**: Habt ihr eine Idee für ein neues Feature? 
  Lasst es uns wissen! Wir sind offen für Anregungen.
- **Bug-Reports**: Findet ihr einen Bug? Meldet ihn gerne.
- **Pull-Requests**: Direkte Verbesserungen sind willkommen – Code, Dokumentation, Übersetzungen.
- **Feedback**: Allgemeines Feedback hilft uns, das Projekt besser zu machen.

### ☕ Unterstützung
Wenn dir die App gefällt und du die Entwicklung der Jugendfeuerwehr-Alarmierung unterstützen möchtest, freue ich mich über einen Kaffee!

[![PayPal](https://img.shields.io/badge/PayPal-Spenden-blue.svg)](https://paypal.me/JFAlarmApp)

### Kontakt:

- GitHub Issues für Bug-Reports und Feature-Vorschläge
- Direkte Kontaktaufnahme bei größeren Ideen oder Fragen

Wir sind gespannt auf eure Vorschläge und bedanken uns für euer Interesse am Projekt!

---

## Schnellstart (empfohlen)

Frontend, Alarm-API, PostgreSQL und Mosquitto starten zusammen per Docker Compose. **Keine Programmierkenntnisse nötig.**

Schritt-für-Schritt: **[STARTEN.md](STARTEN.md)**

Kurzfassung:

1. [Docker Desktop](https://www.docker.com/products/docker-desktop/) installieren und starten.
2. Im Projektordner `start.bat` (Windows) doppelklicken oder `./start.sh` (Mac/Linux) ausführen.
3. Im Browser **http://localhost** öffnen.

Beenden mit `stop.bat` bzw. `./stop.sh`.

| Port | Dienst |
|------|--------|
| `80` | Weboberfläche (Frontend, Alarm-API unter `/api`) |
| `1883` | MQTT TCP für die Android-App |
| `9001` | MQTT WebSocket für die Alarm-API |
| `8081` | Adminer (optionale Datenbank-Oberfläche) |

---

## Systemarchitektur

```
┌──────────────────┐   REST /api    ┌─────────────────┐     MQTT      ┌─────────────────┐
│ AlarmAppFrontend │───────────────▶│ Alarm-API       │──────────────▶│ Mosquitto       │
│ (SvelteKit)      │                │ (Spring Boot)   │               │ (MQTT-Broker)   │
└──────────────────┘                └────────┬────────┘               └────────┬────────┘
                                             │ PostgreSQL                      │ MQTT
                                             ▼                                 ▼
                                    ┌─────────────────┐               ┌─────────────────┐
                                    │ Postgres        │               │ AlarmAppClient  │
                                    └─────────────────┘               │ (Android App)   │
                                                                      └─────────────────┘
```

Das Frontend plant und löst Alarme nur über die REST-API aus. MQTT versendet ausschließlich die Alarm-API über Mosquitto an die Android-App.

Entwicklung ohne vollständigen Stack: API, Postgres und Mosquitto über `AlarmAppServer/docker-compose.yml`, Frontend mit `pnpm dev` (siehe unten).

---

## Server (AlarmAppServer)

Unter `AlarmAppServer` liegen Mosquitto, die Alarm-API und die Backend-Compose-Datei.

Für den kompletten Stack inkl. Weboberfläche **[STARTEN.md](STARTEN.md)** bzw. `docker-compose.yml` im Projektstamm verwenden. `AlarmAppServer/docker-compose.yml` startet nur Broker, Postgres, Adminer und Alarm-API (ohne Frontend; API dann auf Port `8080`).

### Verzeichnisstruktur

```
AlarmAppServer/
├── docker-compose.yml
├── alarm-api/          # Spring-Boot REST-API
└── mosquitto/
    ├── config/         # mosquitto.conf, Passwort- und ACL-Datei
    ├── data/           # persistente Broker-Daten
    └── log/            # Protokolldateien
```

### Mosquitto

Der Broker leitet die von der Alarm-API veröffentlichten Alarme an die Android-App weiter.

| Port | Protokoll | Zweck |
|------|-----------|-------|
| `1883` | MQTT/TCP | Android-App |
| `9001` | MQTT/WebSocket | Alarm-API (nicht das Frontend) |

Aktuelle Broker-Konfiguration (`mosquitto/config/mosquitto.conf`):

- Authentifizierung über `password_file` (kein anonymer Zugriff)
- Topic-Rechte über `acl_file`
- Persistenz ist eingeschaltet
- TLS/SSL ist **nicht** vorkonfiguriert (für Produktion empfohlen, typisch Port `8883`)

```bash
cd AlarmAppServer
docker compose up -d
docker compose logs -f
docker compose down
```

---

## Alarm-API

Spring Boot 3 unter `AlarmAppServer/alarm-api` (Java 21). Die API speichert Connections, Alarmstichworte und Alarme in PostgreSQL, plant den Versand und veröffentlicht zum Zeitpunkt per MQTT über Mosquitto.

Im Docker-Stack ist sie intern auf Port `8080`; über die Weboberfläche erreichbar unter **http://localhost/api**. Lokal ohne Frontend: **http://127.0.0.1:8080**.

| Pfad | Zweck |
|------|--------|
| `GET /api/health` | Erreichbarkeit (`{"status":"UP"}`) |
| `/api/connections` | MQTT-Verbindungen |
| `/api/keywords` | Alarmstichworte |
| `/api/alarms` | Alarme anlegen, ändern, löschen, listen |
| `GET /api/alarms/stream` | Live-Updates (Server-Sent Events) |

Die API verbindet sich intern per WebSocket (`ws://…:9001/mqtt`) mit Mosquitto. In Docker ersetzt `ALARM_MQTT_HOST_OVERRIDE=mosquitto` den Host `localhost` aus gespeicherten Connections.

### MQTT-Nachrichtenformat

```
ALARMSTICHWORT###ADRESSE###INFO
```

Die Felder sind durch `###` (drei Hash-Symbole) getrennt. Dieselbe Zerlegung nutzt die Android-App.

---

## Frontend (AlarmAppFrontend)

SvelteKit-Oberfläche zum Planen und Auslösen von Alarmen. Sie spricht nur die REST-API, kein MQTT.

Verbindungen, Alarmstichworte und Alarme kommen ausschließlich von der API. Es gibt keine lokalen JSON-Fallbacks für Broker-Daten.

Die Datei `AlarmAppFrontend/static/api-config.json` setzt zur Laufzeit nur die API-Basis-URL (kein Rebuild). Im Docker-Image ist `apiBaseUrl` leer, damit der Browser denselben Ursprung (`/api`) nutzt. Für `pnpm dev` zeigt sie auf `http://127.0.0.1:8080`; fehlt sie, greift der Vite-Proxy `/api`.

```bash
cd AlarmAppFrontend
pnpm install
pnpm dev
```

Produktion über Docker Compose (siehe Schnellstart). Details: [AlarmAppFrontend/README.md](AlarmAppFrontend/README.md).

---

## AlarmAppClient

Android-App im Ordner `AlarmAppClient` zum Empfangen der Alarmmeldungen. MQTT über TCP-Port `1883` (optional SSL/TLS).

Verbindungen und Topics werden in den Einstellungen gepflegt, manuell oder per QR-Code (Format siehe unten). Jede Verbindung lässt sich einzeln aktivieren. Schlägt eine Verbindung fehl, wird nur diese deaktiviert; andere bleiben aktiv.

---

## QR-Code für die Android-App

JSON mit exakt diesem Originator; sonst lehnt die App den Code ab.

```json
{
  "originator": "MSJones JF Alarm App",
  "name": "Jugendfeuerwehr",
  "ssl": false,
  "host": "mqtt.example.com",
  "port": 1883,
  "username": "alarm",
  "password": "secret",
  "topic": "JF/Alarm/KB"
}
```

| Feld | Beschreibung | Pflicht |
|------|--------------|---------|
| `originator` | Muss exakt `MSJones JF Alarm App` sein | ✓ |
| `host` | MQTT-Hostname oder IP | ✓ |
| `port` | MQTT-Port (Standard `1883`, mit TLS oft `8883`) | ✓ |
| `ssl` | `true` für MQTT über TLS | ✗ |
| `name` | Anzeigename in der App | ✗ |
| `username` | MQTT-Benutzer | ✗ |
| `password` | MQTT-Passwort | ✗ |
| `topic` | MQTT-Topic, Standard in der App `JF/Alarm/KB` | ✓ |

JSON in einen QR-Generator (z. B. https://www.qr-code-generator.com/) einfügen und den Code verteilen.

---

## Sicherheitsempfehlungen

Die mitgelieferte Mosquitto-Konfiguration nutzt Benutzer/Passwort und ACLs, aber kein TLS.

1. **TLS/SSL**: Für den Betrieb nach außen MQTT verschlüsseln (typisch Port `8883`)
2. **Starke Passwörter**: Zugangsdaten nicht unnötig weitergeben; Dateien unter `mosquitto/config/` schützen
3. **Topic-Rechte**: Publish/Subscribe in der ACL nach Rolle einschränken
4. **Firewall**: MQTT-Ports nur aus vertrauenswürdigen Netzen
5. **Updates**: Docker-Images und Abhängigkeiten aktuell halten

## Fehlerbehebung

Befehle im **Projektstamm**, sofern der komplette Stack über die Root-Compose läuft.

### Frontend
- Stack prüfen: `docker compose ps`
- Health: http://localhost/api/health
- Entwicklung: Vite-Proxy `/api` bzw. `AlarmAppFrontend/static/api-config.json`

### Mosquitto
- Protokolle: `docker compose logs -f mosquitto`
- Ports `1883` (App) und `9001` (API) müssen frei und erreichbar sein

### Alarm-API
- Broker-Hostname in Docker: `ALARM_MQTT_HOST_OVERRIDE=mosquitto`
- Postgres muss gesund sein
- Protokolle: `docker compose logs -f alarm-api`
