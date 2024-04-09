package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobLambdaFromStream;
import org.jobrunr.jobs.lambdas.JobRunrJob;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
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
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public JobDetails toJobDetails(JobLambda lambda) {
        return cache
                .computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate))
                .getJobDetails(lambda);
    }

    @Override
    public JobDetails toJobDetails(IocJobLambda<?> lambda) {
        return cache
                .computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate))
                .getJobDetails(lambda);
    }

    @Override
    public <T> JobDetails toJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
        return cache
                .computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate))
                .getJobDetails(itemFromStream, lambda);
    }

    @Override
    public <S, T> JobDetails toJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
        return cache
                .computeIfAbsent(lambda.getClass(), clazz -> new CacheableJobDetails(delegate))
                .getJobDetails(itemFromStream, lambda);
    }

    private static class CacheableJobDetails {

        private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
        private final JobDetailsGenerator jobDetailsGeneratorDelegate;
        private volatile JobDetails jobDetails;
        private List<JobParameterRetriever> jobParameterRetrievers;

        private CacheableJobDetails(JobDetailsGenerator jobDetailsGeneratorDelegate) {
            this.jobDetailsGeneratorDelegate = jobDetailsGeneratorDelegate;
        }

        public JobDetails getJobDetails(JobLambda lambda) {
            return initOrGetJobDetails(
                    () -> jobDetailsGeneratorDelegate.toJobDetails(lambda),
                    () -> initJobParameterRetrievers(jobDetails, lambda, Optional.empty()),
                    () -> getCachedJobDetails(lambda, Optional.empty()));
        }

        public JobDetails getJobDetails(IocJobLambda<?> lambda) {
            return initOrGetJobDetails(
                    () -> jobDetailsGeneratorDelegate.toJobDetails(lambda),
                    () -> initJobParameterRetrievers(jobDetails, lambda, Optional.empty()),
                    () -> getCachedJobDetails(lambda, Optional.empty()));
        }

        public <T> JobDetails getJobDetails(T itemFromStream, JobLambdaFromStream<T> lambda) {
            return initOrGetJobDetails(
                    () -> jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda),
                    () -> initJobParameterRetrievers(jobDetails, lambda, Optional.of(itemFromStream)),
                    () -> getCachedJobDetails(lambda, Optional.of(itemFromStream)));
        }

        public <S, T> JobDetails getJobDetails(T itemFromStream, IocJobLambdaFromStream<S, T> lambda) {
            return initOrGetJobDetails(
                    () -> jobDetailsGeneratorDelegate.toJobDetails(itemFromStream, lambda),
                    () -> initJobParameterRetrievers(jobDetails, lambda, Optional.of(itemFromStream)),
                    () -> getCachedJobDetails(lambda, Optional.of(itemFromStream)));
        }

        private JobDetails initOrGetJobDetails(Supplier<JobDetails> jobDetailsSupplier, Supplier<List<JobParameterRetriever>> jobParameterRetrieverSupplier, Supplier<JobDetails> getJobDetailsUsingCache) {
            if (this.jobDetails == null) {
                JobDetails jobDetails = initJobDetails(jobDetailsSupplier, jobParameterRetrieverSupplier);
                if (jobDetails != null) return jobDetails;
            }

            if (TRUE.equals(this.jobDetails.getCacheable())) {
                return getJobDetailsUsingCache.get();
            } else {
                return jobDetailsSupplier.get();
            }
        }

        /**
         * On first initialization, this creates the JobDetails, determines whether it is cacheable and returns it.
         *
         * @param jobDetailsSupplier            a Supplier to use when the {@link JobDetails} are null.
         * @param jobParameterRetrieverSupplier a Supplier to use when the List of {@link JobParameterRetriever JobParameterRetrievers} are null.
         * @return JobDetails if it was just initialized, null otherwise.
         */
        private synchronized JobDetails initJobDetails(Supplier<JobDetails> jobDetailsSupplier, Supplier<List<JobParameterRetriever>> jobParameterRetrieverSupplier) {
            if (this.jobDetails == null) {
                this.jobDetails = jobDetailsSupplier.get();
                jobParameterRetrievers = jobParameterRetrieverSupplier.get();
                return this.jobDetails;
            }
            return null;
        }

        private static <T> List<JobParameterRetriever> initJobParameterRetrievers(JobDetails jobDetails, JobRunrJob jobRunrJob, Optional<T> itemFromStream) {
            try {
                List<JobParameterRetriever> parameterRetrievers = new ArrayList<>();
                List<Field> declaredFields = new ArrayList<>(asList(jobRunrJob.getClass().getDeclaredFields()));
                List<JobParameter> jobParameters = jobDetails.getJobParameters();

                if (isParentClassPassedAsFieldToPassJobDetailsClass(declaredFields, jobDetails)
                        || isClassPassedAsFieldToPassJobDetailsClass(declaredFields, jobDetails)) {
                    declaredFields.remove(0);
                }

                for (JobParameter jp : jobParameters) {
                    parameterRetrievers.add(createJobParameterRetriever(jp, jobRunrJob, itemFromStream, declaredFields));
                }

                jobDetails.setCacheable(
                        declaredFields.isEmpty()
                                && jobParameters.size() == parameterRetrievers.size()
                                && (!itemFromStream.isPresent() || parameterRetrievers.stream().anyMatch(r -> r instanceof ItemFromStreamJobParameterRetriever))
                );
                return parameterRetrievers;
            } catch (Exception e) {
                jobDetails.setCacheable(false);
                return emptyList();
            }
        }


        private static boolean isParentClassPassedAsFieldToPassJobDetailsClass(List<Field> declaredFields, JobDetails jobDetails) {
            if (declaredFields.isEmpty()) return false;

            return stream(declaredFields.get(0).getType().getDeclaredFields())
                    .map(Field::getType)
                    .map(Class::getName)
                    .anyMatch(x -> x.equals(jobDetails.getClassName()));
        }

        private static boolean isClassPassedAsFieldToPassJobDetailsClass(List<Field> declaredFields, JobDetails jobDetails) {
            if (declaredFields.isEmpty()) return false;

            return declaredFields.get(0).getType().getName().equals(jobDetails.getClassName());
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
                Object o = methodHandle.invokeExact((Object) job);
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
