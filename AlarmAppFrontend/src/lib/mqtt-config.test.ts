import { describe, expect, it, vi } from 'vitest';
import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import {
	loadMqttConfig,
	MQTT_CONFIG_URL,
	MQTT_LOCAL_CONFIG_URL,
	parseMqttConfig
} from './mqtt-config';

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

describe('loadMqttConfig', () => {
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

		await expect(loadMqttConfig(fetchFn as unknown as typeof fetch)).resolves.toMatchObject({
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

		await expect(loadMqttConfig(fetchFn as unknown as typeof fetch)).resolves.toMatchObject({
			brokerHost: 'shared-broker',
			mqttTopic: 'JF/Shared'
		});
	});

	it('fällt auf die Standardwerte zurück, wenn keine Datei geladen werden kann', async () => {
		const fetchFn = vi.fn(async () => {
			throw new Error('offline');
		});

		await expect(loadMqttConfig(fetchFn as unknown as typeof fetch)).resolves.toEqual(
			DEFAULT_MQTT_SETTINGS
		);
	});
});
