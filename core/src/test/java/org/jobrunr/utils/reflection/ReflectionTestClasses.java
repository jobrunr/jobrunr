package org.jobrunr.utils.reflection;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class ReflectionTestClasses {

    private ReflectionTestClasses() {
    }

    public static abstract class Level0JobRequest implements JobRequest {

    }

    public static abstract class Level0JobRequestHandler<T extends Level0JobRequest> implements JobRequestHandler<T> {
        @Override
        public void run(T jobRequest) throws Exception {
            System.out.println(jobRequest);
        }
    }

    public static abstract class Level1JobRequest
            <T extends Level1JobRequest<T, H>, H extends Level1JobRequestHandler<T, H>> extends Level0JobRequest {
    }

    public static abstract class Level1JobRequestHandler
            <T extends Level1JobRequest<T, H>, H extends Level1JobRequestHandler<T, H>>
            extends Level0JobRequestHandler<T> implements JobRequestHandler<T> {
    }

    public static class Level2JobRequest extends Level1JobRequest<Level2JobRequest, Level2JobRequestHandler> {
        @Override
        public Class<Level2JobRequestHandler> getJobRequestHandler() {
            return Level2JobRequestHandler.class;
        }
    }

    public static class Level2JobRequestHandler
            extends Level1JobRequestHandler<Level2JobRequest, Level2JobRequestHandler> {
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