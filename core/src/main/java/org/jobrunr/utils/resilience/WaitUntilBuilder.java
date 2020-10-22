package org.jobrunr.utils.resilience;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.BooleanSupplier;

public class WaitUntilBuilder implements Callable<Void> {

    private final boolean async;
    private Duration duration;
    private BooleanSupplier condition;
    private Runnable conditionMetRunnable;
    private Runnable conditionNotMetRunnable;
    private FutureTask<Void> futureTask;

    private WaitUntilBuilder(boolean async) {
        this.async = async;
    }

    public static WaitUntilBuilder awaitAsync() {
        return new WaitUntilBuilder(true);
    }

    public static WaitUntilBuilder awaitSync() {
        return new WaitUntilBuilder(false);
    }

    public WaitUntilBuilder atMost(long amount, TemporalUnit temporalUnit) {
        this.duration = Duration.of(amount, temporalUnit);
        return this;
    }

    public WaitUntilBuilder atMost(Duration duration) {
        this.duration = duration;
        return this;
    }

    public WaitUntilBuilder until(BooleanSupplier condition) {
        this.condition = condition;
        return this;
    }

    public WaitUntilBuilder andThen(Runnable conditionMetRunnable) {
        this.conditionMetRunnable = conditionMetRunnable;
        futureTask = new FutureTask<>(this);
        new Thread(futureTask).start();
        return this;
    }

    public WaitUntilBuilder orElse(Runnable conditionNotMetRunnable) {
        this.conditionNotMetRunnable = conditionNotMetRunnable;
        if (!async) {
            try {
                futureTask.get();
            } catch (InterruptedException | ExecutionException e) {
                // mmm, what to do? This can happen (due to stopping server immediately)
            }
        }
        return this;
    }

    @Override
    public Void call() throws Exception {
        Instant endTime = Instant.now().plus(duration);
        while (!condition.getAsBoolean()) {
            if (Instant.now().isAfter(endTime)) {
                if (conditionNotMetRunnable != null) {
                    conditionNotMetRunnable.run();
                    return null;
                }
                throw new IllegalStateException("Condition is not met within " + duration);
            }
            Thread.sleep(200);
        }
        conditionMetRunnable.run();
        return null;
    }
}
