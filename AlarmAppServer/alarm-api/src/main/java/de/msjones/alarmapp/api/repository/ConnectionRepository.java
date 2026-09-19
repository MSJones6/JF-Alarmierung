package de.msjones.alarmapp.api.repository;

import de.msjones.alarmapp.api.domain.ConnectionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenzugriff für MQTT-Connections.
 */
public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID> {

	/**
	 * Liefert alle Connections in Anzeigereihenfolge.
	 *
	 * @return sortierte Connections
	 */
	List<ConnectionEntity> findAllByOrderBySortOrderAscNameAsc();

	/**
	 * Sucht eine Connection anhand des Anzeigenamens.
	 *
	 * @param name Anzeigename
	 * @return gefundene Connection
	 */
	Optional<ConnectionEntity> findByName(String name);
}
