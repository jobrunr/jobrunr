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

class JobDetailsFinderContextTest {

    @Test
    void reproduceIssueStringBuilderAppend() {
        final JobDetailsFinderContext jobDetailsFinderContext = getJobDetailsFinderContext();
        new ALoadOperandInstruction(jobDetailsFinderContext).load(1);
        new InvokeSpecialInstruction(jobDetailsFinderContext).load("java/lang/StringBuilder", "<init>", "()V", false);
        new LdcInstruction(jobDetailsFinderContext).load("Hello ");
        new InvokeVirtualInstruction(jobDetailsFinderContext).load("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        new ALoadOperandInstruction(jobDetailsFinderContext).load(0);
        new InvokeVirtualInstruction(jobDetailsFinderContext).load("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        new InvokeVirtualInstruction(jobDetailsFinderContext).load("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        new InvokeVirtualInstruction(jobDetailsFinderContext).load("org/jobrunr/stubs/TestService", "doWork", "(Ljava/lang/String;)V", false);

        final JobDetails jobDetails = jobDetailsFinderContext.getJobDetails();
        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs("Hello World");
    }

    @Test
    void reproduceIssuePrivateMethod() {
        final JobDetailsFinderContext jobDetailsFinderContext = getJobDetailsFinderContext();
        new ALoadOperandInstruction(jobDetailsFinderContext).load(0);
        new ALoadOperandInstruction(jobDetailsFinderContext).load(1);
        new IConst2OperandInstruction(jobDetailsFinderContext).load();
        new InvokeSpecialInstruction(jobDetailsFinderContext).load("org/jobrunr/stubs/TestService", "aPrivateMethod", "(Ljava/lang/String;I)V", false);

        assertThatThrownBy(() -> jobDetailsFinderContext.getJobDetails())
                .isInstanceOf(JobRunrException.class)
                .hasMessage("The lambda you provided is not valid.");
    }

    private JobDetailsFinderContext getJobDetailsFinderContext() {
        return new JobDetailsFinderContext(Arrays.asList("World", null), toFQClassName("org/jobrunr/examples/webapp/api/JobController"), "lambda$simpleJob$4ffb5ff$1") {

        };
    }

}