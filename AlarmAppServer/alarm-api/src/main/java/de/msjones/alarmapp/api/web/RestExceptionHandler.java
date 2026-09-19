package de.msjones.alarmapp.api.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Liefert einheitliche JSON-Fehlerantworten für die REST-API.
 */
@RestControllerAdvice
public class RestExceptionHandler {

	/**
	 * Wandelt Validierungsfehler in HTTP 400 um.
	 *
	 * @param exception Bean-Validation-Fehler
	 * @return Fehlerkörper
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.orElse("Die Eingabe ist ungültig.");
		return ResponseEntity.badRequest().body(Map.of("message", message));
	}

	/**
	 * Wandelt fehlende Datensätze in HTTP 404 um.
	 *
	 * @param exception Fachfehler
	 * @return Fehlerkörper
	 */
	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
	}

	/**
	 * Wandelt MQTT- und Versandfehler in HTTP 502 um.
	 *
	 * @param exception Fachfehler
	 * @return Fehlerkörper
	 */
	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", exception.getMessage()));
	}
}
