package org.jobrunr.jobs.filters;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.exceptions.JobNotFoundException;
import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.jobrunr.utils.streams.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class JobFilters {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobFilters.class);

    private final List<JobFilter> filters;

    public JobFilters(JobFilter... filters) {
        this(Arrays.asList(filters));
    }

    public JobFilters(List<JobFilter> filters) {
        this.filters = getAllJobFilters(filters);
    }

    private ArrayList<JobFilter> getAllJobFilters(List<JobFilter> jobFilters) {
        final ArrayList<JobFilter> result = new ArrayList<>(Arrays.asList(new DisplayNameFilter(), new RetryFilter()));
        result.addAll(jobFilters);
        return result;
    }

    public void runOnCreatingFilter(List<Job> jobs) {
        jobs.forEach(this::runOnCreatingFilter);
    }

    public void runOnCreatedFilter(List<Job> jobs) {
        jobs.forEach(this::runOnCreatedFilter);
    }

    public void runOnCreatingFilter(AbstractJob job) {
        jobClientFilters(job).forEach(catchThrowable(jobClientFilter -> jobClientFilter.onCreating(job)));
    }

    public void runOnCreatedFilter(AbstractJob job) {
        jobClientFilters(job).forEach(catchThrowable(jobClientFilter -> jobClientFilter.onCreated(job)));
    }

    public void runOnStateElectionFilter(List<Job> jobs) {
        jobs.forEach(this::runOnStateElectionFilter);
    }

    public void runOnStateAppliedFilters(List<Job> jobs) {
        jobs.forEach(this::runOnStateAppliedFilters);
    }

    public void runOnStateElectionFilter(Job job) {
        electStateFilters(job).forEach(catchThrowable(electStateFilter -> electStateFilter.onStateElection(job, job.getJobState())));
    }

    public void runOnStateAppliedFilters(Job job) {
        applyStateFilters(job).forEach(catchThrowable(applyStateFilter -> applyStateFilter.onStateApplied(job, job.getJobState(-2), job.getJobState(-1))));
    }

    public void runOnJobProcessingFilters(Job job) {
        jobServerFilters(job).forEach(catchThrowable(jobServerFilter -> jobServerFilter.onProcessing(job)));
    }

    public void runOnJobProcessedFilters(Job job) {
        jobServerFilters(job).forEach(catchThrowable(jobServerFilter -> jobServerFilter.onProcessed(job)));
    }

    private Stream<ElectStateFilter> electStateFilters(Job job) {
        return electStateFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    private Stream<ApplyStateFilter> applyStateFilters(Job job) {
        return applyStateFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    private Stream<JobServerFilter> jobServerFilters(Job job) {
        return jobServerFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    private Stream<JobClientFilter> jobClientFilters(AbstractJob job) {
        return jobClientFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    private static Stream<ElectStateFilter> electStateFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, ElectStateFilter.class);
    }

    private static Stream<ApplyStateFilter> applyStateFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, ApplyStateFilter.class);
    }

    private static Stream<JobServerFilter> jobServerFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, JobServerFilter.class);
    }

    private static Stream<JobClientFilter> jobClientFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, JobClientFilter.class);
    }

    private static List<JobFilter> initJobFilters(AbstractJob job, List<JobFilter> jobFilters) {
        try {
            final ArrayList<JobFilter> result = new ArrayList<>(jobFilters);
            keepOnlyLastElectStateFilter(result);
            addJobFiltersFromJobAnnotation(job, result);
            return result;
        } catch (JobNotFoundException e) {
            LOGGER.info("Could not collect JobFilters for job {} as the job is not found.", job.getId(), e);
            return emptyList();
        }
    }

    private static void keepOnlyLastElectStateFilter(List<JobFilter> result) {
        if (hasMultipleElectStateFilters(result)) {
            ElectStateFilter firstElectStateFilter = findFirstElectStateFilter(result);
            result.remove(firstElectStateFilter);
        }
    }

    private static void addJobFiltersFromJobAnnotation(AbstractJob job, List<JobFilter> result) {
        final Optional<org.jobrunr.jobs.annotations.Job> jobAnnotation = JobUtils.getJobAnnotation(job.getJobDetails());
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

    static <T extends JobFilter> Consumer<T> catchThrowable(Consumer<T> consumer) {
        return jobClientFilter -> {
            try {
                consumer.accept(jobClientFilter);
            } catch (Exception e) {
                LOGGER.error("Error evaluating jobfilter {}", jobClientFilter.getClass().getName(), e);
            }
        };
    }
}
