package org.jobrunr.configuration;

import org.jobrunr.configuration.JobRunrConfiguration.JobRunrConfigurationResult;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProvider.StorageProviderInfo;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.JsonMapperException;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrConfigurationTest {

    @Mock
    JobActivator jobActivator;

    @Mock
    StorageProvider storageProvider;

    @Mock
    StorageProviderInfo storageProviderInfo;

    @Captor
    ArgumentCaptor<JobMapper> jobMapperCaptor;

    @BeforeEach
    void setUpStorageProvider() {
        lenient().when(storageProvider.getStorageProviderInfo()).thenReturn(storageProviderInfo);
        lenient().when(storageProvider.getLongestRunningBackgroundJobServerId()).thenReturn(randomUUID());
        lenient().when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult());
    }

    @AfterEach
    void tearDown() {
        JobRunr.destroy();
    }

    @Test
    void jsonMapperCanBeConfigured() {
        JsonMapper jsonMapper = new GsonJsonMapper();
        JobRunr.configure()
                .useJsonMapper(jsonMapper)
                .useStorageProvider(storageProvider)
                .initialize();

        verify(storageProvider).setJobMapper(jobMapperCaptor.capture());
        JobMapper jobMapper = jobMapperCaptor.getValue();
        assertThat((JsonMapper) getInternalState(jobMapper, "jsonMapper")).isEqualTo(jsonMapper);
    }

    @Test
    void ifJobActivatorIsAddedAfterBackgroundJobServer() {
        assertThatThrownBy(() -> JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer()
                .useJobActivator(jobActivator)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Please configure the JobActivator before the BackgroundJobServer.");
    }

    @Test
    void backgroundJobServerThrowsExceptionIfNoStorageProviderIsAvailable() {
        assertThatThrownBy(() -> JobRunr.configure()
                .useBackgroundJobServer()
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A StorageProvider is required to use a BackgroundJobServer. Please see the documentation on how to setup a job StorageProvider.");
    }

    @Test
    void backgroundJobServerIsNotInstantiatedIfGuardIsFalse() {
        assertThatCode(() -> JobRunr.configure()
                .useBackgroundJobServerIf(false)
        ).doesNotThrowAnyException();
    }

    @Test
    void backgroundJobServerGivenWorkerCountIsUsed() {
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(4)
                .initialize();
        assertThat(JobRunr.getBackgroundJobServer().getServerStatus().getWorkerPoolSize()).isEqualTo(4);
    }

    @Test
    void dashboardThrowsExceptionIfNoStorageProviderIsAvailable() {
        assertThatThrownBy(() -> JobRunr.configure()
                .useDashboard()
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A StorageProvider is required to use a JobRunrDashboardWebServer. Please see the documentation on how to setup a job StorageProvider.");
    }

    @Test
    void dashboardCanBeConfigured() {
        assertThatCode(() -> JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useDashboard()
        ).doesNotThrowAnyException();
    }

    @Test
    void dashboardIsNotStartedIfGuardIsFalse() {
        assertThatCode(() -> JobRunr.configure()
                .useDashboardIf(false)
        ).doesNotThrowAnyException();
    }

    @Test
    void dashboardPortCanBeConfigured() {
        JobRunrConfiguration configuration = JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useDashboard(9000);
        assertThat(configuration.dashboardWebServer).isNotNull();
        assertThat((int) getInternalState(configuration.dashboardWebServer, "port")).isEqualTo(9000);
    }

    @Test
    void initializeGivesAccessToJobSchedulerAndJobRequestScheduler() {
        JobRunrConfigurationResult configurationResult = JobRunr.configure()
                .useStorageProvider(storageProvider)
                .initialize();

        assertThat(configurationResult.getJobScheduler()).isNotNull();
        assertThat(configurationResult.getJobRequestScheduler()).isNotNull();
    }

    @Test
    void initializeShouldNotThrowAJsonMapperExceptionIfNoneOfTheSupportedJsonLibrariesIsOnTheClasspathButACustomJsonMapperIsProvided() {
        assertThat(ReflectionUtils.classExists("com.fasterxml.jackson.databind.ObjectMapper")).isTrue();

        try (MockedStatic<ReflectionUtils> mockedReflectionUtils = Mockito.mockStatic(ReflectionUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedReflectionUtils.when(() -> ReflectionUtils.classExists(any())).thenReturn(false);

            assertThat(ReflectionUtils.classExists("com.fasterxml.jackson.databind.ObjectMapper")).isFalse();
            assertThatCode(() -> JobRunr.configure().useJsonMapper(new JacksonJsonMapper()).useStorageProvider(storageProvider).initialize())
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void initializeShouldThrowAJsonMapperExceptionIfNoneOfTheSupportedJsonLibrariesIsOnTheClasspathButAndACustomJsonMapperIsNotProvided() {
        assertThat(ReflectionUtils.classExists("com.fasterxml.jackson.databind.ObjectMapper")).isTrue();

        try (MockedStatic<ReflectionUtils> mockedReflectionUtils = Mockito.mockStatic(ReflectionUtils.class, Mockito.CALLS_REAL_METHODS)) {
            mockedReflectionUtils.when(() -> ReflectionUtils.classExists(any())).thenReturn(false);

            assertThat(ReflectionUtils.classExists("com.fasterxml.jackson.databind.ObjectMapper")).isFalse();
            assertThatCode(() -> JobRunr.configure().useStorageProvider(storageProvider).initialize())
                    .isInstanceOf(JsonMapperException.class)
                    .hasMessage("No JsonMapper class is found. Make sure you have either Jackson, Gson or a JsonB compliant library available on your classpath. You may also configure a custom JsonMapper.");
        }
    }
}
