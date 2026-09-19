<script lang="ts">
	/**
	 * Farbige Plakette für ein Alarmstichwort.
	 */
	import { getKeywordBadgeClass } from '$lib/alarm';

	let { keyword }: { keyword: string } = $props();
</script>

<span
	class={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${getKeywordBadgeClass(keyword)}`}
>
	{keyword}
</span>
