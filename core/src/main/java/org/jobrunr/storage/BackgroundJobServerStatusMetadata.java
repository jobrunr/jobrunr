package org.jobrunr.storage;

import org.jobrunr.jobs.filters.BackgroundJobStatisticsFilter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class BackgroundJobServerStatusMetadata {
    private BigDecimal instancePrice;
    private BigDecimal spotPrice;
    private BigDecimal serverSavings;
    private String provider;
    private long processingJobs;
    private long succeededJobs;
    private long failedJobs;

    private BackgroundJobServerStatusMetadata() {
    }

    public BackgroundJobServerStatusMetadata(Instant firstHeartbeat, BackgroundJobStatisticsFilter backgroundJobStatisticsFilter) {
        this(firstHeartbeat, backgroundJobStatisticsFilter.getProcessingJobs(), backgroundJobStatisticsFilter.getSucceededJobs(), backgroundJobStatisticsFilter.getFailedJobs());
    }

    public BackgroundJobServerStatusMetadata(Instant firstHeartbeat, long processingJobs, long succeededJobs, long failedJobs) {
        Map<String, String> environment = System.getenv();
        if (environment.get("JOBRUNR_COST_AWARE_INSTANCE_PRICE") != null && environment.get("JOBRUNR_COST_AWARE_SPOT_PRICE") != null) {
            instancePrice = new BigDecimal(environment.get("JOBRUNR_COST_AWARE_INSTANCE_PRICE"));
            spotPrice = new BigDecimal(environment.get("JOBRUNR_COST_AWARE_SPOT_PRICE"));
            provider = environment.get("JOBRUNR_COST_AWARE_PROVIDER");
            serverSavings = calculateSavingsSince(firstHeartbeat);
        }

        this.processingJobs = processingJobs;
        this.succeededJobs = succeededJobs;
        this.failedJobs = failedJobs;
    }

    public BigDecimal getInstancePrice() {
        return instancePrice;
    }

    public void setInstancePrice(BigDecimal instancePrice) {
        this.instancePrice = instancePrice;
    }

    public BigDecimal getSpotPrice() {
        return spotPrice;
    }

    public void setSpotPrice(BigDecimal spotPrice) {
        this.spotPrice = spotPrice;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public long getProcessingJobs() {
        return processingJobs;
    }

    public void setProcessingJobs(long processingJobs) {
        this.processingJobs = processingJobs;
    }

    public long getSucceededJobs() {
        return succeededJobs;
    }

    public void setSucceededJobs(long succeededJobs) {
        this.succeededJobs = succeededJobs;
    }

    public long getFailedJobs() {
        return failedJobs;
    }

    public void setFailedJobs(long failedJobs) {
        this.failedJobs = failedJobs;
    }

    public BigDecimal getServerSavings() {
        return serverSavings;
    }

    public void setServerSavings(BigDecimal serverSavings) {
        this.serverSavings = serverSavings;
    }

    private BigDecimal calculateSavingsSince(Instant firstHeartbeat) {
        double hoursSinceFirstHeartbeat = Duration.between(firstHeartbeat, Instant.now()).toMinutes() / 60.0;
        BigDecimal amountSpentOnSpot = spotPrice.multiply(BigDecimal.valueOf(hoursSinceFirstHeartbeat));
        BigDecimal amountSpentOnInstance = instancePrice.multiply(BigDecimal.valueOf(hoursSinceFirstHeartbeat));

        return amountSpentOnInstance.subtract(amountSpentOnSpot);
    }

    public static BackgroundJobServerStatusMetadata from(String metadataString) {
        if (metadataString == null) return null;

        String[] metadataEntries = metadataString.split(";");
        BackgroundJobServerStatusMetadata jobServerStatusMetadata = new BackgroundJobServerStatusMetadata();
        for (String entry : metadataEntries) {
            String key = entry.split(":")[0];
            String value = entry.split(":")[1];
            switch (key) {
                case "instancePrice" -> jobServerStatusMetadata.setInstancePrice(new BigDecimal(value));
                case "spotPrice" -> jobServerStatusMetadata.setSpotPrice(new BigDecimal(value));
                case "provider" -> jobServerStatusMetadata.setProvider(value);
                case "savings" -> jobServerStatusMetadata.setServerSavings(new BigDecimal(value));
                case "processingJobs" -> jobServerStatusMetadata.setProcessingJobs(Long.parseLong(value));
                case "succeededJobs" -> jobServerStatusMetadata.setSucceededJobs(Long.parseLong(value));
                case "failedJobs" -> jobServerStatusMetadata.setFailedJobs(Long.parseLong(value));
                default -> throw new IllegalArgumentException("Unrecognized key: " + key);
            }
        }
        return jobServerStatusMetadata;
    }

    @Override
    public String toString() {
        // todo: test length < 512
        StringBuilder metadataBuilder = new StringBuilder();

        if (instancePrice != null && spotPrice != null) {
            metadataBuilder.append("instancePrice:").append(instancePrice).append(";");
            metadataBuilder.append("spotPrice:").append(spotPrice).append(";");
            metadataBuilder.append("provider:").append(provider).append(";");
            metadataBuilder.append("savings:").append(serverSavings).append(";");
        }

        metadataBuilder.append("processingJobs:").append(processingJobs).append(";");
        metadataBuilder.append("succeededJobs:").append(succeededJobs).append(";");
        metadataBuilder.append("failedJobs:").append(failedJobs).append(";");

        return metadataBuilder.toString();
    }
}
