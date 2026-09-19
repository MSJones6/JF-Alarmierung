import { getDemoAlarms } from './demo-data';
import type { AlarmItem, StorageLike } from './types';

/** Schlüssel für gespeicherte Alarmierungen. */
export const ALARMS_STORAGE_KEY = 'jf-alarm-items';

/**
 * Liefert eine Storage-Implementierung oder `null`, wenn keine verfügbar ist.
 *
 * @param storage explizite Implementierung, sonst `localStorage`
 * @returns Storage oder `null` in Umgebungen ohne Web-Storage
 */
function resolveStorage(storage?: StorageLike): StorageLike | null {
	if (storage) {
		return storage;
	}
	if (typeof localStorage === 'undefined') {
		return null;
	}
	return localStorage;
}

/**
 * Liest Alarmierungen aus dem Storage oder liefert die Demo-Daten.
 *
 * @param storage optionale Storage-Implementierung
 * @returns Liste der Alarmierungen
 */
export function loadAlarms(storage?: StorageLike): AlarmItem[] {
	const resolved = resolveStorage(storage);
	if (!resolved) {
		return getDemoAlarms();
	}

	const raw = resolved.getItem(ALARMS_STORAGE_KEY);
	if (!raw) {
		return getDemoAlarms();
	}

	try {
		const parsed = JSON.parse(raw) as AlarmItem[];
		return Array.isArray(parsed) ? parsed : getDemoAlarms();
	} catch {
		return getDemoAlarms();
	}
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
