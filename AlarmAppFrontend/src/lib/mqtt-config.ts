import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import type { MqttSettings } from './types';

/** Öffentliche URL der mitgelieferten Standardkonfiguration. */
export const MQTT_CONFIG_URL = '/mqtt-config.json';

/** Öffentliche URL der optionalen lokalen Überschreibung, ohne Neu-Build. */
export const MQTT_LOCAL_CONFIG_URL = '/mqtt-config.local.json';

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
 * Lädt eine Konfigurationsdatei ohne Browser-Cache.
 *
 * @param url relative URL unter `static/`
 * @param fetchFn optionale Fetch-Funktion, überschreibbar in Tests
 * @returns Einstellungen oder `null`, wenn die Datei fehlt oder ungültig ist
 */
async function fetchMqttConfig(
	url: string,
	fetchFn: typeof fetch
): Promise<MqttSettings | null> {
	try {
		const response = await fetchFn(url, { cache: 'no-store' });
		if (!response.ok) {
			return null;
		}
		return parseMqttConfig(await response.json());
	} catch {
		return null;
	}
}

/**
 * Lädt die MQTT-Broker-Daten zur Laufzeit aus den JSON-Dateien unter `static/`.
 *
 * Zuerst wird `mqtt-config.local.json` gelesen, danach `mqtt-config.json`.
 * So lassen sich Host, Port und Topic ohne erneutes Bauen ändern.
 *
 * @param fetchFn optionale Fetch-Funktion, überschreibbar in Tests
 * @returns geladene oder Standard-Einstellungen
 */
export async function loadMqttConfig(fetchFn: typeof fetch = fetch): Promise<MqttSettings> {
	const localConfig = await fetchMqttConfig(MQTT_LOCAL_CONFIG_URL, fetchFn);
	if (localConfig) {
		return localConfig;
	}

	const sharedConfig = await fetchMqttConfig(MQTT_CONFIG_URL, fetchFn);
	if (sharedConfig) {
		return sharedConfig;
	}

	return { ...DEFAULT_MQTT_SETTINGS };
}
