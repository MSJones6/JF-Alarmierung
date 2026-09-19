import { describe, expect, it } from 'vitest';
import { getDefaultDraft } from './demo-data';

describe('getDefaultDraft', () => {
	it('setzt die Uhrzeit auf den übergebenen lokalen Zeitstempel', () => {
		const draft = getDefaultDraft({ now: new Date(2026, 8, 20, 12, 5, 9) });
		expect(draft.scheduledAt).toBe('2026-09-20T12:05:09');
	});

	it('füllt scheduledAt im datetime-local-Format mit der aktuellen Zeit', () => {
		expect(getDefaultDraft().scheduledAt).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/);
		expect(getDefaultDraft().scheduledAt).not.toBe('2025-04-24T14:30:15');
	});
});
