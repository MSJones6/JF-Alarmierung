package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import de.msjones.alarmapp.api.domain.ConnectionEntity;
import de.msjones.alarmapp.api.domain.KeywordEntity;
import de.msjones.alarmapp.api.dto.AlarmRequest;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.dto.ConnectionRequest;
import de.msjones.alarmapp.api.dto.ConnectionResponse;
import de.msjones.alarmapp.api.dto.KeywordResponse;
import java.util.UUID;

/**
 * Wandelt JPA-Entitäten in API-Objekte um und zurück.
 */
public final class EntityMapper {

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
		return new KeywordResponse(entity.getId(), entity.getName());
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
}
