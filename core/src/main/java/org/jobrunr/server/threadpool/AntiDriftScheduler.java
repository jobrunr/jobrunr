package org.jobrunr.server.threadpool;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

class AntiDriftScheduler implements Runnable {

    private final JobRunrExecutor executor;
    private final List<AntiDriftSchedule> antiDriftSchedules;

    private final Map<AntiDriftSchedule, ScheduledFuture> scheduledTasks;

    public AntiDriftScheduler(JobRunrExecutor executor) {
        this.executor = executor;
        this.antiDriftSchedules = new CopyOnWriteArrayList<>();
        this.scheduledTasks = new ConcurrentHashMap<>();
    }

    public void addSchedule(AntiDriftSchedule antiDriftSchedule) {
        this.antiDriftSchedules.add(antiDriftSchedule);
    }

    @Override
    public void run() {
        Instant now = Instant.now();
        antiDriftSchedules.stream()
                .filter(antiDriftSchedule -> antiDriftSchedule.getScheduledAt().isBefore(now))
                .forEach(this::schedule);
    }

    public void stop() {
        scheduledTasks.values().forEach(sf -> sf.cancel(false));
        scheduledTasks.clear();
    }

    private void schedule(AntiDriftSchedule antiDriftSchedule) {
        Instant nextSchedule = antiDriftSchedule.getNextSchedule();
        Duration duration = Duration.between(Instant.now(), nextSchedule);
        scheduledTasks.put(antiDriftSchedule, executor.schedule(antiDriftSchedule.getRunnable(), duration));
    }
}
