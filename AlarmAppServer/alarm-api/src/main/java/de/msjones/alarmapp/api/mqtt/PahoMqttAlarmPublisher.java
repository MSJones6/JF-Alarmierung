package de.msjones.alarmapp.api.mqtt;

import de.msjones.alarmapp.api.domain.ConnectionEntity;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sendet Alarmnachrichten per Eclipse Paho über denselben WebSocket-Broker wie das Frontend.
 */
@Component
public class PahoMqttAlarmPublisher implements MqttAlarmPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(PahoMqttAlarmPublisher.class);

	private final String hostOverride;
	private final int connectTimeoutSeconds;

	/**
	 * Erzeugt den Publisher mit Docker-Host-Override und Timeout.
	 *
	 * @param hostOverride ersetzt localhost, wenn die API im Container läuft
	 * @param connectTimeoutSeconds Verbindungszeitlimit
	 */
	public PahoMqttAlarmPublisher(
			@Value("${alarm.mqtt.host-override:}") String hostOverride,
			@Value("${alarm.mqtt.connect-timeout-seconds:10}") int connectTimeoutSeconds
	) {
		this.hostOverride = hostOverride;
		this.connectTimeoutSeconds = connectTimeoutSeconds;
	}

	/**
	 * Verbindet sich kurz, veröffentlicht mit QoS 1 und trennt danach wieder.
	 *
	 * @param connection Broker-Zugangsdaten
	 * @param payload MQTT-Nutzdaten
	 */
	@Override
	public void publish(ConnectionEntity connection, String payload) {
		String brokerUri = MqttBrokerUrl.from(connection, hostOverride);
		String clientId = "alarm-api-" + UUID.randomUUID();
		MqttClient client;
		try {
			client = new MqttClient(brokerUri, clientId, new MemoryPersistence());
		} catch (MqttException exception) {
			throw new IllegalStateException(
					"MQTT-Client für " + brokerUri + " konnte nicht erzeugt werden: " + exception.getMessage(),
					exception
			);
		}
		try {
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(false);
			options.setCleanSession(true);
			options.setConnectionTimeout(connectTimeoutSeconds);
			if (connection.getUser() != null && !connection.getUser().isBlank()) {
				options.setUserName(connection.getUser());
			}
			if (connection.getPassword() != null && !connection.getPassword().isBlank()) {
				options.setPassword(connection.getPassword().toCharArray());
			}
			client.connect(options);
			MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
			message.setQos(1);
			message.setRetained(false);
			client.publish(connection.getMqttTopic(), message);
			client.disconnect();
			LOGGER.info("Geplante Alarmierung wurde an {} gesendet.", connection.getMqttTopic());
		} catch (MqttException exception) {
			throw new IllegalStateException(
					"MQTT-Versand an " + brokerUri + " ist fehlgeschlagen: " + exception.getMessage(),
					exception
			);
		} finally {
			try {
				client.close();
			} catch (MqttException exception) {
				LOGGER.debug("MQTT-Client konnte nicht geschlossen werden: {}", exception.getMessage());
			}
		}
	}
}
