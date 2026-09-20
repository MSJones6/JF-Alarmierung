package de.msjones.alarmapp.api.announcement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Prüft den gesprochenen Durchgabetext.
 */
class AnnouncementSpeechTest {

	@Test
	void fromBuildsEinsatzOrtAndInfo() {
		assertThat(AnnouncementSpeech.from("Feueralarm", "Gebäude 3", "Rauchentwicklung"))
				.isEqualTo("Einsatz: Feueralarm. Ort: Gebäude 3. Rauchentwicklung.");
	}

	@Test
	void fromSkipsBlankLocationAndInfo() {
		assertThat(AnnouncementSpeech.from("Warnung", "  ", null)).isEqualTo("Einsatz: Warnung.");
	}

	@Test
	void fromReturnsEmptyWhenEverythingIsBlank() {
		assertThat(AnnouncementSpeech.from(" ", "", null)).isEmpty();
	}

	@Test
	void sanitizeRemovesControlCharacters() {
		assertThat(AnnouncementSpeech.sanitize("Rauch\nim\tKeller")).isEqualTo("Rauch im Keller");
	}
}
