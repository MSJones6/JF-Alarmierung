package de.msjones.alarmapp.api.web;

import de.msjones.alarmapp.api.dto.ConnectionRequest;
import de.msjones.alarmapp.api.dto.ConnectionResponse;
import de.msjones.alarmapp.api.service.ConnectionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Schnittstelle zum Bearbeiten der MQTT-Connections.
 */
@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

	private final ConnectionService connectionService;

	/**
	 * Erzeugt den Controller.
	 *
	 * @param connectionService Fachlogik
	 */
	public ConnectionController(ConnectionService connectionService) {
		this.connectionService = connectionService;
	}

	/**
	 * Liefert alle Connections.
	 *
	 * @return Connection-Liste
	 */
	@GetMapping
	public List<ConnectionResponse> findAll() {
		return connectionService.findAll();
	}

	/**
	 * Legt eine Connection an.
	 *
	 * @param request Schreibdaten
	 * @return gespeicherte Connection
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ConnectionResponse create(@Valid @RequestBody ConnectionRequest request) {
		return connectionService.create(request);
	}

	/**
	 * Ersetzt die komplette Connection-Liste.
	 *
	 * @param requests neue Connections
	 * @return gespeicherte Liste
	 */
	@PutMapping
	public List<ConnectionResponse> replaceAll(@Valid @RequestBody List<@Valid ConnectionRequest> requests) {
		return connectionService.replaceAll(requests);
	}

	/**
	 * Aktualisiert eine Connection.
	 *
	 * @param id Connection-ID
	 * @param request Schreibdaten
	 * @return gespeicherte Connection
	 */
	@PutMapping("/{id}")
	public ConnectionResponse update(
			@PathVariable UUID id,
			@Valid @RequestBody ConnectionRequest request
	) {
		return connectionService.update(id, request);
	}

	/**
	 * Löscht eine Connection.
	 *
	 * @param id Connection-ID
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		connectionService.delete(id);
	}
}
