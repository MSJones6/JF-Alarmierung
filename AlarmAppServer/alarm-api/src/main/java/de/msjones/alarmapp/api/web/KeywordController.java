package de.msjones.alarmapp.api.web;

import de.msjones.alarmapp.api.dto.KeywordRequest;
import de.msjones.alarmapp.api.dto.KeywordResponse;
import de.msjones.alarmapp.api.service.KeywordService;
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
 * REST-Schnittstelle zum Bearbeiten der Alarmstichworte.
 */
@RestController
@RequestMapping("/api/keywords")
public class KeywordController {

	private final KeywordService keywordService;

	/**
	 * Erzeugt den Controller.
	 *
	 * @param keywordService Fachlogik
	 */
	public KeywordController(KeywordService keywordService) {
		this.keywordService = keywordService;
	}

	/**
	 * Liefert alle Alarmstichworte.
	 *
	 * @return Stichwortliste
	 */
	@GetMapping
	public List<KeywordResponse> findAll() {
		return keywordService.findAll();
	}

	/**
	 * Legt ein Alarmstichwort an.
	 *
	 * @param request Schreibdaten
	 * @return gespeichertes Stichwort
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public KeywordResponse create(@Valid @RequestBody KeywordRequest request) {
		return keywordService.create(request);
	}

	/**
	 * Ersetzt die komplette Stichwortliste.
	 *
	 * @param requests Stichworte in Anzeigereihenfolge
	 * @return gespeicherte Stichworte
	 */
	@PutMapping
	public List<KeywordResponse> replaceAll(@Valid @RequestBody List<KeywordRequest> requests) {
		return keywordService.replaceAll(requests);
	}

	/**
	 * Aktualisiert ein Alarmstichwort.
	 *
	 * @param id Stichwort-ID
	 * @param request Schreibdaten
	 * @return gespeichertes Stichwort
	 */
	@PutMapping("/{id}")
	public KeywordResponse update(@PathVariable UUID id, @Valid @RequestBody KeywordRequest request) {
		return keywordService.update(id, request);
	}

	/**
	 * Löscht ein Alarmstichwort.
	 *
	 * @param id Stichwort-ID
	 */
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable UUID id) {
		keywordService.delete(id);
	}
}
