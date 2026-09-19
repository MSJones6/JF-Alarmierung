package de.msjones.alarmapp.api.service;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Plant den MQTT-Versand genau zum nächsten Fälligkeitszeitpunkt und plant bei Änderungen neu.
 */
@Component
@ConditionalOnProperty(name = "alarm.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class AlarmDispatchScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(AlarmDispatchScheduler.class);

	private final AlarmDispatchService alarmDispatchService;
	private final TaskScheduler taskScheduler;
	private final Clock clock;
	private final Duration retryDelay;
	private final Object lock = new Object();

	private ScheduledFuture<?> scheduledFuture;

	/**
	 * Erzeugt den Scheduler.
	 *
	 * @param alarmDispatchService Versandlogik
	 * @param taskScheduler Spring-TaskScheduler
	 * @param zoneId Zeitzone der geplanten Zeitpunkte
	 * @param retryDelayMs Mindestabstand bei bereits fälligen Alarmen (z. B. nach Fehlern)
	 */
	@Autowired
	public AlarmDispatchScheduler(
			AlarmDispatchService alarmDispatchService,
			TaskScheduler taskScheduler,
			@Value("${alarm.scheduler.zone:Europe/Berlin}") String zoneId,
			@Value("${alarm.scheduler.retry-delay-ms:5000}") long retryDelayMs
	) {
		this(
				alarmDispatchService,
				taskScheduler,
				Clock.system(ZoneId.of(zoneId)),
				Duration.ofMillis(retryDelayMs)
		);
	}

	/**
	 * Erzeugt den Scheduler mit fester Uhr, vor allem für Tests.
	 *
	 * @param alarmDispatchService Versandlogik
	 * @param taskScheduler Spring-TaskScheduler
	 * @param clock aktuelle Zeit
	 * @param retryDelay Mindestabstand bei bereits fälligen Alarmen
	 */
	AlarmDispatchScheduler(
			AlarmDispatchService alarmDispatchService,
			TaskScheduler taskScheduler,
			Clock clock,
			Duration retryDelay
	) {
		this.alarmDispatchService = alarmDispatchService;
		this.taskScheduler = taskScheduler;
		this.clock = clock;
		this.retryDelay = retryDelay;
	}

	/**
	 * Plant den ersten Lauf nach dem Anwendungsstart.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		reschedule();
	}

	/**
	 * Bricht den aktuellen Plan ab und plant den nächsten Fälligkeitszeitpunkt neu.
	 * Bereits fällige Alarme werden sofort angestoßen.
	 */
	public void reschedule() {
		scheduleNext(false);
	}

	/**
	 * Sendet alle aktuell fälligen Alarme und plant anschließend den nächsten Lauf.
	 */
	void dispatchDueAlarms() {
		LocalDateTime startedAt = LocalDateTime.now(clock);
		LOGGER.info("Alarm-Dispatch gestartet um {}.", startedAt);
		try {
			for (AlarmEntity due : alarmDispatchService.findDue()) {
				try {
					alarmDispatchService.sendDueAlarm(due.getId());
				} catch (RuntimeException exception) {
					LOGGER.error(
							"Geplante Alarmierung {} konnte nicht gesendet werden: {}",
							due.getId(),
							exception.getMessage(),
							exception
					);
				}
			}
		} finally {
			// Nach einem Lauf: bereits fällige Reste erst nach Retry-Delay, Zukunftstermine exakt.
			scheduleNext(true);
		}
	}

	/**
	 * Plant den nächsten Lauf.
	 *
	 * @param delayIfAlreadyDue {@code true}, wenn bereits fällige Termine um den Retry-Delay
	 *     verschoben werden sollen (verhindert Busy-Loops nach Fehlern); Zukunftstermine
	 *     bleiben immer unverändert
	 */
	private void scheduleNext(boolean delayIfAlreadyDue) {
		synchronized (lock) {
			cancelScheduledRun();
			LocalDateTime nextPlannedAt = alarmDispatchService.findNextPlannedAt().orElse(null);
			if (nextPlannedAt == null) {
				LOGGER.info(
						"Keine geplanten Alarmierungen – warte auf Änderungen (Stand {}).",
						LocalDateTime.now(clock)
				);
				return;
			}
			Instant now = clock.instant();
			Instant target = nextPlannedAt.atZone(clock.getZone()).toInstant();
			if (!target.isAfter(now)) {
				target = delayIfAlreadyDue ? now.plus(retryDelay) : now;
			}
			scheduledFuture = taskScheduler.schedule(this::dispatchDueAlarms, target);
			LOGGER.info(
					"Nächster Alarm-Dispatch um {} (geplant {}, Stand {}).",
					LocalDateTime.ofInstant(target, clock.getZone()),
					nextPlannedAt,
					LocalDateTime.now(clock)
			);
		}
	}

	/**
	 * Bricht einen ausstehenden Lauf ab, sofern vorhanden.
	 */
	private void cancelScheduledRun() {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(false);
			scheduledFuture = null;
		}
	}
}
