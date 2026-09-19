package de.msjones.alarmapp.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.msjones.alarmapp.api.domain.AlarmEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

/**
 * Prüft das zeitgenaue Einplanen und Neu-Planen des Alarm-Dispatchs.
 */
@ExtendWith(MockitoExtension.class)
class AlarmDispatchSchedulerTest {

	@Mock
	private AlarmDispatchService alarmDispatchService;

	@Mock
	private TaskScheduler taskScheduler;

	private AlarmDispatchScheduler alarmDispatchScheduler;
	private final AtomicReference<Runnable> scheduledRunnable = new AtomicReference<>();
	private final AtomicReference<Instant> scheduledInstant = new AtomicReference<>();

	/**
	 * Erzeugt den Scheduler mit fester Uhr und erfasst geplante Läufe.
	 */
	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-09-19T21:40:00Z"), ZoneOffset.UTC);
		alarmDispatchScheduler = new AlarmDispatchScheduler(
				alarmDispatchService,
				taskScheduler,
				clock,
				Duration.ofSeconds(5)
		);
		org.mockito.Mockito.lenient()
				.when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
				.thenAnswer(invocation -> {
					scheduledRunnable.set(invocation.getArgument(0));
					scheduledInstant.set(invocation.getArgument(1));
					return mock(ScheduledFuture.class);
				});
	}

	@Test
	void reschedulePlansNextPlannedAlarmAtDueTime() {
		when(alarmDispatchService.findNextPlannedAt())
				.thenReturn(Optional.of(LocalDateTime.of(2026, 9, 19, 22, 0, 0)));

		alarmDispatchScheduler.reschedule();

		assertThat(scheduledInstant.get()).isEqualTo(Instant.parse("2026-09-19T22:00:00Z"));
	}

	@Test
	void rescheduleRunsImmediatelyWhenAlarmIsAlreadyDue() {
		when(alarmDispatchService.findNextPlannedAt())
				.thenReturn(Optional.of(LocalDateTime.of(2026, 9, 19, 21, 30, 0)));

		alarmDispatchScheduler.reschedule();

		assertThat(scheduledInstant.get()).isEqualTo(Instant.parse("2026-09-19T21:40:00Z"));
	}

	@Test
	void rescheduleDoesNothingWhenNoPlannedAlarmExists() {
		when(alarmDispatchService.findNextPlannedAt()).thenReturn(Optional.empty());

		alarmDispatchScheduler.reschedule();

		verify(taskScheduler, never()).schedule(any(Runnable.class), any(Instant.class));
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void rescheduleCancelsPreviousFutureBeforePlanningAgain() {
		ScheduledFuture firstFuture = mock(ScheduledFuture.class);
		ScheduledFuture secondFuture = mock(ScheduledFuture.class);
		when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
				.thenReturn(firstFuture, secondFuture);
		when(alarmDispatchService.findNextPlannedAt())
				.thenReturn(Optional.of(LocalDateTime.of(2026, 9, 19, 22, 0, 0)));

		alarmDispatchScheduler.reschedule();
		alarmDispatchScheduler.reschedule();

		verify(firstFuture).cancel(false);
	}

	@Test
	void dispatchDueAlarmsKeepsExactDueTimeForFutureAlarms() {
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000021");
		AlarmEntity dueOne = new AlarmEntity();
		dueOne.setId(first);
		when(alarmDispatchService.findDue()).thenReturn(List.of(dueOne));
		when(alarmDispatchService.findNextPlannedAt())
				.thenReturn(Optional.of(LocalDateTime.of(2026, 9, 19, 21, 40, 2)));

		alarmDispatchScheduler.dispatchDueAlarms();

		verify(alarmDispatchService).sendDueAlarm(first);
		assertThat(scheduledInstant.get()).isEqualTo(Instant.parse("2026-09-19T21:40:02Z"));
	}

	@Test
	void dispatchDueAlarmsAppliesRetryDelayOnlyWhenNextIsAlreadyDue() {
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000021");
		AlarmEntity dueOne = new AlarmEntity();
		dueOne.setId(first);
		when(alarmDispatchService.findDue()).thenReturn(List.of(dueOne));
		when(alarmDispatchService.findNextPlannedAt())
				.thenReturn(Optional.of(LocalDateTime.of(2026, 9, 19, 21, 40, 0)));

		alarmDispatchScheduler.dispatchDueAlarms();

		verify(alarmDispatchService).sendDueAlarm(first);
		assertThat(scheduledInstant.get()).isEqualTo(Instant.parse("2026-09-19T21:40:05Z"));
	}

	@Test
	void continuesWithNextAlarmWhenOneDispatchFails() {
		UUID first = UUID.fromString("00000000-0000-0000-0000-000000000021");
		UUID second = UUID.fromString("00000000-0000-0000-0000-000000000022");
		AlarmEntity dueOne = new AlarmEntity();
		dueOne.setId(first);
		AlarmEntity dueTwo = new AlarmEntity();
		dueTwo.setId(second);
		when(alarmDispatchService.findDue()).thenReturn(List.of(dueOne, dueTwo));
		when(alarmDispatchService.findNextPlannedAt()).thenReturn(Optional.empty());
		doThrow(new IllegalStateException("Broker nicht erreichbar"))
				.when(alarmDispatchService)
				.sendDueAlarm(first);

		alarmDispatchScheduler.dispatchDueAlarms();

		verify(alarmDispatchService).sendDueAlarm(second);
	}

	@Test
	void onApplicationReadyTriggersInitialSchedule() {
		when(alarmDispatchService.findNextPlannedAt())
				.thenReturn(Optional.of(LocalDateTime.of(2026, 9, 19, 23, 0, 0)));

		alarmDispatchScheduler.onApplicationReady();

		ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
		assertThat(instantCaptor.getValue()).isEqualTo(Instant.parse("2026-09-19T23:00:00Z"));
	}
}
