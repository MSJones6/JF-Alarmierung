/**
 * Gemeinsame Datentypen der Alarmierungsoberfläche.
 */

/** Status einer Alarmierung in der Übersicht. */
export type AlarmStatus = 'planned' | 'sent';

/** Filter der Alarmierungstabelle. */
export type AlarmFilter = 'planned' | 'sent' | 'all';

/** Sortierbare Spalten der Alarmierungstabelle. */
export type AlarmSortKey = 'scheduledAt' | 'keyword' | 'topic' | 'info';

/** Sortierrichtung einer Tabellenspalte. */
export type SortDirection = 'asc' | 'desc';

/** Eine geplante oder bereits ausgelöste Alarmierung. */
export type AlarmItem = {
	id: string;
	scheduledAt: string;
	topic: string;
	keyword: string;
	info: string;
	status: AlarmStatus;
};

/** Eingabewerte des Formulars „Neue Alarmierung“. */
export type AlarmDraft = {
	scheduledAt: string;
	topic: string;
	keyword: string;
	info: string;
};

/** Verbindungsdaten zum MQTT-Broker. */
export type MqttSettings = {
	useSsl: boolean;
	brokerHost: string;
	brokerPort: string;
	brokerPath: string;
	user: string;
	password: string;
	mqttTopic: string;
};

/** Zustände der Statusanzeige nach einem Versand. */
export type StatusType = 'idle' | 'sending' | 'success' | 'error';

/** Minimale Storage-Schnittstelle für Tests und localStorage. */
export type StorageLike = {
	getItem(key: string): string | null;
	setItem(key: string, value: string): void;
};
