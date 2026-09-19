package de.msjones.alarmapp.api.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import de.msjones.alarmapp.api.domain.ConnectionEntity;
import org.junit.jupiter.api.Test;

/**
 * Prüft die Broker-URL inklusive Docker-Host-Override.
 */
class MqttBrokerUrlTest {

	@Test
	void buildsWebsocketUrlFromConnection() {
		ConnectionEntity connection = connection("broker.local", "9001", "/mqtt", false);

		assertThat(MqttBrokerUrl.from(connection, "")).isEqualTo("ws://broker.local:9001/mqtt");
	}

	@Test
	void replacesLocalhostWithOverride() {
		ConnectionEntity connection = connection("localhost", "9001", "mqtt", true);

		assertThat(MqttBrokerUrl.from(connection, "mosquitto")).isEqualTo("wss://mosquitto:9001/mqtt");
	}

	@Test
	void keepsRemoteHostEvenIfOverrideIsSet() {
		ConnectionEntity connection = connection("mqtt.example", "9001", "/mqtt", false);

		assertThat(MqttBrokerUrl.from(connection, "mosquitto")).isEqualTo("ws://mqtt.example:9001/mqtt");
	}

	/**
	 * Erzeugt eine Testdaten-Connection.
	 *
	 * @param host Broker-Host
	 * @param port Broker-Port
	 * @param path WebSocket-Pfad
	 * @param useSsl TLS-Flag
	 * @return Connection
	 */
	private static ConnectionEntity connection(String host, String port, String path, boolean useSsl) {
		ConnectionEntity connection = new ConnectionEntity();
		connection.setBrokerHost(host);
		connection.setBrokerPort(port);
		connection.setBrokerPath(path);
		connection.setUseSsl(useSsl);
		return connection;
	}
}
