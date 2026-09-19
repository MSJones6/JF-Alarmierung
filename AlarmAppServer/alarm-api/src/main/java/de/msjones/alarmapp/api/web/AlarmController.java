package de.msjones.alarmapp.api.web;

import de.msjones.alarmapp.api.dto.AlarmRequest;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.service.AlarmService;
import de.msjones.alarmapp.api.service.AlarmStreamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST- und Stream-Schnittstelle für geplante und bereits alarmierte Einträge.
 */
@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

	private final AlarmService alarmService;
	private final AlarmStreamService alarmStreamService;

	/**
	 * Erzeugt den Controller.
	 *
	 * @param alarmService Fachlogik
	 * @param alarmStreamService Live-Stream
	 */
	public AlarmController(AlarmService alarmService, AlarmStreamService alarmStreamService) {
		this.alarmService = alarmService;
		this.alarmStreamService = alarmStreamService;
	}

	/**
	 * Liefert alle Alarmierungen.
	 *
	 * @return Alarmliste
	 */
	@GetMapping
	public List<AlarmResponse> findAll() {
		return alarmService.findAll();
	}

	/**
	 * Öffnet den Server-Sent-Events-Stream mit aktuellem Tabellenstand.
	 *
	 * @return SSE-Emitter
	 */
	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() {
		return alarmStreamService.subscribe();
	}

	/**
	 * Legt eine Alarmierung an.
	 *
	 * @param request Schreibdaten
	 * @return gespeicherte Alarmierung
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AlarmResponse create(@Valid @RequestBody AlarmRequest request) {
		return alarmService.create(request);
	}

	/**
	 * Aktualisiert eine Alarmierung.
	 *
	 * @param id Alarm-ID
	 * @param request Schreibdaten
	 * @return gespeicherte Alarmierung
	 */
	@PutMapping("/{id}")
	public AlarmResponse update(@PathVariable UUID id, @Valid @RequestBody AlarmRequest request) {
		return alarmService.update(id, request);
	}

	/**
	 * Löscht eine Alarmierung.
	 *
	 * @param id Alarm-ID
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		alarmService.delete(id);
	}
}
