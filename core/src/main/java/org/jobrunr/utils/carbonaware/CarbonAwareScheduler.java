package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.mapper.JsonMapper;

import java.time.Instant;
import java.util.Optional;

public class CarbonAwareScheduler {

    private final CarbonAwareAPIClient carbonAwareAPIClient;
    private final DayAheadEnergyPrices dayAheadEnergyPrices;

    public CarbonAwareScheduler(JsonMapper jsonMapper, Optional<String> area) {
        carbonAwareAPIClient = new CarbonAwareAPIClient(jsonMapper);
        dayAheadEnergyPrices = carbonAwareAPIClient.fetchLatestDayAheadEnergyPrices(area);
    }

    public void moveToNextState(Job job) {
        JobState jobState = job.getJobState();
        if(!(jobState instanceof CarbonAwareAwaitingState)) {
            throw new IllegalStateException("CarbonAwareScheduler can only handle jobs that are awaiting for the least carbon intense moment");
        }

        CarbonAwareAwaitingState carbonAwareAwaitingState = (CarbonAwareAwaitingState) jobState;

        // This will be the meat / complexer part of the scheduling
        // TODO: if deadline is within 24h do below
        // TODO: if deadline is before sunday noon, wait until sunday and choose best time on Sunday
        // TODO: if deadline is before saturday noon, wait until saturday and choose best time on Saturday
        // TODO: else we wait until day of deadline and TODO 1 comes active

        if(dayAheadEnergyPrices.getErrorMessage() != null) {
            carbonAwareAwaitingState.moveToNextState(job, carbonAwareAwaitingState.getDeadline(), "No carbon intensity info available (" + dayAheadEnergyPrices.getErrorMessage() + "), scheduling job at deadline.");
        } else {
            Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(carbonAwareAwaitingState.getDeadline());
            if(leastExpensiveHour != null) {
                carbonAwareAwaitingState.moveToNextState(job, leastExpensiveHour);
            }
        }
    }

}
