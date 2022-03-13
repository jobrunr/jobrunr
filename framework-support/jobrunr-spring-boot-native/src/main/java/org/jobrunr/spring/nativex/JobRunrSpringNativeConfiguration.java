package org.jobrunr.spring.nativex;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.dashboard.server.sse.SseExchange;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.dashboard.ui.model.VersionUIModel;
import org.jobrunr.dashboard.ui.model.problems.Problem;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.filters.ElectStateFilter;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.states.*;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.spring.annotations.Recurring;
import org.jobrunr.storage.*;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.type.NativeConfiguration;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.springframework.nativex.hint.TypeAccess.*;

@NativeHint(trigger = JobScheduler.class, types = {
        @TypeHint(
                types = {boolean.class, byte.class, char.class, double.class, float.class, int.class, long.class,
                        short.class, Float.class, Short.class, String.class, Long.class, Integer.class, Boolean.class,
                        Byte.class, Character.class, Double.class,
                        AbstractJob.class, AbstractJobState.class,
                        CachingJobDetailsGenerator.class, ConcurrentHashMap.class, DeletedState.class, Duration.class,
                        ElectStateFilter.class, EnqueuedState.class, Enum.class, FailedState.class, Instant.class,
                        JobDashboardLogger.class, JobDashboardLogger.JobDashboardLogLine.class,
                        JobDashboardLogger.JobDashboardLogLines.class, Job.class, JobDetails.class, JobFilter.class,
                        JobState.class, ProcessingState.class,
                        CachingJobDetailsGenerator.class, Recurring.class, RecurringJob.class,
                        ScheduledState.class, StateName.class,
                        SucceededState.class, UUID.class,
                        JobRunr.class, Job.class, org.jobrunr.jobs.annotations.Job.class,
                        JobNotFoundException.class, BackgroundJobServerStatus.class, Problem.class, Page.class,
                        BackgroundJobServerStatus.class, StateName.class, Instant.class, JobStats.class, JobStatsExtended.class, ConcurrentLinkedQueue.class,
                        PageRequest.class, RecurringJobUIModel.class, VersionUIModel.class, SseExchange.class},
                access = {DECLARED_CLASSES, DECLARED_CONSTRUCTORS, DECLARED_FIELDS, DECLARED_METHODS})})
@ResourceHint(patterns = {
        "/org/jobrunr/configuration/JobRunr.class",
        "org/jobrunr/dashboard/frontend/build/.*",
        "org/jobrunr/storage/sql/.*",
        "org/jobrunr/storage/nosql/elasticsearch/migrations/.*",
        "org/jobrunr/storage/nosql/mongo/migrations/.*",
        "org/jobrunr/storage/nosql/redis/migrations/.*",
        "/META-INF/MANIFEST.MF"
})
public class JobRunrSpringNativeConfiguration implements NativeConfiguration {

}