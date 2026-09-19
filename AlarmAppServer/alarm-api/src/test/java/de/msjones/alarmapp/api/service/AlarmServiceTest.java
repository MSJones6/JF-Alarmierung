package de.msjones.alarmapp.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import de.msjones.alarmapp.api.dto.AlarmRequest;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.repository.AlarmRepository;
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
 * Prüft das Anlegen und Aktualisieren von Alarmierungen.
 */
@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

	@Mock
	private AlarmRepository alarmRepository;

	@Mock
	private AlarmStreamService alarmStreamService;

	private AlarmService alarmService;

	/**
	 * Erzeugt den Dienst mit Mock-Abhängigkeiten.
	 */
	@BeforeEach
	void setUp() {
		alarmService = new AlarmService(alarmRepository, alarmStreamService);
	}

	@Test
	void createStoresLocationInsteadOfConnectionNameInPayloadFields() {
		when(alarmRepository.save(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AlarmResponse created = alarmService.create(new AlarmRequest(
				"2025-04-24T14:30:15",
				"Host Nord",
				"Turnhalle",
				"Warnung",
				"Probe",
				"planned"
		));

		assertThat(created.connection()).isEqualTo("Host Nord");
		assertThat(created.location()).isEqualTo("Turnhalle");
		ArgumentCaptor<AlarmEntity> captor = ArgumentCaptor.forClass(AlarmEntity.class);
		verify(alarmRepository).save(captor.capture());
		assertThat(captor.getValue().getLocation()).isEqualTo("Turnhalle");
		verify(alarmStreamService).send("created", created);
	}

	@Test
	void updateMarksAlarmAsSent() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000003");
		AlarmEntity entity = new AlarmEntity();
		entity.setId(id);
		when(alarmRepository.findById(id)).thenReturn(Optional.of(entity));
		when(alarmRepository.save(any(AlarmEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AlarmResponse updated = alarmService.update(id, new AlarmRequest(
				"2025-04-24T14:30:15",
				"Standard",
				"Turnhalle",
				"Feueralarm",
				"Rauch",
				"sent"
		));

		assertThat(updated.status()).isEqualTo("sent");
		verify(alarmStreamService).send("updated", updated);
	}

	@Test
	void findAllMapsEntities() {
		AlarmEntity entity = new AlarmEntity();
		entity.setId(UUID.randomUUID());
		entity.setScheduledAt("2025-04-24T14:30:15");
		entity.setConnectionName("Standard");
		entity.setLocation("Gebäude 3");
		entity.setKeyword("Info");
		entity.setInfo("");
		entity.setStatus("planned");
		when(alarmRepository.findAllByOrderByScheduledAtAsc()).thenReturn(List.of(entity));

		assertThat(alarmService.findAll()).hasSize(1);
		assertThat(alarmService.findAll().getFirst().location()).isEqualTo("Gebäude 3");
	}
}
