package de.msjones.alarmapp.api.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Markiert fehlende Datensätze gegenüber der REST-API.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

	/**
	 * Erzeugt die Ausnahme mit einer lesbaren Meldung.
	 *
	 * @param message Fehlertext
	 */
	public NotFoundException(String message) {
		super(message);
	}
}
