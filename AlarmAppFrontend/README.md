# AlarmAppFrontend

SvelteKit-Oberfläche der JF-Alarmierung. Abhängigkeiten, Build und Dev-Server laufen über **pnpm**.

## Voraussetzungen

- Node.js (siehe `packageManager` in `package.json`)
- [pnpm](https://pnpm.io/) 11

## Entwicklung

```sh
pnpm install
pnpm dev
```

Den Entwicklungsserver direkt im Browser öffnen:

```sh
pnpm dev --open
```

## Qualitätssicherung

```sh
pnpm test
pnpm check
pnpm lint
```

## Build

```sh
pnpm build
pnpm preview
```

Zum Deployen ggf. einen [Adapter](https://svelte.dev/docs/kit/adapters) für die Zielumgebung einrichten.

## MQTT-Broker

Die Verbindungsdaten liegen **nicht** in einer Vite-`.env` (die würde mitgebaut). Stattdessen liest die App zur Laufzeit JSON-Dateien aus `static/`:

1. `static/mqtt-config.local.json` (optional, nicht im Git, ohne Rebuild)
2. `static/mqtt-config.json` (mitgelieferte Standardwerte)

Beispiel für eine lokale Datei:

```sh
cp static/mqtt-config.local.json.example static/mqtt-config.local.json
```

Danach Host, Port, Topic oder Zugangsdaten anpassen und die Seite neu laden. Ein `pnpm build` ist dafür nicht nötig.
