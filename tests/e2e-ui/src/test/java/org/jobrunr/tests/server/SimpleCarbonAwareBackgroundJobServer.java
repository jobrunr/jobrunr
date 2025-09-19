package org.jobrunr.tests.server;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.server.carbonaware.CarbonIntensityApiStubServer;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingStandardCarbonAwareJobProcessingConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;

public class SimpleCarbonAwareBackgroundJobServer extends AbstractSimpleBackgroundJobServer {

    private CarbonIntensityApiStubServer carbonIntensityStubServer;
    private List<RecurringJob> recurringJobs = new ArrayList<>();
    private int bestIntensityMomentToday = LocalTime.now(ZoneId.systemDefault()).getHour();

    public SimpleCarbonAwareBackgroundJobServer() {
        withCarbonAwareJobProcessing(usingStandardCarbonAwareJobProcessingConfiguration()
                .andAreaCode("BE"));
        withJacksonMapper();
        withId(UUID.randomUUID());
    }

    @Override
    public void start() {
        this.carbonIntensityStubServer = new CarbonIntensityApiStubServer()
                .andPort(10000)
                .andBestIntensityMomentTodayAt(bestIntensityMomentToday)
                .andCarbonAwareJobProcessingConfig(carbonAwareConfig)
                .start();

        super.start();
    }

    @Override
    public void stop() {
        this.carbonIntensityStubServer.stop();
        super.stop();
    }

    @Override
    protected StorageProvider initStorageProvider() {
        throw new UnsupportedOperationException("use withStorageProvider() instead");
    }

    public SimpleCarbonAwareBackgroundJobServer andBestIntensityMomentToday(int hour) {
        this.bestIntensityMomentToday = hour;
        return this;
    }

    public SimpleCarbonAwareBackgroundJobServer andRecurringJobs(RecurringJob... recurringJobs) {
        this.recurringJobs = asList(recurringJobs);
        return this;
    }

    @Override
    protected void loadDefaultData(StorageProvider storageProvider) {
        final BackgroundJobServerStatus backgroundJobServerStatus = aDefaultBackgroundJobServerStatus()
                .withPollIntervalInSeconds(5)
                .withIsStarted()
                .withId(id) // make sure this is master and we process tasks
                .build();
        storageProvider.announceBackgroundJobServer(backgroundJobServerStatus);

        for (RecurringJob recurringJob : recurringJobs) {
            storageProvider.saveRecurringJob(recurringJob);
        }
    }
}
