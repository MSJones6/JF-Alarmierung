package de.msjones.alarmapp.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.msjones.alarmapp.api.domain.KeywordEntity;
import de.msjones.alarmapp.api.dto.KeywordRequest;
import de.msjones.alarmapp.api.dto.KeywordResponse;
import de.msjones.alarmapp.api.repository.KeywordRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prüft das Anlegen und Ersetzen von Alarmstichworten inklusive Farbe.
 */
@ExtendWith(MockitoExtension.class)
class KeywordServiceTest {

	@Mock
	private KeywordRepository keywordRepository;

	private KeywordService keywordService;

	/**
	 * Erzeugt den Dienst mit Mock-Abhängigkeiten.
	 */
	@BeforeEach
	void setUp() {
		keywordService = new KeywordService(keywordRepository);
	}

	@Test
	void createStoresNormalizedColor() {
		when(keywordRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of());
		when(keywordRepository.save(any(KeywordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		KeywordResponse created = keywordService.create(new KeywordRequest("Brand", "#DC2626"));

		assertThat(created.name()).isEqualTo("Brand");
		assertThat(created.color()).isEqualTo("#dc2626");
		ArgumentCaptor<KeywordEntity> captor = ArgumentCaptor.forClass(KeywordEntity.class);
		verify(keywordRepository).save(captor.capture());
		assertThat(captor.getValue().getColor()).isEqualTo("#dc2626");
	}

	@Test
	void createUsesDefaultColorWhenMissing() {
		when(keywordRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of());
		when(keywordRepository.save(any(KeywordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		KeywordResponse created = keywordService.create(new KeywordRequest("Info", null));

		assertThat(created.color()).isEqualTo(EntityMapper.DEFAULT_KEYWORD_COLOR);
	}

	@Test
	void updateChangesKeywordColor() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000004");
		KeywordEntity entity = new KeywordEntity();
		entity.setId(id);
		entity.setName("Warnung");
		entity.setColor("#f97316");
		entity.setSortOrder(1);
		when(keywordRepository.findById(id)).thenReturn(Optional.of(entity));
		when(keywordRepository.save(any(KeywordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		KeywordResponse updated = keywordService.update(id, new KeywordRequest("Warnung", "#ea580c"));

		assertThat(updated.color()).isEqualTo("#ea580c");
	}

	@Test
	void replaceAllKeepsColorsInOrder() {
		when(keywordRepository.save(any(KeywordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<KeywordResponse> saved = keywordService.replaceAll(List.of(
				new KeywordRequest("Feueralarm", "#f43f5e"),
				new KeywordRequest("  ", "#000000"),
				new KeywordRequest("Info", "#10b981")
		));

		assertThat(saved).extracting(KeywordResponse::name).containsExactly("Feueralarm", "Info");
		assertThat(saved).extracting(KeywordResponse::color).containsExactly("#f43f5e", "#10b981");
	}
}
