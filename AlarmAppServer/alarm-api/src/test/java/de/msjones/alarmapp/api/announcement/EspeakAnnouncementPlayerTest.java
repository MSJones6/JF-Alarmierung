package de.msjones.alarmapp.api.announcement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Prüft Hilfsmethoden des Audio-Players.
 */
class EspeakAnnouncementPlayerTest {

	@Test
	void pulseAvailableRespectsMissingSocketWithoutPulseServer() {
		boolean envSet = System.getenv("PULSE_SERVER") != null && !System.getenv("PULSE_SERVER").isBlank();
		boolean socketPresent = java.nio.file.Files.exists(java.nio.file.Path.of("/run/pulse/native"));
		assertThat(EspeakAnnouncementPlayer.pulseAvailable()).isEqualTo(envSet || socketPresent);
	}
}
