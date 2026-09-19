package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.repository.AlarmRepository;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Verteilt Alarmänderungen per Server-Sent Events an alle offenen Browser.
 */
@Service
public class AlarmStreamService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmStreamService.class);
	private static final long STREAM_TIMEOUT_MS = 0L;

	private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private final AlarmRepository alarmRepository;

	/**
	 * Erzeugt den Stream-Dienst.
	 *
	 * @param alarmRepository liefert den Tabellenstand für neue Abonnenten
	 */
	public AlarmStreamService(AlarmRepository alarmRepository) {
		this.alarmRepository = alarmRepository;
	}

	/**
	 * Meldet einen Browser am Live-Stream an und sendet zuerst den aktuellen Stand.
	 *
	 * @return SSE-Emitter
	 */
	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
		emitters.add(emitter);
		emitter.onCompletion(() -> emitters.remove(emitter));
		emitter.onTimeout(() -> emitters.remove(emitter));
		emitter.onError(error -> emitters.remove(emitter));
		sendTo(emitter, "snapshot", currentAlarms());
		return emitter;
	}

	/**
	 * Sendet ein benanntes Ereignis an alle verbundenen Browser.
	 *
	 * @param eventName Ereignisname
	 * @param payload JSON-fähiges Objekt
	 */
	public void send(String eventName, Object payload) {
		for (SseEmitter emitter : emitters) {
			sendTo(emitter, eventName, payload);
		}
	}

	/**
	 * Liest den aktuellen Alarmbestand für den Snapshot.
	 *
	 * @return aktuelle Liste
	 */
	public List<AlarmResponse> currentAlarms() {
		return alarmRepository.findAllByOrderByScheduledAtAsc().stream()
				.map(EntityMapper::toResponse)
				.toList();
	}

	/**
	 * Schreibt ein SSE-Ereignis an einen einzelnen Emitter.
	 *
	 * @param emitter Zielverbindung
	 * @param eventName Ereignisname
	 * @param payload Nutzdaten
	 */
	private void sendTo(SseEmitter emitter, String eventName, Object payload) {
		try {
			emitter.send(SseEmitter.event()
					.name(eventName)
					.data(payload, MediaType.APPLICATION_JSON));
		} catch (IOException exception) {
			LOGGER.debug("SSE-Verbindung geschlossen: {}", exception.getMessage());
			emitters.remove(emitter);
			emitter.complete();
		}
	}
}
