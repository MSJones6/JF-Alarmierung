import { describe, expect, it, vi } from 'vitest';
import { getBrokerUrl, publishAlarmMessage } from './mqtt';
import type { MqttConnectFn } from './mqtt';
import { DEFAULT_MQTT_SETTINGS } from './mqtt';

describe('getBrokerUrl', () => {
	it('baut eine WebSocket-URL mit Pfad', () => {
		expect(getBrokerUrl(DEFAULT_MQTT_SETTINGS)).toBe('ws://localhost:9001/mqtt');
	});

	it('nutzt wss und normalisiert den Pfad', () => {
		expect(
			getBrokerUrl({
				...DEFAULT_MQTT_SETTINGS,
				useSsl: true,
				brokerPort: '8084',
				brokerPath: 'mqtt'
			})
		).toBe('wss://localhost:8084/mqtt');
	});
});

describe('publishAlarmMessage', () => {
	it('veröffentlicht die Nachricht nach dem Connect', async () => {
		const publish = vi.fn((_topic, _payload, _opts, callback: (error?: Error) => void) => {
			callback();
		});
		const end = vi.fn();
		const connectFn: MqttConnectFn = () => ({
			connected: false,
			on(event: string, handler: (...args: unknown[]) => void) {
				if (event === 'connect') {
					handler();
				}
				return this as never;
			},
			publish,
			end
		});

		await publishAlarmMessage(DEFAULT_MQTT_SETTINGS, 'Feueralarm###Gebäude 3###Test', connectFn);

		expect(publish).toHaveBeenCalledWith(
			'JF/Alarm',
			'Feueralarm###Gebäude 3###Test',
			{ qos: 1 },
			expect.any(Function)
		);
		expect(end).toHaveBeenCalled();
	});

	it('lehnt bei MQTT-Fehlern ab', async () => {
		const connectFn: MqttConnectFn = () => ({
			connected: false,
			on(event: string, handler: (...args: unknown[]) => void) {
				if (event === 'error') {
					handler(new Error('Broker offline'));
				}
				return this as never;
			},
			publish: vi.fn(),
			end: vi.fn()
		});

		await expect(publishAlarmMessage(DEFAULT_MQTT_SETTINGS, 'payload', connectFn)).rejects.toThrow(
			'Broker offline'
		);
	});
});
