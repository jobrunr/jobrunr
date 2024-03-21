package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;

import java.util.UUID;

public class CarbonAwareJobCreator {

    public static Job createCarbonAwareJob(JobDetails jobDetails, CarbonAwarePeriod carbonAwarePeriod) {
        checkEnabled();
        return new Job(jobDetails, new CarbonAwareAwaitingState(carbonAwarePeriod.getFrom(), carbonAwarePeriod.getTo()));
    }

    public static Job createCarbonAwareJob(UUID id, JobDetails jobDetails, CarbonAwarePeriod carbonAwarePeriod) {
        checkEnabled();
        return new Job(id, jobDetails, new CarbonAwareAwaitingState(carbonAwarePeriod.getFrom(), carbonAwarePeriod.getTo()));
    }

    private static void checkEnabled() {
        if (!CarbonAwareConfiguration.isEnabled()) {
            throw new IllegalStateException("CarbonAwareScheduler is not enabled. Please enable it in the configuration." +
                    "Use `.useCarbonAwareScheduling()` on JobRunrConfiguration.");
        }
    }
}
