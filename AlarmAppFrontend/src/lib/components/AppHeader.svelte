<script lang="ts">
	/**
	 * Kopfzeile der Alarmierungsoberfläche mit Serverstatus, Einstellungen und Benutzerinfo.
	 */
	import Bell from '@lucide/svelte/icons/bell';
	import Settings from '@lucide/svelte/icons/settings';
	import User from '@lucide/svelte/icons/user';
	import BackendStatusBadge from '$lib/components/BackendStatusBadge.svelte';
	import type { BackendConnectionStatus } from '$lib/types';

	let {
		username,
		backendStatus,
		onOpenSettings
	}: {
		username: string;
		backendStatus: BackendConnectionStatus;
		onOpenSettings: () => void;
	} = $props();

	let userOpen = $state(false);
</script>

<header class="rounded-[28px] bg-white px-6 py-4 shadow-sm">
	<div class="flex flex-wrap items-center justify-between gap-4">
		<div class="flex items-center gap-3">
			<div
				class="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-500 text-white shadow-sm"
			>
				<Bell size={22} />
			</div>
			<div>
				<h1 class="text-2xl font-extrabold tracking-tight text-navy">Alarmierung</h1>
				<p class="text-sm text-slate-400">Schnell. Einfach. Zuverlässig.</p>
			</div>
		</div>

		<div class="relative flex flex-wrap items-center justify-end gap-3">
			<BackendStatusBadge status={backendStatus} />
			<button
				type="button"
				class="flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 transition hover:bg-slate-50"
				aria-label="Einstellungen öffnen"
				onclick={onOpenSettings}
			>
				<Settings size={20} />
			</button>
			<button
				type="button"
				class="flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-500 transition hover:bg-slate-50"
				aria-label="Benutzerinformationen anzeigen"
				onclick={() => (userOpen = !userOpen)}
			>
				<User size={20} />
			</button>

			{#if userOpen}
				<div
					class="absolute top-14 right-0 z-20 w-64 rounded-2xl border border-slate-100 bg-white p-4 shadow-xl"
				>
					<p class="text-xs tracking-wide text-slate-400 uppercase">Angemeldet als</p>
					<p class="mt-1 font-semibold text-navy">{username || 'unbekannt'}</p>
					<p class="mt-2 text-sm text-slate-500">
						Die Broker-Zugangsdaten ändern Sie über die Einstellungen.
					</p>
				</div>
			{/if}
		</div>
	</div>
</header>
