package org.jobrunr.server.tasks.startup;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.RecurringJob.CreatedBy.ANNOTATION;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckIfAllJobsExistTaskTest {

    private CheckIfAllJobsExistTask checkIfAllJobsExistTask;

    @Mock
    private BackgroundJobServer backgroundJobServer;

    @Mock
    private StorageProvider storageProvider;

    private ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUp() {
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);

        checkIfAllJobsExistTask = new CheckIfAllJobsExistTask(backgroundJobServer);

        logger = LoggerAssert.initFor(checkIfAllJobsExistTask);
    }

    @Test
    void onRunItDeletesRecurringJobsCreatedViaTheAnnotationAndLogsAllRecurringJobsCreatedViaTheAPIThatDoNotExist() {
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(asList(
                aDefaultRecurringJob().withId("created-by-api").build(),
                aDefaultRecurringJob().withId("created-by-annotation-but-annotation-deleted").withCreatedBy(ANNOTATION).build(),
                aDefaultRecurringJob().withId("created-by-api-but-code-deleted").withJobDetails(classThatDoesNotExistJobDetails()).build(),
                aDefaultRecurringJob().withId("created-by-annotation-but-code-deleted").withJobDetails(methodThatDoesNotExistJobDetails().withMethodName("methodA")).withCreatedBy(ANNOTATION).build(),
                aDefaultRecurringJob().withId("no-created-by-but-code-deleted").withJobDetails(methodThatDoesNotExistJobDetails().withMethodName("methodB")).withCreatedBy(null).build()
        )));

        checkIfAllJobsExistTask.run();

        InOrder inOrder = inOrder(storageProvider);
        inOrder.verify(storageProvider).deleteRecurringJob("created-by-annotation-but-annotation-deleted");
        inOrder.verify(storageProvider).deleteRecurringJob("created-by-annotation-but-code-deleted");

        assertThat(logger)
                .hasWarningMessageContaining("JobRunr found RECURRING jobs that do not exist anymore")
                .hasWarningMessageContaining("i.dont.exist.Class.notImportant(java.lang.Integer)")
                .hasWarningMessageContaining("org.jobrunr.stubs.TestService.methodB(java.lang.Integer)")
                .hasNoErrorLogMessages();
    }

    @Test
    void onRunItLogsAllScheduledJobsThatDoNotExist() {
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult());
        when(storageProvider.getDistinctJobSignatures(SCHEDULED)).thenReturn(Set.of(
                getJobSignature(defaultJobDetails().build()),
                getJobSignature(classThatDoesNotExistJobDetails().build())
        ));

        checkIfAllJobsExistTask.run();

        assertThat(logger)
                .hasWarningMessageContaining("JobRunr found SCHEDULED jobs that do not exist anymore")
                .hasWarningMessageContaining("i.dont.exist.Class.notImportant(java.lang.Integer)")
                .hasNoErrorLogMessages();
    }

    @Test
    void onRunItLogsAllScheduledAndRecurringJobsThatDoNotExist() {
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(asList(
                aDefaultRecurringJob().build(),
                aDefaultRecurringJob().withJobDetails(classThatDoesNotExistJobDetails()).build()
        )));

        when(storageProvider.getDistinctJobSignatures(SCHEDULED)).thenReturn(Set.of(
                getJobSignature(defaultJobDetails().build()),
                getJobSignature(methodThatDoesNotExistJobDetails().build())
        ));

        checkIfAllJobsExistTask.run();

        assertThat(logger)
                .hasWarningMessageContaining("JobRunr found RECURRING AND SCHEDULED jobs that do not exist anymore in your code.")
                .hasWarningMessageContaining("i.dont.exist.Class.notImportant(java.lang.Integer)")
                .hasWarningMessageContaining("org.jobrunr.stubs.TestService.doWorkThatDoesNotExist(java.lang.Integer)")
                .hasNoErrorLogMessages();
    }
}