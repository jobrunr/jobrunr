package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.lambdas.*;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class CachingJobDetailsGenerator implements JobDetailsGenerator {

    private final JobDetailsGenerator delegate;
    private final Map<Class, CacheableJobDetails> cache;

    public CachingJobDetailsGenerator(JobDetailsGenerator delegate) {
        this.delegate = delegate;
        this.cache = new HashMap<>();
    }

    @Override
    public JobDetails toJobDetails(JobLambda lambda) {
        cache.computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate));
        return cache.get(lambda.getClass()).getJobDetails(lambda);
    }

    @Override
    public JobDetails toJobDetails(IocJobLambda lambda) {
        cache.computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate));
        return cache.get(lambda.getClass()).getJobDetails(lambda);
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        cache.computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate));
        return cache.get(lambda.getClass()).getJobDetails(itemFromStream, lambda);
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        cache.computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate));
        return cache.get(lambda.getClass()).getJobDetails(itemFromStream, lambda);
    }

    private static class CacheableJobDetails {

        private final JobDetailsGenerator jobDetailsGeneratorDelegate;
        private JobDetails jobDetails;
        private List<JobParameterRetriever> jobParameterRetrievers;

        private CacheableJobDetails(JobDetailsGenerator jobDetailsGeneratorDelegate) {
            this.jobDetailsGeneratorDelegate = jobDetailsGeneratorDelegate;
        }

        public JobDetails getJobDetails(JobLambda lambda) {
            if (jobDetails == null || !jobDetails.getCacheable()) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.empty());
                return jobDetails;
            } else {
                return getCachedJobDetails(lambda, Optional.empty());
            }
        }

        public JobDetails getJobDetails(IocJobLambda lambda) {
            if (jobDetails == null || !jobDetails.getCacheable()) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.empty());
                return jobDetails;
            } else {
                return getCachedJobDetails(lambda, Optional.empty());
            }
        }

        public <T> JobDetails getJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
            if (jobDetails == null || !jobDetails.getCacheable()) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.of(itemFromStream));
                return jobDetails;
            } else {
                return getCachedJobDetails(lambda, Optional.of(itemFromStream));
            }
        }

        public <S, T> JobDetails getJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
            if (jobDetails == null || !jobDetails.getCacheable()) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.of(itemFromStream));
                return jobDetails;
            } else {
                return getCachedJobDetails(lambda, Optional.of(itemFromStream));
            }
        }

        private static <T> List<JobParameterRetriever> initJobParameterRetrievers(JobDetails jobDetails, JobRunrJob jobRunrJob, Optional<T> itemFromStream) {
            List<JobParameterRetriever> parameterRetrievers = new ArrayList<>();
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Field[] declaredFields = jobRunrJob.getClass().getDeclaredFields();
            List<JobParameter> jobParameters = jobDetails.getJobParameters();

            int amountOfFieldsToHandle = (jobRunrJob instanceof JobLambda || jobRunrJob instanceof JobLambdaFromStream) ? declaredFields.length - 1 : declaredFields.length;
            if (amountOfFieldsToHandle < 0) amountOfFieldsToHandle = 0;
            int amountOfFieldsHandled = 0;
            for (JobParameter jp : jobParameters) {
                JobParameterRetriever jobParameterRetriever = new FixedJobParameterRetriever(jp);
                if (itemFromStream.isPresent() && jp.getObject().equals(itemFromStream.get())) {
                    jobParameterRetriever = new ItemFromStreamJobParameterRetriever(jp);
                } else {
                    for (Field f : declaredFields) {
                        Object valueFromField = ReflectionUtils.getValueFromField(f, jobRunrJob);
                        if (jp.getObject().equals(valueFromField)) {
                            try {
                                MethodHandle e = lookup.unreflectGetter(f);
                                jobParameterRetriever = new MethodHandleJobParameterRetriever(jp, e.asType(e.type().generic()));
                                amountOfFieldsHandled++;
                                break;
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                parameterRetrievers.add(jobParameterRetriever);
            }
            if (jobParameters.size() != parameterRetrievers.size()) {
                throw new IllegalStateException("Not enough ParameterHandles");
            }
            jobDetails.setCacheable(amountOfFieldsToHandle == amountOfFieldsHandled);
            return parameterRetrievers;
        }

        private <T> JobDetails getCachedJobDetails(JobRunrJob job, Optional<T> itemFromStream) {
            return new JobDetails(
                    jobDetails.getClassName(),
                    jobDetails.getStaticFieldName().orElse(null),
                    jobDetails.getMethodName(),
                    jobParameterRetrievers.stream()
                            .map(jobParameterRetriever -> jobParameterRetriever.getJobParameter(job, itemFromStream))
                            .collect(Collectors.toList())
            );
        }
    }

    private interface JobParameterRetriever {
        <T> JobParameter getJobParameter(JobRunrJob job, Optional<T> itemFromStream);
    }

    private static class FixedJobParameterRetriever implements JobParameterRetriever {

        private final JobParameter jobParameter;

        public FixedJobParameterRetriever(JobParameter jobParameter) {
            this.jobParameter = jobParameter;
        }

        @Override
        public <T> JobParameter getJobParameter(JobRunrJob job, Optional<T> itemFromStream) {
            return jobParameter;
        }
    }

    private static class MethodHandleJobParameterRetriever implements JobParameterRetriever {

        private final String jobParameterClassName;
        private final MethodHandle methodHandle;

        public MethodHandleJobParameterRetriever(JobParameter jobParameter, MethodHandle methodHandle) {
            this.jobParameterClassName = jobParameter.getClassName();
            this.methodHandle = methodHandle;
        }

        @Override
        public <T> JobParameter getJobParameter(JobRunrJob job, Optional<T> itemFromStream) {
            try {
                Object o = (Object) methodHandle.invokeExact((Object) job);
                return new JobParameter(jobParameterClassName, o);
            } catch (Throwable throwable) {
                throw new RuntimeException("Exception", throwable);
            }
        }
    }

    private static class ItemFromStreamJobParameterRetriever implements JobParameterRetriever {

        private final String jobParameterClassName;

        public ItemFromStreamJobParameterRetriever(JobParameter jobParameter) {
            this.jobParameterClassName = jobParameter.getClassName();
        }

        @Override
        public <T> JobParameter getJobParameter(JobRunrJob job, Optional<T> itemFromStream) {
            return new JobParameter(jobParameterClassName, itemFromStream.orElseThrow(() -> JobRunrException.shouldNotHappenException("Can not find itemFromStream")));
        }
    }
}
