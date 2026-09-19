import { describe, expect, it, vi } from 'vitest';
import { API_CONFIG_URL, apiUrl, fetchApiHealth, fetchAppSettings, initApiClient } from './api';

describe('initApiClient', () => {
	it('übernimmt die API-Basis-URL aus api-config.json', async () => {
		const fetchFn = vi.fn(async () => ({
			ok: true,
			json: async () => ({ apiBaseUrl: 'http://127.0.0.1:8080/' })
		}));

		await initApiClient(fetchFn as unknown as typeof fetch);
		expect(fetchFn).toHaveBeenCalledWith(API_CONFIG_URL, { cache: 'no-store' });
		expect(apiUrl('/api/health')).toBe('http://127.0.0.1:8080/api/health');
	});

	it('lässt die Basis-URL leer, wenn die Konfiguration fehlt', async () => {
		const fetchFn = vi.fn(async () => {
			throw new Error('offline');
		});

		await initApiClient(fetchFn as unknown as typeof fetch);
		expect(apiUrl('/api/health')).toBe('/api/health');
	});
});

describe('fetchAppSettings', () => {
	it('liest Connections und Stichworte vom REST-Server', async () => {
		const fetchFn = vi.fn(async (url: string) => {
			if (String(url).endsWith('/api/connections')) {
				return {
					ok: true,
					json: async () => [
						{
							id: '1',
							name: 'Standard',
							useSsl: false,
							brokerHost: 'localhost',
							brokerPort: '9001',
							brokerPath: '/mqtt',
							user: 'alarm',
							password: 'alarm',
							mqttTopic: 'JF/Alarm/KB'
						}
					]
				};
			}
			if (String(url).endsWith('/api/keywords')) {
				return {
					ok: true,
					json: async () => [{ id: 'k1', name: 'Feueralarm', color: '#f43f5e' }]
				};
			}
			throw new Error(`unerwarteter Abruf: ${url}`);
		});

		await expect(fetchAppSettings(fetchFn as unknown as typeof fetch)).resolves.toMatchObject({
			keywords: [{ name: 'Feueralarm', color: '#f43f5e' }],
			topics: [expect.objectContaining({ name: 'Standard', brokerHost: 'localhost' })]
		});
	});
});

describe('fetchApiHealth', () => {
	it('meldet die API als erreichbar, wenn der Status UP ist', async () => {
		const fetchFn = vi.fn(async () => ({
			ok: true,
			json: async () => ({ status: 'UP' })
		}));

		await expect(fetchApiHealth(fetchFn as unknown as typeof fetch)).resolves.toBe(true);
		expect(fetchFn).toHaveBeenCalledWith(expect.stringMatching(/\/api\/health$/), {
			cache: 'no-store'
		});
	});

	it('meldet die API als offline, wenn der Abruf fehlschlägt', async () => {
		const fetchFn = vi.fn(async () => {
			throw new Error('offline');
		});

		await expect(fetchApiHealth(fetchFn as unknown as typeof fetch)).resolves.toBe(false);
	});
});
