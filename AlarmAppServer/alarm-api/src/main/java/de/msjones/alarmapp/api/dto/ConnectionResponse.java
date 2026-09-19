package de.msjones.alarmapp.api.dto;

import java.util.UUID;

/**
 * Lesedarstellung einer MQTT-Connection.
 *
 * @param id technische ID
 * @param name Anzeigename
 * @param useSsl TLS-Flag
 * @param brokerHost Broker-Hostname
 * @param brokerPort Broker-Port
 * @param brokerPath WebSocket-Pfad
 * @param user MQTT-Benutzername
 * @param password MQTT-Passwort
 * @param mqttTopic MQTT-Topic
 */
public record ConnectionResponse(
		UUID id,
		String name,
		boolean useSsl,
		String brokerHost,
		String brokerPort,
		String brokerPath,
		String user,
		String password,
		String mqttTopic
) {
}
