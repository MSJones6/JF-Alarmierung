package de.msjones.alarmapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Schreibdaten eines Alarmstichworts.
 *
 * @param name Anzeigename
 * @param color Badge-Farbe im Format `#RRGGBB`, optional mit Standardwert
 */
public record KeywordRequest(
		@NotBlank String name,
		@Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "Farbe muss im Format #RRGGBB angegeben werden.")
		String color
) {
}
