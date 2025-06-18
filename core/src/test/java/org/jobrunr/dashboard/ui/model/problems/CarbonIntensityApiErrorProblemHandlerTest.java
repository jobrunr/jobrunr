package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.server.dashboard.CarbonIntensityApiErrorNotification;
import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CarbonIntensityApiErrorProblemHandlerTest {

    @Mock
    Problems problems;

    @Mock
    StorageProvider storageProvider;

    @Captor
    ArgumentCaptor<Problem> problemArgumentCaptor;

    CarbonIntensityApiErrorProblemHandler handler;

    @BeforeEach
    void setUpNewJobRunrVersionProblemHandler() {
        this.handler = new CarbonIntensityApiErrorProblemHandler(problems, storageProvider);
        handler.onChange(emptyList());
        reset(problems);
    }

    @Test
    void ifNoChangesThenNoProblemsCreated() {
        handler.onChange(emptyList());

        verifyNoInteractions(problems);
    }

    @Test
    void ifChangesDetectedThenProblemCreated() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(CarbonIntensityApiErrorNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        handler.onChange(asList(jobRunrMetadata));

        verify(problems).addProblem(problemArgumentCaptor.capture());
        assertThat(problemArgumentCaptor.getValue()).isInstanceOf(CarbonIntensityApiErrorProblem.class);
    }

    @Test
    void ifDeletedThenProblemIsRemoved() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(CarbonIntensityApiErrorNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        handler.onChange(asList(jobRunrMetadata));
        reset(problems);

        handler.onChange(emptyList());
        verify(problems).removeProblemsOfType(CarbonIntensityApiErrorProblem.PROBLEM_TYPE);
        verify(problems, never()).addProblem(any());
    }

}