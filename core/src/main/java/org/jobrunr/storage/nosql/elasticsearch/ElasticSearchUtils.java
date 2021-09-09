package org.jobrunr.storage.nosql.elasticsearch;

public class ElasticSearchUtils {

    private ElasticSearchUtils() {
    }

    public static final String JOBRUNR_PREFIX = "jobrunr_";

    public static void sleep(long amount) {
        try {
            Thread.sleep(amount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
