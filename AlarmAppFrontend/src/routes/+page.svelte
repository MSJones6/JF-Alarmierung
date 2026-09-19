<script lang="ts">
	/**
	 * Startseite der Alarmierungsoberfläche.
	 */
	import AppHeader from '$lib/components/AppHeader.svelte';
	import BackendOfflineBanner from '$lib/components/BackendOfflineBanner.svelte';
	import NewAlarmCard from '$lib/components/NewAlarmCard.svelte';
	import PlannedAlarmsCard from '$lib/components/PlannedAlarmsCard.svelte';
	import SettingsDialog from '$lib/components/SettingsDialog.svelte';
	import {
		getKeywordNames,
		validateAlarmDraft
	} from '$lib/alarm';
	import {
		createRemoteAlarm,
		deleteRemoteAlarm,
		fetchAlarms,
		fetchApiHealth,
		fetchAppSettings,
		initApiClient,
		saveAppSettings,
		updateRemoteAlarm
	} from '$lib/api';
	import { subscribeAlarmStream } from '$lib/alarm-stream';
	import { startBackendHealthPolling } from '$lib/backend-status';
	import { getDefaultDraft } from '$lib/demo-data';
	import { findTopicConnection, getTopicNames } from '$lib/topic-connection';
	import type {
		AlarmDraft,
		AlarmFilter,
		AlarmItem,
		AlarmSortKey,
		AppSettings,
		BackendConnectionStatus,
		SortDirection,
		StatusType
	} from '$lib/types';
	import { onMount } from 'svelte';

	let alarms = $state<AlarmItem[]>([]);
	let settings = $state<AppSettings>({ keywords: [], topics: [] });
	let draft = $state<AlarmDraft>(getDefaultDraft());
	let filter = $state<AlarmFilter>('planned');
	let sortKey = $state<AlarmSortKey>('scheduledAt');
	let sortDirection = $state<SortDirection>('asc');
	let editingId = $state<string | null>(null);
	let settingsOpen = $state(false);
	let status = $state('');
	let statusType = $state<StatusType>('idle');
	let isSending = $state(false);
	let backendStatus = $state<BackendConnectionStatus>('checking');

	const connectionNames = $derived(getTopicNames(settings.topics));
	const keywordNames = $derived(getKeywordNames(settings.keywords));
	const selectedConnection = $derived(findTopicConnection(settings.topics, draft.connection));
	const backendOnline = $derived(backendStatus === 'online');

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

	// Prüft die API regelmäßig und lädt Daten nur bei erreichbarem Backend.
	onMount(() => {
		let closed = false;
		let closeStream = () => {};
		let stopPolling = () => {};

		/**
		 * Abonniert den Alarm-Stream neu.
		 */
		function connectStream(): void {
			closeStream();
			closeStream = subscribeAlarmStream((next) => {
				alarms = next;
			});
		}

		/**
		 * Lädt Einstellungen und Alarme vom Server und öffnet den Stream.
		 */
		async function refreshFromApi(): Promise<void> {
			try {
				const nextSettings = await fetchAppSettings();
				if (closed) {
					return;
				}
				settings = nextSettings;
				alignDraftWithSettings();
			} catch {
				if (!closed) {
					backendStatus = 'offline';
				}
			}

			try {
				const nextAlarms = await fetchAlarms();
				if (closed) {
					return;
				}
				alarms = nextAlarms;
			} catch {
				if (!closed) {
					alarms = [];
					backendStatus = 'offline';
				}
			}

			if (!closed && backendStatus === 'online') {
				connectStream();
			}
		}

		/**
		 * Übernimmt einen neuen API-Status und lädt Daten beim Wechsel auf online.
		 *
		 * @param next geprüfter Zustand
		 */
		function applyBackendStatus(next: BackendConnectionStatus): void {
			const becameOnline = next === 'online' && backendStatus !== 'online';
			backendStatus = next;
			if (becameOnline) {
				void refreshFromApi();
			}
		}

		void (async () => {
			await initApiClient();
			if (closed) {
				return;
			}
			stopPolling = startBackendHealthPolling({
				check: fetchApiHealth,
				onStatus: applyBackendStatus
			});
		})();

		return () => {
			closed = true;
			stopPolling();
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
		if (!backendOnline) {
			return;
		}
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
		if (!backendOnline) {
			return;
		}
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
		if (!backendOnline) {
			return;
		}
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
	 * Lässt den Server die Alarmierung sofort per MQTT auslösen und als gesendet speichern.
	 * Ein zweiter Klick während des laufenden Versands wird ignoriert.
	 */
	async function sendAlarm(): Promise<void> {
		if (isSending || !backendOnline) {
			return;
		}

		const error = validateAlarmDraft(draft);
		if (error) {
			statusType = 'error';
			status = error;
			return;
		}

		isSending = true;
		statusType = 'sending';
		status = 'Alarmierung wird über den Server gesendet...';

		try {
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
	<AppHeader
		username={selectedConnection?.user ?? ''}
		{backendStatus}
		onOpenSettings={() => (settingsOpen = true)}
	/>
	<BackendOfflineBanner visible={backendStatus === 'offline'} />
	<NewAlarmCard
		bind:draft
		connections={connectionNames}
		keywords={keywordNames}
		isEditing={editingId !== null}
		{isSending}
		{backendOnline}
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
		{backendOnline}
		onEdit={editAlarm}
		onDelete={removeAlarm}
	/>
</div>

<SettingsDialog bind:open={settingsOpen} bind:settings onSave={persistSettings} />
