package de.msjones.alarmapp.api.repository;

import de.msjones.alarmapp.api.domain.KeywordEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Datenzugriff für Alarmstichworte.
 */
public interface KeywordRepository extends JpaRepository<KeywordEntity, UUID> {

	/**
	 * Liefert alle Stichworte in Anzeigereihenfolge.
	 *
	 * @return sortierte Stichworte
	 */
	List<KeywordEntity> findAllByOrderBySortOrderAscNameAsc();

	/**
	 * Sucht ein Stichwort anhand des Namens.
	 *
	 * @param name Anzeigename
	 * @return gefundenes Stichwort
	 */
	Optional<KeywordEntity> findByName(String name);
}
