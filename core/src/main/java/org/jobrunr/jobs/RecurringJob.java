package org.jobrunr.jobs;

import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.scheduling.ScheduleFactory;
import org.jobrunr.scheduling.interval.Interval;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

public class RecurringJob extends AbstractJob {

    private String id;
    private String scheduleExpression;
    private String zoneId;
    private Instant createdAt;

    private RecurringJob() {
        // used for deserialization
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId) {
        this(id, jobDetails, schedule.toString(), zoneId.getId());
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId) {
        super(jobDetails);
        this.id = validateAndSetId(id);
        this.scheduleExpression = scheduleExpression;
        this.zoneId = zoneId;
        ScheduleFactory.getSchedule(scheduleExpression).validateSchedule();
    }

    public RecurringJob(String id, JobDetails jobDetails, String scheduleExpression, String zoneId, String createdAt) {
        this(id, jobDetails, scheduleExpression, zoneId);
        if(createdAt != null && !createdAt.isEmpty()) this.createdAt = Instant.parse(createdAt);
    }

    public RecurringJob(String id, JobDetails jobDetails, Schedule schedule, ZoneId zoneId, Instant createdAt) {
        this(id, jobDetails, schedule.toString(), zoneId.getId(), createdAt.toString());
    }

    @Override
    public String getId() {
        return id;
    }

    public String getScheduleExpression() {
        return scheduleExpression;
    }

    public Job toScheduledJob() {
        Instant nextRun = getNextRun();
        final Job job = new Job(getJobDetails(), new ScheduledState(nextRun, this));
        job.setJobName(getJobName());
        return job;
    }

    public Job toEnqueuedJob() {
        final Job job = new Job(getJobDetails(), new EnqueuedState());
        job.setJobName(getJobName());
        return job;
    }

    public String getZoneId() {
        return zoneId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getNextRun() {
        Schedule schedule = ScheduleFactory.getSchedule(scheduleExpression);

        if(schedule instanceof Interval){
            return schedule.next(createdAt, ZoneId.of(zoneId));
        }
        return schedule.next(ZoneId.of(zoneId));
    }

    private String validateAndSetId(String input) {
        String result = Optional.ofNullable(input).orElse(getJobSignature().replace("$", "_")); //why: to support inner classes

        if (!result.matches("[\\dA-Za-z-_(),.]+")) {
            throw new IllegalArgumentException("The id of a recurring job can only contain letters and numbers.");
        }
        return result;
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
}
