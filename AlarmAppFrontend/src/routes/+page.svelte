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
		getKeywordNames,
		validateAlarmDraft
	} from '$lib/alarm';
	import {
		createRemoteAlarm,
		deleteRemoteAlarm,
		fetchAlarms,
		fetchAppSettings,
		initApiClient,
		saveAppSettings,
		updateRemoteAlarm
	} from '$lib/api';
	import { subscribeAlarmStream } from '$lib/alarm-stream';
	import { getDefaultDraft } from '$lib/demo-data';
	import { DEFAULT_APP_SETTINGS, loadMqttConfig } from '$lib/mqtt-config';
	import { publishAlarmMessage } from '$lib/mqtt';
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

	let alarms = $state<AlarmItem[]>([]);
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
	const keywordNames = $derived(getKeywordNames(settings.keywords));
	const selectedConnection = $derived(findTopicConnection(settings.topics, draft.connection));

	/**
	 * Kopiert die Standardeinstellungen, ohne Arrays zu teilen.
	 *
	 * @param source Vorlage
	 * @returns unabhängige Kopie
	 */
	function parseCopy(source: AppSettings): AppSettings {
		return {
			keywords: source.keywords.map((keyword) => ({ ...keyword })),
			topics: source.topics.map((topic) => ({ ...topic }))
		};
	}

	$effect(() => {
		if (editingId) {
			return;
		}
		if (!connectionNames.includes(draft.connection)) {
			draft.connection = connectionNames[0] ?? '';
		}
		if (!keywordNames.includes(draft.keyword)) {
			draft.keyword = keywordNames[0] ?? '';
		}
	});

	onMount(() => {
		let closed = false;
		let closeStream = () => {};

		void (async () => {
			await initApiClient();
			try {
				settings = await fetchAppSettings();
				alignDraftWithSettings();
			} catch {
				settings = await loadMqttConfig();
				alignDraftWithSettings();
				statusType = 'error';
				status = 'Einstellungen konnten nicht vom Server geladen werden.';
			}

			try {
				alarms = await fetchAlarms();
			} catch {
				alarms = [];
			}

			const stop = subscribeAlarmStream(
				(next) => {
					alarms = next;
				},
				(message) => {
					statusType = 'error';
					status = message;
				}
			);
			if (closed) {
				stop();
				return;
			}
			closeStream = stop;
		})();

		return () => {
			closed = true;
			closeStream();
		};
	});

	/**
	 * Setzt Connection und Stichwort auf gültige Listenwerte, falls sie fehlen.
	 */
	function alignDraftWithSettings(): void {
		if (!connectionNames.includes(draft.connection)) {
			draft.connection = connectionNames[0] ?? '';
		}
		if (!keywordNames.includes(draft.keyword)) {
			draft.keyword = keywordNames[0] ?? '';
		}
	}

	/**
	 * Speichert Connections und Alarmstichworte über die REST-API.
	 *
	 * @param next bearbeitete Einstellungen
	 */
	async function persistSettings(next: AppSettings): Promise<void> {
		settings = await saveAppSettings(next);
		alignDraftWithSettings();
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
			keywords: keywordNames
		});
	}

	/**
	 * Löscht eine Alarmierung nach Bestätigung auf dem Server.
	 */
	async function removeAlarm(alarm: AlarmItem): Promise<void> {
		const confirmed = confirm(`Alarmierung „${alarm.keyword}“ wirklich löschen?`);
		if (!confirmed) {
			return;
		}
		try {
			await deleteRemoteAlarm(alarm.id);
			if (editingId === alarm.id) {
				cancelEdit();
			}
		} catch (error) {
			statusType = 'error';
			status = error instanceof Error ? error.message : 'Alarmierung konnte nicht gelöscht werden.';
		}
	}

	/**
	 * Plant eine neue Alarmierung oder speichert Änderungen auf dem Server.
	 */
	async function scheduleAlarm(): Promise<void> {
		const error = validateAlarmDraft(draft);
		if (error) {
			statusType = 'error';
			status = error;
			return;
		}

		try {
			if (editingId) {
				await updateRemoteAlarm(editingId, draft, 'planned');
				statusType = 'success';
				status = 'Alarmierung wurde aktualisiert.';
				editingId = null;
				return;
			}

			await createRemoteAlarm(draft, 'planned');
			statusType = 'success';
			status = 'Alarmierung wurde geplant.';
		} catch (saveError) {
			statusType = 'error';
			status =
				saveError instanceof Error ? saveError.message : 'Alarmierung konnte nicht gespeichert werden.';
		}
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
				await updateRemoteAlarm(editingId, draft, 'sent');
				editingId = null;
			} else {
				await createRemoteAlarm(draft, 'sent');
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
		keywords={keywordNames}
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
		keywords={settings.keywords}
		bind:filter
		bind:sortKey
		bind:sortDirection
		onEdit={editAlarm}
		onDelete={removeAlarm}
	/>
</div>

<SettingsDialog bind:open={settingsOpen} bind:settings onSave={persistSettings} />
