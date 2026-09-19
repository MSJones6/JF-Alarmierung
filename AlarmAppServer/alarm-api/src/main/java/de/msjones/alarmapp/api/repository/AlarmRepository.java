package de.msjones.alarmapp.api.repository;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenzugriff für geplante und bereits alarmierte Einträge.
 */
public interface AlarmRepository extends JpaRepository<AlarmEntity, UUID> {

	/**
	 * Liefert alle Alarmierungen, zuerst nach Zeitpunkt.
	 *
	 * @return sortierte Alarmierungen
	 */
	List<AlarmEntity> findAllByOrderByScheduledAtAsc();
}
