package de.msjones.alarmapp.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Startklasse der REST-API für Connections, Alarmstichworte und Alarmierungen.
 */
@SpringBootApplication
public class AlarmApiApplication {

	/**
	 * Startet die Spring-Boot-Anwendung.
	 *
	 * @param args Kommandozeilenargumente
	 */
	public static void main(String[] args) {
		SpringApplication.run(AlarmApiApplication.class, args);
	}
}
