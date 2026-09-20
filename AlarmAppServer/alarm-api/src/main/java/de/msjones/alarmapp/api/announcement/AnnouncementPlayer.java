package de.msjones.alarmapp.api.announcement;

/**
 * Spielt Gong und gesprochenen Alarmtext auf dem Server.
 */
public interface AnnouncementPlayer {

	/**
	 * Gibt zuerst den Gong und danach den Text aus.
	 *
	 * @param gongWav Gong als WAV
	 * @param speechText gesprochener Text
	 */
	void playGongAndSpeech(byte[] gongWav, String speechText);
}
