package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.announcement.AlarmAnnouncementService;
import de.msjones.alarmapp.api.domain.AlarmEntity;
import de.msjones.alarmapp.api.domain.ConnectionEntity;
import de.msjones.alarmapp.api.dto.AlarmResponse;
import de.msjones.alarmapp.api.mqtt.MqttAlarmPublisher;
import de.msjones.alarmapp.api.repository.AlarmRepository;
import de.msjones.alarmapp.api.repository.ConnectionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sendet fällige geplante Alarmierungen per MQTT und markiert sie als gesendet.
 */
@Service
public class AlarmDispatchService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmDispatchService.class);
	private static final String STATUS_PLANNED = "planned";
	private static final String STATUS_SENT = "sent";

	private final AlarmRepository alarmRepository;
	private final ConnectionRepository connectionRepository;
	private final MqttAlarmPublisher mqttAlarmPublisher;
	private final AlarmStreamService alarmStreamService;
	private final AlarmAnnouncementService alarmAnnouncementService;
	private final Clock clock;

	/**
	 * Erzeugt den Versanddienst.
	 *
	 * @param alarmRepository Alarme
	 * @param connectionRepository Connections
	 * @param mqttAlarmPublisher MQTT-Versand
	 * @param alarmStreamService Live-Updates
	 * @param alarmAnnouncementService Server-Durchsage
	 * @param zoneId Zeitzone der geplanten Zeitpunkte
	 */
	@Autowired
	public AlarmDispatchService(
			AlarmRepository alarmRepository,
			ConnectionRepository connectionRepository,
			MqttAlarmPublisher mqttAlarmPublisher,
			AlarmStreamService alarmStreamService,
			AlarmAnnouncementService alarmAnnouncementService,
			@Value("${alarm.scheduler.zone:Europe/Berlin}") String zoneId
	) {
		this(
				alarmRepository,
				connectionRepository,
				mqttAlarmPublisher,
				alarmStreamService,
				alarmAnnouncementService,
				Clock.system(ZoneId.of(zoneId))
		);
	}

	/**
	 * Erzeugt den Versanddienst mit einer festen Uhr, vor allem für Tests.
	 *
	 * @param alarmRepository Alarme
	 * @param connectionRepository Connections
	 * @param mqttAlarmPublisher MQTT-Versand
	 * @param alarmStreamService Live-Updates
	 * @param alarmAnnouncementService Server-Durchsage
	 * @param clock aktuelle Zeit
	 */
	AlarmDispatchService(
			AlarmRepository alarmRepository,
			ConnectionRepository connectionRepository,
			MqttAlarmPublisher mqttAlarmPublisher,
			AlarmStreamService alarmStreamService,
			AlarmAnnouncementService alarmAnnouncementService,
			Clock clock
	) {
		this.alarmRepository = alarmRepository;
		this.connectionRepository = connectionRepository;
		this.mqttAlarmPublisher = mqttAlarmPublisher;
		this.alarmStreamService = alarmStreamService;
		this.alarmAnnouncementService = alarmAnnouncementService;
		this.clock = clock;
	}

	/**
	 * Liefert alle geplanten Alarme, deren Zeitpunkt erreicht ist.
	 *
	 * @return fällige Alarmierungen
	 */
	@Transactional(readOnly = true)
	public List<AlarmEntity> findDue() {
		LocalDateTime now = LocalDateTime.now(clock);
		return alarmRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
				STATUS_PLANNED,
				now
		);
	}

	/**
	 * Liefert den Zeitpunkt der nächsten noch geplanten Alarmierung.
	 *
	 * @return nächster Zeitpunkt oder leer, wenn nichts geplant ist
	 */
	@Transactional(readOnly = true)
	public Optional<LocalDateTime> findNextPlannedAt() {
		return alarmRepository.findFirstByStatusOrderByScheduledAtAsc(STATUS_PLANNED)
				.map(AlarmEntity::getScheduledAt);
	}

	/**
	 * Sendet einen geplanten Alarm, sofern er noch fällig und nicht bereits ausgelöst ist.
	 *
	 * @param id Alarm-ID
	 */
	@Transactional
	public void sendDueAlarm(UUID id) {
		AlarmEntity alarm = alarmRepository.findById(id).orElse(null);
		if (alarm == null || !STATUS_PLANNED.equals(alarm.getStatus())) {
			return;
		}
		if (!AlarmPayload.isDue(alarm.getScheduledAt(), LocalDateTime.now(clock))) {
			return;
		}
		publishFor(alarm);
		alarm.setStatus(STATUS_SENT);
		AlarmResponse response = EntityMapper.toResponse(alarmRepository.save(alarm));
		alarmStreamService.send("updated", response);
		LOGGER.info(
				"Geplante Alarmierung {} (fällig {}) wurde als gesendet markiert.",
				id,
				alarm.getScheduledAt()
		);
	}

	/**
	 * Sendet die Alarmierung über die zugehörige MQTT-Connection.
	 *
	 * @param alarm zu sendende Alarmierung
	 */
	public void publishFor(AlarmEntity alarm) {
		ConnectionEntity connection = connectionRepository.findByName(alarm.getConnectionName())
				.orElseThrow(() -> new IllegalStateException(
						"Connection „" + alarm.getConnectionName() + "“ wurde nicht gefunden."
				));
		mqttAlarmPublisher.publish(connection, AlarmPayload.from(alarm));
		alarmAnnouncementService.announceAsync(alarm);
	}
}
