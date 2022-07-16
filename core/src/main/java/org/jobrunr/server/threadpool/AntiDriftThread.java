package org.jobrunr.server.threadpool;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ScheduledFuture;

import static java.time.Duration.ZERO;
import static java.util.Objects.requireNonNull;

final class AntiDriftThread extends Thread {

    private final JobRunrInternalExecutor executor;
    private final BlockingQueue<AntiDriftSchedule> queue;
    private final List<ScheduledFuture<?>> tasks;

    AntiDriftThread(final JobRunrInternalExecutor executor) {
        super();
        this.executor = requireNonNull(executor);
        this.queue = new DelayQueue<>();
        this.tasks = new ArrayList<>();
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()) {
            try {
                final AntiDriftSchedule taken = queue.take();

                queue(taken.getNextSchedule());
                schedule(taken);

                final Iterator<ScheduledFuture<?>> it = tasks.iterator();
                while (it.hasNext()) {
                    final ScheduledFuture<?> f = it.next();
                    if (f.isDone()) {
                        it.remove();
                    }
                }
            } catch (final InterruptedException e) {
                tasks.forEach(sf -> sf.cancel(false));
                tasks.clear();
                return;
            }
        }
    }

    private void schedule(final AntiDriftSchedule s) {
        final ScheduledFuture<?> future = executor.schedule(s.getRunnable(), ZERO);
        tasks.add(future);
    }

    void queue(final AntiDriftSchedule s) {
        queue.add(s);
    }
}
