package de.msjones.alarmapp.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Persistiertes Alarmstichwort für die Auswahlliste.
 */
@Entity
@Table(name = "keywords")
public class KeywordEntity {

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	/**
	 * Erzeugt ein leeres Stichwort für JPA.
	 */
	public KeywordEntity() {
	}

	/**
	 * Liefert die technische ID.
	 *
	 * @return UUID des Stichworts
	 */
	public UUID getId() {
		return id;
	}

	/**
	 * Setzt die technische ID.
	 *
	 * @param id UUID des Stichworts
	 */
	public void setId(UUID id) {
		this.id = id;
	}

	/**
	 * Liefert den Anzeigenamen.
	 *
	 * @return Stichwort
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Anzeigenamen.
	 *
	 * @param name Stichwort
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Liefert die Sortierposition.
	 *
	 * @return Sortierindex
	 */
	public int getSortOrder() {
		return sortOrder;
	}

	/**
	 * Setzt die Sortierposition.
	 *
	 * @param sortOrder Sortierindex
	 */
	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
