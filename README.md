# JF-Alarmierung

Ein umfassendes Alarmbenachrichtigungssystem, das aus drei Hauptkomponenten besteht:
- **AlarmAppServer**: Mosquitto MQTT-Broker für den Nachrichtenrouting
- **AlarmAppFrontend**: Webbasierte Schnittstelle zum Senden von Alarmmeldungen
- **AlarmAppClient**: Android App zum Empfang von Alarmmeldungen
- **MessageSender**: Backend-Dienst zur Erstellung und Weiterleitung von Alarmen

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

## Systemarchitektur

```
┌─────────────────┐     MQTT      ┌─────────────────┐     Websocket  ┌──────────────────┐
│ MessageSender   │──────────────▶│  AlarmAppServer │◀───────────────│ AlarmAppFrontend │
│ (Java Backend)  │               │  (Mosquitto)    │                │ (React Web App)  │
└─────────────────┘               └────────┬────────┘                └──────────────────┘
                                           │ MQTT/WebSocket
                                           ▼
                                  ┌─────────────────┐
                                  │  AlarmAppClient │
                                  │  (Android App)  │
                                  └─────────────────┘
```

## Startreihenfolge

Die Komponenten müssen in folgender Reihenfolge gestartet werden:

1. **AlarmAppServer** (MQTT-Broker) - Zuerst starten
2. **AlarmAppFrontend** - Webinterface zum Senden von Alarmen
3. **MessageSender** - MQTT abonnieren und Nachrichten verarbeiten (bei Bedarf)

---

## Server (AlarmAppServer)

### Beschreibung

Mosquitto MQTT-Broker, der in Docker ausgeführt wird. Behandelt den Nachrichtenrouting zwischen allen Komponenten.

### Konfiguration

Der Server wird über `docker-compose.yml` konfiguriert:

| Port | Protokoll | Zweck |
|------|-----------|-------|
| `1883` | TCP | MQTT-Protokoll (für mobile Clients, Geräte) |
| `9001` | TCP/WebSocket | WebSocket (für browserbasierte Clients) |

### Verzeichnisstruktur

```
AlarmAppServer/
├── docker-compose.yml
└── mosquitto/
    ├── config/         # Konfigurationsdateien
    ├── data/           # Persistente Daten
    └── log/            # Protokolldateien
```

### Ausführungsbefehle

```bash
# Zum Server-Verzeichnis navigieren
cd AlarmAppServer

# Mosquitto-Broker starten
docker-compose up -d

# Protokolle anzeigen
docker-compose logs -f

# Broker stoppen
docker-compose down

# Broker neu starten
docker-compose restart
```

### Broker-Konfiguration

Die Mosquitto-Konfigurationsdatei (`mosquitto/config/mosquitto.conf`) enthält typischerweise:

- **Authentifizierung**: Benutzername/Passwort über `password_file`
- **Autorisierung**: Topic-basierte Zugriffskontrolle über `acl_file`
- **Persistenz**: Aktivieren, falls für Message Queuing benötigt
- **TLS/SSL**: Empfohlen für Produktion (Port 8883)

---

## Frontend (AlarmAppFrontend)

### Beschreibung

React-basierte Webanwendung zum Senden von Alarmmeldungen über MQTT mittels Websockets. Bietet eine benutzerfreundliche Schnittstelle zum Konfigurieren der Verbindungseinstellungen und Senden von Alarmbenachrichtigungen.

### Konfiguration

Das Frontend verwendet die folgenden Standardeinstellungen:

| Einstellung | Standardwert | Beschreibung |
|-------------|---------------|--------------|
| `brokerUrl` | `ws://localhost:9001` | WebSocket-URL zum MQTT-Broker |
| `topic` | `JF/Alarm` | MQTT-Topic für Alarmmeldungen |
| `username` | (leer) | Optionaler MQTT-Authentifizierungsbenutzername |
| `password` | (leer) | Optionales MQTT-Authentifizierungspasswort |
| `alarmstichwort` | (leer) | Alarmstichwort für die Alarmmeldung |
| `adresse` | (leer) | Adresse zu der Alarmierung |
| `infos` | (leer) | Weitere Informationen zur Alarmmeldung |

### Ausführungsbefehle

```bash
# Zum Frontend-Verzeichnis navigieren
cd AlarmAppFrontend

# Abhängigkeiten installieren
npm install

# Entwicklungsserver starten
npm run dev

# Für Produktion bauen
npm run build

# Produktions-Build anzeigen
npm run preview
```

## MessageSender

### Beschreibung

Java-basierter Backend-Dienst, der den MQTT-Broker abonniert und Alarmmeldungen sendet.
**Achtung:** Aktuell nur als Testclient verwendbar. Es ist nur eine Alarmmeldungen fest in den Quelldateien hinterlegt.
**TODO:** Alarmmeldung Konfiguration per API und zeitgesteuertes, automatisches Triggern der vorbereiteten Alarmmeldungen.

### Konfiguration

Die Konfiguration wird über `pom.xml` und Anwendungseigenschaften verwaltet:

| Einstellung | Beschreibung |
|-------------|--------------|
| `mqtt.broker.url` | MQTT-Broker-URL (z.B., `tcp://localhost:1883`) |
| `mqtt.client.id` | Eindeutige Client-Kennung für MQTT-Verbindung |
| `mqtt.topic.subscribe` | Topic zum Abonnieren eingehender Alarme |
| `mqtt.username` | MQTT-Authentifizierungsbenutzername (falls erforderlich) |
| `mqtt.password` | MQTT-Authentifizierungspasswort (falls erforderlich) |

### Ausführungsbefehle

```bash
# Zum MessageSender-Verzeichnis navigieren
cd MessageSender

# Anwendung bauen
mvn clean package

# Anwendung ausführen
java -jar target/messagesender-*.jar

# Oder mit Maven ausführen
mvn spring-boot:run
```

### MQTT-Nachrichtenformat

Das Frontend sendet Alarmmeldungen im folgenden Format:
```
ALARMSTICHWORT###ADRESSE###INFO
```

Die Felder sind durch `###` (drei Hash-Symbole) getrennt.

---

## AlarmAppClient

Android Applikation zum Empfangen der Alarmmeldungen.
Die App wird im Google Play Store bereit gestellt.
**TODO:** Link zum Playstore

### Konfiguration
Die Server und Topics, zu denen sich verbunden werden soll, kann in den Einstellungen eingestellt werden.
Die Einstellungen können entweder händisch oder durch das Scannen eines QR-Codes erfolgen.
Die Erstellung des QR-Codes wird weiter unten erläutert.

Damit der Dienst aktiv wird, muss er in den Einstellungen aktiviert werden.
Alle konfigurierten Server werden kontaktiert.
Sollte ein Server nicht kontaktiert werden können, wird der gesamte Dienst gestoppt.

---

## QR-Code für Android App generieren

Um einen QR-Code für die Android App zu generieren, erstellen Sie einen JSON-String mit folgendem Format:

```json
{
  "originator": "MSJones JF Alarm App",
  "ssl": true,
  "host": "mqtt.jf-alarm.example.com",
  "port": 8443,
  "username": "feuerwehr-alarm",
  "password": "secureToken123",
  "topic": "JF/Alarm"
}
```

### Felder:

| Feld | Beschreibung | Pflicht |
|------|--------------|---------|
| `originator` | Muss exakt "MSJones JF Alarm App" sein | ✓ |
| `host` | MQTT-Server-Hostname oder IP | ✓ |
| `port` | MQTT-Port (Standard: 1883) | ✓ |
| `username` | Authentifizierung Benutzername | ✗ |
| `password` | Authentifizierung Passwort | ✗ |
| `topic` | MQTT-Topic für Alarme | ✓ |

### QR-Code Generator Beispiel (JavaScript):

```javascript
function generateQrCode(settings) {
  const json = JSON.stringify({
    originator: "MSJones JF Alarm App",
    host: settings.host,
    port: settings.port,
    username: settings.username,
    password: settings.password,
    topic: settings.topic
  });

  // Verwenden Sie eine QR-Code Bibliothek wie qrcode.js
  return qrcode.toDataURL(json);
}
```

### Beispiel mit Online-Generatoren:

1. JSON-Objekt erstellen
2. JSON-String kopieren
3. In QR-Code Generator wie https://www.qr-code-generator.com/ einfügen
4. QR-Code herunterladen und verteilen

---

## Sicherheitsempfehlungen

1. **TLS/SSL verwenden**: TLS auf Mosquitto für verschlüsselte Kommunikation aktivieren
2. **Starke Authentifizierung**: Komplexe Passwörter verwenden und Zertifikatauthentifizierung in Betracht ziehen
3. **Topic-Einschränkungen**: Publish/Subscribe-Berechtigungen nach Benutzerrolle einschränken
4. **Firewall**: Zugriff auf MQTT-Ports auf vertrauenswürdige Netzwerke beschränken
5. **Regelmäßige Updates**: Docker-Images und Abhängigkeiten aktuell halten

## Fehlerbehebung

### Frontend-Verbindungsprobleme
- Überprüfen, ob Mosquitto läuft: `docker-compose ps`
- WebSocket-Port 9001 muss erreichbar sein
- Topic-Berechtigungen in Mosquitto ACL validieren

### Server-Probleme
- Protokolle prüfen: `docker-compose logs mosquitto`
- Port-Bindungen überprüfen: `netstat -tlnp | grep -E '1883|9001'`
- Sicherstellen, dass keine Firewall die Ports blockiert

### MessageSender-Probleme
- MQTT-Broker muss erreichbar sein
- Abonniertes Topic muss mit dem Frontend-Publish-Topic übereinstimmen
- Java-Anwendungsprotokolle auf Fehlerdetails prüfen
