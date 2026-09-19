<script lang="ts">
	/**
	 * Dialog für Alarmstichworte und Connections.
	 * Änderungen werden erst beim Speichern übernommen und im Browser persistiert.
	 */
	import { untrack } from 'svelte';
	import X from '@lucide/svelte/icons/x';
	import OptionListEditor from '$lib/components/OptionListEditor.svelte';
	import TopicConnectionsEditor from '$lib/components/TopicConnectionsEditor.svelte';
	import { parseAppSettings, saveMqttSettings } from '$lib/mqtt-config';
	import type { AppSettings } from '$lib/types';

	let {
		open = $bindable(),
		settings = $bindable()
	}: {
		open: boolean;
		settings: AppSettings;
	} = $props();

	/** Bearbeitete Kopie, bis der Dialog gespeichert oder geschlossen wird. */
	let draft = $state<AppSettings>(copySettings(settings));

	$effect(() => {
		if (open) {
			untrack(() => {
				draft = copySettings(settings);
			});
		}
	});

	/**
	 * Kopiert Einstellungen inklusive Connections.
	 *
	 * @param source aktuelle Einstellungen
	 * @returns unabhängige Kopie
	 */
	function copySettings(source: AppSettings): AppSettings {
		return {
			keywords: [...source.keywords],
			topics: source.topics.map((topic) => ({ ...topic }))
		};
	}

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
		settings = parseAppSettings(draft);
		saveMqttSettings(settings);
		open = false;
	}
</script>

{#if open}
	<div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/30 p-4">
		<div class="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-[28px] bg-white p-6 shadow-2xl">
			<div class="mb-5 flex items-start justify-between gap-4">
				<div>
					<h2 class="text-xl font-extrabold text-navy">Einstellungen</h2>
					<p class="text-sm text-slate-400">
						Jede Connection speichert Host, Benutzername, Passwort und die übrigen Verbindungsdaten
						in diesem Browser.
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

			<div class="space-y-6">
				<TopicConnectionsEditor bind:topics={draft.topics} />

				<section class="border-t border-slate-100 pt-5">
					<OptionListEditor
						title="Alarmstichworte"
						placeholder="Neues Alarmstichwort"
						bind:items={draft.keywords}
					/>
				</section>
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
