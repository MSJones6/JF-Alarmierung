import type { AlarmItem } from './types';

/**
 * Liefert die Beispieldaten der Oberfläche, passend zum entworfenen Screenshot.
 *
 * @returns fünf geplante Alarmierungen
 */
export function getDemoAlarms(): AlarmItem[] {
	return [
		{
			id: 'demo-1',
			scheduledAt: '2025-04-24T14:30:15',
			connection: 'Standard',
			location: 'Gebäude 3',
			keyword: 'Feueralarm',
			info: 'Rauchentwicklung im Serverraum. Bitte umgehend prüfen!',
			status: 'planned'
		},
		{
			id: 'demo-2',
			scheduledAt: '2025-04-25T09:00:00',
			connection: 'Standard',
			location: 'IT-Systeme',
			keyword: 'Warnung',
			info: 'Geplante Wartung der Datenbank.',
			status: 'planned'
		},
		{
			id: 'demo-3',
			scheduledAt: '2025-04-26T16:15:30',
			connection: 'Standard',
			location: 'Allgemein',
			keyword: 'Info',
			info: 'Quartalsmeeting im Konferenzraum.',
			status: 'planned'
		},
		{
			id: 'demo-4',
			scheduledAt: '2025-04-28T11:00:00',
			connection: 'Standard',
			location: 'System',
			keyword: 'Test',
			info: 'Monatlicher Funktionstest der Alarmierung.',
			status: 'planned'
		},
		{
			id: 'demo-5',
			scheduledAt: '2025-04-30T08:30:45',
			connection: 'Standard',
			location: 'Eingang',
			keyword: 'Sicherheit',
			info: 'Türkontrolle – ungewöhnliche Aktivität im Eingangsbereich.',
			status: 'planned'
		}
	];
}

/**
 * Liefert die Standardwerte des Formulars „Neue Alarmierung“.
 *
 * @param options optionale Auswahllisten aus den Einstellungen
 * @returns vorausgefüllter Entwurf
 */
export function getDefaultDraft(options?: { connections?: string[]; keywords?: string[] }) {
	return {
		scheduledAt: '2025-04-24T14:30:15',
		connection: options?.connections?.[0] ?? 'Standard',
		location: 'Gebäude 3',
		keyword: options?.keywords?.[0] ?? 'Feueralarm',
		info: 'Rauchentwicklung im Serverraum. Bitte umgehend prüfen!'
	};
}
