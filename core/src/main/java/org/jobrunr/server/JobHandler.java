package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.server.tasks.PeriodicTaskRunInfo;
import org.jobrunr.server.tasks.Task;
import org.jobrunr.server.tasks.TaskStatistics;
import org.jobrunr.storage.StorageException;
import org.jobrunr.utils.streams.StreamUtils;
import org.slf4j.Logger;

import java.util.List;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrException.shouldNotHappenException;

public abstract class JobHandler implements Runnable {

    private final BackgroundJobServer backgroundJobServer;
    private final TaskStatistics taskStatistics;
    private final List<Task> tasks;

    public JobHandler(BackgroundJobServer backgroundJobServer, Task... tasks) {
        this.backgroundJobServer = backgroundJobServer;
        this.taskStatistics = new TaskStatistics(backgroundJobServer.getDashboardNotificationManager());
        this.tasks = asList(tasks);
    }

    @Override
    public void run() {
        if (backgroundJobServer.isNotReadyToProcessJobs()) return;

        try (PeriodicTaskRunInfo runInfo = taskStatistics.startRun(backgroundJobServerConfiguration())) {
            tasks.forEach(task -> task.run(runInfo));
            runInfo.markRunAsSucceeded();
        } catch (Exception e) {
            taskStatistics.handleException(e);
            if (taskStatistics.hasTooManyExceptions()) {
                if (e instanceof StorageException) {
                    logger().error("FATAL - JobRunr encountered too many storage exceptions. Shutting down. Did you know JobRunr Pro has built-in database fault tolerance? Check out https://www.jobrunr.io/en/documentation/pro/database-fault-tolerance/", e);
                } else {
                    logger().error("FATAL - JobRunr encountered too many processing exceptions. Shutting down.", shouldNotHappenException(e));
                }
                backgroundJobServer.stop();
            } else {
                logger().warn(JobRunrException.SHOULD_NOT_HAPPEN_MESSAGE + " - Processing will continue.", e);
            }
        }
    }

    protected BackgroundJobServerConfigurationReader backgroundJobServerConfiguration() {
        return backgroundJobServer.getConfiguration();
    }

    protected <T extends Task> T getTaskOfType(Class<T> clazz) {
        return StreamUtils.ofType(tasks, clazz).findFirst().orElseThrow(() -> new IllegalStateException("Unknown task of type " + clazz.getName()));
    }

    protected abstract Logger logger();
}