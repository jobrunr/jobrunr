package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollIntervalInSecondsTimeBoxIsTooSmallProblemHandlerTest {

    @Mock
    Problems problems;

    @Mock
    StorageProvider storageProvider;

    @Captor
    ArgumentCaptor<Problem> problemArgumentCaptor;

    PollIntervalInSecondsTimeBoxIsTooSmallProblemHandler pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler;

    @BeforeEach
    void setUpNewPollIntervalInSecondsTimeBoxIsTooSmallProblemHandler() {
        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler = new PollIntervalInSecondsTimeBoxIsTooSmallProblemHandler(problems, storageProvider);
        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler.onChange(emptyList());
        reset(problems);
    }

    @Test
    void ifNoChangesOnPollIntervalInSecondsTimeBoxIsTooSmallThenNoProblemsCreated() {
        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler.onChange(emptyList());

        verifyNoInteractions(problems);
    }

    @Test
    void ifChangesOnPollIntervalInSecondsTimeBoxIsTooSmallDetectedThenProblemCreated() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler.onChange(asList(jobRunrMetadata));

        verify(problems).addProblem(problemArgumentCaptor.capture());

        assertThat(problemArgumentCaptor.getValue())
                .isInstanceOf(PollIntervalInSecondsTimeBoxIsTooSmallProblem.class)
                .hasFieldOrPropertyWithValue("pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet", asList(jobRunrMetadata));
    }

    @Test
    void ifPollIntervalInSecondsTimeBoxIsTooSmallIsDeletedThenProblemIsRemoved() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler.onChange(asList(jobRunrMetadata));
        reset(problems);

        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler.onChange(emptyList());
        verify(problems).removeProblemsOfType(PollIntervalInSecondsTimeBoxIsTooSmallProblem.PROBLEM_TYPE);
        verify(problems, never()).addProblem(any());
    }

    @Test
    void ifPollIntervalInSecondsTimeBoxIsTooSmallProblemIsDismissedThenProblemIsRemovedAndDeletedFromStorageProvider() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler.onChange(asList(jobRunrMetadata));
        reset(problems);

        pollIntervalInSecondsTimeBoxIsTooSmallProblemHandler.dismiss();
        verify(problems).removeProblemsOfType(PollIntervalInSecondsTimeBoxIsTooSmallProblem.PROBLEM_TYPE);
        verify(storageProvider).deleteMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName());
    }
}