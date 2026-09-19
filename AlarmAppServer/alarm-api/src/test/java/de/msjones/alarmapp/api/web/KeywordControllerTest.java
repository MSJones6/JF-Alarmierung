package de.msjones.alarmapp.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.msjones.alarmapp.api.dto.KeywordRequest;
import de.msjones.alarmapp.api.dto.KeywordResponse;
import de.msjones.alarmapp.api.service.KeywordService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prüft die REST-Schnittstelle der Alarmstichworte.
 */
@WebMvcTest(KeywordController.class)
class KeywordControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KeywordService keywordService;

	@Test
	void listsKeywords() throws Exception {
		when(keywordService.findAll())
				.thenReturn(List.of(new KeywordResponse(UUID.randomUUID(), "Feueralarm", "#f43f5e")));

		mockMvc.perform(get("/api/keywords"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Feueralarm"))
				.andExpect(jsonPath("$[0].color").value("#f43f5e"));
	}

	@Test
	void createsKeyword() throws Exception {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");
		when(keywordService.create(any(KeywordRequest.class)))
				.thenReturn(new KeywordResponse(id, "Brand", "#dc2626"));

		mockMvc.perform(post("/api/keywords")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Brand\",\"color\":\"#dc2626\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Brand"))
				.andExpect(jsonPath("$.color").value("#dc2626"));
	}

	@Test
	void replacesKeywords() throws Exception {
		when(keywordService.replaceAll(any()))
				.thenReturn(List.of(new KeywordResponse(UUID.randomUUID(), "Info", "#10b981")));

		mockMvc.perform(put("/api/keywords")
						.contentType(MediaType.APPLICATION_JSON)
						.content("[{\"name\":\"Info\",\"color\":\"#10b981\"}]"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Info"))
				.andExpect(jsonPath("$[0].color").value("#10b981"));
	}

	@Test
	void deletesKeyword() throws Exception {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");

		mockMvc.perform(delete("/api/keywords/" + id))
				.andExpect(status().isNoContent());

		verify(keywordService).delete(eq(id));
	}
}
