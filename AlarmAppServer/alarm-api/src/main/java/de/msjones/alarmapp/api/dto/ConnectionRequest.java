package de.msjones.alarmapp.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * Schreibdaten einer MQTT-Connection.
 *
 * @param id optionale ID, wird beim Anlegen erzeugt
 * @param name Anzeigename im Dropdown
 * @param useSsl TLS-Flag
 * @param brokerHost Broker-Hostname
 * @param brokerPort Broker-Port
 * @param brokerPath WebSocket-Pfad
 * @param user MQTT-Benutzername
 * @param password MQTT-Passwort
 * @param mqttTopic MQTT-Topic
 */
public record ConnectionRequest(
		UUID id,
		@NotBlank String name,
		boolean useSsl,
		@NotBlank String brokerHost,
		@NotBlank String brokerPort,
		String brokerPath,
		String user,
		String password,
		@NotBlank String mqttTopic
) {
}
