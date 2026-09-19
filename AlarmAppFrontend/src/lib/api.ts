/**
 * HTTP-Client für die Alarm-API unter AlarmAppServer.
 */
import { getKeywordColor } from './alarm';
import type { AlarmDraft, AlarmItem, AlarmStatus, AppSettings, KeywordOption, TopicConnection } from './types';

/** Basis-URL der REST-API, leer bedeutet gleicher Ursprung (Vite-Proxy). */
export let apiBaseUrl = '';

/**
 * Liest die API-URL aus der Laufzeitkonfiguration.
 *
 * @param fetchFn optionale Fetch-Funktion
 */
export async function initApiClient(fetchFn: typeof fetch = fetch): Promise<void> {
	try {
		const response = await fetchFn('/mqtt-config.json', { cache: 'no-store' });
		if (!response.ok) {
			return;
		}
		const data = (await response.json()) as { apiBaseUrl?: unknown };
		if (typeof data.apiBaseUrl === 'string') {
			apiBaseUrl = data.apiBaseUrl.trim().replace(/\/$/, '');
		}
	} catch {
		apiBaseUrl = '';
	}
}

/**
 * Baut eine absolute oder relative API-URL.
 *
 * @param path Pfad beginnend mit `/`
 * @returns aufrufbare URL
 */
export function apiUrl(path: string): string {
	return `${apiBaseUrl}${path}`;
}

/**
 * Liest alle Alarmierungen vom REST-Server.
 *
 * @param fetchFn optionale Fetch-Funktion
 * @returns Alarmliste
 */
export async function fetchAlarms(fetchFn: typeof fetch = fetch): Promise<AlarmItem[]> {
	return readJson<AlarmItem[]>(fetchFn, '/api/alarms');
}

/**
 * Liest Connections und Alarmstichworte vom REST-Server.
 *
 * @param fetchFn optionale Fetch-Funktion
 * @returns App-Einstellungen
 */
export async function fetchAppSettings(fetchFn: typeof fetch = fetch): Promise<AppSettings> {
	const [topics, keywords] = await Promise.all([
		readJson<TopicConnection[]>(fetchFn, '/api/connections'),
		readJson<KeywordOption[]>(fetchFn, '/api/keywords')
	]);
	return {
		topics,
		keywords: toKeywordOptions(keywords)
	};
}

/**
 * Ersetzt Connections und Alarmstichworte auf dem Server.
 *
 * @param settings aktuelle Einstellungen
 * @param fetchFn optionale Fetch-Funktion
 * @returns gespeicherte Einstellungen
 */
export async function saveAppSettings(
	settings: AppSettings,
	fetchFn: typeof fetch = fetch
): Promise<AppSettings> {
	const [topics, keywords] = await Promise.all([
		writeJson<TopicConnection[]>(fetchFn, '/api/connections', 'PUT', settings.topics),
		writeJson<KeywordOption[]>(fetchFn, '/api/keywords', 'PUT', settings.keywords)
	]);
	return {
		topics,
		keywords: toKeywordOptions(keywords)
	};
}

/**
 * Legt eine Alarmierung auf dem Server an.
 *
 * @param draft Formularwerte
 * @param status geplanter oder gesendeter Status
 * @param fetchFn optionale Fetch-Funktion
 * @returns gespeicherte Alarmierung
 */
export async function createRemoteAlarm(
	draft: AlarmDraft,
	status: AlarmStatus,
	fetchFn: typeof fetch = fetch
): Promise<AlarmItem> {
	return writeJson<AlarmItem>(fetchFn, '/api/alarms', 'POST', toAlarmBody(draft, status));
}

/**
 * Aktualisiert eine Alarmierung auf dem Server.
 *
 * @param id Alarm-ID
 * @param draft Formularwerte
 * @param status geplanter oder gesendeter Status
 * @param fetchFn optionale Fetch-Funktion
 * @returns gespeicherte Alarmierung
 */
export async function updateRemoteAlarm(
	id: string,
	draft: AlarmDraft,
	status: AlarmStatus,
	fetchFn: typeof fetch = fetch
): Promise<AlarmItem> {
	return writeJson<AlarmItem>(fetchFn, `/api/alarms/${id}`, 'PUT', toAlarmBody(draft, status));
}

/**
 * Löscht eine Alarmierung auf dem Server.
 *
 * @param id Alarm-ID
 * @param fetchFn optionale Fetch-Funktion
 */
export async function deleteRemoteAlarm(id: string, fetchFn: typeof fetch = fetch): Promise<void> {
	const response = await fetchFn(apiUrl(`/api/alarms/${id}`), { method: 'DELETE' });
	if (!response.ok && response.status !== 404) {
		throw await toApiError(response);
	}
}

/**
 * Baut den JSON-Körper einer Alarmierung.
 *
 * @param draft Formularwerte
 * @param status Status
 * @returns API-Körper
 */
function toAlarmBody(draft: AlarmDraft, status: AlarmStatus) {
	return {
		scheduledAt: draft.scheduledAt,
		connection: draft.connection,
		location: draft.location.trim(),
		keyword: draft.keyword,
		info: draft.info.trim(),
		status
	};
}

/**
 * Übernimmt Name und Farbe aus der API-Antwort.
 *
 * @param keywords Stichworte vom Server
 * @returns Einstellungsobjekte
 */
function toKeywordOptions(keywords: Array<{ name: string; color?: string }>): KeywordOption[] {
	return keywords.map((keyword) => ({
		name: keyword.name,
		color: getKeywordColor(keyword.name, keyword.color)
	}));
}

/**
 * Liest JSON von der API.
 *
 * @param fetchFn Fetch-Funktion
 * @param path API-Pfad
 * @returns geparste Antwort
 */
async function readJson<T>(fetchFn: typeof fetch, path: string): Promise<T> {
	const response = await fetchFn(apiUrl(path), { cache: 'no-store' });
	if (!response.ok) {
		throw await toApiError(response);
	}
	return (await response.json()) as T;
}

/**
 * Sendet JSON an die API und liest die Antwort.
 *
 * @param fetchFn Fetch-Funktion
 * @param path API-Pfad
 * @param method HTTP-Methode
 * @param body JSON-Körper
 * @returns geparste Antwort
 */
async function writeJson<T>(
	fetchFn: typeof fetch,
	path: string,
	method: 'POST' | 'PUT',
	body: unknown
): Promise<T> {
	const response = await fetchFn(apiUrl(path), {
		method,
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(body)
	});
	if (!response.ok) {
		throw await toApiError(response);
	}
	return (await response.json()) as T;
}

/**
 * Baut einen Fehler aus einer API-Antwort.
 *
 * @param response HTTP-Antwort
 * @returns Fehler
 */
async function toApiError(response: Response): Promise<Error> {
	try {
		const data = (await response.json()) as { message?: string };
		if (typeof data.message === 'string' && data.message.trim()) {
			return new Error(data.message);
		}
	} catch {
		// Antwort ohne JSON
	}
	return new Error(`API-Fehler (${response.status})`);
}
