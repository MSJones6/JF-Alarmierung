package de.msjones.alarmapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * Schreibdaten einer Alarmierung.
 *
 * @param scheduledAt geplanter Zeitpunkt ohne Zeitzone
 * @param connection gewählte Connection
 * @param location Einsatzort
 * @param keyword Alarmstichwort
 * @param info weitere Infos
 * @param status {@code planned} oder {@code sent}
 */
public record AlarmRequest(
		@NotNull LocalDateTime scheduledAt,
		@NotBlank String connection,
		@NotBlank String location,
		@NotBlank String keyword,
		String info,
		@NotBlank @Pattern(regexp = "planned|sent") String status
) {
}
