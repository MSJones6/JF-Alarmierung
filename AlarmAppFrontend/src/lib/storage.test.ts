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
	it('liefert eine leere Liste, wenn noch nichts gespeichert wurde', () => {
		const storage = new MemoryStorage();
		expect(loadAlarms(storage)).toEqual([]);
	});

	it('rundet eigene Alarmierungen über den Storage', () => {
		const storage = new MemoryStorage();
		const alarms = [
			{
				id: 'user-1',
				scheduledAt: '2025-04-24T14:30:15',
				connection: 'Standard',
				location: 'Gebäude 3',
				keyword: 'Feueralarm',
				info: 'Rauchentwicklung',
				status: 'planned' as const
			}
		];
		saveAlarms(alarms, storage);
		expect(storage.getItem(ALARMS_STORAGE_KEY)).toContain('user-1');
		expect(loadAlarms(storage)).toEqual(alarms);
	});

	it('entfernt gespeicherte Demo-Einträge aus der Liste', () => {
		const storage = new MemoryStorage();
		saveAlarms(getDemoAlarms(), storage);
		expect(loadAlarms(storage)).toEqual([]);
	});

	it('fällt bei ungültigem JSON auf eine leere Liste zurück', () => {
		const storage = new MemoryStorage();
		storage.setItem(ALARMS_STORAGE_KEY, '{ungueltig');
		expect(loadAlarms(storage)).toEqual([]);
	});

	it('übernimmt den alten Topic-Namen als Connection und Ort', () => {
		const storage = new MemoryStorage();
		storage.setItem(
			ALARMS_STORAGE_KEY,
			JSON.stringify([
				{
					id: 'legacy-1',
					scheduledAt: '2025-04-24T14:30:15',
					topic: 'Gebäude 3',
					keyword: 'Feueralarm',
					info: 'Rauchentwicklung',
					status: 'planned'
				}
			])
		);

		expect(loadAlarms(storage)).toEqual([
			{
				id: 'legacy-1',
				scheduledAt: '2025-04-24T14:30:15',
				connection: 'Gebäude 3',
				location: 'Gebäude 3',
				keyword: 'Feueralarm',
				info: 'Rauchentwicklung',
				status: 'planned'
			}
		]);
	});
});
