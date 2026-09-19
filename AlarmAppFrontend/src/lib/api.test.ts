import { describe, expect, it, vi } from 'vitest';
import { apiUrl, fetchAppSettings, initApiClient } from './api';

describe('initApiClient', () => {
	it('übernimmt die API-Basis-URL aus mqtt-config.json', async () => {
		const fetchFn = vi.fn(async () => ({
			ok: true,
			json: async () => ({ apiBaseUrl: 'http://127.0.0.1:8080/' })
		}));

		await initApiClient(fetchFn as unknown as typeof fetch);
		expect(apiUrl('/api/health')).toBe('http://127.0.0.1:8080/api/health');
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
					json: async () => [{ id: 'k1', name: 'Feueralarm' }]
				};
			}
			throw new Error(`unerwarteter Abruf: ${url}`);
		});

		await expect(fetchAppSettings(fetchFn as unknown as typeof fetch)).resolves.toMatchObject({
			keywords: ['Feueralarm'],
			topics: [expect.objectContaining({ name: 'Standard', brokerHost: 'localhost' })]
		});
	});
});
