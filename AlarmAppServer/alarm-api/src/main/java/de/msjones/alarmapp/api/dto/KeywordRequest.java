package de.msjones.alarmapp.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Schreibdaten eines Alarmstichworts.
 *
 * @param name Anzeigename
 */
public record KeywordRequest(@NotBlank String name) {
}
