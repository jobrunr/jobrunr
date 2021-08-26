package org.jobrunr.jobs;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.context.JobContext;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class JobDetailsAssert extends AbstractAssert<JobDetailsAssert, JobDetails> {

    private JobDetailsAssert(JobDetails jobDetails) {
        super(jobDetails, JobDetailsAssert.class);
    }

    public static JobDetailsAssert assertThat(JobDetails jobDetails) {
        return new JobDetailsAssert(jobDetails);
    }

    public JobDetailsAssert isCacheable() {
        Assertions.assertThat(actual.getCacheable()).isTrue();
        return this;
    }

    public JobDetailsAssert isCacheable(Function<JobDetails, Boolean> condition) {
        Assertions.assertThat(actual.getCacheable()).isEqualTo(condition.apply(actual));
        return this;
    }

    public JobDetailsAssert hasClass(Class<?> clazz) {
        hasClassName(clazz.getName());
        return this;
    }

    public JobDetailsAssert hasClassName(String className) {
        Assertions.assertThat(actual.getClassName()).isEqualTo(className);
        return this;
    }

    public JobDetailsAssert hasStaticFieldName(String staticFieldName) {
        Assertions.assertThat(actual.getStaticFieldName()).isEqualTo(staticFieldName);
        return this;
    }

    public JobDetailsAssert hasMethodName(String methodName) {
        Assertions.assertThat(actual.getMethodName()).isEqualTo(methodName);
        return this;
    }

    public JobDetailsAssert hasArgs(Object... args) {
        Object[] jobParameterValues = actual.getJobParameterValues();
        for (int i = 0; i < args.length; i++) {
            if (args[i] == JobContext.Null) {
                Assertions.assertThat(actual.getJobParameterTypes()[i]).isEqualTo(JobContext.class);
            } else {
                Assertions.assertThat(jobParameterValues[i]).isEqualTo(args[i]);
            }
        }
        return this;
    }

    public JobDetailsAssert hasArg(Predicate<Object> predicate) {
        Object[] jobParameterValues = actual.getJobParameterValues();
        Assertions.assertThat(Stream.of(jobParameterValues)).describedAs("Arg did not match predicate...").anyMatch(predicate);
        return this;
    }

    public JobDetailsAssert hasNoArgs() {
        Assertions.assertThat(actual.getJobParameters()).isEmpty();
        return this;
    }

    public JobDetailsAssert hasJobContextArg() {
        Assertions.assertThat(actual.getJobParameters()).isNotEmpty();
        Assertions.assertThat(actual.getJobParameters().stream().filter(jobParameter -> jobParameter.getClassName().equals(JobContext.class.getName())).findAny()).isPresent();
        return this;
    }
}
