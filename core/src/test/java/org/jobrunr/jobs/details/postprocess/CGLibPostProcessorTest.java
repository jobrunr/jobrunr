package org.jobrunr.jobs.details.postprocess;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;

class CGLibPostProcessorTest {

    private CGLibPostProcessor cgLibPostProcessor = new CGLibPostProcessor();

    @Test
    void postProcessWithoutCGLibReturnsSameJobDetails() {
        // GIVEN
        final JobDetails jobDetails = defaultJobDetails().build();

        // WHEN
        final JobDetails result = cgLibPostProcessor.postProcess(jobDetails);

        // THEN
        assertThat(result).isSameAs(jobDetails);
    }

    @Test
    void postProcessWithSpringCGLibReturnsUpdatedJobDetails() {
        // GIVEN
        final JobDetails jobDetails = defaultJobDetails().withClassName(TestService.class.getName() + "$$EnhancerBySpringCGLIB$$6aee664d").build();

        // WHEN
        final JobDetails result = cgLibPostProcessor.postProcess(jobDetails);

        // THEN
        assertThat(result)
                .isNotSameAs(jobDetails)
                .hasClass(TestService.class);
    }

    @Test
    void postProcessWithCGLibReturnsUpdatedJobDetails() {
        // GIVEN
        final JobDetails jobDetails = defaultJobDetails().withClassName(TestService.class.getName() + "$$EnhancerByCGLIB$$6aee664d").build();

        // WHEN
        final JobDetails result = cgLibPostProcessor.postProcess(jobDetails);

        // THEN
        assertThat(result)
                .isNotSameAs(jobDetails)
                .hasClass(TestService.class);
    }
}