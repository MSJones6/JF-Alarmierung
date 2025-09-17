package de.msjones.alarmapp.sender;

import org.eclipse.paho.client.mqttv3.*;

public class SendMessage {
    public static void main(String[] args) {
        String broker = "tcp://localhost:1883"; // Adresse des Mosquitto-Brokers
        String clientId = "JavaPublisher";     // Eindeutige Client-ID
        String topic = "JF/Alarm";           // Topic, an das gesendet wird
        String payload = "Hallo vom Java-MQTT-Client!";

        try (IMqttClient client = new MqttClient(broker, clientId)) {
            // Verbindungsoptionen
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            // Mit dem Broker verbinden
            client.connect(options);
            System.out.println("Verbunden mit Broker: " + broker);

            // Nachricht erstellen
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);   // Quality of Service (0, 1 oder 2)
            message.setRetained(false);

            // Nachricht senden
            client.publish(topic, message);
            System.out.println("Nachricht gesendet: " + payload);

            // Verbindung schließen
            client.disconnect();
            System.out.println("Verbindung getrennt.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}

