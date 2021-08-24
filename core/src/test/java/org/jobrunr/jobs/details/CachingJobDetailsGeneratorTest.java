package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.Set;

import static org.jobrunr.JobRunrAssertions.assertThat;

@ExtendWith(CachingJobDetailsGeneratorTest.NotCacheableExceptionExtensionHandler.class)
public class CachingJobDetailsGeneratorTest extends AbstractJobDetailsGeneratorTest {

    public static final Set<String> TESTS_WITH_JOB_DETAILS_THAT_ARE_NOT_CACHEABLE = Set.of(
            "testIocJobLambdaWithObject",
            "testJobLambdaWithObject",
            "testJobLambdaCallingMultiLineStatementSystemOutPrintln"
    );

    @Override
    protected JobDetailsGenerator getJobDetailsGenerator() {
        return new CachingJobDetailsGenerator(new JobDetailsAsmGenerator());
    }

    @Override
    protected JobDetails toJobDetails(JobLambda jobLambda) {
        final JobDetails jobDetails = super.toJobDetails(jobLambda);
        assertThat(jobDetails).isCacheable();
        return jobDetails;
    }

    @Override
    protected JobDetails toJobDetails(IocJobLambda<TestService> iocJobLambda) {
        final JobDetails jobDetails = super.toJobDetails(iocJobLambda);
        assertThat(jobDetails).isCacheable();
        return jobDetails;
    }

    public static class NotCacheableExceptionExtensionHandler implements TestExecutionExceptionHandler {

        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            if (throwable instanceof AssertionError && TESTS_WITH_JOB_DETAILS_THAT_ARE_NOT_CACHEABLE.contains(context.getRequiredTestMethod().getName())) {
                return;
            }
            throw throwable;
        }
    }
}
