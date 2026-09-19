package de.msjones.alarmapp.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Prüft MQTT-Payload und Fälligkeit geplanter Zeitpunkte.
 */
class AlarmPayloadTest {

	@Test
	void serializesKeywordLocationAndInfo() {
		AlarmEntity alarm = new AlarmEntity();
		alarm.setKeyword("Feueralarm");
		alarm.setLocation("Gebäude 3");
		alarm.setInfo("Rauchentwicklung");

		assertThat(AlarmPayload.from(alarm)).isEqualTo("Feueralarm###Gebäude 3###Rauchentwicklung");
	}

	@Test
	void treatsReachedTimestampsAsDue() {
		LocalDateTime now = LocalDateTime.of(2026, 9, 19, 23, 40, 0);

		assertThat(AlarmPayload.isDue(LocalDateTime.of(2026, 9, 19, 23, 40, 0), now)).isTrue();
		assertThat(AlarmPayload.isDue(LocalDateTime.of(2026, 9, 19, 23, 40, 1), now)).isFalse();
	}

	@Test
	void parsesIsoLocalDateTimeWithOptionalSeconds() {
		assertThat(AlarmPayload.parseScheduledAt("2026-09-19T23:40"))
				.isEqualTo(LocalDateTime.of(2026, 9, 19, 23, 40, 0));
		assertThat(AlarmPayload.parseScheduledAt("2026-09-19T23:40:01"))
				.isEqualTo(LocalDateTime.of(2026, 9, 19, 23, 40, 1));
	}

	@Test
	void rejectsBlankTimestamp() {
		assertThatThrownBy(() -> AlarmPayload.parseScheduledAt("  "))
				.isInstanceOf(java.time.format.DateTimeParseException.class);
	}
}
