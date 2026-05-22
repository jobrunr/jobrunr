package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.costaware.CostAwareApiClient;
import org.jobrunr.server.costaware.CostAwareApiClientException;
import org.jobrunr.server.costaware.CostAwareConfiguration;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.Paging;
import org.jobrunr.utils.mapper.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.jobrunr.jobs.states.StateName.ENQUEUED;

public class CostAwareManagementTask extends AbstractJobZooKeeperTask {

    private final Integer minAmountSpotInstances = 0;
    private final Integer maxAmountSpotInstances = 2;
    private final Duration scaleUpLatency = Duration.of(1, ChronoUnit.MINUTES);
    private final Duration scaleDownLatency = Duration.of(2, ChronoUnit.MINUTES);
    private final Duration settlingPeriod = Duration.of(5, ChronoUnit.MINUTES);
    private final JobRunrMetadata clusterId;
    private final CostAwareApiClient costAwareApiClient;
    private Instant lastScaleTime = Instant.MIN;

    public CostAwareManagementTask(BackgroundJobServer backgroundJobServer, CostAwareConfiguration costAwareConfiguration, JsonMapper jsonMapper) {
        super(backgroundJobServer);
        this.clusterId = super.storageProvider.getMetadata("id", "cluster");
        this.costAwareApiClient = new CostAwareApiClient(costAwareConfiguration, jsonMapper, clusterId);
    }

    @Override
    protected void runTask() {
        if (isSettling()) return;

        Duration jobLatency = getCurrentJobLatency();
        try {
            List<BackgroundJobServerStatus> backgroundJobServerSpotInstances = getBackgroundJobServerSpotInstances();
            if (needsToScaleUpBecauseLatencyIsTooHigh(backgroundJobServerSpotInstances, jobLatency)) {
                // scale up
                costAwareApiClient.scaleUp();
                lastScaleTime = Instant.now();
            } else if (needsToScaleDownBecauseLatencyIsLow(backgroundJobServerSpotInstances, jobLatency)) {
                // scale down
                costAwareApiClient.scaleDown();
                lastScaleTime = Instant.now();
            }
        } catch (CostAwareApiClientException e) {
            LOGGER.error("Error scaling SPOT instances", e);
        }
    }

    private boolean needsToScaleUpBecauseLatencyIsTooHigh(List<BackgroundJobServerStatus> backgroundJobServerSpotInstances, Duration jobLatency) {
        // todo: check whether we don't have more than max spot instances
        return backgroundJobServerSpotInstances.size() < maxAmountSpotInstances
                && jobLatency != null && jobLatency.compareTo(scaleUpLatency) > 0;
    }

    private boolean needsToScaleDownBecauseLatencyIsLow(List<BackgroundJobServerStatus> backgroundJobServerSpotInstances, Duration jobLatency) {
        // todo: check whether there are spot instances
        return (backgroundJobServerSpotInstances.size() > minAmountSpotInstances)
                && (jobLatency == null || jobLatency.compareTo(scaleDownLatency) < 0);
    }

    private List<BackgroundJobServerStatus> getBackgroundJobServerSpotInstances() {
        return storageProvider.getBackgroundJobServers()
                .stream()
                .filter(x -> x.getName().contains(" (SPOT)"))
                .toList();
    }

    private boolean isSettling() {
        Instant lastScaleTimePlusSettlingPeriod = lastScaleTime.plus(settlingPeriod);
        return lastScaleTimePlusSettlingPeriod.isAfter(Instant.now());
    }

    private Duration getCurrentJobLatency() {
        List<Job> jobList = storageProvider.getJobList(ENQUEUED, Paging.AmountBasedList.ascOnCreatedAt(1));
        if (jobList.isEmpty()) return null;

        return getJobLatency(jobList.get(0));
    }

    private Duration getJobLatency(Job job) {
        Optional<EnqueuedState> optionalLastEnqueuedState = job.getLastJobStateOfType(EnqueuedState.class);
        if (optionalLastEnqueuedState.isEmpty()) {
            throw new IllegalStateException("Job cannot succeed if it was not enqueued before.");
        }

        return Duration.between(optionalLastEnqueuedState.get().getEnqueuedAt(), runStartTime());
    }
}
