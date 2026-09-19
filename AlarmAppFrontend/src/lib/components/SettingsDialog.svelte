<script lang="ts">
	/**
	 * Dialog für MQTT-Broker-Einstellungen.
	 * Änderungen werden erst beim Speichern übernommen und im Browser persistiert.
	 */
	import { untrack } from 'svelte';
	import X from '@lucide/svelte/icons/x';
	import { parseMqttConfig, saveMqttSettings } from '$lib/mqtt-config';
	import type { MqttSettings } from '$lib/types';

	let {
		open = $bindable(),
		settings = $bindable()
	}: {
		open: boolean;
		settings: MqttSettings;
	} = $props();

	/** Bearbeitete Kopie, bis der Dialog gespeichert oder geschlossen wird. */
	let draft = $state<MqttSettings>({ ...settings });

	$effect(() => {
		if (open) {
			untrack(() => {
				draft = { ...settings };
			});
		}
	});

	/**
	 * Schließt den Einstellungsdialog ohne zu speichern.
	 */
	function close(): void {
		open = false;
	}

	/**
	 * Übernimmt die Eingaben, speichert sie im Browser und schließt den Dialog.
	 */
	function save(): void {
		settings = parseMqttConfig(draft);
		saveMqttSettings(settings);
		open = false;
	}
</script>

{#if open}
	<div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/30 p-4">
		<div class="w-full max-w-lg rounded-[28px] bg-white p-6 shadow-2xl">
			<div class="mb-5 flex items-start justify-between gap-4">
				<div>
					<h2 class="text-xl font-extrabold text-navy">Einstellungen</h2>
					<p class="text-sm text-slate-400">
						Die Werte werden in diesem Browser gespeichert und bleiben nach dem Neuladen erhalten.
					</p>
				</div>
				<button
					class="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-500 hover:bg-slate-50"
					type="button"
					aria-label="Einstellungen schließen"
					onclick={close}
				>
					<X size={18} />
				</button>
			</div>

			<div class="space-y-4">
				<label class="flex items-center gap-3 text-sm font-medium text-slate-700">
					<input
						class="rounded border-slate-300 text-blue-600 focus:ring-blue-400"
						type="checkbox"
						bind:checked={draft.useSsl}
					/>
					SSL-Verschlüsselung (verschlüsselt)
				</label>

				<label class="block text-sm font-medium text-slate-700">
					Hostname
					<input
						class="mt-1 w-full rounded-xl border-slate-200 focus:border-blue-400 focus:ring-blue-400"
						bind:value={draft.brokerHost}
					/>
				</label>

				<label class="block text-sm font-medium text-slate-700">
					Port
					<input
						class="mt-1 w-full rounded-xl border-slate-200 focus:border-blue-400 focus:ring-blue-400"
						type="number"
						min="1"
						max="65535"
						bind:value={draft.brokerPort}
					/>
				</label>

				<label class="block text-sm font-medium text-slate-700">
					Pfad
					<input
						class="mt-1 w-full rounded-xl border-slate-200 focus:border-blue-400 focus:ring-blue-400"
						bind:value={draft.brokerPath}
					/>
				</label>

				<label class="block text-sm font-medium text-slate-700">
					MQTT-Topic
					<input
						class="mt-1 w-full rounded-xl border-slate-200 focus:border-blue-400 focus:ring-blue-400"
						bind:value={draft.mqttTopic}
					/>
				</label>

				<label class="block text-sm font-medium text-slate-700">
					User
					<input
						class="mt-1 w-full rounded-xl border-slate-200 focus:border-blue-400 focus:ring-blue-400"
						bind:value={draft.user}
					/>
				</label>

				<label class="block text-sm font-medium text-slate-700">
					Password
					<input
						class="mt-1 w-full rounded-xl border-slate-200 focus:border-blue-400 focus:ring-blue-400"
						type="password"
						bind:value={draft.password}
					/>
				</label>
			</div>

			<button
				class="mt-6 w-full rounded-xl bg-brand py-3 font-semibold text-white hover:bg-brand-hover"
				type="button"
				onclick={save}
			>
				Speichern
			</button>
		</div>
	</div>
{/if}
