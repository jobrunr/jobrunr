package org.jobrunr.jobs.filters;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.exceptions.JobNotFoundException;
import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public abstract class AbstractJobFilters {

    protected final AbstractJob job;
    protected final List<JobFilter> jobFilters;

    protected AbstractJobFilters(AbstractJob job, JobDefaultFilters jobDefaultFilters) {
        this.job = job;
        this.jobFilters = initJobFilters(job, jobDefaultFilters.getFilters());
    }

    protected List<JobFilter> initJobFilters(AbstractJob job, List<JobFilter> jobFilters) {
        try {
            final ArrayList<JobFilter> result = new ArrayList<>(jobFilters);
            keepOnlyLastElectStateFilter(result);
            addJobFiltersFromJobAnnotation(job, result);
            return result;
        } catch (JobNotFoundException e) {
            return emptyList();
        }
    }

    abstract Logger getLogger();

    private static void keepOnlyLastElectStateFilter(List<JobFilter> result) {
        if (hasMultipleElectStateFilters(result)) {
            ElectStateFilter firstElectStateFilter = findFirstElectStateFilter(result);
            result.remove(firstElectStateFilter);
        }
    }

    private static void addJobFiltersFromJobAnnotation(AbstractJob job, List<JobFilter> result) {
        final Optional<Job> jobAnnotation = JobUtils.getJobAnnotation(job.getJobDetails());
        if (jobAnnotation.isPresent()) {
            final Optional<ElectStateFilter> electStateFilter = getElectStateFilter(jobAnnotation.get());
            if (electStateFilter.isPresent()) { // only one elect state filter can be present
                result.removeIf(jobFilter -> ElectStateFilter.class.isAssignableFrom(jobFilter.getClass()));
                result.add(electStateFilter.get());
            }
            result.addAll(getOtherJobFilter(jobAnnotation.get()));
        }
    }

    private static boolean hasMultipleElectStateFilters(List<JobFilter> result) {
        return result.stream()
                .filter(jobFilter -> ElectStateFilter.class.isAssignableFrom(jobFilter.getClass()))
                .count() > 1;
    }

    private static ElectStateFilter findFirstElectStateFilter(List<JobFilter> result) {
        return result.stream()
                .filter(jobFilter -> ElectStateFilter.class.isAssignableFrom(jobFilter.getClass()))
                .map(ElectStateFilter.class::cast)
                .findFirst()
                .orElseThrow(() -> JobRunrException.shouldNotHappenException("Can not happen..."));
    }

    private static Optional<ElectStateFilter> getElectStateFilter(org.jobrunr.jobs.annotations.Job jobAnnotation) {
        return Stream.of(jobAnnotation.jobFilters())
                .filter(ElectStateFilter.class::isAssignableFrom)
                .findFirst()
                .map(ReflectionUtils::newInstance)
                .map(ElectStateFilter.class::cast);
    }

    private static List<JobFilter> getOtherJobFilter(org.jobrunr.jobs.annotations.Job jobAnnotation) {
        return Stream.of(jobAnnotation.jobFilters())
                .filter(jobFilter -> !ElectStateFilter.class.isAssignableFrom(jobFilter))
                .map(ReflectionUtils::newInstance)
                .collect(toList());
    }

    <T extends JobFilter> Consumer<T> catchThrowable(Consumer<T> consumer) {
        return jobClientFilter -> {
            try {
                consumer.accept(jobClientFilter);
            } catch (Exception e) {
                getLogger().error("Error evaluating jobfilter {}", jobClientFilter.getClass().getName(), e);
            }
        };
    }
}
