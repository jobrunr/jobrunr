package org.jobrunr.dashboard.ui.model.problems;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CpuAllocationIrregularityProblemHandlerTest {

    @Mock
    Problems problems;

    @Mock
    StorageProvider storageProvider;

    @Captor
    ArgumentCaptor<Problem> problemArgumentCaptor;

    CpuAllocationIrregularityProblemHandler cpuAllocationIrregularityProblemHandler;

    @BeforeEach
    void setUpNewJobRunrVersionProblemHandler() {
        cpuAllocationIrregularityProblemHandler = new CpuAllocationIrregularityProblemHandler(problems, storageProvider);
        cpuAllocationIrregularityProblemHandler.onChange(emptyList());
        reset(problems);
    }

    @Test
    void ifNoChangesOnCpuAllocationIrregularitiesThenNoProblemsCreated() {
        cpuAllocationIrregularityProblemHandler.onChange(emptyList());

        verifyNoInteractions(problems);
    }

    @Test
    void ifChangesOnCpuAllocationIrregularitiesDetectedThenProblemCreated() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(CpuAllocationIrregularityNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        cpuAllocationIrregularityProblemHandler.onChange(asList(jobRunrMetadata));

        verify(problems).addProblem(problemArgumentCaptor.capture());

        assertThat(problemArgumentCaptor.getValue())
                .isInstanceOf(CpuAllocationIrregularityProblem.class)
                .hasFieldOrPropertyWithValue("cpuAllocationIrregularityMetadataSet", asList(jobRunrMetadata));

        verify(problems).removeProblemsOfType(PollIntervalInSecondsTimeBoxIsTooSmallProblem.PROBLEM_TYPE);
        verify(storageProvider).deleteMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName());
    }

    @Test
    void ifCpuAllocationIrregularitiesIsDeletedThenProblemIsRemoved() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(CpuAllocationIrregularityNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        cpuAllocationIrregularityProblemHandler.onChange(asList(jobRunrMetadata));
        reset(problems);

        cpuAllocationIrregularityProblemHandler.onChange(emptyList());
        verify(problems).removeProblemsOfType(CpuAllocationIrregularityProblem.PROBLEM_TYPE);
        verify(problems, never()).addProblem(any());
    }

    @Test
    void ifCpuAllocationIrregularitiesIsDismissedThenProblemIsRemovedAndDeletedFromStorageProvider() {
        final JobRunrMetadata jobRunrMetadata = new JobRunrMetadata(CpuAllocationIrregularityNotification.class.getSimpleName(), "BackgroundJobServer " + UUID.randomUUID(), "23");
        cpuAllocationIrregularityProblemHandler.onChange(asList(jobRunrMetadata));
        reset(problems);

        cpuAllocationIrregularityProblemHandler.dismiss();
        verify(problems).removeProblemsOfType(CpuAllocationIrregularityProblem.PROBLEM_TYPE);
        verify(storageProvider).deleteMetadata(CpuAllocationIrregularityNotification.class.getSimpleName());
    }
}