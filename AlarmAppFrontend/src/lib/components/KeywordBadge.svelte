<script lang="ts">
	/**
	 * Farbige Plakette für ein Alarmstichwort.
	 * Die Hintergrundfarbe wird aus der gewählten Farbe aufgehellt.
	 */
	import { findKeywordColor, getKeywordBadgeStyle, getKeywordColor } from '$lib/alarm';
	import type { KeywordOption } from '$lib/types';

	let {
		keyword,
		color,
		keywords = []
	}: {
		keyword: string;
		color?: string;
		keywords?: KeywordOption[];
	} = $props();

	const badgeColor = $derived(
		color ? getKeywordColor(keyword, color) : findKeywordColor(keywords, keyword)
	);
	const badgeStyle = $derived(getKeywordBadgeStyle(badgeColor));
</script>

<span
	class="inline-flex rounded-full px-3 py-1 text-xs font-semibold"
	style:color={badgeStyle.color}
	style:background-color={badgeStyle.backgroundColor}
>
	{keyword}
</span>
