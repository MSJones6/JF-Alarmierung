package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import de.msjones.alarmapp.api.dto.AlarmDeletedEvent;
import de.msjones.alarmapp.api.dto.AlarmRequest;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.repository.AlarmRepository;
import de.msjones.alarmapp.api.web.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fachlogik für geplante und bereits alarmierte Einträge inklusive Stream-Ereignisse.
 */
@Service
public class AlarmService {

	private final AlarmRepository alarmRepository;
	private final AlarmStreamService alarmStreamService;

	/**
	 * Erzeugt den Dienst mit Repository und Stream.
	 *
	 * @param alarmRepository Datenzugriff
	 * @param alarmStreamService Live-Updates an Browser
	 */
	public AlarmService(AlarmRepository alarmRepository, AlarmStreamService alarmStreamService) {
		this.alarmRepository = alarmRepository;
		this.alarmStreamService = alarmStreamService;
	}

	/**
	 * Liefert alle Alarmierungen.
	 *
	 * @return Alarmierungen
	 */
	@Transactional(readOnly = true)
	public List<AlarmResponse> findAll() {
		return alarmRepository.findAllByOrderByScheduledAtAsc().stream()
				.map(EntityMapper::toResponse)
				.toList();
	}

	/**
	 * Legt eine Alarmierung an und benachrichtigt den Stream nach dem Commit.
	 *
	 * @param request Schreibdaten
	 * @return gespeicherte Alarmierung
	 */
	@Transactional
	public AlarmResponse create(AlarmRequest request) {
		AlarmEntity entity = new AlarmEntity();
		entity.setId(UUID.randomUUID());
		EntityMapper.apply(entity, request);
		AlarmResponse response = EntityMapper.toResponse(alarmRepository.save(entity));
		publishAfterCommit("created", response);
		return response;
	}

	/**
	 * Aktualisiert eine Alarmierung und benachrichtigt den Stream nach dem Commit.
	 *
	 * @param id Alarm-ID
	 * @param request Schreibdaten
	 * @return gespeicherte Alarmierung
	 */
	@Transactional
	public AlarmResponse update(UUID id, AlarmRequest request) {
		AlarmEntity entity = alarmRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Alarmierung nicht gefunden."));
		EntityMapper.apply(entity, request);
		AlarmResponse response = EntityMapper.toResponse(alarmRepository.save(entity));
		publishAfterCommit("updated", response);
		return response;
	}

	/**
	 * Löscht eine Alarmierung und benachrichtigt den Stream nach dem Commit.
	 *
	 * @param id Alarm-ID
	 */
	@Transactional
	public void delete(UUID id) {
		if (!alarmRepository.existsById(id)) {
			throw new NotFoundException("Alarmierung nicht gefunden.");
		}
		alarmRepository.deleteById(id);
		publishAfterCommit("deleted", new AlarmDeletedEvent(id));
	}

	/**
	 * Sendet das Stream-Ereignis erst, wenn die Transaktion erfolgreich war.
	 *
	 * @param eventName Ereignisname
	 * @param payload Nutzdaten
	 */
	private void publishAfterCommit(String eventName, Object payload) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			alarmStreamService.send(eventName, payload);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			/**
			 * Verteilt das Ereignis nach dem erfolgreichen Commit.
			 */
			@Override
			public void afterCommit() {
				alarmStreamService.send(eventName, payload);
			}
		});
	}
}
