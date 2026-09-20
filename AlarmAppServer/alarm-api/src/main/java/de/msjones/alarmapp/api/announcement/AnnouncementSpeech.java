package de.msjones.alarmapp.api.announcement;

/**
 * Baut den gesprochenen Text der Server-Durchsage.
 */
public final class AnnouncementSpeech {

	/**
	 * Verhindert die Instanziierung.
	 */
	private AnnouncementSpeech() {
	}

	/**
	 * Formatiert Stichwort, Ort und Infos für die Sprachausgabe.
	 *
	 * @param keyword Einsatzstichwort
	 * @param location Einsatzort
	 * @param info weitere Angaben
	 * @return gesprochener Text ohne Gong
	 */
	public static String from(String keyword, String location, String info) {
		StringBuilder spoken = new StringBuilder();
		appendSegment(spoken, "Einsatz: " + sanitize(keyword));
		appendSegment(spoken, "Ort: " + sanitize(location));
		appendSegment(spoken, sanitize(info));
		if (spoken.isEmpty()) {
			return "";
		}
		if (spoken.charAt(spoken.length() - 1) != '.') {
			spoken.append('.');
		}
		return spoken.toString();
	}

	/**
	 * Hängt ein Segment mit Punkt-Pause an, sofern es Inhalt hat.
	 *
	 * @param spoken bisheriger Text
	 * @param segment nächster Satzteil
	 */
	private static void appendSegment(StringBuilder spoken, String segment) {
		String trimmed = segment.trim();
		if (trimmed.isEmpty() || trimmed.endsWith(":")) {
			return;
		}
		if (!spoken.isEmpty()) {
			spoken.append(". ");
		}
		spoken.append(trimmed);
	}

	/**
	 * Entfernt Steuerzeichen und begrenzt die Länge für die Sprachausgabe.
	 *
	 * @param value Rohtext
	 * @return bereinigter Text
	 */
	static String sanitize(String value) {
		if (value == null) {
			return "";
		}
		String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\t]]+", " ").replaceAll("\\s+", " ").trim();
		if (cleaned.length() > 400) {
			return cleaned.substring(0, 400);
		}
		return cleaned;
	}
}
