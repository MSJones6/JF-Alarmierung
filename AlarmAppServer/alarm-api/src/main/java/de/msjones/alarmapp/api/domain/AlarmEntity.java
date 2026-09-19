package de.msjones.alarmapp.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistierte geplante oder bereits ausgelöste Alarmierung.
 */
@Entity
@Table(name = "alarms")
public class AlarmEntity {

	@Id
	private UUID id;

	@Column(name = "scheduled_at", nullable = false)
	private LocalDateTime scheduledAt;

	@Column(name = "connection_name", nullable = false)
	private String connectionName;

	@Column(nullable = false)
	private String location;

	@Column(nullable = false)
	private String keyword;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String info;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/**
	 * Erzeugt eine leere Alarmierung für JPA.
	 */
	public AlarmEntity() {
	}

	/**
	 * Setzt Zeitstempel vor dem ersten Speichern.
	 */
	@PrePersist
	public void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
		if (id == null) {
			id = UUID.randomUUID();
		}
	}

	/**
	 * Aktualisiert den Änderungszeitstempel vor jedem Speichern.
	 */
	@PreUpdate
	public void onUpdate() {
		updatedAt = Instant.now();
	}

	/**
	 * Liefert die technische ID.
	 *
	 * @return UUID der Alarmierung
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Setzt die technische ID.
	 *
	 * @param id UUID der Alarmierung
	 */
	public void setId(UUID id) {
		this.id = id;
	}

	/**
	 * Liefert den geplanten Zeitpunkt ohne Zeitzone.
	 *
	 * @return lokaler Zeitpunkt
	 */
	public LocalDateTime getScheduledAt() {
		return scheduledAt;
	}

	/**
	 * Setzt den geplanten Zeitpunkt.
	 *
	 * @param scheduledAt lokaler Zeitpunkt
	 */
	public void setScheduledAt(LocalDateTime scheduledAt) {
		this.scheduledAt = scheduledAt;
	}

	/**
	 * Liefert den Connection-Namen.
	 *
	 * @return Connection
	 */
	public String getConnectionName() {
		return connectionName;
	}

	/**
	 * Setzt den Connection-Namen.
	 *
	 * @param connectionName Connection
	 */
	public void setConnectionName(String connectionName) {
		this.connectionName = connectionName;
	}

	/**
	 * Liefert den Einsatzort.
	 *
	 * @return Ort
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Setzt den Einsatzort.
	 *
	 * @param location Ort
	 */
	public void setLocation(String location) {
		this.location = location;
	}

	/**
	 * Liefert das Alarmstichwort.
	 *
	 * @return Stichwort
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * Setzt das Alarmstichwort.
	 *
	 * @param keyword Stichwort
	 */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	/**
	 * Liefert weitere Infos.
	 *
	 * @return Infotext
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * Setzt weitere Infos.
	 *
	 * @param info Infotext
	 */
	public void setInfo(String info) {
		this.info = info;
	}

	/**
	 * Liefert den Status {@code planned} oder {@code sent}.
	 *
	 * @return Status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Setzt den Status {@code planned} oder {@code sent}.
	 *
	 * @param status Status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Liefert den Anlagezeitpunkt.
	 *
	 * @return Erstellungszeit
	 */
	public Instant getCreatedAt() {
		return createdAt;
	}

	/**
	 * Setzt den Anlagezeitpunkt.
	 *
	 * @param createdAt Erstellungszeit
	 */
	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Liefert den letzten Änderungszeitpunkt.
	 *
	 * @return Änderungszeit
	 */
	public Instant getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Setzt den letzten Änderungszeitpunkt.
	 *
	 * @param updatedAt Änderungszeit
	 */
	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
