package org.jobrunr.quarkus.autoconfigure.server;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.lookup.LookupIfProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.JobActivatorShutdownException;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
import org.jobrunr.server.configuration.DefaultBackgroundJobServerWorkerPolicy;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import static java.util.Collections.singletonList;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

@Singleton
public class JobRunrBackgroundJobServerProducer {

    @Inject
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    @Produces
    @DefaultBean
    @Singleton
    BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy() {
        if (jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            BackgroundJobServerThreadType threadType = jobRunrRuntimeConfiguration.backgroundJobServer().threadType().orElse(BackgroundJobServerThreadType.getDefaultThreadType());
            int workerCount = jobRunrRuntimeConfiguration.backgroundJobServer().workerCount().orElse(threadType.getDefaultWorkerCount());
            return new DefaultBackgroundJobServerWorkerPolicy(workerCount, threadType);
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    BackgroundJobServerConfiguration backgroundJobServerConfiguration(BackgroundJobServerWorkerPolicy backgroundJobServerWorkerPolicy) {
        if (jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            final BackgroundJobServerConfiguration backgroundJobServerConfiguration = usingStandardBackgroundJobServerConfiguration();

            backgroundJobServerConfiguration.andBackgroundJobServerWorkerPolicy(backgroundJobServerWorkerPolicy);

            jobRunrRuntimeConfiguration.backgroundJobServer().name().ifPresent(backgroundJobServerConfiguration::andName);
            jobRunrRuntimeConfiguration.backgroundJobServer().pollIntervalInSeconds().ifPresent(backgroundJobServerConfiguration::andPollIntervalInSeconds);
            jobRunrRuntimeConfiguration.backgroundJobServer().serverTimeoutPollIntervalMultiplicand().ifPresent(backgroundJobServerConfiguration::andServerTimeoutPollIntervalMultiplicand);
            jobRunrRuntimeConfiguration.backgroundJobServer().deleteSucceededJobsAfter().ifPresent(backgroundJobServerConfiguration::andDeleteSucceededJobsAfter);
            jobRunrRuntimeConfiguration.backgroundJobServer().permanentlyDeleteDeletedJobsAfter().ifPresent(backgroundJobServerConfiguration::andPermanentlyDeleteDeletedJobsAfter);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwaitingJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andCarbonAwaitingJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().scheduledJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andScheduledJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().orphanedJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andOrphanedJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().succeededJobsRequestSize().ifPresent(backgroundJobServerConfiguration::andSucceededJobsRequestSize);
            jobRunrRuntimeConfiguration.backgroundJobServer().interruptJobsAwaitDurationOnStop().ifPresent(backgroundJobServerConfiguration::andInterruptJobsAwaitDurationOnStopBackgroundJobServer);

            CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration = CarbonAwareJobProcessingConfiguration.usingDisabledCarbonAwareJobProcessingConfiguration();
            carbonAwareJobProcessingConfiguration.andCarbonAwareSchedulingEnabled(jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().enabled());
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().areaCode().ifPresent(carbonAwareJobProcessingConfiguration::andAreaCode);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().dataProvider().ifPresent(carbonAwareJobProcessingConfiguration::andDataProvider);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().externalCode().ifPresent(carbonAwareJobProcessingConfiguration::andExternalCode);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().externalIdentifier().ifPresent(carbonAwareJobProcessingConfiguration::andExternalIdentifier);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().apiClientConnectTimeout().ifPresent(carbonAwareJobProcessingConfiguration::andApiClientConnectTimeout);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().apiClientReadTimeout().ifPresent(carbonAwareJobProcessingConfiguration::andApiClientReadTimeout);
            jobRunrRuntimeConfiguration.backgroundJobServer().carbonAwareJobProcessing().pollIntervalInMinutes().ifPresent(carbonAwareJobProcessingConfiguration::andPollIntervalInMinutes);
            backgroundJobServerConfiguration.andCarbonAwareJobProcessingConfiguration(carbonAwareJobProcessingConfiguration);
            return backgroundJobServerConfiguration;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    @LookupIfProperty(name = "quarkus.jobrunr.background-job-server.enabled", stringValue = "true")
    BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JsonMapper jobRunrJsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        if (jobRunrRuntimeConfiguration.backgroundJobServer().enabled()) {
            int defaultNbrOfRetries = jobRunrRuntimeConfiguration.jobs().defaultNumberOfRetries().orElse(RetryFilter.DEFAULT_NBR_OF_RETRIES);
            int retryBackOffTimeSeed = jobRunrRuntimeConfiguration.jobs().retryBackOffTimeSeed().orElse(RetryFilter.DEFAULT_BACKOFF_POLICY_TIME_SEED);
            BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobRunrJsonMapper, jobActivator, backgroundJobServerConfiguration);
            backgroundJobServer.setJobFilters(singletonList(new RetryFilter(defaultNbrOfRetries, retryBackOffTimeSeed)));
            return backgroundJobServer;
        }
        return null;
    }

    @Produces
    @DefaultBean
    @Singleton
    public JobActivator jobActivator() {
        return new JobActivator() {
            @Override
            public <T> T activateJob(Class<T> aClass) {
                try {
                    return CDI.current().select(aClass).get();
                } catch (IllegalStateException e) {
                    throw new JobActivatorShutdownException("Quarkus CDI container is shutting down", e);
                }
            }
        };
    }
}
