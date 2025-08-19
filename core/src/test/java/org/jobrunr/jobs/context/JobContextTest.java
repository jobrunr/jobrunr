package org.jobrunr.jobs.context;

import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.exceptions.StepExecutionException;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.utils.SleepUtils.sleep;

public class JobContextTest {

    @Test
    void jobContextCanAccessJobInfo() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        assertThat(jobContext.getJobId()).isEqualTo(job.getId());
        assertThat(jobContext.getJobName()).isEqualTo("job1");
        assertThat(jobContext.getJobSignature()).isEqualTo(job.getJobSignature());
        assertThat(jobContext.getJobLabels()).isEqualTo(List.of("my-label"));
        assertThat(jobContext.getCreatedAt()).isEqualTo(job.getCreatedAt());
        assertThat(jobContext.getUpdatedAt()).isEqualTo(job.getUpdatedAt());
        assertThat(jobContext.progressBar(10)).isNotNull();
    }

    @Test
    void jobContextCanSaveMetadata() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        jobContext.saveMetadata("my-key", "my-string");
        jobContext.saveMetadata("my-key", "my-updated-string");
        jobContext.saveMetadataIfAbsent("only-once-key", "my-string");
        jobContext.saveMetadataIfAbsent("only-once-key", "my-other-string");

        assertThat(job)
                .hasMetadata("my-key", "my-updated-string")
                .hasMetadata("only-once-key", "my-string");
    }

    @Test
    void jobContextCanGetMetadata() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        jobContext.saveMetadata("my-key", "my-string");
        jobContext.saveMetadataIfAbsent("only-once-key", "my-only-once-string");

        assertThat((String) jobContext.getMetadata("my-key")).isEqualTo("my-string");
        assertThat((String) jobContext.getMetadata("only-once-key")).isEqualTo("my-only-once-string");
    }

    @Test
    void jobContextNbrOfRetries() {
        final Job job = aFailedJobWithRetries(2) // 0 based
                .withEnqueuedState(Instant.now())
                .withProcessingState()
                .build();

        JobContext jobContext = new JobContext(job);

        Assertions.assertThat(jobContext.currentRetry()).isEqualTo(3);
    }

    @Test
    void jobContextAmountOfFailures() {
        final Job job = aFailedJobWithRetries(2) // 0 based
                .withEnqueuedState(Instant.now())
                .withProcessingState()
                .build();

        JobContext jobContext = new JobContext(job);

        Assertions.assertThat(jobContext.amountOfFailures()).isEqualTo(3);
    }

    @Test
    void jobContextCanRunCheckWithStepCompleted() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        assertThat(jobContext.hasCompletedStep("step-1")).isFalse();
        jobContext.markStepCompleted("step-A");
        assertThat(jobContext.hasCompletedStep("step-1")).isFalse();
        jobContext.markStepCompleted("step-1");
        assertThat(jobContext.hasCompletedStep("step-1")).isTrue();
    }

    @Test
    void jobContextRunsStepOnlyOnce() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        final AtomicInteger counter = new AtomicInteger();
        jobContext.runStepOnce("my-step", counter::incrementAndGet);
        jobContext.runStepOnce("my-step", counter::incrementAndGet);

        assertThat(counter).hasValue(1);
        assertThat(jobContext.hasCompletedStep("my-step")).isTrue();
    }

    @Test
    void jobContextRunsStepOnlyOnceRunnableCanThrowException() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        final AtomicInteger counter = new AtomicInteger();
        jobContext.runStepOnce("my-step", () -> doSomethingThatCanThrowAnException(counter));
        jobContext.runStepOnce("my-step", () -> doSomethingThatCanThrowAnException(counter));

        assertThat(counter).hasValue(1);
        assertThat(jobContext.hasCompletedStep("my-step")).isTrue();
    }

    @Test
    void jobContextRunsStepOnlyOnceRunnableCanThrowExceptionWhichIsTransformedToAStepExecutionException() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        final AtomicInteger counter = new AtomicInteger();
        assertThatCode(() -> jobContext.runStepOnce("my-step", () -> doSomethingThatThrowsAnException(counter)))
                .isInstanceOf(StepExecutionException.class)
                .hasMessageContaining("Exception during execution of step 'my-step'");

        assertThatCode(() -> jobContext.runStepOnce("my-step", () -> doSomethingThatThrowsAnException(counter)))
                .doesNotThrowAnyException();

        assertThat(counter).hasValue(2);
        assertThat(jobContext.hasCompletedStep("my-step")).isTrue();
    }

    @Test
    void jobContextRunsStepOnlyOnceCanReturnObject() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        final AtomicInteger counter = new AtomicInteger();
        String resultA = jobContext.runStepOnce("my-step", () -> getSomethingWithCounter(counter));
        String resultB = jobContext.runStepOnce("my-step", () -> getSomethingWithCounter(counter));

        assertThat(resultA).isEqualTo(resultB);
        assertThat(jobContext.hasCompletedStep("my-step")).isTrue();
    }

    @Test
    void jobContextRunsStepOnlyOnceSupplierCanThrowException() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        final AtomicInteger counter = new AtomicInteger();
        String resultA = jobContext.runStepOnce("my-step", () -> getSomethingThatCanThrowAnException(counter));
        String resultB = jobContext.runStepOnce("my-step", () -> getSomethingThatCanThrowAnException(counter));

        assertThat(resultA).isEqualTo(resultB);
        assertThat(jobContext.hasCompletedStep("my-step")).isTrue();
    }

    @Test
    void jobContextRunsStepOnlyOnceSupplierCanThrowExceptionWhichIsTransformedToAStepExecutionException() {
        final Job job = aJobInProgress().withName("job1").withLabels("my-label").build();

        JobContext jobContext = new JobContext(job);

        final AtomicInteger counter = new AtomicInteger();
        assertThatCode(() -> jobContext.runStepOnce("my-step", () -> getSomethingThatThrowsAnException(counter)))
                .isInstanceOf(StepExecutionException.class)
                .hasMessageContaining("Exception during execution of step 'my-step'");

        assertThatCode(() -> jobContext.runStepOnce("my-step", () -> getSomethingThatThrowsAnException(counter)))
                .doesNotThrowAnyException();

        assertThat(counter).hasValue(2);
        assertThat(jobContext.hasCompletedStep("my-step")).isTrue();
    }

    @Test
    void jobContextIsThreadSafeUsingJackson() throws InterruptedException {
        jobContextIsThreadsafe(new JobMapper(new JacksonJsonMapper()));
    }

    @Test
    void jobContextIsThreadSafeUsingGson() throws InterruptedException {
        jobContextIsThreadsafe(new JobMapper(new GsonJsonMapper()));
    }

    @Test
    void jobContextIsThreadSafeUsingJsonB() throws InterruptedException {
        jobContextIsThreadsafe(new JobMapper(new JsonbJsonMapper()));
    }

    void jobContextIsThreadsafe(JobMapper jobMapper) throws InterruptedException {
        final Job job = aJobInProgress().withName("job1").build();

        JobContext jobContext = new JobContext(job);

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final Thread thread1 = new Thread(updateJobContextRunnable(job, jobContext, countDownLatch));
        final Thread thread2 = new Thread(serializingRunnable(job, jobContext, jobMapper, countDownLatch));

        thread1.start();
        thread2.start();

        countDownLatch.await(5, TimeUnit.SECONDS);

        assertThat(job)
                .hasMetadata("key0", "value0")
                .hasMetadata("key99", "value99");
    }

    private Runnable updateJobContextRunnable(Job job, JobContext jobContext, CountDownLatch countDownLatch) {
        return () -> {
            try {
                for (int i = 0; i < 100; i++) {
                    jobContext.saveMetadata("key" + i, "value" + i);
                    sleep(5);
                }
                countDownLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Runnable serializingRunnable(Job job, JobContext jobContext, JobMapper jobMapper, CountDownLatch countDownLatch) {
        return () -> {
            try {
                for (int i = 0; i < 100; i++) {
                    jobMapper.serializeJob(job);
                    sleep(5);
                }
                countDownLatch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    private void doSomethingThatCanThrowAnException(AtomicInteger counter) throws InterruptedException {
        Thread.sleep(1);
        counter.incrementAndGet();
    }

    private void doSomethingThatThrowsAnException(AtomicInteger counter) throws Exception {
        counter.incrementAndGet();
        if (counter.get() == 1) {
            throw new Exception("Something went wrong");
        }
    }

    private String getSomethingWithCounter(AtomicInteger counter) {
        return "test-" + counter.incrementAndGet();
    }

    private String getSomethingThatCanThrowAnException(AtomicInteger counter) throws InterruptedException {
        Thread.sleep(1);
        counter.incrementAndGet();
        return "test-" + counter.incrementAndGet();
    }

    private String getSomethingThatThrowsAnException(AtomicInteger counter) throws Exception {
        counter.incrementAndGet();
        if (counter.get() == 1) {
            throw new Exception("Something went wrong");
        }
        return "test-" + counter.get();
    }
}
