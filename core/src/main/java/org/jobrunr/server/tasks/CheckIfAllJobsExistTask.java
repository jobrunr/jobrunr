package org.jobrunr.server.tasks;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.JobUtils.jobExists;

public class CheckIfAllJobsExistTask implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobServer.class);

    private final StorageProvider storageProvider;

    public CheckIfAllJobsExistTask(BackgroundJobServer backgroundJobServer) {
        storageProvider = backgroundJobServer.getStorageProvider();
    }

    @Override
    public void run() {
        try {
            Set<String> distinctJobSignatures = storageProvider.getDistinctJobSignatures(StateName.SCHEDULED);
            Set<String> jobsThatCannotBeFound = distinctJobSignatures.stream().filter(job -> !jobExists(job)).collect(toSet());
            if (!jobsThatCannotBeFound.isEmpty()) {
                LOGGER.warn("JobRunr found SCHEDULED jobs that do not exist anymore in your code. These jobs will fail with a JobNotFoundException (due to a ClassNotFoundException or a MethodNotFoundException)." +
                        "\n\tBelow you can find the method signatures of the jobs that cannot be found anymore: " +
                        jobsThatCannotBeFound.stream().map(sign -> "\n\t" + sign + ",").collect(Collectors.joining())
                );
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected exception running `CheckIfAllJobsExistTask`", shouldNotHappenException(e));
        }
    }
}
