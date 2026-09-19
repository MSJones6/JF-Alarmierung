package de.msjones.alarmapp.api.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Erlaubt dem Frontend den Zugriff auf REST und den Alarm-Stream.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final List<String> allowedOrigins;

	/**
	 * Liest die erlaubten Ursprünge aus der Konfiguration.
	 *
	 * @param allowedOrigins kommaseparierte Ursprünge
	 */
	public CorsConfig(@Value("${alarm.cors.allowed-origins}") String allowedOrigins) {
		this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList();
	}

	/**
	 * Registriert CORS für alle API-Pfade inklusive Event-Streams.
	 *
	 * @param registry Spring-CORS-Registry
	 */
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.exposedHeaders("*")
				.allowCredentials(false);
	}
}
