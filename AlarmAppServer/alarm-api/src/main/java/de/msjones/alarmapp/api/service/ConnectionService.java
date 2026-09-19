package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.ConnectionEntity;
import de.msjones.alarmapp.api.dto.ConnectionRequest;
import de.msjones.alarmapp.api.dto.ConnectionResponse;
import de.msjones.alarmapp.api.repository.ConnectionRepository;
import de.msjones.alarmapp.api.web.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachlogik für das Anlegen, Ändern und Ersetzen von Connections.
 */
@Service
public class ConnectionService {

	private final ConnectionRepository connectionRepository;

	/**
	 * Erzeugt den Dienst mit dem Connection-Repository.
	 *
	 * @param connectionRepository Datenzugriff
	 */
	public ConnectionService(ConnectionRepository connectionRepository) {
		this.connectionRepository = connectionRepository;
	}

	/**
	 * Liefert alle Connections in Anzeigereihenfolge.
	 *
	 * @return Connections
	 */
	@Transactional(readOnly = true)
	public List<ConnectionResponse> findAll() {
		return connectionRepository.findAllByOrderBySortOrderAscNameAsc().stream()
				.map(EntityMapper::toResponse)
				.toList();
	}

	/**
	 * Legt eine neue Connection an.
	 *
	 * @param request Schreibdaten
	 * @return gespeicherte Connection
	 */
	@Transactional
	public ConnectionResponse create(ConnectionRequest request) {
		ConnectionEntity entity = new ConnectionEntity();
		entity.setId(EntityMapper.resolveId(request.id()));
		EntityMapper.apply(entity, request, nextSortOrder());
		return EntityMapper.toResponse(connectionRepository.save(entity));
	}

	/**
	 * Aktualisiert eine vorhandene Connection.
	 *
	 * @param id Connection-ID
	 * @param request Schreibdaten
	 * @return gespeicherte Connection
	 */
	@Transactional
	public ConnectionResponse update(UUID id, ConnectionRequest request) {
		ConnectionEntity entity = connectionRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Connection nicht gefunden."));
		String password = request.password();
		if (password == null || password.isBlank()) {
			password = entity.getPassword();
		}
		EntityMapper.apply(
				entity,
				new ConnectionRequest(
						id,
						request.name(),
						request.useSsl(),
						request.brokerHost(),
						request.brokerPort(),
						request.brokerPath(),
						request.user(),
						password,
						request.mqttTopic()
				),
				entity.getSortOrder()
		);
		return EntityMapper.toResponse(connectionRepository.save(entity));
	}

	/**
	 * Ersetzt die komplette Connection-Liste durch die übergebenen Werte.
	 *
	 * @param requests neue Liste
	 * @return gespeicherte Connections
	 */
	@Transactional
	public List<ConnectionResponse> replaceAll(List<ConnectionRequest> requests) {
		connectionRepository.deleteAllInBatch();
		connectionRepository.flush();
		List<ConnectionEntity> saved = new ArrayList<>();
		int sortOrder = 0;
		for (ConnectionRequest request : requests) {
			ConnectionEntity entity = new ConnectionEntity();
			entity.setId(EntityMapper.resolveId(request.id()));
			EntityMapper.apply(entity, request, sortOrder++);
			saved.add(connectionRepository.save(entity));
		}
		return saved.stream().map(EntityMapper::toResponse).toList();
	}

	/**
	 * Löscht eine Connection.
	 *
	 * @param id Connection-ID
	 */
	@Transactional
	public void delete(UUID id) {
		if (!connectionRepository.existsById(id)) {
			throw new NotFoundException("Connection nicht gefunden.");
		}
		connectionRepository.deleteById(id);
	}

	/**
	 * Ermittelt den nächsten freien Sortierindex.
	 *
	 * @return nächster Index
	 */
	private int nextSortOrder() {
		return connectionRepository.findAllByOrderBySortOrderAscNameAsc().size();
	}
}
