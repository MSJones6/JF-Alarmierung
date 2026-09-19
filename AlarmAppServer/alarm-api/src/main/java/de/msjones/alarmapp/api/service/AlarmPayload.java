package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Baut MQTT-Payloads und prüft geplante Zeitpunkte.
 */
public final class AlarmPayload {

	/**
	 * Verhindert die Instanziierung.
	 */
	private AlarmPayload() {
	}

	/**
	 * Serialisiert Stichwort, Ort und Infos im Android-Format.
	 *
	 * @param alarm persistierte Alarmierung
	 * @return {@code Stichwort###Ort###Info}
	 */
	public static String from(AlarmEntity alarm) {
		String keyword = alarm.getKeyword() == null ? "" : alarm.getKeyword();
		String location = alarm.getLocation() == null ? "" : alarm.getLocation();
		String info = alarm.getInfo() == null ? "" : alarm.getInfo();
		return keyword + "###" + location + "###" + info;
	}

	/**
	 * Wandelt einen lokalen ISO-Zeitstempel in ein Datum um.
	 *
	 * @param scheduledAt Wert im Format {@code YYYY-MM-DDTHH:mm} oder mit Sekunden
	 * @return geparster Zeitpunkt
	 */
	public static LocalDateTime parseScheduledAt(String scheduledAt) {
		if (scheduledAt == null || scheduledAt.isBlank()) {
			throw new DateTimeParseException("Geplanter Zeitpunkt fehlt.", "", 0);
		}
		String normalized = scheduledAt.trim();
		if (normalized.length() == 16) {
			normalized += ":00";
		}
		return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	/**
	 * Prüft, ob der geplante Zeitpunkt erreicht oder überschritten ist.
	 *
	 * @param scheduledAt geplanter Zeitpunkt
	 * @param now aktueller Zeitpunkt in der Scheduler-Zone
	 * @return {@code true}, wenn der Alarm fällig ist
	 */
	public static boolean isDue(LocalDateTime scheduledAt, LocalDateTime now) {
		return scheduledAt != null && !scheduledAt.isAfter(now);
	}
}
