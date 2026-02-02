package de.msjones.alarmapp.sender;

import org.eclipse.paho.client.mqttv3.*;

public class SendMessage {
    public static void main(String[] args) {
        String broker = "tcp://localhost:1883"; // Adresse des Mosquitto-Brokers
        String clientId = "JavaPublisher";     // Eindeutige Client-ID
        String topic = "JF/Alarm";           // Topic, an das gesendet wird
        String payload = "Brand 3###Hauptstrasse 12, 66346 Püttlingen (Köllerbach)###Keine Person in Wohnung";

        // Authentication credentials - use environment variables or defaults
        String username = "alarm";
        String password = "alarm";

        // Fallback to defaults if environment variables are not set
        if (username == null) {
            System.err.println("Warning: MQTT_USERNAME is not set. Can't establish connection.");
        }
        if (password == null) {
            System.err.println("Warning: MQTT_PASSWORD is not set. Can't establish connection.");
        }

        try (IMqttClient client = new MqttClient(broker, clientId)) {
            // Verbindungsoptionen
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setUserName(username);
            options.setPassword(password.toCharArray());

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

