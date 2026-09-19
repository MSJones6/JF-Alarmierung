/**
 * Berechnung und Zerlegung für die analoge Uhrzeitwahl.
 */

/** Schritt der Uhrwahl. */
export type ClockStep = 'hours' | 'minutes' | 'seconds';

/** Aufgelöste Uhrzeit ohne Datum. */
export type ClockTime = {
	hours: number;
	minutes: number;
	seconds: number;
};

/** Kombinierter datetime-local-Wert. */
export type DateTimeParts = ClockTime & {
	date: string;
};

/** Kantenlänge des Zifferblatts in SVG-Einheiten. */
export const CLOCK_SIZE = 280;

/** Mittelpunkt des Zifferblatts. */
export const CLOCK_CENTER = CLOCK_SIZE / 2;

/** Radius der inneren Stunden (0–11). */
export const HOUR_INNER_RADIUS = 62;

/** Radius der äußeren Stunden (12–23). */
export const HOUR_OUTER_RADIUS = 108;

/** Radius für Minuten und Sekunden. */
export const MINUTE_RADIUS = 108;

/**
 * Begrenzt eine Zahl auf einen inklusiven Bereich.
 *
 * @param value Rohwert
 * @param min untere Grenze
 * @param max obere Grenze
 * @returns begrenzter Wert
 */
function clamp(value: number, min: number, max: number): number {
	if (!Number.isFinite(value)) {
		return min;
	}
	return Math.min(max, Math.max(min, Math.trunc(value)));
}

/**
 * Füllt eine Zahl auf zwei Stellen.
 *
 * @param value Zahl
 * @returns zweistelliger Text
 */
export function padClockUnit(value: number): string {
	return String(value).padStart(2, '0');
}

/**
 * Zerlegt einen datetime-local-Wert in Datum und Uhrzeit.
 *
 * @param value Formularwert `YYYY-MM-DDTHH:mm:ss`
 * @returns Datum und Uhrzeitteile
 */
export function splitDateTimeLocal(value: string): DateTimeParts {
	const [datePart = '', timePart = '00:00:00'] = value.includes('T')
		? value.split('T')
		: ['', value];
	const [hours = '0', minutes = '0', seconds = '0'] = timePart.split(':');
	return {
		date: /^\d{4}-\d{2}-\d{2}$/.test(datePart) ? datePart : '',
		hours: clamp(Number.parseInt(hours, 10), 0, 23),
		minutes: clamp(Number.parseInt(minutes, 10), 0, 59),
		seconds: clamp(Number.parseInt(seconds, 10), 0, 59)
	};
}

/**
 * Fügt Datum und Uhrzeit zu einem datetime-local-Wert zusammen.
 *
 * @param parts Datum und Uhrzeit
 * @returns Formularwert
 */
export function joinDateTimeLocal(parts: DateTimeParts): string {
	const date = parts.date || '1970-01-01';
	return `${date}T${padClockUnit(parts.hours)}:${padClockUnit(parts.minutes)}:${padClockUnit(parts.seconds)}`;
}

/**
 * Liefert den nächsten Schritt nach einer Auswahl auf dem Zifferblatt.
 *
 * @param step aktueller Schritt
 * @returns nächster Schritt oder `null` nach den Sekunden
 */
export function nextClockStep(step: ClockStep): ClockStep | null {
	if (step === 'hours') {
		return 'minutes';
	}
	if (step === 'minutes') {
		return 'seconds';
	}
	return null;
}

/**
 * Liefert den sichtbaren Titel des aktuellen Schritts.
 *
 * @param step aktueller Schritt
 * @returns Hinweistext
 */
export function clockStepLabel(step: ClockStep): string {
	switch (step) {
		case 'hours':
			return 'Stunden wählen';
		case 'minutes':
			return 'Minuten wählen';
		default:
			return 'Sekunden wählen';
	}
}

/**
 * Wandelt einen Punkt relativ zur 12-Uhr-Position in einen Winkel (0–360) um.
 *
 * @param dx Abstand vom Mittelpunkt nach rechts
 * @param dy Abstand vom Mittelpunkt nach unten
 * @returns Winkel in Grad, 0 liegt oben
 */
export function clockAngleFromDelta(dx: number, dy: number): number {
	return (Math.atan2(dy, dx) * (180 / Math.PI) + 90 + 360) % 360;
}

/**
 * Liest den Uhrwert aus Zeigerkoordinaten im SVG.
 *
 * Stunden nutzen zwei Ringe: innen 0–11, außen 12–23.
 * Minuten und Sekunden liegen auf dem äußeren Ring (0–59).
 *
 * @param x X-Koordinate im Zifferblatt
 * @param y Y-Koordinate im Zifferblatt
 * @param step aktueller Schritt
 * @returns gewählter Wert
 */
export function clockValueFromOffset(x: number, y: number, step: ClockStep): number {
	const dx = x - CLOCK_CENTER;
	const dy = y - CLOCK_CENTER;
	const angle = clockAngleFromDelta(dx, dy);
	if (step === 'hours') {
		const hour12 = Math.round(angle / 30) % 12;
		const distance = Math.hypot(dx, dy);
		const midRadius = (HOUR_INNER_RADIUS + HOUR_OUTER_RADIUS) / 2;
		return distance < midRadius ? hour12 : (hour12 + 12) % 24;
	}
	return Math.round(angle / 6) % 60;
}

/**
 * Liefert die Position eines Zahlenwerts auf dem Zifferblatt.
 *
 * @param value Stunden, Minuten oder Sekunden
 * @param step aktueller Schritt
 * @returns SVG-Koordinaten
 */
export function clockPointForValue(
	value: number,
	step: ClockStep
): { x: number; y: number } {
	const unit = step === 'hours' ? value % 12 : value;
	const max = step === 'hours' ? 12 : 60;
	const angle = (unit / max) * 2 * Math.PI - Math.PI / 2;
	const radius =
		step === 'hours' && value < 12 ? HOUR_INNER_RADIUS : step === 'hours' ? HOUR_OUTER_RADIUS : MINUTE_RADIUS;
	return {
		x: CLOCK_CENTER + radius * Math.cos(angle),
		y: CLOCK_CENTER + radius * Math.sin(angle)
	};
}

/**
 * Beschriftungen auf dem Zifferblatt.
 *
 * @param step aktueller Schritt
 * @returns Werte und Koordinaten
 */
export function clockLabels(step: ClockStep): Array<{ value: number; label: string; x: number; y: number }> {
	if (step === 'hours') {
		return Array.from({ length: 24 }, (_, value) => {
			const point = clockPointForValue(value, 'hours');
			return { value, label: padClockUnit(value), ...point };
		});
	}
	return Array.from({ length: 12 }, (_, index) => {
		const value = index * 5;
		const point = clockPointForValue(value, step);
		return { value, label: padClockUnit(value), ...point };
	});
}

/**
 * Liefert den aktuell gewählten Zahlenwert eines Schritts.
 *
 * @param time Uhrzeit
 * @param step aktueller Schritt
 * @returns Stunden, Minuten oder Sekunden
 */
export function clockValueForStep(time: ClockTime, step: ClockStep): number {
	if (step === 'hours') {
		return time.hours;
	}
	if (step === 'minutes') {
		return time.minutes;
	}
	return time.seconds;
}

/**
 * Schreibt einen Zifferblattwert in die Uhrzeit.
 *
 * @param time bisherige Uhrzeit
 * @param step aktueller Schritt
 * @param value neuer Wert
 * @returns aktualisierte Uhrzeit
 */
export function applyClockValue(time: ClockTime, step: ClockStep, value: number): ClockTime {
	if (step === 'hours') {
		return { ...time, hours: clamp(value, 0, 23) };
	}
	if (step === 'minutes') {
		return { ...time, minutes: clamp(value, 0, 59) };
	}
	return { ...time, seconds: clamp(value, 0, 59) };
}
