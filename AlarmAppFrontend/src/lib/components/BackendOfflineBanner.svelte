<script lang="ts">
	/**
	 * Hinweis, wenn die Alarm-API nicht erreichbar ist.
	 * Die übrige Seite bleibt sichtbar, Aktionen sind pausiert.
	 */
	import ServerOff from '@lucide/svelte/icons/server-off';

	let { visible }: { visible: boolean } = $props();
</script>

{#if visible}
	<section
		class="overflow-hidden rounded-[28px] border border-rose-100 bg-white px-6 py-5 shadow-sm"
		role="status"
		aria-live="polite"
	>
		<div class="flex items-start gap-4">
			<div
				class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-rose-50 text-rose-500"
			>
				<ServerOff size={22} />
			</div>
			<div>
				<h2 class="text-lg font-extrabold text-navy">Backend nicht erreichbar</h2>
				<p class="mt-1 text-sm leading-6 text-slate-500">
					Die Alarm-API antwortet derzeit nicht. Die Oberfläche bleibt sichtbar, Senden und Speichern
					sind pausiert. Die Verbindung wird automatisch erneut geprüft.
				</p>
			</div>
		</div>
	</section>
{/if}
