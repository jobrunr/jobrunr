package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.costaware.CostAwareApiClient;
import org.jobrunr.server.costaware.CostAwareApiClientException;
import org.jobrunr.server.costaware.CostAwareConfiguration;
import org.jobrunr.server.costaware.CostAwareConfigurationReader;
import org.jobrunr.server.costaware.CostAwareTotalSavings;
import org.jobrunr.server.tasks.zookeeper.CostAwareManagementTask.SpotScalingMetadata.ScalingDirection;
import org.jobrunr.server.tasks.zookeeper.CostAwareManagementTask.SpotScalingMetadata.ScalingStatus;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.Paging;
import org.jobrunr.utils.mapper.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.jobrunr.jobs.states.StateName.ENQUEUED;

// TODO CostAwareAutoScalingTask?
// TODO there are almost no tests covering the newly added classes
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

        // TODO how about having the configuration as an attribute?
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
        checkForScaleComplete();
        if (isSettling()) return;
        // TODO should the cluster id be fetched on construction?
        JobRunrMetadata clusterId = super.storageProvider.getMetadata("id", "cluster");

        Duration jobLatency = getQueueLatency();
        try {
            // TODO getBackgroundJobServerSpotInstances is called twice within a run
            long amountOfSpotBackgroundJobServers = getAmountOfSpotBackgroundJobServers();
            if (needsToScaleUpBecauseLatencyIsTooHigh(amountOfSpotBackgroundJobServers, jobLatency)) {
                // TODO to be extracted into scaleUp and scaleDown methods
                // scale up
                SpotScalingMetadata spotScalingMetadata = new SpotScalingMetadata(ScalingDirection.UP, amountOfSpotBackgroundJobServers, ScalingStatus.PROCESSING);
                saveSpotScalingMetadata(spotScalingMetadata);
                LOGGER.info("JobRunr is scaling up, creating a new spot instance");
                costAwareApiClient.scaleUp(clusterId.getValue());
                lastScaleTime = Instant.now();
                spotScalingMetadata.setScalingStatus(ScalingStatus.PROVISIONED);
                saveSpotScalingMetadata(spotScalingMetadata);
            } else if (needsToScaleDownBecauseLatencyIsLow(amountOfSpotBackgroundJobServers, jobLatency)) {
                // scale down
                SpotScalingMetadata spotScalingMetadata = new SpotScalingMetadata(ScalingDirection.DOWN, amountOfSpotBackgroundJobServers, ScalingStatus.PROCESSING);
                saveSpotScalingMetadata(spotScalingMetadata);
                LOGGER.info("JobRunr is scaling down, removing the oldest spot instance");
                costAwareApiClient.scaleDown(clusterId.getValue());
                lastScaleTime = Instant.now();
                spotScalingMetadata.setScalingStatus(ScalingStatus.SCALED_DOWN);
                saveSpotScalingMetadata(spotScalingMetadata);
            }
        } catch (CostAwareApiClientException e) {
            SpotScalingMetadata scalingMetadata = getSpotScalingMetadata();
            scalingMetadata.setScalingStatus(ScalingStatus.FAILED);
            saveSpotScalingMetadata(scalingMetadata);
            LOGGER.error("Error scaling SPOT instances", e);
        }
    }

    private void checkForScaleComplete() {
        SpotScalingMetadata scalingMetadata = getSpotScalingMetadata();
        if (scalingMetadata == null) return;
        // TODO aren't we logging too much?
        if (scalingMetadata.getDirection() == ScalingDirection.UP) {
            if (getAmountOfSpotBackgroundJobServers() >= scalingMetadata.getAmountOfSpotBackgroundJobServers() + 1) {
                LOGGER.info("Scaling up a spot instance is complete");
                super.storageProvider.deleteMetadata("spot-scaling", "cluster");
            }
        } else if (scalingMetadata.getDirection() == ScalingDirection.DOWN) {
            if (getAmountOfSpotBackgroundJobServers() <= scalingMetadata.getAmountOfSpotBackgroundJobServers() - 1) {
                LOGGER.info("Scaling down a spot instance is complete");
                super.storageProvider.deleteMetadata("spot-scaling", "cluster");
            }
        }
    }

    private void saveGlobalCostSavingsIfNecessary() {
        // TODO why not save every poll interval?
        if (hasRecentlyCalculatedSavings()) return;

        CostAwareTotalSavings costAwareTotalSavings = getCostAwareTotalSavings();
        List<BackgroundJobServerStatus> backgroundJobServers = storageProvider.getBackgroundJobServers();
        costAwareTotalSavings.save(backgroundJobServers, backgroundJobServer.getConfiguration().getPollInterval());
        saveCostAwareTotalSavings(costAwareTotalSavings);
        lastSavingsCalculationTime = Instant.now();
    }

    private boolean needsToScaleUpBecauseLatencyIsTooHigh(long amountOfSpotBackgroundJobServers, Duration jobLatency) {
        return amountOfSpotBackgroundJobServers < minAmountSpotInstances
                || (amountOfSpotBackgroundJobServers < maxAmountSpotInstances && jobLatency.compareTo(scaleUpLatency) > 0);
    }

    private boolean needsToScaleDownBecauseLatencyIsLow(long amountOfSpotBackgroundJobServers, Duration jobLatency) {
        return amountOfSpotBackgroundJobServers > minAmountSpotInstances
                && jobLatency.compareTo(scaleDownLatency) < 0;
    }

    private long getAmountOfSpotBackgroundJobServers() {
        return storageProvider.getBackgroundJobServers()
                .stream()
                .filter(x -> x.getMetadata() != null && x.getMetadata().getSpotPrice() != null)
                .count();
    }

    private boolean isSettling() {
        Instant lastScaleTimePlusSettlingPeriod = lastScaleTime.plus(settlingPeriod);
        return lastScaleTimePlusSettlingPeriod.isAfter(Instant.now());
    }

    private Duration getQueueLatency() {
        List<Job> jobList = storageProvider.getJobList(ENQUEUED, Paging.AmountBasedList.ascOnCreatedAt(1));
        if (jobList.isEmpty()) return Duration.ZERO;

        return getJobLatency(jobList.get(0));
    }

    private Duration getJobLatency(Job job) {
        EnqueuedState jobState = job.getJobState();
        return Duration.between(jobState.getEnqueuedAt(), runStartTime()); // TODO why run start time, why not Instant.now()?
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
        // TODO shall we add these strings as constant to StorageProviderUtils?
        storageProvider.saveMetadata(new JobRunrMetadata("total-savings", "cluster", costAwareTotalSavingsAsJson));
    }

    public SpotScalingMetadata getSpotScalingMetadata() {
        JobRunrMetadata metadata = storageProvider.getMetadata("spot-scaling", "cluster");
        if (metadata == null) return null;
        return backgroundJobServer.getJsonMapper().deserialize(metadata.getValue(), SpotScalingMetadata.class);
    }

    public void saveSpotScalingMetadata(SpotScalingMetadata spotScalingMetadata) {
        String spotScalingMetadataAsString = backgroundJobServer.getJsonMapper().serialize(spotScalingMetadata);
        JobRunrMetadata jobRunrMetadata = new JobRunrMetadata("spot-scaling", "cluster", spotScalingMetadataAsString);
        storageProvider.saveMetadata(jobRunrMetadata);
    }

    public static class SpotScalingMetadata {
        // TODO make the fields final?
        private ScalingDirection direction;
        private long amountOfSpotBackgroundJobServers; // TODO amountOfRegisteredServers?
        private ScalingStatus scalingStatus;

        private SpotScalingMetadata() {
        }

        public SpotScalingMetadata(ScalingDirection direction, long amountOfSpotBackgroundJobServers, ScalingStatus scalingStatus) {
            this.direction = direction;
            this.amountOfSpotBackgroundJobServers = amountOfSpotBackgroundJobServers;
            this.scalingStatus = scalingStatus;
        }

        public ScalingDirection getDirection() {
            return direction;
        }

        public long getAmountOfSpotBackgroundJobServers() {
            return amountOfSpotBackgroundJobServers;
        }

        public ScalingStatus getScalingStatus() {
            return scalingStatus;
        }

        public void setScalingStatus(ScalingStatus scalingStatus) {
            this.scalingStatus = scalingStatus;
        }

        public enum ScalingStatus {
            PROCESSING, PROVISIONED, SCALED_DOWN, FAILED // TODO use provisioned, should it be SCALED_UP? or should SCALED_DOWN become DECOMMISSIONED?
        }

        public enum ScalingDirection {
            UP, DOWN
        }
    }
}
