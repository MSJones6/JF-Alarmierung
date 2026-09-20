package de.msjones.alarmapp.api.announcement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Prüft den erzeugten Gong als gültige WAV-Datei.
 */
class GongToneWavTest {

	@Test
	void createWritesRiffWaveHeaderAndAudioData() {
		byte[] wav = GongToneWav.create();
		assertThat(new String(wav, 0, 4)).isEqualTo("RIFF");
		assertThat(new String(wav, 8, 4)).isEqualTo("WAVE");
		assertThat(wav.length).isGreaterThan(44);
	}
}
