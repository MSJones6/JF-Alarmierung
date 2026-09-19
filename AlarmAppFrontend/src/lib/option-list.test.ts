import { describe, expect, it } from 'vitest';
import { addUniqueKeyword, addUniqueOption, normalizeOptions, removeKeyword, removeOption, updateKeywordColor } from './option-list';

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

describe('Alarmstichworte mit Farbe', () => {
	it('hängt ein neues Stichwort mit Farbe an', () => {
		expect(addUniqueKeyword([{ name: 'Feueralarm', color: '#f43f5e' }], 'Brand', '#dc2626')).toEqual([
			{ name: 'Feueralarm', color: '#f43f5e' },
			{ name: 'Brand', color: '#dc2626' }
		]);
	});

	it('lehnt leere und vorhandene Stichworte ab', () => {
		expect(addUniqueKeyword([{ name: 'Feueralarm', color: '#f43f5e' }], '  ', '#dc2626')).toBeNull();
		expect(addUniqueKeyword([{ name: 'Feueralarm', color: '#f43f5e' }], 'feueralarm', '#dc2626')).toBeNull();
	});

	it('entfernt ein Stichwort anhand des Namens', () => {
		expect(
			removeKeyword(
				[
					{ name: 'Feueralarm', color: '#f43f5e' },
					{ name: 'Warnung', color: '#f97316' }
				],
				'Feueralarm'
			)
		).toEqual([{ name: 'Warnung', color: '#f97316' }]);
	});

	it('aktualisiert die Badge-Farbe eines Stichworts', () => {
		expect(
			updateKeywordColor([{ name: 'Info', color: '#10b981' }], 'Info', '#DC2626')
		).toEqual([{ name: 'Info', color: '#dc2626' }]);
	});
});
