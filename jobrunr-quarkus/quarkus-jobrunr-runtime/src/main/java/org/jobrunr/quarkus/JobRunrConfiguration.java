package org.jobrunr.quarkus;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import org.jobrunr.quarkus.configuation.BackgroundJobServerConfiguration;
import org.jobrunr.quarkus.configuation.DashboardConfiguration;
import org.jobrunr.quarkus.configuation.DatabaseConfiguration;
import org.jobrunr.quarkus.configuation.JobSchedulerConfiguration;

@ConfigRoot(name = "jobrunr", phase = ConfigPhase.RUN_TIME)
public class JobRunrConfiguration {

    DatabaseConfiguration database;

    JobSchedulerConfiguration jobScheduler;

    BackgroundJobServerConfiguration backgroundJobServer;

    DashboardConfiguration dashboard;

}

