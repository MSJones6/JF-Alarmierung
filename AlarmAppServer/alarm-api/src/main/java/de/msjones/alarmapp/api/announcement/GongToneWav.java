package de.msjones.alarmapp.api.announcement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Erzeugt einen kurzen zweifachen Aufmerksamkeitston als WAV.
 */
public final class GongToneWav {

	private static final int SAMPLE_RATE = 22_050;
	private static final double FIRST_HZ = 784;
	private static final double SECOND_HZ = 523;
	private static final int FIRST_MS = 350;
	private static final int GAP_MS = 90;
	private static final int SECOND_MS = 420;

	/**
	 * Verhindert die Instanziierung.
	 */
	private GongToneWav() {
	}

	/**
	 * Liefert eine WAV-Datei mit zwei nacheinander erklingenden Tönen.
	 *
	 * @return PCM-WAV-Bytes
	 */
	public static byte[] create() {
		short[] samples = concatenate(
				tone(FIRST_HZ, FIRST_MS),
				silence(GAP_MS),
				tone(SECOND_HZ, SECOND_MS)
		);
		try {
			return toWav(samples);
		} catch (IOException exception) {
			throw new IllegalStateException("Gong-WAV konnte nicht erzeugt werden.", exception);
		}
	}

	/**
	 * Erzeugt einen Sinuston mit Ein- und Ausblenden.
	 *
	 * @param frequencyHz Frequenz
	 * @param durationMs Dauer
	 * @return Samples
	 */
	private static short[] tone(double frequencyHz, int durationMs) {
		int count = samplesFor(durationMs);
		short[] samples = new short[count];
		int fade = Math.max(1, SAMPLE_RATE / 80);
		for (int index = 0; index < count; index++) {
			double envelope = 1.0;
			if (index < fade) {
				envelope = index / (double) fade;
			} else if (index > count - fade) {
				envelope = (count - index) / (double) fade;
			}
			double value = Math.sin(2 * Math.PI * frequencyHz * index / SAMPLE_RATE) * envelope;
			samples[index] = (short) Math.round(value * 24_000);
		}
		return samples;
	}

	/**
	 * Erzeugt Stille.
	 *
	 * @param durationMs Dauer
	 * @return Samples
	 */
	private static short[] silence(int durationMs) {
		return new short[samplesFor(durationMs)];
	}

	/**
	 * Rechnet Millisekunden in Sampleanzahl um.
	 *
	 * @param durationMs Dauer
	 * @return Anzahl Samples
	 */
	private static int samplesFor(int durationMs) {
		return Math.max(1, SAMPLE_RATE * durationMs / 1000);
	}

	/**
	 * Hängt Sample-Folgen aneinander.
	 *
	 * @param parts Teile
	 * @return Gesamtsignal
	 */
	private static short[] concatenate(short[]... parts) {
		int total = 0;
		for (short[] part : parts) {
			total += part.length;
		}
		short[] merged = new short[total];
		int offset = 0;
		for (short[] part : parts) {
			System.arraycopy(part, 0, merged, offset, part.length);
			offset += part.length;
		}
		return merged;
	}

	/**
	 * Packt 16-Bit-Mono-PCM in einen WAV-Container.
	 *
	 * @param samples PCM-Samples
	 * @return Dateiinhalt
	 * @throws IOException bei Schreibfehlern
	 */
	private static byte[] toWav(short[] samples) throws IOException {
		int dataSize = samples.length * 2;
		ByteArrayOutputStream output = new ByteArrayOutputStream(44 + dataSize);
		output.write("RIFF".getBytes());
		writeLittleInt(output, 36 + dataSize);
		output.write("WAVE".getBytes());
		output.write("fmt ".getBytes());
		writeLittleInt(output, 16);
		writeLittleShort(output, (short) 1);
		writeLittleShort(output, (short) 1);
		writeLittleInt(output, SAMPLE_RATE);
		writeLittleInt(output, SAMPLE_RATE * 2);
		writeLittleShort(output, (short) 2);
		writeLittleShort(output, (short) 16);
		output.write("data".getBytes());
		writeLittleInt(output, dataSize);
		ByteBuffer buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN);
		for (short sample : samples) {
			buffer.putShort(sample);
		}
		output.write(buffer.array());
		return output.toByteArray();
	}

	/**
	 * Schreibt eine 32-Bit-Zahl little-endian.
	 *
	 * @param output Ziel
	 * @param value Wert
	 * @throws IOException bei Schreibfehlern
	 */
	private static void writeLittleInt(ByteArrayOutputStream output, int value) throws IOException {
		output.write(value & 0xff);
		output.write((value >> 8) & 0xff);
		output.write((value >> 16) & 0xff);
		output.write((value >> 24) & 0xff);
	}

	/**
	 * Schreibt eine 16-Bit-Zahl little-endian.
	 *
	 * @param output Ziel
	 * @param value Wert
	 * @throws IOException bei Schreibfehlern
	 */
	private static void writeLittleShort(ByteArrayOutputStream output, short value) throws IOException {
		output.write(value & 0xff);
		output.write((value >> 8) & 0xff);
	}
}
