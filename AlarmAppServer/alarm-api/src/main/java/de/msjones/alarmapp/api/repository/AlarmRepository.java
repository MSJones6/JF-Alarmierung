package de.msjones.alarmapp.api.repository;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

	/**
	 * Liefert Alarmierungen eines Status, zuerst nach Zeitpunkt.
	 *
	 * @param status {@code planned} oder {@code sent}
	 * @return sortierte Alarmierungen
	 */
	List<AlarmEntity> findByStatusOrderByScheduledAtAsc(String status);

	/**
	 * Liefert fällige Alarmierungen eines Status bis einschließlich {@code scheduledAt}.
	 *
	 * @param status {@code planned} oder {@code sent}
	 * @param scheduledAt spätester noch fälliger Zeitpunkt
	 * @return sortierte fällige Alarmierungen
	 */
	List<AlarmEntity> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
			String status,
			LocalDateTime scheduledAt
	);

	/**
	 * Liefert die zeitlich nächste Alarmierung eines Status.
	 *
	 * @param status {@code planned} oder {@code sent}
	 * @return früheste Alarmierung oder leer
	 */
	Optional<AlarmEntity> findFirstByStatusOrderByScheduledAtAsc(String status);
}
