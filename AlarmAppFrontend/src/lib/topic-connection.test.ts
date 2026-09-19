import { describe, expect, it } from 'vitest';
import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import {
	createTopicConnection,
	findTopicConnection,
	getTopicNames,
	nextTopicName
} from './topic-connection';

describe('createTopicConnection', () => {
	it('füllt fehlende Broker-Daten mit Standardwerten', () => {
		const topic = createTopicConnection({ name: 'Host 1', id: 'host-1', brokerHost: 'broker-1' });
		expect(topic).toMatchObject({
			id: 'host-1',
			name: 'Host 1',
			brokerHost: 'broker-1',
			user: DEFAULT_MQTT_SETTINGS.user,
			mqttTopic: DEFAULT_MQTT_SETTINGS.mqttTopic
		});
	});
});

describe('findTopicConnection', () => {
	it('findet die Verbindung zum gewählten Namen', () => {
		const topics = [
			createTopicConnection({ id: 'a', name: 'Topic 1', brokerHost: 'host-1' }),
			createTopicConnection({ id: 'b', name: 'Topic 2', brokerHost: 'host-2' })
		];
		expect(findTopicConnection(topics, 'Topic 2')?.brokerHost).toBe('host-2');
		expect(findTopicConnection(topics, 'fehlt')).toBeUndefined();
	});
});

describe('getTopicNames', () => {
	it('liefert die Namen für das Dropdown', () => {
		expect(
			getTopicNames([
				createTopicConnection({ id: 'a', name: 'Topic 1' }),
				createTopicConnection({ id: 'b', name: 'Topic 2' })
			])
		).toEqual(['Topic 1', 'Topic 2']);
	});
});

describe('nextTopicName', () => {
	it('nummeriert doppelte Standardnamen', () => {
		expect(nextTopicName([])).toBe('Neue Connection');
		expect(nextTopicName([createTopicConnection({ id: 'a', name: 'Neue Connection' })])).toBe(
			'Neue Connection 2'
		);
	});
});
