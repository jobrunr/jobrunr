package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest({JobRunrProducer.class})
class JobRunrProducerWithCustomJobDetailsGeneratorTest {

    @Inject
    Instance<JobDetailsGenerator> jobDetailsGeneratorInstance;
    @Inject
    Instance<JobScheduler> jobSchedulerInstance;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.job-scheduler.enabled", value = "true")
    void jobDetailsGeneratorCanBeOverridden() {
        assertThat(jobDetailsGeneratorInstance.isResolvable()).isTrue();
        JobDetailsGenerator customJobDetailsGenerator = jobDetailsGeneratorInstance.get();
        assertThat(customJobDetailsGenerator).isInstanceOf(CustomJobDetailsGenerator.class);
        assertThat(jobSchedulerInstance.get()).extracting("jobDetailsGenerator").isSameAs(customJobDetailsGenerator);
    }

    @Singleton
    static class CustomJobDetailsGenerator extends CachingJobDetailsGenerator {
    }
}
