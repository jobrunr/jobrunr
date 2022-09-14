package org.jobrunr.jobs;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;

class JobListVersionerTest {

    @Test
    void testJobListVersionerOnCommitVersionIsIncreased() {
        Job job1 = aScheduledJob().withVersion(5).build();
        Job job2 = aScheduledJob().withVersion(5).build();
        JobListVersioner jobListVersioner = new JobListVersioner(asList(job1, job2));

        jobListVersioner.commitVersions();

        assertThat(job1).hasVersion(6);
        assertThat(job2).hasVersion(6);
    }

    @Test
    void testJobListVersionerOnRollbackOfSomeJobsVersionIsDecreasedForThoseJobs() {
        Job job1 = aScheduledJob().withVersion(5).build();
        Job job2 = aScheduledJob().withVersion(5).build();
        JobListVersioner jobListVersioner = new JobListVersioner(asList(job1, job2));

        jobListVersioner.rollbackVersions(asList(job2));
        jobListVersioner.close();

        assertThat(job1).hasVersion(6);
        assertThat(job2).hasVersion(5);
    }

    @Test
    void testJobListVersionerInTryWithResourcesOnRollbackOfSomeJobsVersionIsDecreasedForThoseJobs() {
        Job job1 = aScheduledJob().withVersion(5).build();
        Job job2 = aScheduledJob().withVersion(5).build();
        try(JobListVersioner jobListVersioner = new JobListVersioner(asList(job1, job2))) {
            jobListVersioner.rollbackVersions(asList(job2));
        }

        assertThat(job1).hasVersion(6);
        assertThat(job2).hasVersion(5);
    }

}