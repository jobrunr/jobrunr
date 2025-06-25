package org.jobrunr;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterAssert;
import net.javacrumbs.jsonunit.assertj.JsonAssert;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.IdListAssert;
import org.assertj.db.DatabaseAssertions;
import org.jobrunr.dashboard.server.http.client.HttpResponseAssert;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobAssert;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobDetailsAssert;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.RecurringJobAssert;
import org.jobrunr.jobs.carbonaware.CarbonAwareJobProcessingConfigurationAssert;
import org.jobrunr.jobs.carbonaware.CarbonIntensityForecastAssert;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriodAssert;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerAssert;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServerConfigurationAssert;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.JobRunrMetadataAssert;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderAssert;

import javax.sql.DataSource;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;

public class JobRunrAssertions extends Assertions {

    public static Condition<Throwable> failedJob(Job job) {
        return new Condition<>(x -> x instanceof ConcurrentJobModificationException && ((ConcurrentJobModificationException) x).getConcurrentUpdatedJobs().contains(job), "Should contain job");
    }

    public static <T extends Job> IdListAssert<Job, JobAssert> assertThatJobs(Page<T> jobPage) {
        return assertThatJobs(jobPage.getItems());
    }

    public static <T extends Job> IdListAssert<Job, JobAssert> assertThatJobs(List<T> jobs) {
        return new IdListAssert<>(jobs, JobAssert::new).usingRecursiveFieldByFieldElementComparatorIgnoringFields("locker", "newState", "jobHistory.exception", "stateIndexBeforeStateChange");
    }

    public static IdListAssert<RecurringJob, RecurringJobAssert> assertThatRecurringJobs(List<RecurringJob> recurringJobs) {
        return new IdListAssert<>(recurringJobs, RecurringJobAssert::new).usingRecursiveFieldByFieldElementComparatorIgnoringFields("locker", "jobHistory.exception", "nextRunAt");
    }

    public static JobAssert assertThat(Job job) {
        return JobAssert.assertThat(job);
    }

    public static RecurringJobAssert assertThat(RecurringJob recurringJob) {
        return RecurringJobAssert.assertThat(recurringJob);
    }

    public static JobDetailsAssert assertThat(JobDetails jobDetails) {
        return JobDetailsAssert.assertThat(jobDetails);
    }

    public static JobRunrMetadataAssert assertThat(JobRunrMetadata metadata) {
        return JobRunrMetadataAssert.assertThat(metadata);
    }

    public static BackgroundJobServerConfigurationAssert assertThat(BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        return BackgroundJobServerConfigurationAssert.assertThat(backgroundJobServerConfiguration);
    }

    public static BackgroundJobServerAssert assertThat(BackgroundJobServer backgroundJobServer) {
        return BackgroundJobServerAssert.assertThat(backgroundJobServer);
    }

    public static StorageProviderAssert assertThat(StorageProvider storageProvider) {
        return StorageProviderAssert.assertThat(storageProvider);
    }

    public static HttpResponseAssert assertThat(HttpResponse httpResponse) {
        return HttpResponseAssert.assertThat(httpResponse);
    }

    public static JsonAssert.ConfigurableJsonAssert assertThatJson(String json) {
        return JsonAssertions.assertThatJson(json);
    }

    public static MeterAssert assertThat(Meter meter) {
        return MeterAssert.assertThat(meter);
    }

    public static LoggerAssert assertThat(ListAppender<ILoggingEvent> listAppender) {
        return LoggerAssert.assertThat(listAppender);
    }

    public static DatabaseAssertions assertThat(DataSource dataSource) {
        return DatabaseAssertions.assertThat(dataSource);
    }

    public static CarbonIntensityForecastAssert assertThat(CarbonIntensityForecast carbonIntensityForecast) {
        return CarbonIntensityForecastAssert.assertThat(carbonIntensityForecast);
    }

    public static CarbonAwarePeriodAssert assertThat(CarbonAwarePeriod carbonAwarePeriod) {
        return CarbonAwarePeriodAssert.assertThat(carbonAwarePeriod);
    }

    public static CarbonAwareJobProcessingConfigurationAssert assertThat(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        return CarbonAwareJobProcessingConfigurationAssert.assertThat(carbonAwareJobProcessingConfiguration);
    }

    public static CarbonAwareJobProcessingConfigurationAssert assertThat(CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration) {
        return CarbonAwareJobProcessingConfigurationAssert.assertThat(carbonAwareJobProcessingConfiguration);
    }

    public static String contentOfResource(String resourceName) {
        try (InputStream inputStream = JobRunrAssertions.class.getResourceAsStream(resourceName)) {
            assert inputStream != null;

            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
