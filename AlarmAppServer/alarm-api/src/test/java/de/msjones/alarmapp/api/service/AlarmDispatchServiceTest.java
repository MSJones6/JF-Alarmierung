package de.msjones.alarmapp.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.msjones.alarmapp.api.announcement.AlarmAnnouncementService;
import de.msjones.alarmapp.api.domain.AlarmEntity;
import de.msjones.alarmapp.api.domain.ConnectionEntity;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.mqtt.MqttAlarmPublisher;
import de.msjones.alarmapp.api.repository.AlarmRepository;
import de.msjones.alarmapp.api.repository.ConnectionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prüft das Auslösen fälliger geplanter Alarmierungen.
 */
@ExtendWith(MockitoExtension.class)
class AlarmDispatchServiceTest {

	@Mock
	private AlarmRepository alarmRepository;

	@Mock
	private ConnectionRepository connectionRepository;

	@Mock
	private MqttAlarmPublisher mqttAlarmPublisher;

	@Mock
	private AlarmStreamService alarmStreamService;

	@Mock
	private AlarmAnnouncementService alarmAnnouncementService;

	private AlarmDispatchService alarmDispatchService;

	/**
	 * Erzeugt den Dienst mit einer festen Uhrzeit.
	 */
	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-09-19T21:40:00Z"), ZoneOffset.UTC);
		alarmDispatchService = new AlarmDispatchService(
				alarmRepository,
				connectionRepository,
				mqttAlarmPublisher,
				alarmStreamService,
				alarmAnnouncementService,
				clock
		);
	}

	@Test
	void findDueReturnsOnlyReachedPlannedAlarms() {
		AlarmEntity due = planned(LocalDateTime.of(2026, 9, 19, 21, 40, 0));
		LocalDateTime now = LocalDateTime.of(2026, 9, 19, 21, 40, 0);
		when(alarmRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("planned", now))
				.thenReturn(List.of(due));

		assertThat(alarmDispatchService.findDue()).containsExactly(due);
	}

	@Test
	void findNextPlannedAtReturnsEarliestPlannedTimestamp() {
		AlarmEntity next = planned(LocalDateTime.of(2026, 9, 19, 22, 0, 0));
		when(alarmRepository.findFirstByStatusOrderByScheduledAtAsc("planned")).thenReturn(Optional.of(next));

		assertThat(alarmDispatchService.findNextPlannedAt())
				.contains(LocalDateTime.of(2026, 9, 19, 22, 0, 0));
	}

	@Test
	void sendDueAlarmPublishesPayloadAndMarksSent() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
		AlarmEntity alarm = planned(LocalDateTime.of(2026, 9, 19, 21, 39, 0));
		alarm.setId(id);
		alarm.setConnectionName("Standard");
		alarm.setKeyword("Feueralarm");
		alarm.setLocation("Gebäude 3");
		alarm.setInfo("Rauch");
		ConnectionEntity connection = new ConnectionEntity();
		connection.setName("Standard");
		when(alarmRepository.findById(id)).thenReturn(Optional.of(alarm));
		when(connectionRepository.findByName("Standard")).thenReturn(Optional.of(connection));
		when(alarmRepository.save(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		alarmDispatchService.sendDueAlarm(id);

		verify(mqttAlarmPublisher).publish(connection, "Feueralarm###Gebäude 3###Rauch");
		verify(alarmAnnouncementService).announceAsync(alarm);
		ArgumentCaptor<AlarmEntity> captor = ArgumentCaptor.forClass(AlarmEntity.class);
		verify(alarmRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo("sent");
		verify(alarmStreamService).send(eq("updated"), any(AlarmResponse.class));
	}

	@Test
	void sendDueAlarmSkipsAlarmsThatAreNotYetDue() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000011");
		AlarmEntity alarm = planned(LocalDateTime.of(2026, 9, 19, 21, 41, 0));
		alarm.setId(id);
		when(alarmRepository.findById(id)).thenReturn(Optional.of(alarm));

		alarmDispatchService.sendDueAlarm(id);

		verify(mqttAlarmPublisher, never()).publish(any(), any());
		verify(alarmAnnouncementService, never()).announceAsync(any());
		verify(alarmRepository, never()).save(any());
	}

	@Test
	void sendDueAlarmFailsWhenConnectionIsMissing() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000012");
		AlarmEntity alarm = planned(LocalDateTime.of(2026, 9, 19, 21, 39, 0));
		alarm.setId(id);
		alarm.setConnectionName("Unbekannt");
		when(alarmRepository.findById(id)).thenReturn(Optional.of(alarm));
		when(connectionRepository.findByName("Unbekannt")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> alarmDispatchService.sendDueAlarm(id))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Unbekannt");
		verify(mqttAlarmPublisher, never()).publish(any(), any());
		verify(alarmAnnouncementService, never()).announceAsync(any());
	}

	@Test
	void sendDueAlarmKeepsPlannedStatusWhenMqttFails() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000013");
		AlarmEntity alarm = planned(LocalDateTime.of(2026, 9, 19, 21, 39, 0));
		alarm.setId(id);
		alarm.setConnectionName("Standard");
		ConnectionEntity connection = new ConnectionEntity();
		when(alarmRepository.findById(id)).thenReturn(Optional.of(alarm));
		when(connectionRepository.findByName("Standard")).thenReturn(Optional.of(connection));
		doThrow(new IllegalStateException("Broker nicht erreichbar"))
				.when(mqttAlarmPublisher)
				.publish(any(), any());

		assertThatThrownBy(() -> alarmDispatchService.sendDueAlarm(id))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Broker nicht erreichbar");
		verify(alarmRepository, never()).save(any());
		verify(alarmAnnouncementService, never()).announceAsync(any());
		assertThat(alarm.getStatus()).isEqualTo("planned");
	}

	/**
	 * Erzeugt eine geplante Testdaten-Alarmierung.
	 *
	 * @param scheduledAt Zeitpunkt
	 * @return Alarm
	 */
	private static AlarmEntity planned(LocalDateTime scheduledAt) {
		AlarmEntity alarm = new AlarmEntity();
		alarm.setScheduledAt(scheduledAt);
		alarm.setStatus("planned");
		return alarm;
	}
}
