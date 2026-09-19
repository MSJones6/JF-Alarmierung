import { describe, expect, it } from 'vitest';
import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import { parseAppSettings, parseMqttConfig } from './mqtt-config';

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
	it('übernimmt gespeicherte Stichwortfarben', () => {
		const parsed = parseAppSettings({
			keywords: [{ name: 'Brand', color: '#dc2626' }]
		});
		expect(parsed.keywords).toEqual([{ name: 'Brand', color: '#dc2626' }]);
	});

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

		expect(parsed.keywords).toEqual([{ name: 'Brand', color: '#64748b' }]);
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
