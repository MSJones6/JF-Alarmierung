-- Speichert den geplanten Alarmzeitpunkt als echtes Datum/Zeit statt als Text.
-- Bestehende ISO-Werte ohne Sekunden werden vor dem Cast auf Sekunden ergänzt.
ALTER TABLE alarms
	ALTER COLUMN scheduled_at TYPE TIMESTAMP WITHOUT TIME ZONE
	USING (
		CASE
			WHEN length(btrim(scheduled_at)) = 16 THEN (btrim(scheduled_at) || ':00')::timestamp
			ELSE btrim(scheduled_at)::timestamp
		END
	);

DROP INDEX IF EXISTS idx_alarms_scheduled_at;

-- Beschleunigt die Suche nach fälligen geplanten Alarmierungen.
CREATE INDEX idx_alarms_status_scheduled_at ON alarms (status, scheduled_at);
