package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.jobrunr.JobRunrAssertions.assertThat;
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
        assertThat(jobContext.getJobLabels()).isEqualTo(Set.of("my-label"));
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
}
