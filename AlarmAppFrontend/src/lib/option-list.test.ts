import { describe, expect, it } from 'vitest';
import { addUniqueOption, normalizeOptions, removeOption } from './option-list';

describe('normalizeOptions', () => {
	it('entfernt Leerzeichen, Leereinträge und Duplikate', () => {
		expect(
			normalizeOptions([' Gebäude 3 ', '', 'Gebäude 3', 'IT-Systeme', 12] as unknown[])
		).toEqual(['Gebäude 3', 'IT-Systeme']);
	});
});

describe('addUniqueOption', () => {
	it('hängt einen neuen Eintrag an', () => {
		expect(addUniqueOption(['Feueralarm'], 'Warnung')).toEqual(['Feueralarm', 'Warnung']);
	});

	it('lehnt leere und vorhandene Werte ab', () => {
		expect(addUniqueOption(['Feueralarm'], '  ')).toBeNull();
		expect(addUniqueOption(['Feueralarm'], 'feueralarm')).toBeNull();
	});
});

describe('removeOption', () => {
	it('entfernt den gewählten Eintrag', () => {
		expect(removeOption(['Feueralarm', 'Warnung'], 'Feueralarm')).toEqual(['Warnung']);
	});
});
