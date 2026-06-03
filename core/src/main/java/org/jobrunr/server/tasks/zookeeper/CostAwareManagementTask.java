package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.costaware.CostAwareApiClient;
import org.jobrunr.server.costaware.CostAwareApiClientException;
import org.jobrunr.server.costaware.CostAwareConfiguration;
import org.jobrunr.server.costaware.CostAwareConfigurationReader;
import org.jobrunr.server.costaware.CostAwareTotalSavings;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.Paging;
import org.jobrunr.utils.mapper.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.jobrunr.jobs.states.StateName.ENQUEUED;

public class CostAwareManagementTask extends AbstractJobZooKeeperTask {

    private final Integer minAmountSpotInstances;
    private final Integer maxAmountSpotInstances;
    private final Duration scaleUpLatency;
    private final Duration scaleDownLatency;
    private final Duration settlingPeriod;
    private final CostAwareApiClient costAwareApiClient;
    private Instant lastScaleTime = Instant.MIN;
    private Instant lastSavingsCalculationTime = Instant.MIN;

    public CostAwareManagementTask(BackgroundJobServer backgroundJobServer, CostAwareConfiguration costAwareConfiguration, JsonMapper jsonMapper) {
        super(backgroundJobServer);
        CostAwareConfigurationReader configurationReader = new CostAwareConfigurationReader(costAwareConfiguration);
        this.costAwareApiClient = new CostAwareApiClient(configurationReader, jsonMapper);

        this.minAmountSpotInstances = configurationReader.getMinSpotInstances();
        this.maxAmountSpotInstances = configurationReader.getMaxSpotInstances();
        this.scaleUpLatency = configurationReader.getScaleUpLatency();
        this.scaleDownLatency = configurationReader.getScaleDownLatency();
        this.settlingPeriod = configurationReader.getSettlingPeriod();
    }

    @Override
    protected void runTask() {
        if (costAwareApiClient.isDisabled()) return;

        scaleIfNecessary();
        saveGlobalCostSavingsIfNecessary();
    }

    private void scaleIfNecessary() {
        if ((isSettling())) return;
        JobRunrMetadata clusterId = super.storageProvider.getMetadata("id", "cluster");

        Duration jobLatency = getCurrentJobLatency();
        try {
            List<BackgroundJobServerStatus> backgroundJobServerSpotInstances = getBackgroundJobServerSpotInstances();
            if (needsToScaleUpBecauseLatencyIsTooHigh(backgroundJobServerSpotInstances, jobLatency)) {
                // scale up
                costAwareApiClient.scaleUp(clusterId.getValue());
                lastScaleTime = Instant.now();
            } else if (needsToScaleDownBecauseLatencyIsLow(backgroundJobServerSpotInstances, jobLatency)) {
                // scale down
                costAwareApiClient.scaleDown(clusterId.getValue());
                lastScaleTime = Instant.now();
            }
        } catch (CostAwareApiClientException e) {
            LOGGER.error("Error scaling SPOT instances", e);
        }
    }

    private void saveGlobalCostSavingsIfNecessary() {
        if (hasRecentlyCalculatedSavings()) return;

        CostAwareTotalSavings costAwareTotalSavings = getCostAwareTotalSavings();
        List<BackgroundJobServerStatus> backgroundJobServers = storageProvider.getBackgroundJobServers();
        costAwareTotalSavings.save(backgroundJobServers);
        saveCostAwareTotalSavings(costAwareTotalSavings);
        lastSavingsCalculationTime = Instant.now();
    }

    private boolean needsToScaleUpBecauseLatencyIsTooHigh(List<BackgroundJobServerStatus> backgroundJobServerSpotInstances, Duration jobLatency) {
        return backgroundJobServerSpotInstances.size() < maxAmountSpotInstances
                && jobLatency != null && jobLatency.compareTo(scaleUpLatency) > 0;
    }

    private boolean needsToScaleDownBecauseLatencyIsLow(List<BackgroundJobServerStatus> backgroundJobServerSpotInstances, Duration jobLatency) {
        return (backgroundJobServerSpotInstances.size() > minAmountSpotInstances)
                && (jobLatency == null || jobLatency.compareTo(scaleDownLatency) < 0);
    }

    private List<BackgroundJobServerStatus> getBackgroundJobServerSpotInstances() {
        return storageProvider.getBackgroundJobServers()
                .stream()
                .filter(x -> x.getMetadata() != null && x.getMetadata().getSpotPrice() != null)
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

    private boolean hasRecentlyCalculatedSavings() {
        return lastSavingsCalculationTime.plus(backgroundJobServer.getConfiguration().getPollInterval().multipliedBy(4)).isAfter(Instant.now());
    }

    private CostAwareTotalSavings getCostAwareTotalSavings() {
        JobRunrMetadata jobRunrMetadata = storageProvider.getMetadata("total-savings", "cluster");
        if (jobRunrMetadata == null) return new CostAwareTotalSavings();
        String totalSavingsJson = jobRunrMetadata.getValue();
        return backgroundJobServer.getJsonMapper().deserialize(totalSavingsJson, CostAwareTotalSavings.class);
    }

    private void saveCostAwareTotalSavings(CostAwareTotalSavings costAwareTotalSavings) {
        String costAwareTotalSavingsAsJson = backgroundJobServer.getJsonMapper().serialize(costAwareTotalSavings);
        storageProvider.saveMetadata(new JobRunrMetadata("total-savings", "cluster", costAwareTotalSavingsAsJson));
    }
}
