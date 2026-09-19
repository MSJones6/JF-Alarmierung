import { describe, expect, it } from 'vitest';
import { applyAlarmStreamEvent } from './alarm-stream';
import type { AlarmItem } from './types';

/** Testdaten für Stream-Ereignisse. */
const sample: AlarmItem = {
	id: '1',
	scheduledAt: '2025-04-24T14:30:15',
	connection: 'Standard',
	location: 'Turnhalle',
	keyword: 'Feueralarm',
	info: 'Rauch',
	status: 'planned'
};

describe('applyAlarmStreamEvent', () => {
	it('ersetzt die Liste durch einen Snapshot', () => {
		expect(applyAlarmStreamEvent([], { event: 'snapshot', alarms: [sample] })).toEqual([sample]);
	});

	it('fügt neue Alarmierungen hinzu und aktualisiert vorhandene', () => {
		const created = applyAlarmStreamEvent([], { event: 'created', alarm: sample });
		expect(created).toEqual([sample]);
		const updated = applyAlarmStreamEvent(created, {
			event: 'updated',
			alarm: { ...sample, status: 'sent' }
		});
		expect(updated[0]?.status).toBe('sent');
	});

	it('entfernt gelöschte Alarmierungen', () => {
		expect(applyAlarmStreamEvent([sample], { event: 'deleted', id: '1' })).toEqual([]);
	});
});
