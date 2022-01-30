package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.lambdas.*;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class CachingJobDetailsGenerator implements JobDetailsGenerator {

    private final JobDetailsGenerator delegate;
    private final Map<Class<?>, CacheableJobDetails> cache;

    public CachingJobDetailsGenerator() {
        this(new JobDetailsAsmGenerator());
    }

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
    public JobDetails toJobDetails(IocJobLambda<?> lambda) {
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

        private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
        private final JobDetailsGenerator jobDetailsGeneratorDelegate;
        private JobDetails jobDetails;
        private List<JobParameterRetriever> jobParameterRetrievers;

        private CacheableJobDetails(JobDetailsGenerator jobDetailsGeneratorDelegate) {
            this.jobDetailsGeneratorDelegate = jobDetailsGeneratorDelegate;
        }

        public JobDetails getJobDetails(JobLambda lambda) {
            if (jobDetails == null) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.empty());
                return jobDetails;
            } else if (TRUE.equals(jobDetails.getCacheable())) {
                return getCachedJobDetails(lambda, Optional.empty());
            } else {
                return jobDetailsGeneratorDelegate.toJobDetails(lambda);
            }
        }

        public JobDetails getJobDetails(IocJobLambda lambda) {
            if (jobDetails == null) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.empty());
                return jobDetails;
            } else if (TRUE.equals(jobDetails.getCacheable())) {
                return getCachedJobDetails(lambda, Optional.empty());
            } else {
                return jobDetailsGeneratorDelegate.toJobDetails(lambda);
            }
        }

        public <T> JobDetails getJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
            if (jobDetails == null) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.of(itemFromStream));
                return jobDetails;
            } else if (TRUE.equals(jobDetails.getCacheable())) {
                return getCachedJobDetails(lambda, Optional.of(itemFromStream));
            } else {
                return jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda);
            }
        }

        public <S, T> JobDetails getJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
            if (jobDetails == null) {
                jobDetails = jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda);
                jobParameterRetrievers = initJobParameterRetrievers(jobDetails, lambda, Optional.of(itemFromStream));
                return jobDetails;
            } else if (TRUE.equals(jobDetails.getCacheable())) {
                return getCachedJobDetails(lambda, Optional.of(itemFromStream));
            } else {
                return jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda);
            }
        }

        private static <T> List<JobParameterRetriever> initJobParameterRetrievers(JobDetails jobDetails, JobRunrJob jobRunrJob, Optional<T> itemFromStream) {
            try {
                List<JobParameterRetriever> parameterRetrievers = new ArrayList<>();
                List<Field> declaredFields = new ArrayList<>(asList(jobRunrJob.getClass().getDeclaredFields()));
                List<JobParameter> jobParameters = jobDetails.getJobParameters();

                if (!declaredFields.isEmpty()
                        && !(declaredFields.get(0).getType().getName().startsWith("java."))
                        && (jobRunrJob instanceof JobLambda || jobRunrJob instanceof JobLambdaFromStream)) {
                    declaredFields.remove(0);
                }

                for (JobParameter jp : jobParameters) {
                    parameterRetrievers.add(createJobParameterRetriever(jp, jobRunrJob, itemFromStream, declaredFields));
                }

                jobDetails.setCacheable(declaredFields.isEmpty() && jobParameters.size() == parameterRetrievers.size());
                return parameterRetrievers;
            } catch (Exception e) {
                jobDetails.setCacheable(false);
                return emptyList();
            }
        }

        private static <T> JobParameterRetriever createJobParameterRetriever(JobParameter jp, JobRunrJob jobRunrJob, Optional<T> itemFromStream, List<Field> declaredFields) throws IllegalAccessException {
            JobParameterRetriever jobParameterRetriever = new FixedJobParameterRetriever(jp);
            if (itemFromStream.isPresent() && jp.getObject().equals(itemFromStream.get())) {
                jobParameterRetriever = new ItemFromStreamJobParameterRetriever(jp);
            } else {
                final ListIterator<Field> fieldIterator = declaredFields.listIterator();
                while (fieldIterator.hasNext()) {
                    Field f = fieldIterator.next();
                    Object valueFromField = ReflectionUtils.getValueFromField(f, jobRunrJob);
                    if (jp.getObject().equals(valueFromField)) {
                        MethodHandle e = lookup.unreflectGetter(f);
                        jobParameterRetriever = new MethodHandleJobParameterRetriever(jp, e.asType(e.type().generic()));
                        fieldIterator.remove();
                        break;
                    }
                }
            }
            return jobParameterRetriever;
        }

        private <T> JobDetails getCachedJobDetails(JobRunrJob job, Optional<T> itemFromStream) {
            final JobDetails cachedJobDetails = new JobDetails(
                    this.jobDetails.getClassName(),
                    this.jobDetails.getStaticFieldName(),
                    this.jobDetails.getMethodName(),
                    jobParameterRetrievers.stream()
                            .map(jobParameterRetriever -> jobParameterRetriever.getJobParameter(job, itemFromStream))
                            .collect(Collectors.toList())
            );
            cachedJobDetails.setCacheable(true);
            return cachedJobDetails;
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
                throw shouldNotHappenException(throwable);
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
            return new JobParameter(jobParameterClassName, itemFromStream.orElseThrow(() -> shouldNotHappenException("Can not find itemFromStream")));
        }
    }
}
