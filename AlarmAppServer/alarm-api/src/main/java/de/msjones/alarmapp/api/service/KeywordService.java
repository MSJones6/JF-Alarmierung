package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.KeywordEntity;
import de.msjones.alarmapp.api.dto.KeywordRequest;
import de.msjones.alarmapp.api.dto.KeywordResponse;
import de.msjones.alarmapp.api.repository.KeywordRepository;
import de.msjones.alarmapp.api.web.NotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachlogik für Alarmstichworte.
 */
@Service
public class KeywordService {

	private final KeywordRepository keywordRepository;

	/**
	 * Erzeugt den Dienst mit dem Stichwort-Repository.
	 *
	 * @param keywordRepository Datenzugriff
	 */
	public KeywordService(KeywordRepository keywordRepository) {
		this.keywordRepository = keywordRepository;
	}

	/**
	 * Liefert alle Stichworte in Anzeigereihenfolge.
	 *
	 * @return Stichworte
	 */
	@Transactional(readOnly = true)
	public List<KeywordResponse> findAll() {
		return keywordRepository.findAllByOrderBySortOrderAscNameAsc().stream()
				.map(EntityMapper::toResponse)
				.toList();
	}

	/**
	 * Legt ein neues Stichwort an.
	 *
	 * @param request Schreibdaten
	 * @return gespeichertes Stichwort
	 */
	@Transactional
	public KeywordResponse create(KeywordRequest request) {
		KeywordEntity entity = new KeywordEntity();
		entity.setId(UUID.randomUUID());
		EntityMapper.apply(entity, request, keywordRepository.findAllByOrderBySortOrderAscNameAsc().size());
		return EntityMapper.toResponse(keywordRepository.save(entity));
	}

	/**
	 * Aktualisiert ein vorhandenes Stichwort.
	 *
	 * @param id Stichwort-ID
	 * @param request Schreibdaten
	 * @return gespeichertes Stichwort
	 */
	@Transactional
	public KeywordResponse update(UUID id, KeywordRequest request) {
		KeywordEntity entity = keywordRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Alarmstichwort nicht gefunden."));
		EntityMapper.apply(entity, request, entity.getSortOrder());
		return EntityMapper.toResponse(keywordRepository.save(entity));
	}

	/**
	 * Ersetzt die komplette Stichwortliste.
	 *
	 * @param requests neue Stichworte in Anzeigereihenfolge
	 * @return gespeicherte Stichworte
	 */
	@Transactional
	public List<KeywordResponse> replaceAll(List<KeywordRequest> requests) {
		keywordRepository.deleteAllInBatch();
		keywordRepository.flush();
		List<KeywordEntity> saved = new ArrayList<>();
		int sortOrder = 0;
		if (requests == null) {
			return List.of();
		}
		for (KeywordRequest request : requests) {
			if (request == null || request.name() == null || request.name().isBlank()) {
				continue;
			}
			KeywordEntity entity = new KeywordEntity();
			entity.setId(UUID.randomUUID());
			EntityMapper.apply(entity, request, sortOrder++);
			saved.add(keywordRepository.save(entity));
		}
		return saved.stream().map(EntityMapper::toResponse).toList();
	}

	/**
	 * Löscht ein Stichwort.
	 *
	 * @param id Stichwort-ID
	 */
	@Transactional
	public void delete(UUID id) {
		if (!keywordRepository.existsById(id)) {
			throw new NotFoundException("Alarmstichwort nicht gefunden.");
		}
		keywordRepository.deleteById(id);
	}
}
