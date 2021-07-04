package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.*;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

class JobDetailsBuilderTest {

    @Test
    void reproduceIssueStringBuilderAppend() {
        final JobDetailsBuilder jobDetailsBuilder = getJobDetailsBuilder();
        new ALoadOperandInstruction(jobDetailsBuilder).load(1);
        new InvokeSpecialInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "<init>", "()V", false);
        new LdcInstruction(jobDetailsBuilder).load("Hello ");
        new InvokeVirtualInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        new ALoadOperandInstruction(jobDetailsBuilder).load(0);
        new InvokeVirtualInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        new InvokeVirtualInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        new InvokeVirtualInstruction(jobDetailsBuilder).load("org/jobrunr/stubs/TestService", "doWork", "(Ljava/lang/String;)V", false);

        final JobDetails jobDetails = jobDetailsBuilder.getJobDetails();
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs("Hello World");
    }

    @Test
    void reproduceIssuePrivateMethod() {
        final JobDetailsBuilder jobDetailsBuilder = getJobDetailsBuilder();
        new ALoadOperandInstruction(jobDetailsBuilder).load(0);
        new ALoadOperandInstruction(jobDetailsBuilder).load(1);
        new IConst2OperandInstruction(jobDetailsBuilder).load();
        new InvokeSpecialInstruction(jobDetailsBuilder).load("org/jobrunr/stubs/TestService", "aPrivateMethod", "(Ljava/lang/String;I)V", false);

        assertThatThrownBy(jobDetailsBuilder::getJobDetails)
                .isInstanceOf(JobRunrException.class)
                .hasMessage("The lambda you provided is not valid.");
    }

    private JobDetailsBuilder getJobDetailsBuilder() {
        return new JobDetailsBuilder(Arrays.asList("World", null), toFQClassName("org/jobrunr/examples/webapp/api/JobController"), "lambda$simpleJob$4ffb5ff$1") {

        };
    }

}