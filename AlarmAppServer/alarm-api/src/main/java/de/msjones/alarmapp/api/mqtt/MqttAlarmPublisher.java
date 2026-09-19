package de.msjones.alarmapp.api.mqtt;

import de.msjones.alarmapp.api.domain.ConnectionEntity;

/**
 * Veröffentlicht Alarmnachrichten am MQTT-Broker.
 */
public interface MqttAlarmPublisher {

	/**
	 * Verbindet sich mit der Connection und sendet die Nachricht.
	 *
	 * @param connection Broker-Zugangsdaten
	 * @param payload MQTT-Nutzdaten im Format {@code Stichwort###Ort###Info}
	 */
	void publish(ConnectionEntity connection, String payload);
}
