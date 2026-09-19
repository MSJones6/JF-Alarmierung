<script lang="ts">
	/**
	 * Startseite der Alarmierungsoberfläche.
	 */
	import AppHeader from '$lib/components/AppHeader.svelte';
	import NewAlarmCard from '$lib/components/NewAlarmCard.svelte';
	import PlannedAlarmsCard from '$lib/components/PlannedAlarmsCard.svelte';
	import SettingsDialog from '$lib/components/SettingsDialog.svelte';
	import {
		buildMqttPayload,
		createAlarm,
		deleteAlarm,
		updateAlarm,
		validateAlarmDraft
	} from '$lib/alarm';
	import { getDefaultDraft } from '$lib/demo-data';
	import { loadMqttConfig } from '$lib/mqtt-config';
	import { DEFAULT_MQTT_SETTINGS, publishAlarmMessage } from '$lib/mqtt';
	import { loadAlarms, saveAlarms } from '$lib/storage';
	import type {
		AlarmDraft,
		AlarmFilter,
		AlarmItem,
		AlarmSortKey,
		MqttSettings,
		SortDirection,
		StatusType
	} from '$lib/types';
	import { onMount } from 'svelte';

	let alarms = $state<AlarmItem[]>(loadAlarms());
	let settings = $state<MqttSettings>({ ...DEFAULT_MQTT_SETTINGS });
	let draft = $state<AlarmDraft>(getDefaultDraft());
	let filter = $state<AlarmFilter>('planned');
	let sortKey = $state<AlarmSortKey>('scheduledAt');
	let sortDirection = $state<SortDirection>('asc');
	let editingId = $state<string | null>(null);
	let settingsOpen = $state(false);
	let status = $state('');
	let statusType = $state<StatusType>('idle');
	let isSending = $state(false);

	$effect(() => {
		saveAlarms(alarms);
	});

	onMount(() => {
		void loadMqttConfig().then((loaded) => {
			settings = loaded;
		});
	});

	/**
	 * Übernimmt einen Listeneintrag ins Formular zur Bearbeitung.
	 */
	function editAlarm(alarm: AlarmItem): void {
		editingId = alarm.id;
		draft = {
			scheduledAt: alarm.scheduledAt,
			topic: alarm.topic,
			keyword: alarm.keyword,
			info: alarm.info
		};
		status = '';
		statusType = 'idle';
	}

	/**
	 * Bricht die Bearbeitung ab und stellt die Formular-Standardwerte wieder her.
	 */
	function cancelEdit(): void {
		editingId = null;
		draft = getDefaultDraft();
	}

	/**
	 * Löscht eine Alarmierung nach Bestätigung.
	 */
	function removeAlarm(alarm: AlarmItem): void {
		const confirmed = confirm(`Alarmierung „${alarm.keyword}“ wirklich löschen?`);
		if (!confirmed) {
			return;
		}
		alarms = deleteAlarm(alarms, alarm.id);
		if (editingId === alarm.id) {
			cancelEdit();
		}
	}

	/**
	 * Plant eine neue Alarmierung oder speichert Änderungen.
	 */
	function scheduleAlarm(): void {
		const error = validateAlarmDraft(draft);
		if (error) {
			statusType = 'error';
			status = error;
			return;
		}

		if (editingId) {
			alarms = updateAlarm(alarms, editingId, draft);
			statusType = 'success';
			status = 'Alarmierung wurde aktualisiert.';
			editingId = null;
			return;
		}

		alarms = [...alarms, createAlarm(draft, 'planned')];
		statusType = 'success';
		status = 'Alarmierung wurde geplant.';
	}

	/**
	 * Sendet die Alarmierung sofort per MQTT und merkt sie als bereits alarmiert.
	 */
	async function sendAlarm(): Promise<void> {
		const error = validateAlarmDraft(draft);
		if (error) {
			statusType = 'error';
			status = error;
			return;
		}

		isSending = true;
		statusType = 'sending';
		status = 'Verbindung zum Broker wird hergestellt...';

		try {
			await publishAlarmMessage(settings, buildMqttPayload(draft));

			if (editingId) {
				alarms = updateAlarm(alarms, editingId, draft).map((item) =>
					item.id === editingId ? { ...item, status: 'sent' as const } : item
				);
				editingId = null;
			} else {
				alarms = [...alarms, createAlarm(draft, 'sent')];
			}

			statusType = 'success';
			status = 'Nachricht erfolgreich gesendet.';
		} catch (sendError) {
			statusType = 'error';
			status =
				sendError instanceof Error
					? `MQTT Fehler: ${sendError.message}`
					: 'MQTT Fehler beim Senden.';
		} finally {
			isSending = false;
		}
	}
</script>

<div class="mx-auto flex max-w-6xl flex-col gap-5">
	<AppHeader username={settings.user} onOpenSettings={() => (settingsOpen = true)} />
	<NewAlarmCard
		bind:draft
		isEditing={editingId !== null}
		{isSending}
		{status}
		{statusType}
		onDirectAlarm={sendAlarm}
		onSchedule={scheduleAlarm}
		onCancelEdit={cancelEdit}
	/>
	<PlannedAlarmsCard
		{alarms}
		bind:filter
		bind:sortKey
		bind:sortDirection
		onEdit={editAlarm}
		onDelete={removeAlarm}
	/>
</div>

<SettingsDialog bind:open={settingsOpen} bind:settings />
