package de.msjones.alarmapp.sender;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class SendMessage {

    private final static String QUEUE_NAME = "Alarm";

    public static void main(String[] args) {
        // ConnectionFactory konfigurieren
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // RabbitMQ-Server-Adresse
        factory.setPort(5672);
        factory.setUsername("user"); // Standard-Benutzername
        factory.setPassword("password"); // Standard-Passwort

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Queue deklarieren (falls sie noch nicht existiert)
//            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            String message = "Alarm TLF!";
            // Nachricht an die Queue senden
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes("UTF-8"));
            System.out.println(" [x] Gesendet: '" + message + "'");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

