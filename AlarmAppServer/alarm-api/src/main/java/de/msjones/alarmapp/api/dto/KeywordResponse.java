package de.msjones.alarmapp.api.dto;

import java.util.UUID;

/**
 * Lesedarstellung eines Alarmstichworts.
 *
 * @param id technische ID
 * @param name Anzeigename
 */
public record KeywordResponse(UUID id, String name) {
}
