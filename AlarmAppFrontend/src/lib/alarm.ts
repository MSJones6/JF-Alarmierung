import type { AlarmDraft, AlarmFilter, AlarmItem, AlarmSortKey, KeywordOption, SortDirection } from './types';

/** Standard-Einsatzorte für ältere Formularwerte. */
export const TOPICS = ['Gebäude 3', 'IT-Systeme', 'Allgemein', 'System', 'Eingang'];

/** Standard-Alarmstichworte für das Dropdown. */
export const KEYWORDS = ['Feueralarm', 'Warnung', 'Info', 'Test', 'Sicherheit'];

/** Standardfarbe für unbekannte Alarmstichworte. */
export const DEFAULT_KEYWORD_COLOR = '#64748b';

/** Bisherige Badge-Farben der Standard-Stichworte. */
export const DEFAULT_KEYWORD_COLORS: Record<string, string> = {
	Feueralarm: '#f43f5e',
	Warnung: '#f97316',
	Info: '#10b981',
	Test: '#0ea5e9',
	Sicherheit: '#8b5cf6'
};

/** Standard-Alarmstichworte inklusive Badge-Farbe. */
export const DEFAULT_KEYWORD_OPTIONS: KeywordOption[] = KEYWORDS.map((name) => ({
	name,
	color: DEFAULT_KEYWORD_COLORS[name] ?? DEFAULT_KEYWORD_COLOR
}));

/** Erlaubtes Hex-Format für Badge-Farben. */
const HEX_COLOR = /^#([0-9a-fA-F]{6})$/;

/**
 * Normalisiert eine Hex-Farbe auf `#rrggbb`.
 *
 * @param color Rohwert
 * @returns gültige Farbe oder die Standardfarbe
 */
export function normalizeKeywordColor(color: string | undefined | null): string {
	if (typeof color === 'string' && HEX_COLOR.test(color.trim())) {
		return color.trim().toLowerCase();
	}
	return DEFAULT_KEYWORD_COLOR;
}

/**
 * Liefert die Badge-Farbe eines Stichworts.
 *
 * @param keyword Alarmstichwort
 * @param color optionale gespeicherte Farbe
 * @returns Hex-Farbe
 */
export function getKeywordColor(keyword: string, color?: string | null): string {
	if (color) {
		return normalizeKeywordColor(color);
	}
	return DEFAULT_KEYWORD_COLORS[keyword] ?? DEFAULT_KEYWORD_COLOR;
}

/**
 * Mischt eine Hex-Farbe mit Weiß, analog zu Tailwind `*-100` bei `*-500`.
 *
 * @param hex Ausgangsfarbe
 * @param whiteRatio Anteil Weiß zwischen 0 und 1
 * @returns aufgehellte Hex-Farbe
 */
export function mixHexWithWhite(hex: string, whiteRatio = 0.85): string {
	const normalized = normalizeKeywordColor(hex);
	const channels = [1, 3, 5].map((offset) => parseInt(normalized.slice(offset, offset + 2), 16));
	const mix = (channel: number) => Math.round(channel + (255 - channel) * whiteRatio);
	return `#${channels.map((channel) => mix(channel).toString(16).padStart(2, '0')).join('')}`;
}

/**
 * Liefert Inline-Styles für die farbige Stichwort-Plakette.
 *
 * @param color gewählte Badge-Farbe
 * @returns Text- und Hintergrundfarbe
 */
export function getKeywordBadgeStyle(color: string): { color: string; backgroundColor: string } {
	const textColor = normalizeKeywordColor(color);
	return {
		color: textColor,
		backgroundColor: mixHexWithWhite(textColor)
	};
}

/**
 * Liefert nur die Namen einer Stichwortliste.
 *
 * @param keywords Stichworte mit Farbe
 * @returns Namen in derselben Reihenfolge
 */
export function getKeywordNames(keywords: KeywordOption[]): string[] {
	return keywords.map((keyword) => keyword.name);
}

/**
 * Sucht die Farbe zu einem Stichwortnamen.
 *
 * @param keywords bekannte Stichworte
 * @param keyword gesuchter Name
 * @returns gespeicherte oder Standardfarbe
 */
export function findKeywordColor(keywords: KeywordOption[], keyword: string): string {
	const match = keywords.find((item) => item.name === keyword);
	return getKeywordColor(keyword, match?.color);
}

/**
 * Formatiert einen lokalen ISO-Zeitstempel ins Anzeigeformat der Oberfläche.
 *
 * @param isoLocal Zeitstempel im Format `YYYY-MM-DDTHH:mm` oder `YYYY-MM-DDTHH:mm:ss`
 * @returns deutscher Zeitstring, z. B. `24.04.2025 14:30:15`
 */
export function formatGermanDateTime(isoLocal: string): string {
	const [datePart, timePart = '00:00:00'] = isoLocal.split('T');
	const [year, month, day] = datePart.split('-');
	const normalizedTime = timePart.length === 5 ? `${timePart}:00` : timePart.slice(0, 8);
	return `${day}.${month}.${year} ${normalizedTime}`;
}

/**
 * Baut das MQTT-Payload im Format der Android-App.
 *
 * Erwartetes Format: `Alarmstichwort###Ort###Sonstiges`
 *
 * @param draft aktuelle Formularwerte
 * @returns serialisierte Alarmnachricht
 */
export function buildMqttPayload(draft: AlarmDraft): string {
	return `${draft.keyword}###${draft.location}###${draft.info}`;
}

/**
 * Prüft, ob das Formular die Pflichtfelder für eine Alarmierung enthält.
 *
 * @param draft aktuelle Formularwerte
 * @returns Fehlermeldung oder `null`, wenn die Eingabe gültig ist
 */
export function validateAlarmDraft(draft: AlarmDraft): string | null {
	if (!draft.connection.trim()) {
		return 'Bitte wählen Sie eine Connection.';
	}
	if (!draft.scheduledAt.trim()) {
		return 'Bitte wählen Sie einen Zeitpunkt.';
	}
	if (!draft.location.trim()) {
		return 'Bitte geben Sie einen Ort ein.';
	}
	if (!draft.keyword.trim()) {
		return 'Bitte wählen Sie ein Alarmstichwort.';
	}
	return null;
}

/**
 * Erzeugt einen neuen Alarmierungseintrag.
 *
 * @param draft Formularwerte
 * @param status geplanter oder bereits gesendeter Status
 * @param idFactory optionale ID-Erzeugung, standardmäßig `crypto.randomUUID`
 * @returns neuer Listeneintrag
 */
export function createAlarm(
	draft: AlarmDraft,
	status: AlarmItem['status'],
	idFactory: () => string = () => crypto.randomUUID()
): AlarmItem {
	return {
		id: idFactory(),
		scheduledAt: draft.scheduledAt,
		connection: draft.connection,
		location: draft.location.trim(),
		keyword: draft.keyword,
		info: draft.info.trim(),
		status
	};
}

/**
 * Aktualisiert einen vorhandenen Eintrag mit neuen Formularwerten.
 *
 * @param alarms aktuelle Liste
 * @param id ID des zu ändernden Eintrags
 * @param draft neue Formularwerte
 * @returns Liste mit dem aktualisierten Eintrag
 */
export function updateAlarm(alarms: AlarmItem[], id: string, draft: AlarmDraft): AlarmItem[] {
	return alarms.map((alarm) =>
		alarm.id === id
			? {
					...alarm,
					scheduledAt: draft.scheduledAt,
					connection: draft.connection,
					location: draft.location.trim(),
					keyword: draft.keyword,
					info: draft.info.trim()
				}
			: alarm
	);
}

/**
 * Entfernt eine Alarmierung anhand ihrer ID.
 *
 * @param alarms aktuelle Liste
 * @param id ID des zu löschenden Eintrags
 * @returns Liste ohne den Eintrag
 */
export function deleteAlarm(alarms: AlarmItem[], id: string): AlarmItem[] {
	return alarms.filter((alarm) => alarm.id !== id);
}

/**
 * Filtert Alarmierungen nach dem gewählten Tab.
 *
 * @param alarms vollständige Liste
 * @param filter aktiver Tab
 * @returns sichtbare Einträge
 */
export function filterAlarms(alarms: AlarmItem[], filter: AlarmFilter): AlarmItem[] {
	if (filter === 'all') {
		return alarms;
	}
	return alarms.filter((alarm) => alarm.status === filter);
}

/**
 * Sortiert Alarmierungen nach der gewählten Spalte.
 *
 * @param alarms zu sortierende Liste
 * @param sortKey Spalte
 * @param direction aufsteigend oder absteigend
 * @returns neue, sortierte Liste
 */
export function sortAlarms(
	alarms: AlarmItem[],
	sortKey: AlarmSortKey,
	direction: SortDirection
): AlarmItem[] {
	const factor = direction === 'asc' ? 1 : -1;
	return [...alarms].sort((left, right) => {
		const leftValue = left[sortKey].toLocaleLowerCase('de-DE');
		const rightValue = right[sortKey].toLocaleLowerCase('de-DE');
		return leftValue.localeCompare(rightValue, 'de-DE') * factor;
	});
}

/**
 * Baut den Fußzeilentext unter der Tabelle.
 *
 * @param count Anzahl der aktuell sichtbaren Einträge
 * @param filter aktiver Tab
 * @returns deutscher Zählertext
 */
export function getFilterCountLabel(count: number, filter: AlarmFilter): string {
	switch (filter) {
		case 'planned':
			return `${count} geplante Alarmierung${count === 1 ? '' : 'en'}`;
		case 'sent':
			return `${count} bereits alarmierte Alarmierung${count === 1 ? '' : 'en'}`;
		default:
			return `${count} Alarmierung${count === 1 ? '' : 'en'}`;
	}
}

/**
 * Wechselt die Sortierung: gleiche Spalte dreht die Richtung, neue Spalte startet aufsteigend.
 *
 * @param currentKey bisherige Spalte
 * @param currentDirection bisherige Richtung
 * @param nextKey angeklickte Spalte
 * @returns neue Sortierung
 */
export function toggleSort(
	currentKey: AlarmSortKey,
	currentDirection: SortDirection,
	nextKey: AlarmSortKey
): { sortKey: AlarmSortKey; sortDirection: SortDirection } {
	if (currentKey === nextKey) {
		return {
			sortKey: nextKey,
			sortDirection: currentDirection === 'asc' ? 'desc' : 'asc'
		};
	}
	return { sortKey: nextKey, sortDirection: 'asc' };
}
