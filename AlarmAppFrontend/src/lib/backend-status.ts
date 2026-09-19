import type { BackendConnectionStatus } from './types';

/** Abstand zwischen zwei Health-Prüfungen in Millisekunden. */
export const BACKEND_HEALTH_INTERVAL_MS = 5000;

/**
 * Prüft, ob die Health-Antwort den Server als erreichbar ausweist.
 *
 * @param raw geparste JSON-Antwort
 * @returns `true`, wenn der Status `UP` ist
 */
export function isApiHealthy(raw: unknown): boolean {
	if (raw === null || typeof raw !== 'object') {
		return false;
	}
	const status = (raw as Record<string, unknown>).status;
	return typeof status === 'string' && status.toUpperCase() === 'UP';
}

/**
 * Liefert den sichtbaren Text für den Serverstatus.
 *
 * @param status aktueller Verbindungszustand
 * @returns Kurzbezeichnung für die Kopfzeile
 */
export function backendStatusLabel(status: BackendConnectionStatus): string {
	switch (status) {
		case 'online':
			return 'Server online';
		case 'offline':
			return 'Server offline';
		default:
			return 'Serverprüfung';
	}
}

/**
 * Startet eine wiederholte Health-Prüfung.
 *
 * Die erste Prüfung läuft sofort. Nach `stop` werden keine weiteren
 * Statusänderungen mehr gemeldet.
 *
 * @param options Prüf-Callback, Status-Callback und optionales Intervall
 * @returns Funktion zum Beenden der Prüfung
 */
export function startBackendHealthPolling(options: {
	check: () => Promise<boolean>;
	onStatus: (status: BackendConnectionStatus) => void;
	intervalMs?: number;
}): () => void {
	const intervalMs = options.intervalMs ?? BACKEND_HEALTH_INTERVAL_MS;
	let stopped = false;
	let timer: ReturnType<typeof setTimeout> | undefined;

	/**
	 * Führt eine Prüfung aus und plant die nächste.
	 */
	async function tick(): Promise<void> {
		if (stopped) {
			return;
		}
		const online = await options.check();
		if (stopped) {
			return;
		}
		options.onStatus(online ? 'online' : 'offline');
		timer = setTimeout(() => {
			void tick();
		}, intervalMs);
	}

	void tick();

	return () => {
		stopped = true;
		if (timer !== undefined) {
			clearTimeout(timer);
		}
	};
}
