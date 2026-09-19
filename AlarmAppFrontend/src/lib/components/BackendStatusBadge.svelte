<script lang="ts">
	/**
	 * Kompakte Statusanzeige für die Erreichbarkeit der Alarm-API.
	 */
	import { backendStatusLabel } from '$lib/backend-status';
	import type { BackendConnectionStatus } from '$lib/types';

	let { status }: { status: BackendConnectionStatus } = $props();

	const label = $derived(backendStatusLabel(status));

	/**
	 * Liefert Rahmen- und Textfarben der Statuskapsel.
	 *
	 * @param current aktueller Zustand
	 * @returns Tailwind-Klassen
	 */
	function badgeClasses(current: BackendConnectionStatus): string {
		switch (current) {
			case 'online':
				return 'border-emerald-100 bg-emerald-50 text-emerald-700';
			case 'offline':
				return 'border-rose-100 bg-rose-50 text-rose-700';
			default:
				return 'border-amber-100 bg-amber-50 text-amber-700';
		}
	}

	/**
	 * Liefert die Punktfarbe in der Statuskapsel.
	 *
	 * @param current aktueller Zustand
	 * @returns Tailwind-Klasse
	 */
	function dotClass(current: BackendConnectionStatus): string {
		switch (current) {
			case 'online':
				return 'bg-emerald-500';
			case 'offline':
				return 'bg-rose-500';
			default:
				return 'bg-amber-400';
		}
	}
</script>

<span
	class={`inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm font-semibold ${badgeClasses(status)}`}
	role="status"
	aria-live="polite"
	title={label}
>
	<span class="relative flex h-2.5 w-2.5">
		{#if status === 'online'}
			<span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-50"
			></span>
		{/if}
		<span class={`relative inline-flex h-2.5 w-2.5 rounded-full ${dotClass(status)}`}></span>
	</span>
	<span class="hidden sm:inline">{label}</span>
	<span class="sm:hidden">{status === 'online' ? 'Online' : status === 'offline' ? 'Offline' : 'Prüfung'}</span>
</span>
