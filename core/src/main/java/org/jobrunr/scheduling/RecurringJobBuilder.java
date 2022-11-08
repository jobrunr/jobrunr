package org.jobrunr.scheduling;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.scheduling.interval.Interval;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

//TODO @Renaud: javadocs + tests
public class RecurringJobBuilder {

    private String id;
    private Schedule schedule;
    private ZoneId zoneId;
    private JobDetails jobDetails;
    private Instant createdAt;

    private RecurringJobBuilder() {
        // why: builder pattern
    }

    public static RecurringJobBuilder aRecurringJob() {
        return new RecurringJobBuilder();
    }

    public RecurringJobBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public RecurringJobBuilder withCron(String cron) {
        this.schedule = CronExpression.create(cron);
        return this;
    }

    public RecurringJobBuilder withDuration(Duration duration) {
        this.schedule = new Interval(duration);
        return this;
    }

    public RecurringJobBuilder withZoneId(String zoneId) {
        this.zoneId = ZoneId.of(zoneId);
        return this;
    }

    public RecurringJobBuilder withZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    public RecurringJobBuilder withJobDetails(JobDetails jobDetails) {
        this.jobDetails = jobDetails;
        return this;
    }

    public RecurringJobBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    RecurringJob build() {
        return new RecurringJob(id, jobDetails, schedule, zoneId, createdAt);
    }
}
