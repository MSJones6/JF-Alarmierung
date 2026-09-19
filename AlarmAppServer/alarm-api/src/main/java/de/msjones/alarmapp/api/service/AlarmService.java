package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import de.msjones.alarmapp.api.dto.AlarmDeletedEvent;
import de.msjones.alarmapp.api.dto.AlarmRequest;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.repository.AlarmRepository;
import de.msjones.alarmapp.api.web.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Fachlogik für geplante und bereits alarmierte Einträge inklusive Stream-Ereignisse.
 */
@Service
public class AlarmService {

	private static final String STATUS_SENT = "sent";

	private final AlarmRepository alarmRepository;
	private final AlarmStreamService alarmStreamService;
	private final AlarmDispatchService alarmDispatchService;
	private final ObjectProvider<AlarmDispatchScheduler> alarmDispatchScheduler;

	/**
	 * Erzeugt den Dienst mit Repository, Stream, MQTT-Versand und optionalem Scheduler.
	 *
	 * @param alarmRepository Datenzugriff
	 * @param alarmStreamService Live-Updates an Browser
	 * @param alarmDispatchService MQTT-Versand für sofortige Alarmierungen
	 * @param alarmDispatchScheduler optionaler Dispatch-Scheduler
	 */
	public AlarmService(
			AlarmRepository alarmRepository,
			AlarmStreamService alarmStreamService,
			AlarmDispatchService alarmDispatchService,
			ObjectProvider<AlarmDispatchScheduler> alarmDispatchScheduler
	) {
		this.alarmRepository = alarmRepository;
		this.alarmStreamService = alarmStreamService;
		this.alarmDispatchService = alarmDispatchService;
		this.alarmDispatchScheduler = alarmDispatchScheduler;
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
	 * Bei Status {@code sent} wird zuvor per MQTT ausgelöst.
	 *
	 * @param request Schreibdaten
	 * @return gespeicherte Alarmierung
	 */
	@Transactional
	public AlarmResponse create(AlarmRequest request) {
		AlarmEntity entity = new AlarmEntity();
		entity.setId(UUID.randomUUID());
		EntityMapper.apply(entity, request);
		publishIfSent(entity);
		AlarmResponse response = EntityMapper.toResponse(alarmRepository.save(entity));
		publishAfterCommit("created", response);
		rescheduleDispatchAfterCommit();
		return response;
	}

	/**
	 * Aktualisiert eine Alarmierung und benachrichtigt den Stream nach dem Commit.
	 * Bei Status {@code sent} wird zuvor per MQTT ausgelöst.
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
		publishIfSent(entity);
		AlarmResponse response = EntityMapper.toResponse(alarmRepository.save(entity));
		publishAfterCommit("updated", response);
		rescheduleDispatchAfterCommit();
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
		rescheduleDispatchAfterCommit();
	}

	/**
	 * Löst die Alarmierung sofort aus, wenn sie als gesendet gespeichert wird.
	 *
	 * @param entity zu speichernde Alarmierung
	 */
	private void publishIfSent(AlarmEntity entity) {
		if (STATUS_SENT.equals(entity.getStatus())) {
			alarmDispatchService.publishFor(entity);
		}
	}

	/**
	 * Plant den Alarm-Dispatch nach dem Commit neu, sofern der Scheduler aktiv ist.
	 */
	private void rescheduleDispatchAfterCommit() {
		AlarmDispatchScheduler scheduler = alarmDispatchScheduler.getIfAvailable();
		if (scheduler == null) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			scheduler.reschedule();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			/**
			 * Plant den nächsten Dispatch nach dem erfolgreichen Commit.
			 */
			@Override
			public void afterCommit() {
				scheduler.reschedule();
			}
		});
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
