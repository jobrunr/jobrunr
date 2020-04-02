package org.jobrunr.jobs.filters;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.jobrunr.utils.streams.StreamUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class JobFilterUtils {

    private JobFilterUtils() {
    }

    public static Stream<ElectStateFilter> electStateFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, ElectStateFilter.class);
    }

    public static Stream<ApplyStateFilter> applyStateFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, ApplyStateFilter.class);
    }

    public static Stream<JobServerFilter> jobServerFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, JobServerFilter.class);
    }

    public static Stream<JobClientFilter> jobClientFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, JobClientFilter.class);
    }

    public static List<JobFilter> initJobFilters(AbstractJob job, List<JobFilter> jobFilters) {
        final ArrayList<JobFilter> result = new ArrayList<>(jobFilters);
        keepOnlyLastElectStateFilter(result);
        addJobFiltersFromJobAnnotation(job, result);
        return result;
    }

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
}
