package de.msjones.alarmapp.api.dto;

import java.util.UUID;

/**
 * Lesedarstellung einer Alarmierung für REST und Stream.
 *
 * @param id technische ID
 * @param scheduledAt geplanter Zeitpunkt
 * @param connection Connection-Name
 * @param location Einsatzort
 * @param keyword Alarmstichwort
 * @param info weitere Infos
 * @param status {@code planned} oder {@code sent}
 */
public record AlarmResponse(
		UUID id,
		String scheduledAt,
		String connection,
		String location,
		String keyword,
		String info,
		String status
) {
}
