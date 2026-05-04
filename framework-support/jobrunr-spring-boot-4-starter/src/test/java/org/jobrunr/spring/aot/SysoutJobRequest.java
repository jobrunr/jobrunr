package org.jobrunr.spring.aot;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

public class SysoutJobRequest implements JobRequest {

    private final String toPrint;

    protected SysoutJobRequest() {
        this(null);
    }

    @JsonCreator
    public SysoutJobRequest(@JsonProperty("toPrint") String toPrint) {
        this.toPrint = toPrint;
    }

    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return SysoutJobRequestHandler.class;
    }

    @Component
    public static class SysoutJobRequestHandler implements JobRequestHandler<SysoutJobRequest> {

        @Override
        public void run(SysoutJobRequest jobRequest) throws Exception {
            System.out.println("Content from jobRequest: " + jobRequest.toPrint);
        }
    }
}
