import { describe, expect, it } from 'vitest';
import { getDemoAlarms } from './demo-data';
import { ALARMS_STORAGE_KEY, loadAlarms, saveAlarms } from './storage';
import type { StorageLike } from './types';

/**
 * Einfacher In-Memory-Storage für die Persistenztests.
 */
class MemoryStorage implements StorageLike {
	private readonly values = new Map<string, string>();

	/**
	 * Liest einen gespeicherten Wert.
	 */
	getItem(key: string): string | null {
		return this.values.get(key) ?? null;
	}

	/**
	 * Speichert einen Wert.
	 */
	setItem(key: string, value: string): void {
		this.values.set(key, value);
	}
}

describe('Storage', () => {
	it('liefert Demo-Daten, wenn noch nichts gespeichert wurde', () => {
		const storage = new MemoryStorage();
		expect(loadAlarms(storage)).toEqual(getDemoAlarms());
	});

	it('rundet Alarmierungen über den Storage', () => {
		const storage = new MemoryStorage();
		const alarms = getDemoAlarms().slice(0, 1);
		saveAlarms(alarms, storage);
		expect(storage.getItem(ALARMS_STORAGE_KEY)).toContain('demo-1');
		expect(loadAlarms(storage)).toEqual(alarms);
	});

	it('fällt bei ungültigem JSON auf die Demo-Daten zurück', () => {
		const storage = new MemoryStorage();
		storage.setItem(ALARMS_STORAGE_KEY, '{ungueltig');
		expect(loadAlarms(storage)).toEqual(getDemoAlarms());
	});
});
