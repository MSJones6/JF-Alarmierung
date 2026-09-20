package de.msjones.alarmapp.api.announcement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spielt Gong und Sprache über PulseAudio/{@code paplay} oder ALSA/{@code aplay}.
 */
@Component
public class EspeakAnnouncementPlayer implements AnnouncementPlayer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EspeakAnnouncementPlayer.class);
	private static final long TIMEOUT_SECONDS = 45;
	private static final Path PULSE_SOCKET = Path.of("/run/pulse/native");

	private final String voice;
	private final int speechRate;
	private final String alsaDevice;

	/**
	 * Erzeugt den Player mit Sprach- und Ausgabeeinstellungen.
	 *
	 * @param voice espeak-ng-Stimme, typisch {@code de}
	 * @param speechRate Wörter pro Minute
	 * @param alsaDevice optionales ALSA-Gerät, falls PulseAudio fehlt
	 */
	public EspeakAnnouncementPlayer(
			@Value("${alarm.announcement.voice:de}") String voice,
			@Value("${alarm.announcement.speech-rate:130}") int speechRate,
			@Value("${alarm.announcement.alsa-device:}") String alsaDevice
	) {
		this.voice = voice;
		this.speechRate = speechRate;
		this.alsaDevice = alsaDevice == null ? "" : alsaDevice.trim();
	}

	@Override
	public void playGongAndSpeech(byte[] gongWav, String speechText) {
		Path directory = null;
		try {
			directory = Files.createTempDirectory("alarm-announcement-");
			Path gongFile = directory.resolve("gong.wav");
			Path speechFile = directory.resolve("speech.wav");
			Files.write(gongFile, gongWav);
			playWav(gongFile);
			if (speechText != null && !speechText.isBlank()) {
				run(
						"espeak-ng",
						"-v",
						voice,
						"-s",
						Integer.toString(speechRate),
						"-w",
						speechFile.toAbsolutePath().toString(),
						speechText
				);
				playWav(speechFile);
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Durchsage wurde unterbrochen.");
		} catch (IOException | RuntimeException exception) {
			LOGGER.warn("Durchsage konnte nicht ausgegeben werden: {}", exception.getMessage());
		} finally {
			deleteQuietly(directory);
		}
	}

	/**
	 * Spielt eine WAV-Datei über PulseAudio, sonst über ALSA.
	 *
	 * @param wavDate Datei
	 * @throws IOException bei Startfehlern
	 * @throws InterruptedException bei Abbruch
	 */
	private void playWav(Path wavDate) throws IOException, InterruptedException {
		if (pulseAvailable()) {
			try {
				run("paplay", wavDate.toAbsolutePath().toString());
				return;
			} catch (IOException | RuntimeException exception) {
				LOGGER.warn("paplay fehlgeschlagen, versuche aplay: {}", exception.getMessage());
			}
		}
		List<String> command = new ArrayList<>();
		command.add("aplay");
		if (!alsaDevice.isEmpty()) {
			command.add("-D");
			command.add(alsaDevice);
		}
		command.add(wavDate.toAbsolutePath().toString());
		run(command.toArray(String[]::new));
	}

	/**
	 * Prüft, ob der Pulse-/PipeWire-Socket des Hosts gemountet ist.
	 *
	 * @return {@code true}, wenn {@code paplay} bevorzugt werden soll
	 */
	static boolean pulseAvailable() {
		String server = System.getenv("PULSE_SERVER");
		return (server != null && !server.isBlank()) || Files.exists(PULSE_SOCKET);
	}

	/**
	 * Startet ein Kommando ohne Shell und wartet auf das Ende.
	 *
	 * @param command Programm und Argumente
	 * @throws IOException bei Startfehlern
	 * @throws InterruptedException bei Abbruch
	 */
	private static void run(String... command) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(command)
				.redirectErrorStream(true)
				.start();
		byte[] output = process.getInputStream().readAllBytes();
		boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
			throw new IllegalStateException("Kommando hat das Zeitlimit überschritten: " + command[0]);
		}
		String text = new String(output, StandardCharsets.UTF_8).trim();
		if (process.exitValue() != 0) {
			throw new IllegalStateException(
					"Kommando " + command[0] + " endete mit Code " + process.exitValue()
							+ (text.isEmpty() ? "" : ": " + text)
			);
		}
		if (!text.isEmpty()) {
			LOGGER.info("{}: {}", command[0], text);
		}
	}

	/**
	 * Löscht temporäre Dateien, Fehler werden ignoriert.
	 *
	 * @param directory Arbeitsverzeichnis oder {@code null}
	 */
	private static void deleteQuietly(Path directory) {
		if (directory == null) {
			return;
		}
		try (var paths = Files.walk(directory)) {
			paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
					// temporäre Dateien dürfen liegen bleiben
				}
			});
		} catch (IOException ignored) {
			// temporäre Dateien dürfen liegen bleiben
		}
	}
}
