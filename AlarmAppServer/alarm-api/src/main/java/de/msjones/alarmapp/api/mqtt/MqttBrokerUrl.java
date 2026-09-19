package de.msjones.alarmapp.api.mqtt;

import de.msjones.alarmapp.api.domain.ConnectionEntity;

/**
 * Baut die Broker-URL für den serverseitigen MQTT-Versand.
 */
public final class MqttBrokerUrl {

	private MqttBrokerUrl() {
	}

	/**
	 * Erzeugt die WebSocket-URL analog zum Frontend, ersetzt lokales Hostnamen bei Bedarf.
	 *
	 * @param connection gespeicherte Connection
	 * @param hostOverride optionaler Hostname für Docker, ersetzt {@code localhost}
	 * @return Broker-URL
	 */
	public static String from(ConnectionEntity connection, String hostOverride) {
		String protocol = connection.isUseSsl() ? "wss" : "ws";
		String host = resolveHost(connection.getBrokerHost(), hostOverride);
		String port = connection.getBrokerPort() == null ? "" : connection.getBrokerPort().trim();
		String path = normalizePath(connection.getBrokerPath());
		return protocol + "://" + host + ":" + port + path;
	}

	/**
	 * Ersetzt Loopback-Adressen durch den Override, damit der Container Mosquitto erreicht.
	 *
	 * @param host gespeicherter Host
	 * @param hostOverride optionaler interner Hostname
	 * @return verwendeter Host
	 */
	static String resolveHost(String host, String hostOverride) {
		String trimmed = host == null ? "" : host.trim();
		if (hostOverride != null && !hostOverride.isBlank() && isLoopback(trimmed)) {
			return hostOverride.trim();
		}
		return trimmed;
	}

	/**
	 * Prüft, ob der Host nur innerhalb desselben Rechners erreichbar ist.
	 *
	 * @param host Hostname
	 * @return {@code true} bei localhost
	 */
	private static boolean isLoopback(String host) {
		return "localhost".equalsIgnoreCase(host)
				|| "127.0.0.1".equals(host)
				|| "::1".equals(host);
	}

	/**
	 * Stellt einen führenden Schrägstrich sicher.
	 *
	 * @param path gespeicherter Pfad
	 * @return Pfad oder leer
	 */
	private static String normalizePath(String path) {
		if (path == null || path.isBlank()) {
			return "";
		}
		String trimmed = path.trim();
		return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
	}
}
