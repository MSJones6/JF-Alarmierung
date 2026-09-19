import { describe, expect, it, vi } from 'vitest';
import { KEYWORDS } from './alarm';
import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import {
	DEFAULT_APP_SETTINGS,
	loadMqttConfig,
	loadStoredMqttSettings,
	MQTT_CONFIG_URL,
	MQTT_LOCAL_CONFIG_URL,
	MQTT_SETTINGS_STORAGE_KEY,
	parseAppSettings,
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

describe('parseAppSettings', () => {
	it('legt für jedes Topic eine eigene Verbindung an', () => {
		const parsed = parseAppSettings({
			keywords: ['Brand'],
			topics: [
				{
					name: 'Topic 1',
					brokerHost: 'host-1',
					user: 'user-1',
					password: 'pass-1',
					mqttTopic: 'JF/One'
				},
				{
					name: 'Topic 2',
					brokerHost: 'host-2',
					user: 'user-2',
					password: 'pass-2',
					mqttTopic: 'JF/Two'
				}
			]
		});

		expect(parsed.keywords).toEqual(['Brand']);
		expect(parsed.topics).toEqual([
			expect.objectContaining({
				name: 'Topic 1',
				brokerHost: 'host-1',
				user: 'user-1',
				password: 'pass-1',
				mqttTopic: 'JF/One'
			}),
			expect.objectContaining({
				name: 'Topic 2',
				brokerHost: 'host-2',
				user: 'user-2',
				password: 'pass-2',
				mqttTopic: 'JF/Two'
			})
		]);
	});

	it('ergänzt alte Topic-Namen mit den globalen Broker-Daten', () => {
		const parsed = parseAppSettings({
			brokerHost: 'legacy-host',
			user: 'legacy-user',
			password: 'legacy-pass',
			mqttTopic: 'JF/Legacy',
			topics: ['Wache 1', 'Wache 2']
		});

		expect(parsed.topics).toEqual([
			expect.objectContaining({
				name: 'Wache 1',
				brokerHost: 'legacy-host',
				user: 'legacy-user',
				mqttTopic: 'JF/Legacy'
			}),
			expect.objectContaining({
				name: 'Wache 2',
				brokerHost: 'legacy-host',
				user: 'legacy-user',
				mqttTopic: 'JF/Legacy'
			})
		]);
	});

	it('erzeugt ein Standard-Topic, wenn nur Broker-Daten vorhanden sind', () => {
		expect(parseAppSettings({ brokerHost: 'local' }).topics).toEqual([
			expect.objectContaining({
				name: 'Standard',
				brokerHost: 'local'
			})
		]);
	});
});

describe('gespeicherte MQTT-Einstellungen', () => {
	it('rundet Topic-Verbindungen und Zugangsdaten über den Storage', () => {
		const storage = new MemoryStorage();
		const settings = parseAppSettings({
			keywords: ['Brandmelder'],
			topics: [
				{
					id: 'wache',
					name: 'Gerätehaus',
					brokerHost: 'saved-broker',
					user: 'saved-user',
					password: 'saved-pass',
					mqttTopic: 'JF/Saved'
				}
			]
		});

		saveMqttSettings(settings, storage);

		expect(storage.getItem(MQTT_SETTINGS_STORAGE_KEY)).toContain('saved-user');
		expect(storage.getItem(MQTT_SETTINGS_STORAGE_KEY)).toContain('Gerätehaus');
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
			parseAppSettings({
				keywords: ['Eigene Meldung'],
				topics: [{ name: 'Ui Topic', brokerHost: 'ui-broker', mqttTopic: 'JF/Ui' }]
			}),
			storage
		);
		const fetchFn = vi.fn(async () => {
			throw new Error('Dateien dürfen nicht gelesen werden');
		});

		await expect(loadMqttConfig(fetchFn as unknown as typeof fetch, storage)).resolves.toMatchObject(
			{
				keywords: ['Eigene Meldung'],
				topics: [expect.objectContaining({ brokerHost: 'ui-broker', mqttTopic: 'JF/Ui' })]
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
			keywords: KEYWORDS,
			topics: [expect.objectContaining({ brokerHost: 'local-broker', name: 'Standard' })]
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
			topics: [expect.objectContaining({ brokerHost: 'shared-broker', mqttTopic: 'JF/Shared' })]
		});
	});

	it('fällt auf die Standardwerte zurück, wenn keine Datei geladen werden kann', async () => {
		const fetchFn = vi.fn(async () => {
			throw new Error('offline');
		});

		await expect(
			loadMqttConfig(fetchFn as unknown as typeof fetch, new MemoryStorage())
		).resolves.toEqual(parseAppSettings(DEFAULT_APP_SETTINGS));
	});
});
