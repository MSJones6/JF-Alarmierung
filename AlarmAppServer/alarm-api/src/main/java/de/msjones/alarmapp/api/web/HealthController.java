package de.msjones.alarmapp.api.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Einfacher Lebenszeichen-Endpunkt für Docker-Healthchecks.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

	/**
	 * Meldet, dass die API erreichbar ist.
	 *
	 * @return Statusobjekt
	 */
	@GetMapping
	public Map<String, String> health() {
		return Map.of("status", "UP");
	}
}
