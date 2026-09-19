import mqtt from 'mqtt';
import type { MqttSettings } from './types';

/** Standardwerte für die Broker-Verbindung aus der bisherigen React-App. */
export const DEFAULT_MQTT_SETTINGS: MqttSettings = {
	useSsl: false,
	brokerHost: 'localhost',
	brokerPort: '9001',
	brokerPath: '/mqtt',
	user: 'alarm',
	password: 'alarm',
	mqttTopic: 'JF/Alarm'
};

/** Minimaler MQTT-Client, den der Versand benötigt. */
export type MqttPublisher = {
	connected: boolean;
	on(event: string, handler: (...args: unknown[]) => void): unknown;
	publish(
		topic: string,
		payload: string,
		opts: { qos: number },
		callback: (error?: Error) => void
	): unknown;
	end(): unknown;
};

/** Funktionssignatur zum Aufbau einer MQTT-Verbindung, überschreibbar in Tests. */
export type MqttConnectFn = (url: string, options: mqtt.IClientOptions) => MqttPublisher;

/**
 * Baut die WebSocket-URL zum MQTT-Broker.
 *
 * @param settings aktuelle Broker-Einstellungen
 * @returns vollständige Broker-URL inklusive optionalem Pfad
 */
export function getBrokerUrl(settings: MqttSettings): string {
	const protocol = settings.useSsl ? 'wss://' : 'ws://';
	let url = `${protocol}${settings.brokerHost}:${settings.brokerPort}`;
	if (settings.brokerPath) {
		const normalizedPath = settings.brokerPath.startsWith('/')
			? settings.brokerPath
			: `/${settings.brokerPath}`;
		url += normalizedPath;
	}
	return url;
}

/**
 * Verbindet sich mit dem Broker und veröffentlicht eine Alarmnachricht.
 *
 * @param settings Broker-Zugangsdaten
 * @param payload Nachricht im Format `Stichwort###Ort###Info`
 * @param connectFn optionale Connect-Funktion, standardmäßig `mqtt.connect`
 * @returns Promise, das nach dem Versand aufgelöst oder bei Fehlern abgelehnt wird
 */
export function publishAlarmMessage(
	settings: MqttSettings,
	payload: string,
	connectFn: MqttConnectFn = mqtt.connect as MqttConnectFn
): Promise<void> {
	return new Promise((resolve, reject) => {
		const mqttOptions: mqtt.IClientOptions = {
			username: settings.user || undefined,
			password: settings.password || undefined,
			clientId: 'alarmapp_' + Math.random().toString(16).slice(2, 10),
			clean: true,
			reconnectPeriod: 0,
			connectTimeout: 10000,
			keepalive: 60,
			rejectUnauthorized: false
		};

		try {
			const client = connectFn(getBrokerUrl(settings), mqttOptions);
			let settled = false;

			const finish = (error?: Error) => {
				if (settled) {
					return;
				}
				settled = true;
				clearTimeout(timeoutId);
				client.end();
				if (error) {
					reject(error);
				} else {
					resolve();
				}
			};

			const timeoutId = setTimeout(() => {
				finish(
					new Error(`Verbindungstimeout - Broker antwortet nicht (Port: ${settings.brokerPort})`)
				);
			}, 10000);

			client.on('connect', () => {
				client.publish(settings.mqttTopic, payload, { qos: 1 }, (error?: Error) => {
					finish(error);
				});
			});

			client.on('error', (...args: unknown[]) => {
				const error = args[0];
				finish(error instanceof Error ? error : new Error(String(error)));
			});
		} catch (error) {
			reject(error instanceof Error ? error : new Error(String(error)));
		}
	});
}
