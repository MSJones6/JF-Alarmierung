import { describe, expect, it } from 'vitest';
import {
	applyClockValue,
	CLOCK_CENTER,
	clockAngleFromDelta,
	clockLabels,
	clockStepLabel,
	clockValueFromOffset,
	HOUR_INNER_RADIUS,
	HOUR_OUTER_RADIUS,
	joinDateTimeLocal,
	nextClockStep,
	padClockUnit,
	splitDateTimeLocal
} from './clock-time';

describe('splitDateTimeLocal', () => {
	it('zerlegt Datum und Uhrzeit mit Sekunden', () => {
		expect(splitDateTimeLocal('2026-09-20T08:05:09')).toEqual({
			date: '2026-09-20',
			hours: 8,
			minutes: 5,
			seconds: 9
		});
	});

	it('begrenzt unbrauchbare Zeitanteile', () => {
		expect(splitDateTimeLocal('ungueltig')).toMatchObject({
			hours: 0,
			minutes: 0,
			seconds: 0
		});
	});
});

describe('joinDateTimeLocal', () => {
	it('setzt zweistellige Uhrzeit zusammen', () => {
		expect(
			joinDateTimeLocal({
				date: '2026-09-20',
				hours: 8,
				minutes: 5,
				seconds: 9
			})
		).toBe('2026-09-20T08:05:09');
	});
});

describe('nextClockStep', () => {
	it('läuft von Stunden über Minuten zu Sekunden', () => {
		expect(nextClockStep('hours')).toBe('minutes');
		expect(nextClockStep('minutes')).toBe('seconds');
		expect(nextClockStep('seconds')).toBeNull();
	});
});

describe('clockStepLabel', () => {
	it('liefert deutsche Schritttexte', () => {
		expect(clockStepLabel('hours')).toBe('Stunden wählen');
		expect(clockStepLabel('minutes')).toBe('Minuten wählen');
		expect(clockStepLabel('seconds')).toBe('Sekunden wählen');
	});
});

describe('clockValueFromOffset', () => {
	it('liest innere Stunden oben als 0', () => {
		expect(clockValueFromOffset(CLOCK_CENTER, CLOCK_CENTER - HOUR_INNER_RADIUS, 'hours')).toBe(0);
	});

	it('liest äußere Stunden oben als 12', () => {
		expect(clockValueFromOffset(CLOCK_CENTER, CLOCK_CENTER - HOUR_OUTER_RADIUS, 'hours')).toBe(12);
	});

	it('liest innere Stunden rechts als 3', () => {
		expect(clockValueFromOffset(CLOCK_CENTER + HOUR_INNER_RADIUS, CLOCK_CENTER, 'hours')).toBe(3);
	});

	it('liest Minuten oben als 0 und rechts als 15', () => {
		expect(clockValueFromOffset(CLOCK_CENTER, CLOCK_CENTER - HOUR_OUTER_RADIUS, 'minutes')).toBe(0);
		expect(clockValueFromOffset(CLOCK_CENTER + HOUR_OUTER_RADIUS, CLOCK_CENTER, 'minutes')).toBe(15);
	});
});

describe('clockLabels', () => {
	it('zeigt 24 Stunden auf zwei Ringen', () => {
		const labels = clockLabels('hours');
		expect(labels).toHaveLength(24);
		expect(labels[0]).toMatchObject({ value: 0, label: '00' });
		expect(labels[12]).toMatchObject({ value: 12, label: '12' });
	});

	it('zeigt Minuten in Fünferschritten', () => {
		expect(clockLabels('minutes').map((item) => item.value)).toEqual([
			0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55
		]);
	});
});

describe('applyClockValue', () => {
	it('schreibt nur den aktuellen Schritt', () => {
		const time = { hours: 10, minutes: 20, seconds: 30 };
		expect(applyClockValue(time, 'minutes', 44)).toEqual({
			hours: 10,
			minutes: 44,
			seconds: 30
		});
	});
});

describe('padClockUnit', () => {
	it('füllt einstellige Werte auf', () => {
		expect(padClockUnit(7)).toBe('07');
	});
});

describe('clockAngleFromDelta', () => {
	it('legt 12 Uhr auf 0 Grad', () => {
		expect(clockAngleFromDelta(0, -10)).toBeCloseTo(0);
		expect(clockAngleFromDelta(10, 0)).toBeCloseTo(90);
	});
});
