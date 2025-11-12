package org.jobrunr.utils.reflection;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class ReflectionTestClasses {

    private ReflectionTestClasses() {
    }

    public static abstract class Level0JobRequest implements JobRequest {

    }

    public static class Level0JobRequestHandler<T extends Level0JobRequest> implements JobRequestHandler<T> {
        @Override
        public void run(T jobRequest) throws Exception {
            System.out.println(jobRequest);
        }
    }

    public static class Level1JobRequest extends Level0JobRequest {
        @Override
        public Class<Level1JobRequestHandler> getJobRequestHandler() {
            return Level1JobRequestHandler.class;
        }
    }

    public static class Level1JobRequestHandler extends Level0JobRequestHandler<Level1JobRequest> {
        @Override
        public void run(Level1JobRequest jobRequest) throws Exception {
            super.run(jobRequest);
        }

    }

    public static class GenericJobRequest implements JobRequest {
        @Override
        public Class<GenericJobRequestHandler> getJobRequestHandler() {
            return GenericJobRequestHandler.class;
        }
    }

    public static class GenericJobRequestHandler<T extends JobRequest> implements JobRequestHandler<T> {

        @Override
        public void run(T jobRequest) throws Exception {
            System.out.println(jobRequest);
        }

    }

    public static class MyAsyncJobRequest implements JobRequest {

        @Override
        public Class<MyAsyncJobRequestHandler> getJobRequestHandler() {
            return MyAsyncJobRequestHandler.class;
        }

    }

    public static class MyAsyncJobRequestHandler implements AsyncJobRequestHandler<MyAsyncJobRequest> {

        public void runAsync(MyAsyncJobRequest jobRequest) {
            System.out.println("Run async...");
        }

    }

    public interface AsyncJobRequestHandler<T extends JobRequest> extends JobRequestHandler<T> {

        @Override
        default void run(T jobRequest) throws Exception {
            System.out.println("Running async...");
            runAsync(jobRequest);
        }

        void runAsync(T jobRequest);
    }
}
