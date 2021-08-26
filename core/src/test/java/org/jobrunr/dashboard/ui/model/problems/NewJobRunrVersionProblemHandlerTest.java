package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.server.dashboard.NewJobRunrVersionNotification;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NewJobRunrVersionProblemHandlerTest {

    @Mock
    Problems problems;

    @Mock
    StorageProvider storageProvider;

    @Captor
    ArgumentCaptor<Problem> problemArgumentCaptor;

    NewJobRunrVersionProblemHandler jobRunrVersionProblemHandler;

    @BeforeEach
    void setUpNewJobRunrVersionProblemHandler() {
        jobRunrVersionProblemHandler = new NewJobRunrVersionProblemHandler(problems, storageProvider);
    }

    @Test
    void ifNewVersionFoundThenProblemIsCreated() {
        jobRunrVersionProblemHandler.onChange(asList(new JobRunrMetadata(NewJobRunrVersionNotification.class.getSimpleName(), "cluster", "4.0.0")));
        jobRunrVersionProblemHandler.onChange(asList(new JobRunrMetadata(NewJobRunrVersionNotification.class.getSimpleName(), "cluster", "4.0.0")));

        verify(problems, times(1)).addProblem(problemArgumentCaptor.capture());

        assertThat(problemArgumentCaptor.getValue())
                .isInstanceOf(NewJobRunrVersionProblem.class)
                .hasFieldOrPropertyWithValue("latestVersion", "4.0.0");
    }

    @Test
    void ifVersionIsUpdatedThenProblemIsDismissed() {
        jobRunrVersionProblemHandler.onChange(asList(new JobRunrMetadata(NewJobRunrVersionNotification.class.getSimpleName(), "cluster", "4.0.0")));
        jobRunrVersionProblemHandler.onChange(asList());
        jobRunrVersionProblemHandler.onChange(asList());

        verify(problems, times(1)).removeProblemsOfType(NewJobRunrVersionProblem.PROBLEM_TYPE);
    }

}