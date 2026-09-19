import { describe, expect, it } from 'vitest';
import {
	buildMqttPayload,
	createAlarm,
	deleteAlarm,
	filterAlarms,
	formatGermanDateTime,
	getFilterCountLabel,
	getKeywordBadgeClass,
	sortAlarms,
	toggleSort,
	updateAlarm,
	validateAlarmDraft
} from './alarm';
import type { AlarmItem } from './types';

/** Testdaten für Filter- und Sortierfälle. */
const sampleAlarms: AlarmItem[] = [
	{
		id: '1',
		scheduledAt: '2025-04-24T14:30:15',
		topic: 'Gebäude 3',
		keyword: 'Feueralarm',
		info: 'Rauchentwicklung',
		status: 'planned'
	},
	{
		id: '2',
		scheduledAt: '2025-04-25T09:00:00',
		topic: 'IT-Systeme',
		keyword: 'Warnung',
		info: 'Wartung',
		status: 'sent'
	}
];

describe('formatGermanDateTime', () => {
	it('formatiert lokale ISO-Zeitstempel mit Sekunden', () => {
		expect(formatGermanDateTime('2025-04-24T14:30:15')).toBe('24.04.2025 14:30:15');
	});

	it('ergänzt fehlende Sekunden', () => {
		expect(formatGermanDateTime('2025-04-25T09:00')).toBe('25.04.2025 09:00:00');
	});
});

describe('buildMqttPayload', () => {
	it('serialisiert Stichwort, Ort und Infos im Android-Format', () => {
		expect(
			buildMqttPayload({
				scheduledAt: '2025-04-24T14:30:15',
				topic: 'Gebäude 3',
				keyword: 'Feueralarm',
				info: 'Rauchentwicklung im Serverraum'
			})
		).toBe('Feueralarm###Gebäude 3###Rauchentwicklung im Serverraum');
	});
});

describe('validateAlarmDraft', () => {
	it('akzeptiert vollständige Eingaben', () => {
		expect(
			validateAlarmDraft({
				scheduledAt: '2025-04-24T14:30:15',
				topic: 'Gebäude 3',
				keyword: 'Feueralarm',
				info: 'Test'
			})
		).toBeNull();
	});

	it('lehnt leere Pflichtfelder ab', () => {
		expect(
			validateAlarmDraft({
				scheduledAt: '',
				topic: 'Gebäude 3',
				keyword: 'Feueralarm',
				info: ''
			})
		).toBe('Bitte wählen Sie einen Zeitpunkt.');
	});
});

describe('Alarmlisten-Operationen', () => {
	it('erzeugt Einträge mit dem gewünschten Status', () => {
		const alarm = createAlarm(
			{
				scheduledAt: '2025-04-24T14:30:15',
				topic: 'Gebäude 3',
				keyword: 'Feueralarm',
				info: '  Rauch  '
			},
			'planned',
			() => 'generated-id'
		);

		expect(alarm).toMatchObject({
			id: 'generated-id',
			info: 'Rauch',
			status: 'planned'
		});
	});

	it('aktualisiert und löscht Einträge', () => {
		const updated = updateAlarm(sampleAlarms, '1', {
			scheduledAt: '2025-04-24T15:00:00',
			topic: 'Eingang',
			keyword: 'Info',
			info: 'Aktualisiert'
		});
		expect(updated[0]?.topic).toBe('Eingang');
		expect(deleteAlarm(updated, '1')).toHaveLength(1);
	});

	it('filtert nach Status', () => {
		expect(filterAlarms(sampleAlarms, 'planned')).toHaveLength(1);
		expect(filterAlarms(sampleAlarms, 'sent')).toHaveLength(1);
		expect(filterAlarms(sampleAlarms, 'all')).toHaveLength(2);
	});

	it('sortiert nach Stichwort aufsteigend', () => {
		const sorted = sortAlarms(sampleAlarms, 'keyword', 'asc');
		expect(sorted.map((item) => item.keyword)).toEqual(['Feueralarm', 'Warnung']);
	});

	it('wechselt die Sortierrichtung bei gleicher Spalte', () => {
		expect(toggleSort('scheduledAt', 'asc', 'scheduledAt')).toEqual({
			sortKey: 'scheduledAt',
			sortDirection: 'desc'
		});
		expect(toggleSort('scheduledAt', 'asc', 'keyword')).toEqual({
			sortKey: 'keyword',
			sortDirection: 'asc'
		});
	});
});

describe('Anzeigehilfen', () => {
	it('liefert farbige Plakettenklassen', () => {
		expect(getKeywordBadgeClass('Feueralarm')).toContain('rose');
		expect(getKeywordBadgeClass('Unbekannt')).toContain('slate');
	});

	it('bildet den Zählertext zum aktiven Filter', () => {
		expect(getFilterCountLabel(5, 'planned')).toBe('5 geplante Alarmierungen');
		expect(getFilterCountLabel(1, 'sent')).toBe('1 bereits alarmierte Alarmierung');
		expect(getFilterCountLabel(2, 'all')).toBe('2 Alarmierungen');
	});
});
