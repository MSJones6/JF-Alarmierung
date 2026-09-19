import type { MqttSettings } from './types';

/** Standardwerte für neue Broker-Verbindungen im Einstellungsdialog. */
export const DEFAULT_MQTT_SETTINGS: MqttSettings = {
	useSsl: false,
	brokerHost: 'localhost',
	brokerPort: '9001',
	brokerPath: '/mqtt',
	user: 'alarm',
	password: 'alarm',
	mqttTopic: 'JF/Alarm'
};
