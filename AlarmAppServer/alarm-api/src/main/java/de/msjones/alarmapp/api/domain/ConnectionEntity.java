package de.msjones.alarmapp.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Persistierte MQTT-Connection mit Broker-Zugangsdaten.
 */
@Entity
@Table(name = "connections")
public class ConnectionEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "use_ssl", nullable = false)
	private boolean useSsl;

	@Column(name = "broker_host", nullable = false)
	private String brokerHost;

	@Column(name = "broker_port", nullable = false, length = 16)
	private String brokerPort;

	@Column(name = "broker_path", nullable = false)
	private String brokerPath;

	@Column(name = "broker_user", nullable = false)
	private String user;

	@Column(nullable = false)
	private String password;

	@Column(name = "mqtt_topic", nullable = false)
	private String mqttTopic;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	/**
	 * Erzeugt eine leere Connection für JPA.
	 */
	public ConnectionEntity() {
	}

	/**
	 * Liefert die technische ID.
	 *
	 * @return UUID der Connection
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Setzt die technische ID.
	 *
	 * @param id UUID der Connection
	 */
	public void setId(UUID id) {
		this.id = id;
	}

	/**
	 * Liefert den Anzeigenamen im Dropdown.
	 *
	 * @return Connection-Name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Anzeigenamen im Dropdown.
	 *
	 * @param name Connection-Name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gibt an, ob TLS verwendet wird.
	 *
	 * @return {@code true} bei SSL
	 */
	public boolean isUseSsl() {
		return useSsl;
	}

	/**
	 * Schaltet TLS ein oder aus.
	 *
	 * @param useSsl {@code true} bei SSL
	 */
	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	/**
	 * Liefert den Broker-Host.
	 *
	 * @return Hostname
	 */
	public String getBrokerHost() {
		return brokerHost;
	}

	/**
	 * Setzt den Broker-Host.
	 *
	 * @param brokerHost Hostname
	 */
	public void setBrokerHost(String brokerHost) {
		this.brokerHost = brokerHost;
	}

	/**
	 * Liefert den Broker-Port.
	 *
	 * @return Port als Text
	 */
	public String getBrokerPort() {
		return brokerPort;
	}

	/**
	 * Setzt den Broker-Port.
	 *
	 * @param brokerPort Port als Text
	 */
	public void setBrokerPort(String brokerPort) {
		this.brokerPort = brokerPort;
	}

	/**
	 * Liefert den WebSocket-Pfad.
	 *
	 * @return Pfad
	 */
	public String getBrokerPath() {
		return brokerPath;
	}

	/**
	 * Setzt den WebSocket-Pfad.
	 *
	 * @param brokerPath Pfad
	 */
	public void setBrokerPath(String brokerPath) {
		this.brokerPath = brokerPath;
	}

	/**
	 * Liefert den MQTT-Benutzernamen.
	 *
	 * @return Benutzername
	 */
	public String getUser() {
		return user;
	}

	/**
	 * Setzt den MQTT-Benutzernamen.
	 *
	 * @param user Benutzername
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * Liefert das MQTT-Passwort.
	 *
	 * @return Passwort
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Setzt das MQTT-Passwort.
	 *
	 * @param password Passwort
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Liefert das MQTT-Topic.
	 *
	 * @return Topic
	 */
	public String getMqttTopic() {
		return mqttTopic;
	}

	/**
	 * Setzt das MQTT-Topic.
	 *
	 * @param mqttTopic Topic
	 */
	public void setMqttTopic(String mqttTopic) {
		this.mqttTopic = mqttTopic;
	}

	/**
	 * Liefert die Sortierposition in der Liste.
	 *
	 * @return Sortierindex
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Setzt die Sortierposition in der Liste.
	 *
	 * @param sortOrder Sortierindex
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
