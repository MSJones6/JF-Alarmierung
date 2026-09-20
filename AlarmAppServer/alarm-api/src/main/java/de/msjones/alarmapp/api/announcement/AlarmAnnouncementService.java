package de.msjones.alarmapp.api.announcement;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Startet die Server-Durchsage parallel zum MQTT-Versand, sofern sie eingeschaltet ist.
 */
@Service
public class AlarmAnnouncementService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmAnnouncementService.class);

	private final boolean enabled;
	private final AnnouncementPlayer player;
	private final Executor executor;

	/**
	 * Erzeugt den Dienst mit Hintergrund-Thread für die Sprachausgabe.
	 *
	 * @param enabled ob Durchsagen aktiv sind
	 * @param player Gong- und Sprachausgabe
	 */
	@Autowired
	public AlarmAnnouncementService(
			@Value("${alarm.announcement.enabled:false}") boolean enabled,
			AnnouncementPlayer player
	) {
		this(enabled, player, Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "alarm-announcement");
			thread.setDaemon(true);
			return thread;
		}));
	}

	/**
	 * Erzeugt den Dienst mit einem vorgegebenen Executor, vor allem für Tests.
	 *
	 * @param enabled ob Durchsagen aktiv sind
	 * @param player Gong- und Sprachausgabe
	 * @param executor Ausführung der Durchsage
	 */
	AlarmAnnouncementService(boolean enabled, AnnouncementPlayer player, Executor executor) {
		this.enabled = enabled;
		this.player = player;
		this.executor = executor;
	}

	/**
	 * Gibt Gong und Text im Hintergrund aus, ohne den MQTT-Versand zu blockieren.
	 *
	 * @param alarm ausgelöste Alarmierung
	 */
	public void announceAsync(AlarmEntity alarm) {
		if (!enabled) {
			return;
		}
		String speech = AnnouncementSpeech.from(alarm.getKeyword(), alarm.getLocation(), alarm.getInfo());
		if (speech.isBlank()) {
			LOGGER.info("Durchsage übersprungen, weil Stichwort, Ort und Infos leer sind.");
			return;
		}
		executor.execute(() -> play(speech));
	}

	/**
	 * Spielt Gong und Sprache und fängt Fehler ab.
	 *
	 * @param speech gesprochener Text
	 */
	private void play(String speech) {
		try {
			LOGGER.info("Durchsage: {}", speech);
			player.playGongAndSpeech(GongToneWav.create(), speech);
		} catch (RuntimeException exception) {
			LOGGER.warn("Durchsage fehlgeschlagen: {}", exception.getMessage());
		}
	}
}
