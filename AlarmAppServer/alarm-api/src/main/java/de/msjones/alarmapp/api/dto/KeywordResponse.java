package de.msjones.alarmapp.api.dto;

import java.util.UUID;

/**
 * Lesedarstellung eines Alarmstichworts.
 *
 * @param id technische ID
 * @param name Anzeigename
 * @param color Badge-Farbe im Format `#RRGGBB`
 */
public record KeywordResponse(UUID id, String name, String color) {
}
