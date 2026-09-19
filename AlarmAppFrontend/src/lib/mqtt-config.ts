/**
 * Bereinigt Einstellungen aus dem Dialog.
 *
 * Verbindungen und Stichworte kommen nur von der REST-API, nicht aus lokalen JSON-Dateien.
 */
import { DEFAULT_KEYWORD_OPTIONS, getKeywordColor } from './alarm';
import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import { createTopicConnection } from './topic-connection';
import type { AppSettings, KeywordOption, MqttSettings, TopicConnection } from './types';

/** Standard-Connection für unvollständige Einstellungen im Dialog. */
export const DEFAULT_TOPIC_CONNECTION: TopicConnection = createTopicConnection({
	id: 'standard',
	name: 'Standard',
	mqttTopic: 'JF/Alarm/KB'
});

/** Fallback-Werte zum Bereinigen unvollständiger Einstellungen im Dialog. */
export const DEFAULT_APP_SETTINGS: AppSettings = {
	keywords: DEFAULT_KEYWORD_OPTIONS.map((keyword) => ({ ...keyword })),
	topics: [{ ...DEFAULT_TOPIC_CONNECTION }]
};

/**
 * Wandelt unbekannte JSON-Daten in MQTT-Einstellungen um.
 *
 * Unbekannte oder fehlende Felder werden durch die Standardwerte ersetzt.
 * Eine numerische Portangabe wird als Text übernommen.
 *
 * @param raw geparstes JSON
 * @returns vollständige Broker-Einstellungen
 */
export function parseMqttConfig(raw: unknown): MqttSettings {
	const data =
		raw !== null && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};

	return {
		useSsl: typeof data.useSsl === 'boolean' ? data.useSsl : DEFAULT_MQTT_SETTINGS.useSsl,
		brokerHost: readText(data.brokerHost, DEFAULT_MQTT_SETTINGS.brokerHost),
		brokerPort: readText(data.brokerPort, DEFAULT_MQTT_SETTINGS.brokerPort),
		brokerPath: readText(data.brokerPath, DEFAULT_MQTT_SETTINGS.brokerPath),
		user: readText(data.user, DEFAULT_MQTT_SETTINGS.user),
		password: readText(data.password, DEFAULT_MQTT_SETTINGS.password),
		mqttTopic: readText(data.mqttTopic, DEFAULT_MQTT_SETTINGS.mqttTopic)
	};
}

/**
 * Wandelt unbekannte JSON-Daten in eine Connection um.
 *
 * @param raw geparstes JSON
 * @param fallback Broker-Werte, wenn Felder fehlen
 * @returns Connection
 */
export function parseTopicConnection(
	raw: unknown,
	fallback: MqttSettings = DEFAULT_MQTT_SETTINGS
): TopicConnection {
	const data =
		raw !== null && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
	const mqtt = parseMqttConfig({ ...fallback, ...data });
	const name = readText(data.name, 'Standard');
	const id = readText(data.id, name);

	return {
		...mqtt,
		id,
		name
	};
}

/**
 * Wandelt unbekannte JSON-Daten in vollständige App-Einstellungen um.
 *
 * Jede Connection trägt eigene Host-, Konto- und Broker-Daten. Alte Namenslisten
 * werden mit den globalen Broker-Feldern zu Verbindungen ergänzt.
 *
 * @param raw geparstes JSON
 * @returns Stichworte und Connections
 */
export function parseAppSettings(raw: unknown): AppSettings {
	const data =
		raw !== null && typeof raw === 'object' ? (raw as Record<string, unknown>) : {};
	const mqttFallback = parseMqttConfig(data);

	return {
		keywords: parseKeywords(data.keywords),
		topics: parseTopicConnections(data.topics, mqttFallback)
	};
}

/**
 * Liest einen Text- oder Zahlenwert aus der Konfiguration.
 *
 * @param value Rohwert aus JSON
 * @param fallback Standardwert
 * @returns Textwert
 */
function readText(value: unknown, fallback: string): string {
	if (typeof value === 'string' && value.trim().length > 0) {
		return value;
	}
	if (typeof value === 'number' && Number.isFinite(value)) {
		return String(value);
	}
	return fallback;
}

/**
 * Liest Alarmstichworte inklusive Farbe oder fällt auf die Standardwerte zurück.
 *
 * Alte Namenslisten ohne Farbe werden mit den bisherigen Badge-Farben ergänzt.
 *
 * @param value Rohwert aus JSON
 * @returns bereinigte Stichwortliste
 */
function parseKeywords(value: unknown): KeywordOption[] {
	if (!Array.isArray(value) || value.length === 0) {
		return DEFAULT_KEYWORD_OPTIONS.map((keyword) => ({ ...keyword }));
	}

	const unique: KeywordOption[] = [];
	const seen = new Set<string>();

	for (const item of value) {
		const parsed = parseKeywordOption(item);
		if (!parsed) {
			continue;
		}
		const key = parsed.name.toLocaleLowerCase('de-DE');
		if (seen.has(key)) {
			continue;
		}
		seen.add(key);
		unique.push(parsed);
	}

	if (unique.length === 0) {
		return DEFAULT_KEYWORD_OPTIONS.map((keyword) => ({ ...keyword }));
	}
	return unique;
}

/**
 * Wandelt einen Listen-Eintrag in ein Alarmstichwort mit Farbe um.
 *
 * @param raw Rohwert aus JSON
 * @returns Stichwort oder `null`
 */
function parseKeywordOption(raw: unknown): KeywordOption | null {
	if (typeof raw === 'string') {
		const name = raw.trim();
		if (!name) {
			return null;
		}
		return { name, color: getKeywordColor(name) };
	}
	if (raw === null || typeof raw !== 'object') {
		return null;
	}
	const data = raw as Record<string, unknown>;
	const name = typeof data.name === 'string' ? data.name.trim() : '';
	if (!name) {
		return null;
	}
	return {
		name,
		color: getKeywordColor(name, typeof data.color === 'string' ? data.color : undefined)
	};
}

/**
 * Liest Connections aus JSON.
 *
 * @param raw Rohwert aus JSON
 * @param fallback Broker-Werte für Namenslisten und fehlende Felder
 * @returns Connections
 */
function parseTopicConnections(raw: unknown, fallback: MqttSettings): TopicConnection[] {
	if (!Array.isArray(raw) || raw.length === 0) {
		return [
			parseTopicConnection(
				{ ...fallback, name: 'Standard', id: 'standard' },
				fallback
			)
		];
	}

	const connections: TopicConnection[] = [];
	const usedIds = new Set<string>();

	for (const item of raw) {
		const parsed =
			typeof item === 'string'
				? parseTopicConnection({ ...fallback, name: item.trim(), id: item.trim() }, fallback)
				: parseTopicConnection(item, fallback);
		if (!parsed.name) {
			continue;
		}
		let { id } = parsed;
		if (usedIds.has(id)) {
			id = `${id}-${connections.length}`;
		}
		usedIds.add(id);
		connections.push({ ...parsed, id });
	}

	if (connections.length === 0) {
		return [
			parseTopicConnection({ ...fallback, name: 'Standard', id: 'standard' }, fallback)
		];
	}

	return connections;
}
