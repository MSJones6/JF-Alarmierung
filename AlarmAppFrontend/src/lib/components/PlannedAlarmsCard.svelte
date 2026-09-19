<script lang="ts">
	/**
	 * Tabelle der geplanten und bereits ausgelösten Alarmierungen.
	 */
	import Calendar from '@lucide/svelte/icons/calendar';
	import Check from '@lucide/svelte/icons/check';
	import ChevronDown from '@lucide/svelte/icons/chevron-down';
	import ChevronsUpDown from '@lucide/svelte/icons/chevrons-up-down';
	import Clock from '@lucide/svelte/icons/clock';
	import List from '@lucide/svelte/icons/list';
	import Pencil from '@lucide/svelte/icons/pencil';
	import Trash2 from '@lucide/svelte/icons/trash-2';
	import { filterAlarms, formatGermanDateTime, getFilterCountLabel, sortAlarms } from '$lib/alarm';
	import KeywordBadge from '$lib/components/KeywordBadge.svelte';
	import type { AlarmFilter, AlarmItem, AlarmSortKey, KeywordOption, SortDirection } from '$lib/types';

	let {
		alarms,
		keywords = [],
		filter = $bindable(),
		sortKey = $bindable(),
		sortDirection = $bindable(),
		onEdit,
		onDelete
	}: {
		alarms: AlarmItem[];
		keywords?: KeywordOption[];
		filter: AlarmFilter;
		sortKey: AlarmSortKey;
		sortDirection: SortDirection;
		onEdit: (alarm: AlarmItem) => void;
		onDelete: (alarm: AlarmItem) => void;
	} = $props();

	const visibleAlarms = $derived(sortAlarms(filterAlarms(alarms, filter), sortKey, sortDirection));

	/**
	 * Setzt den Tab-Filter der Tabelle.
	 */
	function setFilter(next: AlarmFilter): void {
		filter = next;
	}

	/**
	 * Wechselt die Sortierung der angeklickten Spalte.
	 */
	function setSort(nextKey: AlarmSortKey): void {
		if (sortKey === nextKey) {
			sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
			return;
		}
		sortKey = nextKey;
		sortDirection = 'asc';
	}
</script>

<section class="rounded-[28px] bg-white px-6 py-6 shadow-sm">
	<div class="mb-6 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
		<div class="flex items-start gap-3">
			<Clock class="mt-1 text-blue-500" size={26} />
			<div>
				<h2 class="text-2xl font-extrabold text-navy">Geplante Alarmierungen</h2>
				<p class="text-sm text-slate-400">
					Hier sehen Sie alle geplanten Alarmierungen. Sie können diese bearbeiten oder löschen.
				</p>
			</div>
		</div>

		<div class="flex flex-wrap items-center gap-2">
			<button
				class={`inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition ${
					filter === 'planned'
						? 'bg-blue-600 text-white shadow-sm'
						: 'text-slate-500 hover:bg-slate-100'
				}`}
				type="button"
				onclick={() => setFilter('planned')}
			>
				<Calendar size={16} />
				Geplant
			</button>
			<button
				class={`inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition ${
					filter === 'sent'
						? 'bg-blue-600 text-white shadow-sm'
						: 'text-slate-500 hover:bg-slate-100'
				}`}
				type="button"
				onclick={() => setFilter('sent')}
			>
				<Check size={16} />
				Bereits alarmiert
			</button>
			<button
				class={`inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition ${
					filter === 'all'
						? 'bg-blue-600 text-white shadow-sm'
						: 'text-slate-500 hover:bg-slate-100'
				}`}
				type="button"
				onclick={() => setFilter('all')}
			>
				<List size={16} />
				Alle
			</button>
		</div>
	</div>

	<div class="overflow-x-auto">
		<table class="w-full min-w-[720px] border-separate border-spacing-y-1 text-left">
			<thead>
				<tr class="text-xs font-semibold tracking-wide text-slate-400 uppercase">
					<th class="px-3 pb-3">
						<button
							class="inline-flex items-center gap-1"
							type="button"
							onclick={() => setSort('scheduledAt')}
						>
							Zeit
							{#if sortKey === 'scheduledAt'}
								<ChevronDown class={sortDirection === 'desc' ? 'rotate-180' : ''} size={14} />
							{:else}
								<ChevronsUpDown size={14} />
							{/if}
						</button>
					</th>
					<th class="px-3 pb-3">
						<button
							class="inline-flex items-center gap-1"
							type="button"
							onclick={() => setSort('keyword')}
						>
							Alarmstichwort
							<ChevronsUpDown size={14} />
						</button>
					</th>
					<th class="px-3 pb-3">
						<button
							class="inline-flex items-center gap-1"
							type="button"
							onclick={() => setSort('location')}
						>
							Ort
							<ChevronsUpDown size={14} />
						</button>
					</th>
					<th class="px-3 pb-3">
						<button
							class="inline-flex items-center gap-1"
							type="button"
							onclick={() => setSort('info')}
						>
							Weitere Infos
							<ChevronsUpDown size={14} />
						</button>
					</th>
					<th class="px-3 pb-3 text-right">Aktionen</th>
				</tr>
			</thead>
			<tbody>
				{#each visibleAlarms as alarm (alarm.id)}
					<tr class="text-sm text-slate-600">
						<td class="rounded-l-2xl bg-slate-50/80 px-3 py-2.5">
							<div class="flex items-center gap-3">
								<div
									class="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-slate-400 shadow-sm"
								>
									<Calendar size={16} />
								</div>
								<span class="font-medium text-slate-600"
									>{formatGermanDateTime(alarm.scheduledAt)}</span
								>
							</div>
						</td>
						<td class="bg-slate-50/80 px-3 py-2.5">
							<KeywordBadge keyword={alarm.keyword} {keywords} />
						</td>
						<td class="bg-slate-50/80 px-3 py-2.5 font-medium text-slate-700">{alarm.location}</td>
						<td class="bg-slate-50/80 px-3 py-2.5 text-slate-500">{alarm.info}</td>
						<td class="rounded-r-2xl bg-slate-50/80 px-3 py-2.5">
							<div class="flex justify-end gap-2">
								<button
									class="flex h-9 w-9 items-center justify-center rounded-xl bg-sky-50 text-sky-500 transition hover:bg-sky-100"
									type="button"
									aria-label="Alarmierung bearbeiten"
									onclick={() => onEdit(alarm)}
								>
									<Pencil size={16} />
								</button>
								<button
									class="flex h-9 w-9 items-center justify-center rounded-xl bg-rose-50 text-rose-500 transition hover:bg-rose-100"
									type="button"
									aria-label="Alarmierung löschen"
									onclick={() => onDelete(alarm)}
								>
									<Trash2 size={16} />
								</button>
							</div>
						</td>
					</tr>
				{:else}
					<tr>
						<td class="px-3 py-8 text-center text-slate-400" colspan="5">
							Keine Alarmierungen in dieser Ansicht.
						</td>
					</tr>
				{/each}
			</tbody>
		</table>
	</div>

	<div class="mt-4 flex items-center gap-2 text-sm text-slate-400">
		<List size={16} />
		{getFilterCountLabel(visibleAlarms.length, filter)}
	</div>
</section>
