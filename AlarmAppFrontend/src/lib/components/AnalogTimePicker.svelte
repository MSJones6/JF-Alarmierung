<script lang="ts">
	/**
	 * Dialog zur analogen Uhrzeitwahl wie auf dem Handy.
	 * Zuerst Stunden, dann Minuten, zuletzt Sekunden.
	 */
	import { untrack } from 'svelte';
	import X from '@lucide/svelte/icons/x';
	import { toDateTimeLocalValue } from '$lib/alarm';
	import {
		applyClockValue,
		CLOCK_CENTER,
		CLOCK_SIZE,
		clockLabels,
		clockPointForValue,
		clockStepLabel,
		clockValueForStep,
		clockValueFromOffset,
		joinDateTimeLocal,
		nextClockStep,
		padClockUnit,
		splitDateTimeLocal,
		type ClockStep,
		type ClockTime
	} from '$lib/clock-time';

	let {
		open = $bindable(false),
		value,
		onApply
	}: {
		open: boolean;
		value: string;
		onApply: (next: string) => void;
	} = $props();

	/** Aktueller Auswahlschritt auf dem Zifferblatt. */
	let step = $state<ClockStep>('hours');

	/** Datum, das zusammen mit der Uhrzeit übernommen wird. */
	let date = $state('');

	/** Bearbeitete Uhrzeit, bis der Dialog bestätigt wird. */
	let time = $state<ClockTime>({ hours: 0, minutes: 0, seconds: 0 });

	const labels = $derived(clockLabels(step));
	const selectedValue = $derived(clockValueForStep(time, step));
	const hand = $derived(clockPointForValue(selectedValue, step));
	const stepTitle = $derived(clockStepLabel(step));

	$effect(() => {
		if (open) {
			untrack(() => {
				const parts = splitDateTimeLocal(value);
				date = parts.date || toDateTimeLocalValue().slice(0, 10);
				time = {
					hours: parts.hours,
					minutes: parts.minutes,
					seconds: parts.seconds
				};
				step = 'hours';
			});
		}
	});

	/**
	 * Schließt den Dialog ohne die Uhrzeit zu übernehmen.
	 */
	function close(): void {
		open = false;
	}

	/**
	 * Schließt den Dialog bei Escape ohne Übernahme.
	 *
	 * @param event Tastaturereignis
	 */
	function onKeydown(event: KeyboardEvent): void {
		if (open && event.key === 'Escape') {
			close();
		}
	}

	/**
	 * Übernimmt Datum und Uhrzeit ins Formular.
	 */
	function confirm(): void {
		onApply(
			joinDateTimeLocal({
				date,
				hours: time.hours,
				minutes: time.minutes,
				seconds: time.seconds
			})
		);
		open = false;
	}

	/**
	 * Wechselt den Auswahlschritt über die Digitalanzeige.
	 *
	 * @param next gewünschter Schritt
	 */
	function selectStep(next: ClockStep): void {
		step = next;
	}

	/**
	 * Setzt den Uhrwert aus einer Zeigerposition.
	 *
	 * @param event Zeigerereignis auf dem SVG
	 */
	function applyPointer(event: PointerEvent): void {
		const svg = event.currentTarget as SVGSVGElement;
		const rect = svg.getBoundingClientRect();
		if (rect.width === 0 || rect.height === 0) {
			return;
		}
		const x = ((event.clientX - rect.left) / rect.width) * CLOCK_SIZE;
		const y = ((event.clientY - rect.top) / rect.height) * CLOCK_SIZE;
		time = applyClockValue(time, step, clockValueFromOffset(x, y, step));
	}

	/**
	 * Startet das Ziehen auf dem Zifferblatt.
	 *
	 * @param event Zeigerereignis
	 */
	function onPointerDown(event: PointerEvent): void {
		(event.currentTarget as SVGSVGElement).setPointerCapture(event.pointerId);
		applyPointer(event);
	}

	/**
	 * Bewegt den Zeiger, solange die Taste gedrückt ist.
	 *
	 * @param event Zeigerereignis
	 */
	function onPointerMove(event: PointerEvent): void {
		if (!event.currentTarget || !(event.currentTarget as SVGSVGElement).hasPointerCapture(event.pointerId)) {
			return;
		}
		applyPointer(event);
	}

	/**
	 * Beendet die Auswahl und wechselt zum nächsten Schritt.
	 *
	 * @param event Zeigerereignis
	 */
	function onPointerUp(event: PointerEvent): void {
		const svg = event.currentTarget as SVGSVGElement;
		if (svg.hasPointerCapture(event.pointerId)) {
			svg.releasePointerCapture(event.pointerId);
		}
		const next = nextClockStep(step);
		if (next) {
			step = next;
		}
	}

	/**
	 * Bricht das Ziehen ab, ohne den Schritt zu wechseln.
	 *
	 * @param event Zeigerereignis
	 */
	function onPointerCancel(event: PointerEvent): void {
		const svg = event.currentTarget as SVGSVGElement;
		if (svg.hasPointerCapture(event.pointerId)) {
			svg.releasePointerCapture(event.pointerId);
		}
	}

	/**
	 * Steuert das Zifferblatt per Pfeiltasten.
	 *
	 * @param event Tastaturereignis
	 */
	function onClockKeydown(event: KeyboardEvent): void {
		const max = step === 'hours' ? 23 : 59;
		if (event.key === 'Enter') {
			const next = nextClockStep(step);
			if (next) {
				step = next;
			}
			return;
		}
		let nextValue = selectedValue;
		if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
			nextValue = (selectedValue + 1) % (max + 1);
		} else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
			nextValue = (selectedValue + max) % (max + 1);
		} else {
			return;
		}
		event.preventDefault();
		time = applyClockValue(time, step, nextValue);
	}

	/**
	 * Liefert die Hervorhebung der Digitalanzeige.
	 *
	 * @param current angezeigter Schritt
	 * @returns Tailwind-Klassen
	 */
	function digitClasses(current: ClockStep): string {
		return step === current
			? 'rounded-xl bg-blue-600 px-2 py-1 text-white'
			: 'rounded-xl px-2 py-1 text-navy hover:bg-slate-100';
	}
</script>

<svelte:window onkeydown={onKeydown} />

{#if open}
	<div class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/30 p-4">
		<div
			class="w-full max-w-sm rounded-[28px] bg-white p-6 shadow-2xl"
			role="dialog"
			aria-modal="true"
			aria-labelledby="analog-time-title"
			tabindex="-1"
		>
			<div class="mb-4 flex items-start justify-between gap-3">
				<div>
					<h2 id="analog-time-title" class="text-xl font-extrabold text-navy">Zeit einstellen</h2>
					<p class="text-sm text-slate-400">{stepTitle}</p>
				</div>
				<button
					type="button"
					class="flex h-10 w-10 items-center justify-center rounded-xl text-slate-400 hover:bg-slate-50"
					aria-label="Zeitwahl schließen"
					onclick={close}
				>
					<X size={20} />
				</button>
			</div>

			<label class="mb-4 block">
				<span class="mb-2 block text-sm font-semibold text-slate-600">Datum</span>
				<input
					class="w-full rounded-xl border-slate-200 py-3 text-slate-700 shadow-none focus:border-blue-400 focus:ring-blue-400"
					type="date"
					lang="de"
					bind:value={date}
				/>
			</label>

			<div class="mb-4 flex items-center justify-center gap-1 text-3xl font-extrabold tracking-tight">
				<button type="button" class={digitClasses('hours')} onclick={() => selectStep('hours')}>
					{padClockUnit(time.hours)}
				</button>
				<span class="text-slate-300">:</span>
				<button type="button" class={digitClasses('minutes')} onclick={() => selectStep('minutes')}>
					{padClockUnit(time.minutes)}
				</button>
				<span class="text-slate-300">:</span>
				<button type="button" class={digitClasses('seconds')} onclick={() => selectStep('seconds')}>
					{padClockUnit(time.seconds)}
				</button>
			</div>

			<svg
				class="mx-auto block touch-none select-none"
				width="280"
				height="280"
				viewBox="0 0 {CLOCK_SIZE} {CLOCK_SIZE}"
				role="slider"
				tabindex="0"
				aria-label={stepTitle}
				aria-valuemin={0}
				aria-valuemax={step === 'hours' ? 23 : 59}
				aria-valuenow={selectedValue}
				onpointerdown={onPointerDown}
				onpointermove={onPointerMove}
				onpointerup={onPointerUp}
				onpointercancel={onPointerCancel}
				onkeydown={onClockKeydown}
			>
				<circle cx={CLOCK_CENTER} cy={CLOCK_CENTER} r="136" fill="#f1f5f9" />
				<line
					x1={CLOCK_CENTER}
					y1={CLOCK_CENTER}
					x2={hand.x}
					y2={hand.y}
					stroke="#2563eb"
					stroke-width="3"
					stroke-linecap="round"
				/>
				{#each labels as label (label.value)}
					{@const selected = label.value === selectedValue}
					<circle
						cx={label.x}
						cy={label.y}
						r={selected ? 18 : 16}
						fill={selected ? '#2563eb' : 'transparent'}
					/>
					<text
						x={label.x}
						y={label.y}
						text-anchor="middle"
						dominant-baseline="middle"
						fill={selected ? '#ffffff' : '#1e3a8a'}
						font-size={step === 'hours' && label.value < 12 ? 13 : 14}
						font-weight="700"
						pointer-events="none"
					>
						{label.label}
					</text>
				{/each}
				{#if step !== 'hours' && selectedValue % 5 !== 0}
					<circle cx={hand.x} cy={hand.y} r="16" fill="#2563eb" />
					<text
						x={hand.x}
						y={hand.y}
						text-anchor="middle"
						dominant-baseline="middle"
						fill="#ffffff"
						font-size="13"
						font-weight="700"
						pointer-events="none"
					>
						{padClockUnit(selectedValue)}
					</text>
				{/if}
				<circle cx={CLOCK_CENTER} cy={CLOCK_CENTER} r="6" fill="#2563eb" />
			</svg>

			<div class="mt-5 grid grid-cols-2 gap-3">
				<button
					type="button"
					class="rounded-xl border border-slate-200 py-3 font-semibold text-slate-500 hover:bg-slate-50"
					onclick={close}
				>
					Abbrechen
				</button>
				<button
					type="button"
					class="rounded-xl bg-brand py-3 font-semibold text-white hover:bg-brand-hover"
					onclick={confirm}
				>
					Übernehmen
				</button>
			</div>
		</div>
	</div>
{/if}
