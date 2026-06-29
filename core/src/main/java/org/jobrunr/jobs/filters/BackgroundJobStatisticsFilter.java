package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class BackgroundJobStatisticsFilter implements JobServerFilter {

    private final AtomicLong processingJobs;
    private final AtomicLong succeededJobs;
    private final AtomicLong failedJobs;
    private final BigDecimal instancePrice;
    private final BigDecimal spotPrice;

    public BackgroundJobStatisticsFilter() {
        processingJobs = new AtomicLong();
        succeededJobs = new AtomicLong();
        failedJobs = new AtomicLong();
        Map<String, String> environment = System.getenv();

        if (environment.get("JOBRUNR_COST_AWARE_INSTANCE_PRICE") != null && environment.get("JOBRUNR_COST_AWARE_SPOT_PRICE") != null) {
            instancePrice = new BigDecimal(environment.get("JOBRUNR_COST_AWARE_INSTANCE_PRICE"));
            spotPrice = new BigDecimal(environment.get("JOBRUNR_COST_AWARE_SPOT_PRICE"));
        } else {
            instancePrice = BigDecimal.ZERO;
            spotPrice = BigDecimal.ZERO;
        }
    }

    @Override
    public void onProcessing(Job job) {
        job.getMetadata().put("instancePrice", instancePrice);
        job.getMetadata().put("spotPrice", spotPrice);
        // TODO For this increment and decrement, I'd use onThreadReleased and onThreadOccupied, shall we copy it from Pro?
        processingJobs.incrementAndGet();
    }

    @Override
    public void onProcessingSucceeded(Job job) {
        // TODO We're alreaddy using atomic values, no need for synchronized on top?
        synchronized (this) {
            if (processingJobs.get() > 0) {
                processingJobs.decrementAndGet();
            }
            succeededJobs.incrementAndGet();
        }
    }

    @Override
    public void onProcessingFailed(Job job, Exception e) {
        // TODO We're alreaddy using atomic values, no need for synchronized on top?
        synchronized (this) {
            if (processingJobs.get() > 0) {
                processingJobs.decrementAndGet();
            }
            failedJobs.incrementAndGet();
        }
    }

    public Long getProcessingJobs() {
        return processingJobs.get();
    }

    public Long getSucceededJobs() {
        return succeededJobs.get();
    }

    public Long getFailedJobs() {
        return failedJobs.get();
    }
}
