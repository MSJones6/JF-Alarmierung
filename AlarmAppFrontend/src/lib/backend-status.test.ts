import { afterEach, describe, expect, it, vi } from 'vitest';
import {
	backendStatusLabel,
	isApiHealthy,
	startBackendHealthPolling
} from './backend-status';

describe('isApiHealthy', () => {
	it('erkennt den Status UP unabhängig von der Großschreibung', () => {
		expect(isApiHealthy({ status: 'UP' })).toBe(true);
		expect(isApiHealthy({ status: 'up' })).toBe(true);
	});

	it('lehnt fehlende oder ungültige Antworten ab', () => {
		expect(isApiHealthy(null)).toBe(false);
		expect(isApiHealthy({ status: 'DOWN' })).toBe(false);
		expect(isApiHealthy('UP')).toBe(false);
	});
});

describe('backendStatusLabel', () => {
	it('liefert Kurztexte für die Kopfzeile', () => {
		expect(backendStatusLabel('online')).toBe('Server online');
		expect(backendStatusLabel('offline')).toBe('Server offline');
		expect(backendStatusLabel('checking')).toBe('Serverprüfung');
	});
});

describe('startBackendHealthPolling', () => {
	afterEach(() => {
		vi.useRealTimers();
	});

	it('meldet den ersten Status sofort und den nächsten nach dem Intervall', async () => {
		vi.useFakeTimers();
		const check = vi.fn().mockResolvedValueOnce(true).mockResolvedValueOnce(false);
		const onStatus = vi.fn();

		const stop = startBackendHealthPolling({ check, onStatus, intervalMs: 1000 });
		await Promise.resolve();
		expect(onStatus).toHaveBeenCalledWith('online');

		await vi.advanceTimersByTimeAsync(1000);
		expect(onStatus).toHaveBeenCalledWith('offline');
		stop();
	});

	it('meldet nach dem Stoppen keine weiteren Statusänderungen', async () => {
		vi.useFakeTimers();
		const check = vi.fn().mockResolvedValue(true);
		const onStatus = vi.fn();

		const stop = startBackendHealthPolling({ check, onStatus, intervalMs: 1000 });
		await Promise.resolve();
		stop();
		await vi.advanceTimersByTimeAsync(5000);

		expect(check).toHaveBeenCalledTimes(1);
		expect(onStatus).toHaveBeenCalledTimes(1);
	});
});
