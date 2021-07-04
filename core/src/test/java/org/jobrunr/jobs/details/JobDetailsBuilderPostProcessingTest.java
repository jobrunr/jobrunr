package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.ALoadOperandInstruction;
import org.jobrunr.jobs.details.instructions.InvokeSpecialInstruction;
import org.jobrunr.jobs.details.instructions.InvokeVirtualInstruction;
import org.jobrunr.jobs.details.instructions.LdcInstruction;
import org.jobrunr.jobs.details.postprocess.JobDetailsPostProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobDetailsBuilderPostProcessingTest {

    @Spy
    private JobDetailsBuilder jobDetailsBuilder = getJobDetailsBuilder();

    @Mock
    private JobDetailsPostProcessor jobDetailsPostProcessor;

    @BeforeEach
    void setupJobDetailsPostProcessor() {
        when(jobDetailsPostProcessor.postProcess(any(JobDetails.class))).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void assertPostProcessorsAreCalled() {
        new ALoadOperandInstruction(jobDetailsBuilder).load(1);
        new InvokeSpecialInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "<init>", "()V", false);
        new LdcInstruction(jobDetailsBuilder).load("Hello ");
        new InvokeVirtualInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        new ALoadOperandInstruction(jobDetailsBuilder).load(0);
        new InvokeVirtualInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        new InvokeVirtualInstruction(jobDetailsBuilder).load("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        new InvokeVirtualInstruction(jobDetailsBuilder).load("org/jobrunr/stubs/TestService", "doWork", "(Ljava/lang/String;)V", false);

        final JobDetails jobDetails = jobDetailsBuilder.getJobDetails();

        verify(jobDetailsPostProcessor).postProcess(jobDetails);
    }

    private JobDetailsBuilder getJobDetailsBuilder() {
        return new JobDetailsBuilder(Arrays.asList("World", null), toFQClassName("org/jobrunr/examples/webapp/api/JobController"), "lambda$simpleJob$4ffb5ff$1") {

            @Override
            List<JobDetailsPostProcessor> getJobDetailsPostProcessors() {
                final List<JobDetailsPostProcessor> jobDetailsPostProcessors = new ArrayList<>(super.getJobDetailsPostProcessors());
                jobDetailsPostProcessors.add(jobDetailsPostProcessor);
                return jobDetailsPostProcessors;
            }
        };
    }

}