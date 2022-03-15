package org.jobrunr.stubs;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMDCJobRequest implements JobRequest {

    private final String key;

    protected TestMDCJobRequest() {
        this(null);
    }

    public TestMDCJobRequest(String key) {
        this.key = key;
    }

        @Override
    public Class<TestMDCJobRequestHandler> getJobRequestHandler() {
        return TestMDCJobRequestHandler.class;
    }

    public String getKey() {
        return key;
    }

    public static class TestMDCJobRequestHandler implements JobRequestHandler<TestMDCJobRequest> {

        @Override
        @Job(name = "Doing some hard work for customerId: %X{customer.id}", retries = 1)
        public void run(TestMDCJobRequest jobRequest) {
            assertThat(MDC.get(jobRequest.getKey())).isNotNull();
            String result = jobRequest.getKey() + ": " + MDC.get(jobRequest.getKey()) + "; ";

            System.out.println("Running job request with MDC data in background: " + result);
        }
    }
}
