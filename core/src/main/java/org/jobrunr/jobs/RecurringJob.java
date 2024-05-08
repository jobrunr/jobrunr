package org.jobrunr.jobs;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.scheduling.ScheduleExpressionType;
import org.jobrunr.scheduling.TemporalWrapper;
import org.jobrunr.scheduling.cron.CarbonAwareCronExpression;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.time.Duration.between;
import static java.util.Collections.emptyList;

public class RecurringJob extends AbstractJob {

    public static Map<String, Function<RecurringJob, Comparable>> ALLOWED_SORT_COLUMNS = new HashMap<>();

    static {
        ALLOWED_SORT_COLUMNS.put(StorageProviderUtils.RecurringJobs.FIELD_ID, RecurringJob::getId);
        ALLOWED_SORT_COLUMNS.put(StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT, RecurringJob::getCreatedAt);
    }

    private String id;
    private Schedule schedule;
    private String zoneId;
    private Instant createdAt;

    private RecurringJob() {
        // used for deserialization
    }

    public RecurringJob(String id, JobDetails jobDetails, String schedule, String zoneId) {
        this(id, jobDetails, ScheduleExpressionType.getSchedule(schedule), ZoneId.of(zoneId));
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId) {
        this(id, jobDetails, schedule, zoneId, Instant.now(Clock.system(zoneId)));
    }

    public RecurringJob(String id, JobDetails jobDetails, String schedule, String zoneId, String createdAt) {
        this(id, jobDetails, ScheduleExpressionType.getSchedule(schedule), ZoneId.of(zoneId), Instant.parse(createdAt));
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, Instant createdAt) {
        super(jobDetails);
        this.id = validateAndSetId(id);
        this.zoneId = zoneId.getId();
        this.schedule = schedule;
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return id;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * Returns the next job to for this recurring job based on the current instant.
     *
     * @return the next job to for this recurring job based on the current instant.
     */
    @Deprecated
    public Job toScheduledJob() {
        return toJob(new ScheduledState(getNextRun().getInstant(), this));
    }

    /**
     * Creates all jobs that must be scheduled between the given start and end time.
     *
     * @param from the start time from which to create Scheduled Jobs
     * @param upTo the end time until which to create Scheduled Jobs
     * @return creates all jobs that must be scheduled
     */
    @Deprecated
    public List<Job> toScheduledJobs(Instant from, Instant upTo) {
        List<Job> jobs = new ArrayList<>();
        Instant nextRun = getNextRun(from).getInstant();
        while (nextRun.isBefore(upTo)) {
            jobs.add(toJob(new ScheduledState(nextRun, this)));
            nextRun = getNextRun(nextRun).getInstant();
        }
        return jobs;
    }

    public List<Job> toJobsWith1FutureRun(Instant from, Instant now) {
        if (from.isAfter(now)) return emptyList();

        List<Job> jobs = new ArrayList<>();
        TemporalWrapper nextRun = getNextRun(from);
        if (nextRun.isInstant()) {
            Instant nextRunInstant = nextRun.getInstant();
            while (nextRunInstant.isBefore(now)) {
                jobs.add(toJob(new ScheduledState(nextRunInstant, this)));
                nextRun = getNextRun(nextRunInstant);
            }
            // add 1 more job
            jobs.add(toJob(new ScheduledState(nextRunInstant, this)));
        }
        if (nextRun.isCarbonAwarePeriod()) {
            Job awaitingJob = toJob(new CarbonAwareAwaitingState(nextRun.getCarbonAwarePeriod()));
            JobRunr.getBackgroundJobServer().getCarbonAwareJobManager().moveToNextState(awaitingJob);
            jobs.add(awaitingJob);
        }
        return jobs;
    }

    public Job toEnqueuedJob() {
        return toJob(new EnqueuedState());
    }

    public String getZoneId() {
        return zoneId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public TemporalWrapper getNextRun() {
        return getNextRun(Instant.now());
    }

    public TemporalWrapper getNextRun(Instant sinceInstant) {
        return schedule.next(createdAt, sinceInstant, ZoneId.of(zoneId));
    }

    private String validateAndSetId(String input) {
        String result = Optional.ofNullable(input).orElse(getJobSignature().replace(" ", "").replace("$", "_")); //why: to support inner classes

        if (result.length() >= 128 && input == null) {
            //why: id's should be identical for identical recurring jobs as otherwise we would generate duplicate recurring jobs after restarts
            result = StringUtils.md5Checksum(result);
        } else if (result.length() >= 128) {
            throw new IllegalArgumentException("The id of a recurring job must be smaller than 128 characters.");
        } else if (!result.matches("[\\dA-Za-z-_(),.]+")) {
            throw new IllegalArgumentException("The id of a recurring job can only contain letters and numbers.");
        }

        return result;
    }

    private Job toJob(JobState jobState) {
        final Job job = new Job(getJobDetails(), jobState);
        job.setJobName(getJobName());
        job.setRecurringJobId(getId());
        job.setAmountOfRetries(getAmountOfRetries());
        job.setLabels(getLabels());
        return job;
    }

    @Override
    public String toString() {
        return "RecurringJob{" +
                "id=" + id +
                ", version='" + getVersion() + '\'' +
                ", identity='" + System.identityHashCode(this) + '\'' +
                ", jobSignature='" + getJobSignature() + '\'' +
                ", jobName='" + getJobName() + '\'' +
                '}';
    }

    public Duration durationBetweenRecurringJobInstances() {
        Instant base = Instant.EPOCH.plusSeconds(3600);
        if (schedule instanceof CarbonAwareCronExpression) {
            return Duration.ofHours(1); //why: we can't know the exact duration between carbon-aware awaiting jobs and we don't care. Just put a duration that does now cause problems
        }
        Instant run1 = schedule.next(base, base, ZoneOffset.UTC).getInstant();
        Instant run2 = schedule.next(base, run1, ZoneOffset.UTC).getInstant();
        return between(run1, run2);
    }
}
