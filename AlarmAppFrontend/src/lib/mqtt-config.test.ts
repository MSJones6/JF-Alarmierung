import { describe, expect, it, vi } from 'vitest';
import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import {
	loadMqttConfig,
	loadStoredMqttSettings,
	MQTT_CONFIG_URL,
	MQTT_LOCAL_CONFIG_URL,
	MQTT_SETTINGS_STORAGE_KEY,
	parseMqttConfig,
	saveMqttSettings
} from './mqtt-config';
import type { StorageLike } from './types';

/**
 * Einfacher In-Memory-Storage für die Konfigurationstests.
 */
class MemoryStorage implements StorageLike {
	private readonly values = new Map<string, string>();

	/**
	 * Liest einen gespeicherten Wert.
	 */
	getItem(key: string): string | null {
		return this.values.get(key) ?? null;
	}

	/**
	 * Speichert einen Wert.
	 */
	setItem(key: string, value: string): void {
		this.values.set(key, value);
	}
}

describe('parseMqttConfig', () => {
	it('übernimmt gültige Felder und fällt sonst auf Standardwerte zurück', () => {
		expect(
			parseMqttConfig({
				useSsl: true,
				brokerHost: 'broker.local',
				brokerPort: 8084,
				mqttTopic: 'JF/Test'
			})
		).toEqual({
			...DEFAULT_MQTT_SETTINGS,
			useSsl: true,
			brokerHost: 'broker.local',
			brokerPort: '8084',
			mqttTopic: 'JF/Test'
		});
	});

	it('ignoriert unbrauchbare Nutzdaten', () => {
		expect(parseMqttConfig(null)).toEqual(DEFAULT_MQTT_SETTINGS);
		expect(parseMqttConfig('ungueltig')).toEqual(DEFAULT_MQTT_SETTINGS);
	});
});

describe('gespeicherte MQTT-Einstellungen', () => {
	it('rundet geänderte Broker-Daten über den Storage', () => {
		const storage = new MemoryStorage();
		const settings = {
			...DEFAULT_MQTT_SETTINGS,
			brokerHost: 'saved-broker',
			mqttTopic: 'JF/Saved'
		};

		saveMqttSettings(settings, storage);

		expect(storage.getItem(MQTT_SETTINGS_STORAGE_KEY)).toContain('saved-broker');
		expect(loadStoredMqttSettings(storage)).toEqual(settings);
	});

	it('liefert null, wenn noch nichts gespeichert wurde', () => {
		expect(loadStoredMqttSettings(new MemoryStorage())).toBeNull();
	});

	it('fällt bei ungültigem JSON auf null zurück', () => {
		const storage = new MemoryStorage();
		storage.setItem(MQTT_SETTINGS_STORAGE_KEY, '{ungueltig');
		expect(loadStoredMqttSettings(storage)).toBeNull();
	});
});

describe('loadMqttConfig', () => {
	it('bevorzugt im Browser gespeicherte Einstellungen', async () => {
		const storage = new MemoryStorage();
		saveMqttSettings(
			{
				...DEFAULT_MQTT_SETTINGS,
				brokerHost: 'ui-broker',
				mqttTopic: 'JF/Ui'
			},
			storage
		);
		const fetchFn = vi.fn(async () => {
			throw new Error('Dateien dürfen nicht gelesen werden');
		});

		await expect(loadMqttConfig(fetchFn as unknown as typeof fetch, storage)).resolves.toMatchObject(
			{
				brokerHost: 'ui-broker',
				mqttTopic: 'JF/Ui'
			}
		);
		expect(fetchFn).not.toHaveBeenCalled();
	});

	it('bevorzugt die lokale Konfigurationsdatei', async () => {
		const fetchFn = vi.fn(async (url: string) => {
			if (url === MQTT_LOCAL_CONFIG_URL) {
				return {
					ok: true,
					json: async () => ({ brokerHost: 'local-broker' })
				};
			}
			throw new Error(`unerwarteter Abruf: ${url}`);
		});

		await expect(
			loadMqttConfig(fetchFn as unknown as typeof fetch, new MemoryStorage())
		).resolves.toMatchObject({
			brokerHost: 'local-broker',
			mqttTopic: 'JF/Alarm'
		});
	});

	it('nutzt mqtt-config.json, wenn keine lokale Datei vorhanden ist', async () => {
		const fetchFn = vi.fn(async (url: string) => {
			if (url === MQTT_LOCAL_CONFIG_URL) {
				return { ok: false, json: async () => ({}) };
			}
			if (url === MQTT_CONFIG_URL) {
				return {
					ok: true,
					json: async () => ({ brokerHost: 'shared-broker', mqttTopic: 'JF/Shared' })
				};
			}
			throw new Error(`unerwarteter Abruf: ${url}`);
		});

		await expect(
			loadMqttConfig(fetchFn as unknown as typeof fetch, new MemoryStorage())
		).resolves.toMatchObject({
			brokerHost: 'shared-broker',
			mqttTopic: 'JF/Shared'
		});
	});

	it('fällt auf die Standardwerte zurück, wenn keine Datei geladen werden kann', async () => {
		const fetchFn = vi.fn(async () => {
			throw new Error('offline');
		});

		await expect(
			loadMqttConfig(fetchFn as unknown as typeof fetch, new MemoryStorage())
		).resolves.toEqual(DEFAULT_MQTT_SETTINGS);
	});
});
