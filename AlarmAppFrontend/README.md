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

Zum Deployen mit Docker Compose siehe **[STARTEN.md](../STARTEN.md)** im Projektstamm. Der Docker-Build erzeugt ein statisches SPA hinter nginx.

Zum manuellen Deployen ggf. einen [Adapter](https://svelte.dev/docs/kit/adapters) für die Zielumgebung einrichten.

## API

Verbindungen, Alarmstichworte und Alarme kommen ausschließlich vom Spring-Server. Die optionale Datei `static/api-config.json` setzt nur die API-Basis-URL zur Laufzeit (kein Rebuild). Fehlt sie oder ist sie ungültig, verwendet das Frontend denselben Ursprung bzw. den Vite-Proxy `/api`.

Broker-Zugangsdaten werden in der API gepflegt, nicht im Frontend.
