import { DEFAULT_MQTT_SETTINGS } from './mqtt';
import type { TopicConnection } from './types';

/**
 * Erzeugt eine Connection mit Standardwerten.
 *
 * @param overrides optionale Felder, die die Standards überschreiben
 * @returns vollständige Connection
 */
export function createTopicConnection(
	overrides: Partial<TopicConnection> = {}
): TopicConnection {
	const name = overrides.name?.trim() || 'Neue Connection';
	return {
		...DEFAULT_MQTT_SETTINGS,
		...overrides,
		name,
		id: overrides.id?.trim() || crypto.randomUUID()
	};
}

/**
 * Findet die Connection zum gewählten Anzeigenamen.
 *
 * @param topics gespeicherte Verbindungen
 * @param name gewählter Dropdown-Wert
 * @returns Verbindung oder `undefined`
 */
export function findTopicConnection(
	topics: TopicConnection[],
	name: string
): TopicConnection | undefined {
	return topics.find((topic) => topic.name === name);
}

/**
 * Liefert die Namen der Connections für das Dropdown.
 *
 * @param topics gespeicherte Verbindungen
 * @returns Connection-Namen
 */
export function getTopicNames(topics: TopicConnection[]): string[] {
	return topics.map((topic) => topic.name);
}

/**
 * Erzeugt einen noch freien Namen für eine neue Connection.
 *
 * @param topics vorhandene Verbindungen
 * @returns eindeutiger Anzeigename
 */
export function nextTopicName(topics: TopicConnection[]): string {
	const used = new Set(topics.map((topic) => topic.name.toLocaleLowerCase('de-DE')));
	if (!used.has('neue connection')) {
		return 'Neue Connection';
	}

	let index = 2;
	while (used.has(`neue connection ${index}`)) {
		index += 1;
	}
	return `Neue Connection ${index}`;
}
