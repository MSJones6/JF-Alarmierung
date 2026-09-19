<script lang="ts">
	/**
	 * Liste und Formular für Connections.
	 * Jede Connection speichert Host, Kontodaten und die übrigen Broker-Einstellungen.
	 */
	import Plus from '@lucide/svelte/icons/plus';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { createTopicConnection, nextTopicName } from '$lib/topic-connection';
	import type { TopicConnection } from '$lib/types';

	let {
		topics = $bindable()
	}: {
		topics: TopicConnection[];
	} = $props();

	let selectedId = $state(topics[0]?.id ?? '');

	const selectedIndex = $derived(topics.findIndex((topic) => topic.id === selectedId));

	$effect(() => {
		if (selectedIndex >= 0) {
			return;
		}
		selectedId = topics[0]?.id ?? '';
	});

	/**
	 * Legt eine neue Connection an und wählt sie aus.
	 */
	function addTopic(): void {
		const template = selectedIndex >= 0 ? topics[selectedIndex] : topics[0];
		const created = createTopicConnection({
			...template,
			id: crypto.randomUUID(),
			name: nextTopicName(topics)
		});
		topics = [...topics, created];
		selectedId = created.id;
	}

	/**
	 * Entfernt die aktuell gewählte Connection.
	 */
	function removeSelected(): void {
		if (selectedIndex < 0) {
			return;
		}
		const current = topics[selectedIndex];
		const confirmed = confirm(`Connection „${current.name}“ wirklich löschen?`);
		if (!confirmed) {
			return;
		}
		topics = topics.filter((topic) => topic.id !== current.id);
	}
</script>

<div class="space-y-4">
	<div class="flex items-center justify-between gap-3">
		<div>
			<h3 class="text-sm font-semibold tracking-wide text-slate-500 uppercase">Connections</h3>
			<p class="text-sm text-slate-400">
				Jede Connection verbindet sich mit dem hinterlegten Host und dessen Zugangsdaten.
			</p>
		</div>
		<button
			class="inline-flex items-center gap-2 rounded-xl bg-brand px-3 py-2 text-sm font-semibold text-white hover:bg-brand-hover"
			type="button"
			onclick={addTopic}
		>
			<Plus size={16} />
			Connection hinzufügen
		</button>
	</div>

	{#if topics.length === 0}
		<p class="rounded-xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
			Noch keine Connection angelegt. Über „Connection hinzufügen“ eine Verbindung speichern.
		</p>
	{:else}
		<div class="flex flex-wrap gap-2">
			{#each topics as topic (topic.id)}
				<button
					class={`rounded-full px-3 py-1.5 text-sm font-medium ${
						topic.id === selectedId
							? 'bg-blue-600 text-white'
							: 'bg-slate-100 text-slate-600 hover:bg-slate-200'
					}`}
					type="button"
					onclick={() => (selectedId = topic.id)}
				>
					{topic.name}
				</button>
			{/each}
		</div>
	{/if}

	{#if selectedIndex >= 0}
		<div class="space-y-4 rounded-2xl border border-slate-100 bg-slate-50 p-4">
			<div class="flex items-start justify-between gap-3">
				<label class="block flex-1 text-sm font-medium text-slate-700">
					Name im Dropdown
					<input
						class="mt-1 w-full rounded-xl border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-400"
						bind:value={topics[selectedIndex].name}
					/>
				</label>
				<button
					class="mt-7 flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 hover:text-rose-500"
					type="button"
					aria-label="Gewählte Connection löschen"
					onclick={removeSelected}
				>
					<Trash2 size={18} />
				</button>
			</div>

			<label class="flex items-center gap-3 text-sm font-medium text-slate-700">
				<input
					class="rounded border-slate-300 text-blue-600 focus:ring-blue-400"
					type="checkbox"
					bind:checked={topics[selectedIndex].useSsl}
				/>
				SSL-Verschlüsselung (verschlüsselt)
			</label>

			<label class="block text-sm font-medium text-slate-700">
				Hostname
				<input
					class="mt-1 w-full rounded-xl border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-400"
					bind:value={topics[selectedIndex].brokerHost}
				/>
			</label>

			<label class="block text-sm font-medium text-slate-700">
				Port
				<input
					class="mt-1 w-full rounded-xl border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-400"
					type="number"
					min="1"
					max="65535"
					bind:value={topics[selectedIndex].brokerPort}
				/>
			</label>

			<label class="block text-sm font-medium text-slate-700">
				Pfad
				<input
					class="mt-1 w-full rounded-xl border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-400"
					bind:value={topics[selectedIndex].brokerPath}
				/>
			</label>

			<label class="block text-sm font-medium text-slate-700">
				MQTT-Topic
				<input
					class="mt-1 w-full rounded-xl border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-400"
					bind:value={topics[selectedIndex].mqttTopic}
				/>
			</label>

			<label class="block text-sm font-medium text-slate-700">
				Benutzername
				<input
					class="mt-1 w-full rounded-xl border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-400"
					bind:value={topics[selectedIndex].user}
				/>
			</label>

			<label class="block text-sm font-medium text-slate-700">
				Passwort
				<input
					class="mt-1 w-full rounded-xl border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-400"
					type="password"
					bind:value={topics[selectedIndex].password}
				/>
			</label>
		</div>
	{/if}
</div>
