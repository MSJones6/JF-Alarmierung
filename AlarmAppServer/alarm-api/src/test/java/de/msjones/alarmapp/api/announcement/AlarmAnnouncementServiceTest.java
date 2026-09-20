package de.msjones.alarmapp.api.announcement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prüft das Ein- und Ausschalten der Server-Durchsage.
 */
@ExtendWith(MockitoExtension.class)
class AlarmAnnouncementServiceTest {

	@Mock
	private AnnouncementPlayer player;

	@Test
	void announceAsyncDoesNothingWhenDisabled() {
		AlarmAnnouncementService service = new AlarmAnnouncementService(false, player, Runnable::run);
		AlarmEntity alarm = alarm("Feueralarm", "Gebäude 3", "Rauch");

		service.announceAsync(alarm);

		verify(player, never()).playGongAndSpeech(any(), any());
	}

	@Test
	void announceAsyncPlaysGongAndSpeechWhenEnabled() {
		AlarmAnnouncementService service = new AlarmAnnouncementService(true, player, Runnable::run);
		AlarmEntity alarm = alarm("Feueralarm", "Gebäude 3", "Rauch");

		service.announceAsync(alarm);

		verify(player).playGongAndSpeech(
				any(byte[].class),
				eq("Einsatz: Feueralarm. Ort: Gebäude 3. Rauch.")
		);
	}

	@Test
	void announceAsyncSkipsEmptyAlarms() {
		AlarmAnnouncementService service = new AlarmAnnouncementService(true, player, Runnable::run);

		service.announceAsync(alarm(" ", "", null));

		verify(player, never()).playGongAndSpeech(any(), any());
	}

	/**
	 * Erzeugt eine Testdaten-Alarmierung.
	 *
	 * @param keyword Stichwort
	 * @param location Ort
	 * @param info Infos
	 * @return Alarm
	 */
	private static AlarmEntity alarm(String keyword, String location, String info) {
		AlarmEntity alarm = new AlarmEntity();
		alarm.setKeyword(keyword);
		alarm.setLocation(location);
		alarm.setInfo(info);
		return alarm;
	}
}
