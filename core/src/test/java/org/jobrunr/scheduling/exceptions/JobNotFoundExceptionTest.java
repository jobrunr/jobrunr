package org.jobrunr.scheduling.exceptions;

import org.jobrunr.jobs.JobParameterNotDeserializableException;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.*;

class JobNotFoundExceptionTest {

    @Test
    void shouldReturnCorrectMessageForClassThatDoesNotExist() {
        final JobNotFoundException jobNotFoundException = new JobNotFoundException(classThatDoesNotExistJobDetails().build());

        assertThat(jobNotFoundException).hasMessage("i.dont.exist.Class.notImportant(java.lang.Integer)");
    }

    @Test
    void shouldReturnCorrectMessageForMethodThatDoesNotExist() {
        final JobNotFoundException jobNotFoundException = new JobNotFoundException(methodThatDoesNotExistJobDetails().build());

        assertThat(jobNotFoundException).hasMessage("org.jobrunr.stubs.TestService.doWorkThatDoesNotExist(java.lang.Integer)");
    }

    @Test
    void shouldReturnCorrectMessageForJobParameterThatDoesNotExist() {
        final JobNotFoundException jobNotFoundException = new JobNotFoundException(jobParameterThatDoesNotExistJobDetails().build());

        assertThat(jobNotFoundException).hasMessage("org.jobrunr.stubs.TestService.doWork(i.dont.exist.Class)");
    }

    @Test
    void shouldReturnCorrectMessageForJobParameterNotDeserializableException() {
        final JobNotFoundException jobNotFoundException = new JobNotFoundException(
                jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(new JobParameterNotDeserializableException("i.dont.exist.Class", "Class not found"))
                        .build());

        assertThat(jobNotFoundException).hasMessage("org.jobrunr.stubs.TestService.doWork(i.dont.exist.Class)\n" +
                "\tcaused by: one of the JobParameters is not deserializable anymore");
    }

}