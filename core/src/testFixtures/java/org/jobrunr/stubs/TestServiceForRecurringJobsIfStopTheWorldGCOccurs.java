package org.jobrunr.stubs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

public class TestServiceForRecurringJobsIfStopTheWorldGCOccurs {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestServiceForRecurringJobsIfStopTheWorldGCOccurs.class);

    private static int processedJobs = 0;
    private static Instant firstRun = null;

    public void doWork() throws Exception {
        if(firstRun == null) firstRun = Instant.now();
        Instant shouldRunAt = firstRun.plusSeconds(processedJobs * 5);
        Instant now = Instant.now();
        processedJobs++;
        LOGGER.info("Processed job {} that should run at {} (drift: {}ms)", processedJobs, shouldRunAt, Duration.between(now, shouldRunAt).toMillis());
        if(processedJobs > 2 && Duration.between(shouldRunAt, now).getSeconds() < 5) {
            LOGGER.info("JobRunr recovered from long GC and all jobs were executed");
        }
    }

    public void resetProcessedJobs() {
        processedJobs = 0;
        firstRun = null;
    }
}

