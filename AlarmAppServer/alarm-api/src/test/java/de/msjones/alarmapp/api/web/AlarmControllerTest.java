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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.msjones.alarmapp.api.dto.AlarmRequest;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.service.AlarmService;
import de.msjones.alarmapp.api.service.AlarmStreamService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Prüft REST und Stream der Alarmierungen.
 */
@WebMvcTest(AlarmController.class)
class AlarmControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AlarmService alarmService;

	@MockitoBean
	private AlarmStreamService alarmStreamService;

	/**
	 * Liefert eine Beispiel-Alarmierung.
	 *
	 * @return Testdaten
	 */
	private AlarmResponse sample() {
		return new AlarmResponse(
				UUID.fromString("00000000-0000-0000-0000-000000000003"),
				"2025-04-24T14:30:15",
				"Standard",
				"Turnhalle",
				"Feueralarm",
				"Rauchentwicklung",
				"planned"
		);
	}

	@Test
	void listsAlarms() throws Exception {
		when(alarmService.findAll()).thenReturn(List.of(sample()));

		mockMvc.perform(get("/api/alarms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].location").value("Turnhalle"))
				.andExpect(jsonPath("$[0].connection").value("Standard"));
	}

	@Test
	void createsAlarm() throws Exception {
		when(alarmService.create(any(AlarmRequest.class))).thenReturn(sample());

		mockMvc.perform(post("/api/alarms")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
									"scheduledAt": "2025-04-24T14:30:15",
									"connection": "Standard",
									"location": "Turnhalle",
									"keyword": "Feueralarm",
									"info": "Rauchentwicklung",
									"status": "planned"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.keyword").value("Feueralarm"));
	}

	@Test
	void rejectsInvalidStatus() throws Exception {
		mockMvc.perform(post("/api/alarms")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
									"scheduledAt": "2025-04-24T14:30:15",
									"connection": "Standard",
									"location": "Turnhalle",
									"keyword": "Feueralarm",
									"status": "unknown"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updatesAlarm() throws Exception {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000003");
		when(alarmService.update(eq(id), any(AlarmRequest.class))).thenReturn(sample());

		mockMvc.perform(put("/api/alarms/" + id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
									"scheduledAt": "2025-04-24T14:30:15",
									"connection": "Standard",
									"location": "Turnhalle",
									"keyword": "Feueralarm",
									"info": "Rauchentwicklung",
									"status": "sent"
								}
								"""))
				.andExpect(status().isOk());
	}

	@Test
	void deletesAlarm() throws Exception {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000003");

		mockMvc.perform(delete("/api/alarms/" + id))
				.andExpect(status().isNoContent());

		verify(alarmService).delete(eq(id));
	}

	@Test
	void opensAlarmStream() throws Exception {
		when(alarmStreamService.subscribe()).thenReturn(new SseEmitter(0L));

		mockMvc.perform(get("/api/alarms/stream").accept(MediaType.TEXT_EVENT_STREAM))
				.andExpect(request().asyncStarted());
	}
}
