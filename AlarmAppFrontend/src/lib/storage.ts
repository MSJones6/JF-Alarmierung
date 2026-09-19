import type { AlarmItem, StorageLike } from './types';

/** Schlüssel für gespeicherte Alarmierungen. */
export const ALARMS_STORAGE_KEY = 'jf-alarm-items';

/**
 * Liefert eine Storage-Implementierung oder `null`, wenn keine verfügbar ist.
 *
 * @param storage explizite Implementierung, sonst `localStorage`
 * @returns Storage oder `null` in Umgebungen ohne Web-Storage
 */
export function resolveStorage(storage?: StorageLike): StorageLike | null {
	if (storage) {
		return storage;
	}
	if (typeof localStorage === 'undefined') {
		return null;
	}
	return localStorage;
}

/**
 * Liest Alarmierungen aus dem Storage oder liefert eine leere Liste.
 *
 * @param storage optionale Storage-Implementierung
 * @returns Liste der Alarmierungen
 */
export function loadAlarms(storage?: StorageLike): AlarmItem[] {
	const resolved = resolveStorage(storage);
	if (!resolved) {
		return [];
	}

	const raw = resolved.getItem(ALARMS_STORAGE_KEY);
	if (!raw) {
		return [];
	}

	try {
		const parsed = JSON.parse(raw) as unknown;
		if (!Array.isArray(parsed)) {
			return [];
		}
		return parsed
			.map(normalizeAlarm)
			.filter((alarm): alarm is AlarmItem => alarm !== null)
			.filter((alarm) => !alarm.id.startsWith('demo-'));
	} catch {
		return [];
	}
}

/**
 * Ergänzt ältere Einträge um Connection und Ort.
 *
 * @param raw gespeicherter Listeneintrag
 * @returns normalisierte Alarmierung oder `null`
 */
function normalizeAlarm(raw: unknown): AlarmItem | null {
	if (raw === null || typeof raw !== 'object') {
		return null;
	}
	const data = raw as Record<string, unknown>;
	if (typeof data.id !== 'string' || data.id.length === 0) {
		return null;
	}

	const legacyTopic = typeof data.topic === 'string' ? data.topic : '';
	const connection =
		typeof data.connection === 'string' && data.connection.trim().length > 0
			? data.connection
			: legacyTopic;
	const location =
		typeof data.location === 'string' && data.location.trim().length > 0
			? data.location
			: legacyTopic;

	return {
		id: data.id,
		scheduledAt: typeof data.scheduledAt === 'string' ? data.scheduledAt : '',
		connection,
		location,
		keyword: typeof data.keyword === 'string' ? data.keyword : '',
		info: typeof data.info === 'string' ? data.info : '',
		status: data.status === 'sent' ? 'sent' : 'planned'
	};
}

/**
 * Speichert die Alarmierungsliste.
 *
 * @param alarms aktuelle Liste
 * @param storage optionale Storage-Implementierung
 */
export function saveAlarms(alarms: AlarmItem[], storage?: StorageLike): void {
	const resolved = resolveStorage(storage);
	if (!resolved) {
		return;
	}
	resolved.setItem(ALARMS_STORAGE_KEY, JSON.stringify(alarms));
}
