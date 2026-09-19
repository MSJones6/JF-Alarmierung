-- Speichert die Badge-Farbe eines Alarmstichworts als Hex-Wert.
ALTER TABLE keywords
	ADD COLUMN color VARCHAR(7) NOT NULL DEFAULT '#64748b';

UPDATE keywords SET color = '#f43f5e' WHERE name = 'Feueralarm';
UPDATE keywords SET color = '#f97316' WHERE name = 'Warnung';
UPDATE keywords SET color = '#10b981' WHERE name = 'Info';
UPDATE keywords SET color = '#0ea5e9' WHERE name = 'Test';
UPDATE keywords SET color = '#8b5cf6' WHERE name = 'Sicherheit';
