/**
 * Gemeinsame Datentypen der Alarmierungsoberfläche.
 */

/** Status einer Alarmierung in der Übersicht. */
export type AlarmStatus = 'planned' | 'sent';

/** Filter der Alarmierungstabelle. */
export type AlarmFilter = 'planned' | 'sent' | 'all';

/** Sortierbare Spalten der Alarmierungstabelle. */
export type AlarmSortKey = 'scheduledAt' | 'keyword' | 'location' | 'info';

/** Sortierrichtung einer Tabellenspalte. */
export type SortDirection = 'asc' | 'desc';

/** Eine geplante oder bereits ausgelöste Alarmierung. */
export type AlarmItem = {
	id: string;
	scheduledAt: string;
	connection: string;
	location: string;
	keyword: string;
	info: string;
	status: AlarmStatus;
};

/** Eingabewerte des Formulars „Neue Alarmierung“. */
export type AlarmDraft = {
	scheduledAt: string;
	connection: string;
	location: string;
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

/** Eine auswählbare Connection mit eigener Broker-Verbindung. */
export type TopicConnection = MqttSettings & {
	id: string;
	name: string;
};

/** Auswählbares Alarmstichwort mit Badge-Farbe. */
export type KeywordOption = {
	name: string;
	color: string;
};

/** App-Einstellungen: Stichworte und Connections. */
export type AppSettings = {
	keywords: KeywordOption[];
	topics: TopicConnection[];
};

/** Zustände der Statusanzeige nach einem Versand. */
export type StatusType = 'idle' | 'sending' | 'success' | 'error';

/** Erreichbarkeit der Alarm-API aus Sicht der Oberfläche. */
export type BackendConnectionStatus = 'checking' | 'online' | 'offline';
