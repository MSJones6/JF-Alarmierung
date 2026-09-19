package de.msjones.alarmapp.api.config;

import de.msjones.alarmapp.api.domain.ConnectionEntity;
import de.msjones.alarmapp.api.domain.KeywordEntity;
import de.msjones.alarmapp.api.repository.ConnectionRepository;
import de.msjones.alarmapp.api.repository.KeywordRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Legt Standard-Connections und Alarmstichworte an, wenn die Datenbank leer ist.
 */
@Component
public class DataInitializer implements ApplicationRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataInitializer.class);

	/**
	 * Standard-Alarmstichworte mit den bisherigen Badge-Farben.
	 *
	 * @param name Anzeigename
	 * @param color Badge-Farbe
	 */
	private record KeywordSeed(String name, String color) {
	}

	private static final List<KeywordSeed> DEFAULT_KEYWORDS = List.of(
			new KeywordSeed("Feueralarm", "#f43f5e"),
			new KeywordSeed("Warnung", "#f97316"),
			new KeywordSeed("Info", "#10b981"),
			new KeywordSeed("Test", "#0ea5e9"),
			new KeywordSeed("Sicherheit", "#8b5cf6")
	);

	private final ConnectionRepository connectionRepository;
	private final KeywordRepository keywordRepository;

	/**
	 * Erzeugt den Initializer.
	 *
	 * @param connectionRepository Connections
	 * @param keywordRepository Stichworte
	 */
	public DataInitializer(
			ConnectionRepository connectionRepository,
			KeywordRepository keywordRepository
	) {
		this.connectionRepository = connectionRepository;
		this.keywordRepository = keywordRepository;
	}

	/**
	 * Füllt leere Tabellen mit den mitgelieferten Standardwerten.
	 *
	 * @param args Startargumente
	 */
	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (keywordRepository.count() == 0) {
			int sortOrder = 0;
			for (KeywordSeed seed : DEFAULT_KEYWORDS) {
				KeywordEntity keyword = new KeywordEntity();
				keyword.setId(UUID.randomUUID());
				keyword.setName(seed.name());
				keyword.setColor(seed.color());
				keyword.setSortOrder(sortOrder++);
				keywordRepository.save(keyword);
			}
			LOGGER.info("Standard-Alarmstichworte wurden angelegt.");
		}
		if (connectionRepository.count() == 0) {
			ConnectionEntity connection = new ConnectionEntity();
			connection.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
			connection.setName("Standard");
			connection.setUseSsl(false);
			connection.setBrokerHost("localhost");
			connection.setBrokerPort("9001");
			connection.setBrokerPath("/mqtt");
			connection.setUser("alarm");
			connection.setPassword("alarm");
			connection.setMqttTopic("JF/Alarm/KB");
			connection.setSortOrder(0);
			connectionRepository.save(connection);
			LOGGER.info("Standard-Connection wurde angelegt.");
		}
	}
}
