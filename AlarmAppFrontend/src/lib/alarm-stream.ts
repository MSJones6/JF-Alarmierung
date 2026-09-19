import type { AlarmItem } from './types';
import { apiUrl } from './api';

/** Nutzdaten eines Alarm-Stream-Ereignisses. */
export type AlarmStreamPayload =
	| { event: 'snapshot'; alarms: AlarmItem[] }
	| { event: 'created' | 'updated'; alarm: AlarmItem }
	| { event: 'deleted'; id: string };

/**
 * Wendet ein Stream-Ereignis auf die lokale Alarmliste an.
 *
 * @param alarms bisherige Liste
 * @param payload Stream-Ereignis
 * @returns neue Liste
 */
export function applyAlarmStreamEvent(alarms: AlarmItem[], payload: AlarmStreamPayload): AlarmItem[] {
	switch (payload.event) {
		case 'snapshot':
			return payload.alarms;
		case 'created':
			if (alarms.some((alarm) => alarm.id === payload.alarm.id)) {
				return alarms.map((alarm) => (alarm.id === payload.alarm.id ? payload.alarm : alarm));
			}
			return [...alarms, payload.alarm];
		case 'updated':
			if (alarms.some((alarm) => alarm.id === payload.alarm.id)) {
				return alarms.map((alarm) => (alarm.id === payload.alarm.id ? payload.alarm : alarm));
			}
			return [...alarms, payload.alarm];
		case 'deleted':
			return alarms.filter((alarm) => alarm.id !== payload.id);
	}
}

/**
 * Abonniert den Server-Sent-Events-Stream der Alarmtabelle.
 *
 * @param onAlarms Callback mit dem aktuellen Tabellenstand
 * @param onError Callback bei Verbindungsfehlern
 * @returns Funktion zum Schließen des Streams
 */
export function subscribeAlarmStream(
	onAlarms: (alarms: AlarmItem[]) => void,
	onError?: (message: string) => void
): () => void {
	let current: AlarmItem[] = [];
	const source = new EventSource(apiUrl('/api/alarms/stream'));

	/**
	 * Übernimmt ein JSON-Ereignis in die lokale Liste.
	 *
	 * @param payload Stream-Ereignis
	 */
	function apply(payload: AlarmStreamPayload): void {
		current = applyAlarmStreamEvent(current, payload);
		onAlarms(current);
	}

	source.addEventListener('snapshot', (event) => {
		apply({ event: 'snapshot', alarms: JSON.parse((event as MessageEvent).data) as AlarmItem[] });
	});
	source.addEventListener('created', (event) => {
		apply({ event: 'created', alarm: JSON.parse((event as MessageEvent).data) as AlarmItem });
	});
	source.addEventListener('updated', (event) => {
		apply({ event: 'updated', alarm: JSON.parse((event as MessageEvent).data) as AlarmItem });
	});
	source.addEventListener('deleted', (event) => {
		const data = JSON.parse((event as MessageEvent).data) as { id: string };
		apply({ event: 'deleted', id: data.id });
	});
	source.onerror = () => {
		if (source.readyState === EventSource.CLOSED) {
			onError?.('Die Verbindung zur Alarmtabelle wurde unterbrochen.');
		}
	};

	return () => source.close();
}
