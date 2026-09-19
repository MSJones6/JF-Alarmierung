<script lang="ts">
	/**
	 * Eingabe zum Anlegen und Einfärben von Alarmstichworten.
	 * Die Badge-Hintergrundfarbe wird automatisch zur gewählten Farbe passend aufgehellt.
	 */
	import Plus from '@lucide/svelte/icons/plus';
	import X from '@lucide/svelte/icons/x';
	import { DEFAULT_KEYWORD_COLOR, getKeywordBadgeStyle } from '$lib/alarm';
	import { addUniqueKeyword, removeKeyword, updateKeywordColor } from '$lib/option-list';
	import type { KeywordOption } from '$lib/types';

	let {
		title,
		items = $bindable(),
		placeholder
	}: {
		title: string;
		items: KeywordOption[];
		placeholder: string;
	} = $props();

	let inputValue = $state('');
	let selectedColor = $state(DEFAULT_KEYWORD_COLOR);
	let error = $state('');

	/**
	 * Übernimmt den aktuellen Eingabetext mit der gewählten Farbe in die Liste.
	 */
	function addItem(): void {
		const next = addUniqueKeyword(items, inputValue, selectedColor);
		if (!next) {
			error = inputValue.trim()
				? 'Dieser Eintrag ist bereits vorhanden.'
				: 'Bitte einen Namen eingeben.';
			return;
		}
		items = next;
		inputValue = '';
		selectedColor = DEFAULT_KEYWORD_COLOR;
		error = '';
	}

	/**
	 * Entfernt ein Stichwort aus der Liste.
	 *
	 * @param name zu löschendes Stichwort
	 */
	function removeItem(name: string): void {
		items = removeKeyword(items, name);
		error = '';
	}

	/**
	 * Übernimmt eine neue Badge-Farbe für ein vorhandenes Stichwort.
	 *
	 * @param name zu änderndes Stichwort
	 * @param color neue Farbe
	 */
	function changeColor(name: string, color: string): void {
		items = updateKeywordColor(items, name, color);
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
		<input
			class="h-11 w-11 shrink-0 cursor-pointer rounded-xl border border-slate-200 bg-white p-1"
			type="color"
			bind:value={selectedColor}
			aria-label="Farbe für neues Alarmstichwort"
			title="Farbe wählen"
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
		{#each items as item (item.name)}
			{@const badgeStyle = getKeywordBadgeStyle(item.color)}
			<span
				class="inline-flex items-center gap-1 rounded-full px-3 py-1 text-sm font-semibold"
				style:color={badgeStyle.color}
				style:background-color={badgeStyle.backgroundColor}
			>
				{item.name}
				<input
					class="h-5 w-5 cursor-pointer rounded-full border border-white/70 bg-transparent p-0"
					type="color"
					value={item.color}
					aria-label="Farbe für {item.name}"
					title="Farbe für {item.name}"
					onchange={(event) => changeColor(item.name, event.currentTarget.value)}
				/>
				<button
					class="rounded-full p-0.5 hover:bg-white/70"
					type="button"
					aria-label="{item.name} entfernen"
					onclick={() => removeItem(item.name)}
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
