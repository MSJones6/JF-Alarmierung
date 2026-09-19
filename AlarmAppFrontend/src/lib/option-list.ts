import type { KeywordOption } from './types';
import { getKeywordColor, normalizeKeywordColor } from './alarm';

/**
 * Hilfsfunktionen für pflegbare Auswahllisten in den Einstellungen.
 */

/**
 * Entfernt Leerzeichen und leere Werte aus einer Auswahlliste.
 *
 * @param items Rohwerte
 * @returns bereinigte Liste ohne Duplikate
 */
export function normalizeOptions(items: unknown[]): string[] {
	const unique: string[] = [];
	const seen = new Set<string>();

	for (const item of items) {
		if (typeof item !== 'string') {
			continue;
		}
		const value = item.trim();
		if (!value) {
			continue;
		}
		const key = value.toLocaleLowerCase('de-DE');
		if (seen.has(key)) {
			continue;
		}
		seen.add(key);
		unique.push(value);
	}

	return unique;
}

/**
 * Fügt einen neuen Eintrag hinzu, sofern er nicht leer oder schon vorhanden ist.
 *
 * @param items aktuelle Liste
 * @param raw neuer Wert
 * @returns aktualisierte Liste oder `null`, wenn nichts übernommen wurde
 */
export function addUniqueOption(items: string[], raw: string): string[] | null {
	const value = raw.trim();
	if (!value) {
		return null;
	}

	const exists = items.some(
		(item) => item.toLocaleLowerCase('de-DE') === value.toLocaleLowerCase('de-DE')
	);
	if (exists) {
		return null;
	}

	return [...items, value];
}

/**
 * Entfernt einen Eintrag aus der Auswahlliste.
 *
 * @param items aktuelle Liste
 * @param value zu löschender Wert
 * @returns Liste ohne den Eintrag
 */
export function removeOption(items: string[], value: string): string[] {
	return items.filter((item) => item !== value);
}

/**
 * Fügt ein neues Alarmstichwort mit Farbe hinzu, sofern der Name noch frei ist.
 *
 * @param items aktuelle Liste
 * @param raw neuer Name
 * @param color gewählte Badge-Farbe
 * @returns aktualisierte Liste oder `null`, wenn nichts übernommen wurde
 */
export function addUniqueKeyword(
	items: KeywordOption[],
	raw: string,
	color: string
): KeywordOption[] | null {
	const name = raw.trim();
	if (!name) {
		return null;
	}

	const exists = items.some(
		(item) => item.name.toLocaleLowerCase('de-DE') === name.toLocaleLowerCase('de-DE')
	);
	if (exists) {
		return null;
	}

	return [...items, { name, color: getKeywordColor(name, color) }];
}

/**
 * Entfernt ein Alarmstichwort anhand des Namens.
 *
 * @param items aktuelle Liste
 * @param name zu löschendes Stichwort
 * @returns Liste ohne den Eintrag
 */
export function removeKeyword(items: KeywordOption[], name: string): KeywordOption[] {
	return items.filter((item) => item.name !== name);
}

/**
 * Aktualisiert die Badge-Farbe eines Alarmstichworts.
 *
 * @param items aktuelle Liste
 * @param name zu änderndes Stichwort
 * @param color neue Farbe
 * @returns Liste mit aktualisierter Farbe
 */
export function updateKeywordColor(
	items: KeywordOption[],
	name: string,
	color: string
): KeywordOption[] {
	const normalized = normalizeKeywordColor(color);
	return items.map((item) => (item.name === name ? { ...item, color: normalized } : item));
}
