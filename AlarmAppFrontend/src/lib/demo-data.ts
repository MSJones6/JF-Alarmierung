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
			topic: 'Gebäude 3',
			keyword: 'Feueralarm',
			info: 'Rauchentwicklung im Serverraum. Bitte umgehend prüfen!',
			status: 'planned'
		},
		{
			id: 'demo-2',
			scheduledAt: '2025-04-25T09:00:00',
			topic: 'IT-Systeme',
			keyword: 'Warnung',
			info: 'Geplante Wartung der Datenbank.',
			status: 'planned'
		},
		{
			id: 'demo-3',
			scheduledAt: '2025-04-26T16:15:30',
			topic: 'Allgemein',
			keyword: 'Info',
			info: 'Quartalsmeeting im Konferenzraum.',
			status: 'planned'
		},
		{
			id: 'demo-4',
			scheduledAt: '2025-04-28T11:00:00',
			topic: 'System',
			keyword: 'Test',
			info: 'Monatlicher Funktionstest der Alarmierung.',
			status: 'planned'
		},
		{
			id: 'demo-5',
			scheduledAt: '2025-04-30T08:30:45',
			topic: 'Eingang',
			keyword: 'Sicherheit',
			info: 'Türkontrolle – ungewöhnliche Aktivität im Eingangsbereich.',
			status: 'planned'
		}
	];
}

/**
 * Liefert die Standardwerte des Formulars „Neue Alarmierung“.
 *
 * @returns vorausgefüllter Entwurf passend zum Screenshot
 */
export function getDefaultDraft() {
	return {
		scheduledAt: '2025-04-24T14:30:15',
		topic: 'Gebäude 3',
		keyword: 'Feueralarm',
		info: 'Rauchentwicklung im Serverraum. Bitte umgehend prüfen!'
	};
}
