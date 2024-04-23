package org.jobrunr.utils;

import org.jobrunr.dashboard.server.sse.SseExchange;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.dashboard.ui.model.VersionUIModel;
import org.jobrunr.dashboard.ui.model.problems.CpuAllocationIrregularityProblem;
import org.jobrunr.dashboard.ui.model.problems.PollIntervalInSecondsTimeBoxIsTooSmallProblem;
import org.jobrunr.dashboard.ui.model.problems.Problem;
import org.jobrunr.dashboard.ui.model.problems.ScheduledJobsNotFoundProblem;
import org.jobrunr.dashboard.ui.model.problems.SevereJobRunrExceptionProblem;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.filters.ElectStateFilter;
import org.jobrunr.jobs.states.AbstractJobState;
import org.jobrunr.jobs.states.DeletedState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.JobStatsExtended;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.db2.DB2StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProvider;
import org.jobrunr.storage.sql.mysql.MySqlStorageProvider;
import org.jobrunr.storage.sql.oracle.OracleStorageProvider;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.jobrunr.storage.sql.sqlite.SqLiteStorageProvider;
import org.jobrunr.storage.sql.sqlserver.SQLServerStorageProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class GraalVMUtils {

    public static boolean isRunningInGraalVMNativeMode() {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    public static List<Class> JOBRUNR_CLASSES = Arrays.asList(
            // primitives
            boolean.class, byte.class, char.class, double.class, float.class, int.class, long.class, short.class,
            // wrapper types
            Boolean.class, Byte.class, Character.class, Float.class, Integer.class, Long.class, Short.class,
            // Java core types
            ArrayList.class, ConcurrentHashMap.class, ConcurrentLinkedQueue.class, CopyOnWriteArrayList.class, Duration.class, HashSet.class, Instant.class, UUID.class,
            // JobRunr States
            AbstractJobState.class, DeletedState.class, EnqueuedState.class, FailedState.class, JobState.class, ProcessingState.class, ScheduledState.class, StateName.class, SucceededState.class,
            // JobRunr Job
            AbstractJob.class, Job.class, JobDetails.class, JobDetailsAsmGenerator.class, JobParameter.class, RecurringJob.class,
            // JobRunr annotation
            org.jobrunr.jobs.annotations.Job.class, Recurring.class,
            // JobRunr Dashboard
            BackgroundJobServerStatus.class, JobDashboardLogger.class, JobDashboardLogger.JobDashboardLogLine.class, JobDashboardLogger.JobDashboardLogLines.class, JobStats.class, JobStatsExtended.class, JobStatsExtended.Estimation.class, JobRunrMetadata.class, Page.class, AmountRequest.class, OffsetBasedPageRequest.class, RecurringJobUIModel.class, VersionUIModel.class, SseExchange.class,
            // JobRunr Dashboard Problems
            CpuAllocationIrregularityProblem.class, PollIntervalInSecondsTimeBoxIsTooSmallProblem.class, Problem.class, ScheduledJobsNotFoundProblem.class, SevereJobRunrExceptionProblem.class,
            // Other
            CachingJobDetailsGenerator.class, ElectStateFilter.class,
            // Storage Providers
            DefaultSqlStorageProvider.class, DB2StorageProvider.class, H2StorageProvider.class, MariaDbStorageProvider.class, MySqlStorageProvider.class, OracleStorageProvider.class, PostgresStorageProvider.class, SQLServerStorageProvider.class, SqLiteStorageProvider.class
    );
}
