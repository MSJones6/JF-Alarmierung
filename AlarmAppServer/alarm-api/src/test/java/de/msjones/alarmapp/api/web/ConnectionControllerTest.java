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

import de.msjones.alarmapp.api.dto.ConnectionRequest;
import de.msjones.alarmapp.api.dto.ConnectionResponse;
import de.msjones.alarmapp.api.service.ConnectionService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prüft die REST-Schnittstelle der Connections.
 */
@WebMvcTest(ConnectionController.class)
class ConnectionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ConnectionService connectionService;

	/**
	 * Liefert eine Beispiel-Connection.
	 *
	 * @return Testdaten
	 */
	private ConnectionResponse sample() {
		return new ConnectionResponse(
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				"Standard",
				false,
				"localhost",
				"9001",
				"/mqtt",
				"alarm",
				"alarm",
				"JF/Alarm/KB"
		);
	}

	@Test
	void listsConnections() throws Exception {
		when(connectionService.findAll()).thenReturn(List.of(sample()));

		mockMvc.perform(get("/api/connections"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Standard"))
				.andExpect(jsonPath("$[0].brokerHost").value("localhost"));
	}

	@Test
	void createsConnection() throws Exception {
		when(connectionService.create(any(ConnectionRequest.class))).thenReturn(sample());

		mockMvc.perform(post("/api/connections")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
									"name": "Standard",
									"useSsl": false,
									"brokerHost": "localhost",
									"brokerPort": "9001",
									"brokerPath": "/mqtt",
									"user": "alarm",
									"password": "alarm",
									"mqttTopic": "JF/Alarm/KB"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.mqttTopic").value("JF/Alarm/KB"));
	}

	@Test
	void rejectsBlankConnectionName() throws Exception {
		mockMvc.perform(post("/api/connections")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
									"name": "",
									"useSsl": false,
									"brokerHost": "localhost",
									"brokerPort": "9001",
									"mqttTopic": "JF/Alarm/KB"
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void replacesConnections() throws Exception {
		when(connectionService.replaceAll(any())).thenReturn(List.of(sample()));

		mockMvc.perform(put("/api/connections")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								[{
									"id": "00000000-0000-0000-0000-000000000001",
									"name": "Standard",
									"useSsl": false,
									"brokerHost": "localhost",
									"brokerPort": "9001",
									"mqttTopic": "JF/Alarm/KB"
								}]
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Standard"));
	}

	@Test
	void deletesConnection() throws Exception {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

		mockMvc.perform(delete("/api/connections/" + id))
				.andExpect(status().isNoContent());

		verify(connectionService).delete(eq(id));
	}
}
