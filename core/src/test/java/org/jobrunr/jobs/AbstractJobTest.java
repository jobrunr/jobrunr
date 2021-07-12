package org.jobrunr.jobs;

import org.jobrunr.utils.resilience.Lock;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class AbstractJobTest {

    @Test
    void increaseVersion() {
        Job job = anEnqueuedJob().build();
        assertThat(job.getVersion()).isZero();

        assertThat(job.increaseVersion()).isEqualTo(1);
        assertThat(job.getVersion()).isEqualTo(1);

        assertThat(job.increaseVersion()).isEqualTo(2);
        assertThat(job.getVersion()).isEqualTo(2);
    }

    @Test
    void whileLockedJobCannotBeLockedForOtherSaveAction() {
        Job job = anEnqueuedJob().build();
        final AtomicBoolean atomicBoolean = new AtomicBoolean();

        final Lock lock = job.lock();
        await()
                .during(1, TimeUnit.SECONDS)
                .atMost(2, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    new Thread(() -> {
                        job.lock();
                        atomicBoolean.set(true);
                    }).start();

                    assertThat(lock.isLocked()).isTrue();
                    assertThat(atomicBoolean).isFalse();
                });
    }
}