package org.jobrunr.server.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ScheduledFuture;

import static java.time.Duration.ZERO;
import static java.util.Objects.requireNonNull;

final class AntiDriftThread extends Thread {

    private final JobRunrInternalExecutor executor;
    private final BlockingQueue<AntiDriftSchedule> schedules;
    private final List<ScheduledFuture<?>> scheduledTasks;

    AntiDriftThread(final JobRunrInternalExecutor executor) {
        super();
        this.executor = requireNonNull(executor);
        this.schedules = new DelayQueue<>();
        this.scheduledTasks = new ArrayList<>();
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()) {
            try {
                final AntiDriftSchedule taken = schedules.take();

                addSchedule(taken.getNextSchedule());
                schedule(taken);
            } catch (final InterruptedException e) {
                scheduledTasks.forEach(sf -> sf.cancel(false));
                scheduledTasks.clear();
                return;
            }
        }
    }

    private void schedule(final AntiDriftSchedule s) {
        final ScheduledFuture<?> future = executor.schedule(s.getRunnable(), ZERO);
        scheduledTasks.add(future);
    }

    void addSchedule(final AntiDriftSchedule s) {
        schedules.add(s);
    }
}
