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
	import { DEFAULT_APP_SETTINGS, loadMqttConfig } from '$lib/mqtt-config';
	import { publishAlarmMessage } from '$lib/mqtt';
	import { loadAlarms, saveAlarms } from '$lib/storage';
	import { findTopicConnection, getTopicNames } from '$lib/topic-connection';
	import type {
		AlarmDraft,
		AlarmFilter,
		AlarmItem,
		AlarmSortKey,
		AppSettings,
		SortDirection,
		StatusType
	} from '$lib/types';
	import { onMount } from 'svelte';

	let alarms = $state<AlarmItem[]>(loadAlarms());
	let settings = $state<AppSettings>(parseCopy(DEFAULT_APP_SETTINGS));
	let draft = $state<AlarmDraft>(getDefaultDraft());
	let filter = $state<AlarmFilter>('planned');
	let sortKey = $state<AlarmSortKey>('scheduledAt');
	let sortDirection = $state<SortDirection>('asc');
	let editingId = $state<string | null>(null);
	let settingsOpen = $state(false);
	let status = $state('');
	let statusType = $state<StatusType>('idle');
	let isSending = $state(false);

	const connectionNames = $derived(getTopicNames(settings.topics));
	const selectedConnection = $derived(findTopicConnection(settings.topics, draft.connection));

	/**
	 * Kopiert die Standardeinstellungen, ohne Arrays zu teilen.
	 *
	 * @param source Vorlage
	 * @returns unabhängige Kopie
	 */
	function parseCopy(source: AppSettings): AppSettings {
		return {
			keywords: [...source.keywords],
			topics: source.topics.map((topic) => ({ ...topic }))
		};
	}

	$effect(() => {
		saveAlarms(alarms);
	});

	$effect(() => {
		if (editingId) {
			return;
		}
		if (!connectionNames.includes(draft.connection)) {
			draft.connection = connectionNames[0] ?? '';
		}
		if (!settings.keywords.includes(draft.keyword)) {
			draft.keyword = settings.keywords[0] ?? '';
		}
	});

	onMount(() => {
		void loadMqttConfig().then((loaded) => {
			settings = loaded;
			alignDraftWithSettings();
		});
	});

	/**
	 * Setzt Connection und Stichwort auf gültige Listenwerte, falls sie fehlen.
	 */
	function alignDraftWithSettings(): void {
		if (!connectionNames.includes(draft.connection)) {
			draft.connection = connectionNames[0] ?? '';
		}
		if (!settings.keywords.includes(draft.keyword)) {
			draft.keyword = settings.keywords[0] ?? '';
		}
	}

	/**
	 * Übernimmt einen Listeneintrag ins Formular zur Bearbeitung.
	 */
	function editAlarm(alarm: AlarmItem): void {
		editingId = alarm.id;
		draft = {
			scheduledAt: alarm.scheduledAt,
			connection: alarm.connection,
			location: alarm.location,
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
		draft = getDefaultDraft({
			connections: connectionNames,
			keywords: settings.keywords
		});
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
	 * Ein zweiter Klick während des laufenden Versands wird ignoriert.
	 */
	async function sendAlarm(): Promise<void> {
		if (isSending) {
			return;
		}

		const error = validateAlarmDraft(draft);
		if (error) {
			statusType = 'error';
			status = error;
			return;
		}

		const connection = findTopicConnection(settings.topics, draft.connection);
		if (!connection) {
			statusType = 'error';
			status = 'Bitte wählen Sie eine Connection mit hinterlegter Verbindung.';
			return;
		}

		isSending = true;
		statusType = 'sending';
		status = `Verbindung zu ${connection.brokerHost} wird hergestellt...`;

		try {
			await publishAlarmMessage(connection, buildMqttPayload(draft));

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
	<AppHeader username={selectedConnection?.user ?? ''} onOpenSettings={() => (settingsOpen = true)} />
	<NewAlarmCard
		bind:draft
		connections={connectionNames}
		keywords={settings.keywords}
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
