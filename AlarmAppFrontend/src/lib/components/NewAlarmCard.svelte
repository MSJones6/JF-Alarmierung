<script lang="ts">
	/**
	 * Formular zum direkten Auslösen oder Planen einer Alarmierung.
	 */
	import Calendar from '@lucide/svelte/icons/calendar';
	import FileText from '@lucide/svelte/icons/file-text';
	import Plus from '@lucide/svelte/icons/plus';
	import Tag from '@lucide/svelte/icons/tag';
	import Zap from '@lucide/svelte/icons/zap';
	import { formatGermanDateTime, KEYWORDS, TOPICS } from '$lib/alarm';
	import type { AlarmDraft, StatusType } from '$lib/types';

	let {
		draft = $bindable(),
		isEditing,
		isSending,
		status,
		statusType,
		onDirectAlarm,
		onSchedule,
		onCancelEdit
	}: {
		draft: AlarmDraft;
		isEditing: boolean;
		isSending: boolean;
		status: string;
		statusType: StatusType;
		onDirectAlarm: () => void;
		onSchedule: () => void;
		onCancelEdit: () => void;
	} = $props();

	/**
	 * Liefert die Farbklassen der Statusmeldung.
	 */
	function statusClasses(type: StatusType): string {
		switch (type) {
			case 'success':
				return 'border-emerald-200 bg-emerald-50 text-emerald-700';
			case 'error':
				return 'border-rose-200 bg-rose-50 text-rose-700';
			case 'sending':
				return 'border-amber-200 bg-amber-50 text-amber-700';
			default:
				return '';
		}
	}
</script>

<section class="rounded-[28px] bg-white px-6 py-6 shadow-sm">
	<div class="mb-6 flex items-start gap-3">
		<div
			class="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-blue-500 text-white"
		>
			<Plus size={18} />
		</div>
		<div>
			<h2 class="text-2xl font-extrabold text-navy">
				{isEditing ? 'Alarmierung bearbeiten' : 'Neue Alarmierung'}
			</h2>
			<p class="text-sm text-slate-400">
				Erstellen Sie eine sofortige Alarmierung oder planen Sie eine für einen späteren Zeitpunkt.
			</p>
		</div>
	</div>

	<div class="space-y-5">
		<label class="block">
			<span class="mb-2 block text-sm font-semibold text-slate-600">Zeit</span>
			<div class="datetime-wrap">
				<Calendar
					class="pointer-events-none absolute top-1/2 left-4 z-10 -translate-y-1/2 text-slate-400"
					size={18}
				/>
				<span
					class="pointer-events-none absolute top-1/2 left-12 z-10 -translate-y-1/2 text-slate-700"
				>
					{formatGermanDateTime(draft.scheduledAt)}
				</span>
				<input
					class="datetime-input w-full rounded-xl border-slate-200 py-3 pr-12 pl-12 text-transparent caret-transparent shadow-none focus:border-blue-400 focus:ring-blue-400"
					type="datetime-local"
					step="1"
					lang="de"
					bind:value={draft.scheduledAt}
				/>
				<Calendar
					class="pointer-events-none absolute top-1/2 right-4 -translate-y-1/2 text-slate-400"
					size={18}
				/>
			</div>
		</label>

		<div class="grid gap-5 md:grid-cols-2">
			<label class="block">
				<span class="mb-2 block text-sm font-semibold text-slate-600">Topic</span>
				<div class="relative">
					<Tag
						class="pointer-events-none absolute top-1/2 left-4 z-10 -translate-y-1/2 text-slate-400"
						size={18}
					/>
					<select
						class="w-full appearance-none rounded-xl border-slate-200 py-3 pr-10 pl-12 text-slate-700 shadow-none focus:border-blue-400 focus:ring-blue-400"
						bind:value={draft.topic}
					>
						{#each TOPICS as topic (topic)}
							<option value={topic}>{topic}</option>
						{/each}
					</select>
				</div>
			</label>

			<label class="block">
				<span class="mb-2 block text-sm font-semibold text-slate-600">Alarmstichwort</span>
				<div class="relative">
					<Tag
						class="pointer-events-none absolute top-1/2 left-4 z-10 -translate-y-1/2 text-slate-400"
						size={18}
					/>
					<select
						class="w-full appearance-none rounded-xl border-slate-200 py-3 pr-10 pl-12 text-slate-700 shadow-none focus:border-blue-400 focus:ring-blue-400"
						bind:value={draft.keyword}
					>
						{#each KEYWORDS as keyword (keyword)}
							<option value={keyword}>{keyword}</option>
						{/each}
					</select>
				</div>
			</label>
		</div>

		<label class="block">
			<span class="mb-2 block text-sm font-semibold text-slate-600">Weitere Infos</span>
			<div class="relative">
				<FileText class="pointer-events-none absolute top-4 left-4 text-slate-400" size={18} />
				<textarea
					class="min-h-20 w-full resize-y rounded-xl border-slate-200 py-3 pr-4 pl-12 text-slate-700 shadow-none focus:border-blue-400 focus:ring-blue-400"
					bind:value={draft.info}></textarea>
			</div>
		</label>
	</div>

	<div class="mt-6 grid gap-4 md:grid-cols-2">
		<button
			class="inline-flex items-center justify-center gap-2 rounded-xl bg-alarm py-3.5 font-semibold text-white shadow-sm transition hover:bg-alarm-hover disabled:cursor-not-allowed disabled:opacity-70"
			type="button"
			disabled={isSending}
			onclick={() => {
				if (!isSending) {
					onDirectAlarm();
				}
			}}
		>
			<Zap size={18} />
			Direkt alarmieren
		</button>
		<button
			class="inline-flex items-center justify-center gap-2 rounded-xl bg-brand py-3.5 font-semibold text-white shadow-sm transition hover:bg-brand-hover"
			type="button"
			onclick={onSchedule}
		>
			<Calendar size={18} />
			{isEditing ? 'Änderungen speichern' : 'Alarmierung planen'}
		</button>
	</div>

	{#if isEditing}
		<button
			class="mt-3 text-sm font-medium text-slate-500 hover:text-navy"
			type="button"
			onclick={onCancelEdit}
		>
			Bearbeitung abbrechen
		</button>
	{/if}

	{#if status}
		<div
			class={`mt-4 rounded-xl border px-4 py-3 text-center text-sm font-medium ${statusClasses(statusType)}`}
		>
			{status}
		</div>
	{/if}
</section>
