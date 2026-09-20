# JF-Alarmierung starten

Du brauchst **kein** Programmierwissen. Es reicht Docker und ein Doppelklick.

## 1. Docker installieren

1. [Docker Desktop](https://www.docker.com/products/docker-desktop/) herunterladen und installieren.
2. Docker Desktop **starten** und warten, bis es bereit ist (grünes Icon).

## 2. Dieses Projekt holen

Den Ordner `JF-Alarmierung` auf den Rechner legen (ZIP entpacken oder mit Git klonen).

## 3. Alles bauen und starten

| System | Was tun |
|--------|---------|
| Windows | `start.bat` doppelklicken |
| Mac / Linux | Terminal im Projektordner öffnen und `./start.sh` ausführen |

Beim ersten Mal dauert der Download und der Build einige Minuten. Danach geht es schneller.

## 4. Im Browser öffnen

**http://localhost**

Die Android-App verbindet sich mit dem MQTT-Broker auf **Port 1883** (dieser Rechner).

Optionale Durchsage am Server (Gong plus Sprache): in `docker-compose.yml` bei `alarm-api` `ALARM_ANNOUNCEMENT_ENABLED=true` setzen und unter Linux `/dev/snd` freigeben – Details in der README unter „Durchsage auf dem Server“.

Optional: Datenbank ansehen unter http://localhost:8081 (Server `postgres`, Benutzer `alarm`, Passwort `alarm`).

## Beenden

| System | Was tun |
|--------|---------|
| Windows | `stop.bat` doppelklicken |
| Mac / Linux | `./stop.sh` |

Die gespeicherten Alarme bleiben in der Datenbank erhalten.

## Neu bauen nach einer Änderung

Einfach erneut `start.bat` / `./start.sh` ausführen. Es wird immer neu gebaut und dann gestartet.

## Wenn etwas nicht klappt

- **Docker läuft nicht:** Docker Desktop starten und den Start erneut versuchen.
- **Container-Namen oder Ports schon vergeben:** Falls ihr früher nur den Ordner `AlarmAppServer` gestartet habt, dort zuerst `docker compose down` ausführen und danach im Projektstamm erneut starten.
- **Port 80 ist schon belegt:** In `docker-compose.yml` bei `frontend` die Zeile `"80:80"` in `"8088:80"` ändern. Dann im Browser **http://localhost:8088** öffnen.
- **Logs ansehen:** Im Projektordner `docker compose logs -f` ausführen.
