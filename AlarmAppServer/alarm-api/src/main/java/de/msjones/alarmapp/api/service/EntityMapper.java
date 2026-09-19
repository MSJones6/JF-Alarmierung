package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import de.msjones.alarmapp.api.domain.ConnectionEntity;
import de.msjones.alarmapp.api.domain.KeywordEntity;
import de.msjones.alarmapp.api.dto.AlarmRequest;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.dto.ConnectionRequest;
import de.msjones.alarmapp.api.dto.ConnectionResponse;
import de.msjones.alarmapp.api.dto.KeywordRequest;
import de.msjones.alarmapp.api.dto.KeywordResponse;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Wandelt JPA-Entitäten in API-Objekte um und zurück.
 */
public final class EntityMapper {

	/** Standardfarbe für unbekannte oder fehlende Stichwortfarben. */
	public static final String DEFAULT_KEYWORD_COLOR = "#64748b";

	/** Erlaubtes Hex-Format für Badge-Farben. */
	private static final Pattern KEYWORD_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

	/**
	 * Verhindert die Instanziierung.
	 */
	private EntityMapper() {
	}

	/**
	 * Mappt eine Connection in die Lesedarstellung.
	 *
	 * @param entity persistierte Connection
	 * @return API-Antwort
	 */
	public static ConnectionResponse toResponse(ConnectionEntity entity) {
		return new ConnectionResponse(
				entity.getId(),
				entity.getName(),
				entity.isUseSsl(),
				entity.getBrokerHost(),
				entity.getBrokerPort(),
				entity.getBrokerPath(),
				entity.getUser(),
				entity.getPassword(),
				entity.getMqttTopic()
		);
	}

	/**
	 * Übernimmt Schreibdaten in eine Connection-Entität.
	 *
	 * @param entity Zielentität
	 * @param request Schreibdaten
	 * @param sortOrder Sortierindex
	 */
	public static void apply(ConnectionEntity entity, ConnectionRequest request, int sortOrder) {
		entity.setName(request.name().trim());
		entity.setUseSsl(request.useSsl());
		entity.setBrokerHost(request.brokerHost().trim());
		entity.setBrokerPort(request.brokerPort().trim());
		entity.setBrokerPath(blankToDefault(request.brokerPath(), "/mqtt"));
		entity.setUser(blankToDefault(request.user(), ""));
		entity.setPassword(request.password() == null ? "" : request.password());
		entity.setMqttTopic(request.mqttTopic().trim());
		entity.setSortOrder(sortOrder);
	}

	/**
	 * Mappt ein Stichwort in die Lesedarstellung.
	 *
	 * @param entity persistiertes Stichwort
	 * @return API-Antwort
	 */
	public static KeywordResponse toResponse(KeywordEntity entity) {
		return new KeywordResponse(entity.getId(), entity.getName(), entity.getColor());
	}

	/**
	 * Übernimmt Schreibdaten in ein Stichwort.
	 *
	 * @param entity Zielentität
	 * @param request Schreibdaten
	 * @param sortOrder Sortierindex
	 */
	public static void apply(KeywordEntity entity, KeywordRequest request, int sortOrder) {
		entity.setName(request.name().trim());
		entity.setColor(normalizeKeywordColor(request.color()));
		entity.setSortOrder(sortOrder);
	}

	/**
	 * Mappt eine Alarmierung in die Lesedarstellung.
	 *
	 * @param entity persistierte Alarmierung
	 * @return API-Antwort
	 */
	public static AlarmResponse toResponse(AlarmEntity entity) {
		return new AlarmResponse(
				entity.getId(),
				entity.getScheduledAt(),
				entity.getConnectionName(),
				entity.getLocation(),
				entity.getKeyword(),
				entity.getInfo(),
				entity.getStatus()
		);
	}

	/**
	 * Übernimmt Schreibdaten in eine Alarm-Entität.
	 *
	 * @param entity Zielentität
	 * @param request Schreibdaten
	 */
	public static void apply(AlarmEntity entity, AlarmRequest request) {
		entity.setScheduledAt(request.scheduledAt().trim());
		entity.setConnectionName(request.connection().trim());
		entity.setLocation(request.location().trim());
		entity.setKeyword(request.keyword().trim());
		entity.setInfo(request.info() == null ? "" : request.info().trim());
		entity.setStatus(request.status().trim());
	}

	/**
	 * Liefert eine vorhandene oder neue UUID.
	 *
	 * @param id optionale ID
	 * @return UUID
	 */
	public static UUID resolveId(UUID id) {
		return id == null ? UUID.randomUUID() : id;
	}

	/**
	 * Ersetzt leere Texte durch einen Standardwert.
	 *
	 * @param value Rohwert
	 * @param fallback Standard
	 * @return bereinigter Text
	 */
	private static String blankToDefault(String value, String fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		return value.trim();
	}

	/**
	 * Normalisiert eine Stichwortfarbe auf `#rrggbb`.
	 *
	 * @param color Rohwert
	 * @return gültige Hex-Farbe
	 */
	public static String normalizeKeywordColor(String color) {
		if (color == null || color.isBlank()) {
			return DEFAULT_KEYWORD_COLOR;
		}
		String trimmed = color.trim();
		if (!KEYWORD_COLOR_PATTERN.matcher(trimmed).matches()) {
			return DEFAULT_KEYWORD_COLOR;
		}
		return trimmed.toLowerCase(Locale.ROOT);
	}
}
