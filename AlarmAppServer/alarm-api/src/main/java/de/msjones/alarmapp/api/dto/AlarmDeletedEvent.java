package de.msjones.alarmapp.api.dto;

import java.util.UUID;

/**
 * Stream-Ereignis beim Löschen einer Alarmierung.
 *
 * @param id gelöschte ID
 */
public record AlarmDeletedEvent(UUID id) {
}
