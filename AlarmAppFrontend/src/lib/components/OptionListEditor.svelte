<script lang="ts">
	/**
	 * Eingabe zum Anlegen und Entfernen von Dropdown-Einträgen.
	 */
	import Plus from '@lucide/svelte/icons/plus';
	import X from '@lucide/svelte/icons/x';
	import { addUniqueOption, removeOption } from '$lib/option-list';

	let {
		title,
		items = $bindable(),
		placeholder
	}: {
		title: string;
		items: string[];
		placeholder: string;
	} = $props();

	let inputValue = $state('');
	let error = $state('');

	/**
	 * Übernimmt den aktuellen Eingabetext in die Liste.
	 */
	function addItem(): void {
		const next = addUniqueOption(items, inputValue);
		if (!next) {
			error = inputValue.trim()
				? 'Dieser Eintrag ist bereits vorhanden.'
				: 'Bitte einen Namen eingeben.';
			return;
		}
		items = next;
		inputValue = '';
		error = '';
	}

	/**
	 * Entfernt einen Eintrag aus der Liste.
	 *
	 * @param value zu löschender Eintrag
	 */
	function removeItem(value: string): void {
		items = removeOption(items, value);
		error = '';
	}

	/**
	 * Legt den Eintrag per Enter-Taste an.
	 *
	 * @param event Tastaturereignis des Eingabefelds
	 */
	function handleKeydown(event: KeyboardEvent): void {
		if (event.key === 'Enter') {
			event.preventDefault();
			addItem();
		}
	}
</script>

<div>
	<p class="text-sm font-medium text-slate-700">{title}</p>
	<div class="mt-2 flex gap-2">
		<input
			class="w-full rounded-xl border-slate-200 focus:border-blue-400 focus:ring-blue-400"
			{placeholder}
			bind:value={inputValue}
			onkeydown={handleKeydown}
		/>
		<button
			class="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand text-white hover:bg-brand-hover"
			type="button"
			aria-label="{title} hinzufügen"
			onclick={addItem}
		>
			<Plus size={18} />
		</button>
	</div>
	{#if error}
		<p class="mt-2 text-sm text-rose-600">{error}</p>
	{/if}
	<div class="mt-3 flex flex-wrap gap-2">
		{#each items as item (item)}
			<span
				class="inline-flex items-center gap-1 rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-700"
			>
				{item}
				<button
					class="rounded-full p-0.5 text-slate-400 hover:bg-white hover:text-rose-500"
					type="button"
					aria-label="{item} entfernen"
					onclick={() => removeItem(item)}
				>
					<X size={14} />
				</button>
			</span>
		{/each}
		{#if items.length === 0}
			<p class="text-sm text-slate-400">Noch keine Einträge.</p>
		{/if}
	</div>
</div>
