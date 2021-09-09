package org.jobrunr.server.tasks;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.utils.CollectionUtils.asSet;
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
            final Set<String> distinctRecurringJobSignatures = getDistinctRecurringJobSignaturesThatDoNotExistAnymore();
            final Set<String> distinctScheduledJobSignatures = getDistinctScheduledJobSignaturesThatDoNotExistAnymore();

            Set<String> jobsThatCannotBeFound = asSet(distinctRecurringJobSignatures, distinctScheduledJobSignatures);

            if (!distinctRecurringJobSignatures.isEmpty() || !distinctScheduledJobSignatures.isEmpty()) {
                String jobStateThatIsNotFound = jobTypeNotFoundLabel(distinctRecurringJobSignatures, distinctScheduledJobSignatures);
                LOGGER.warn("JobRunr found {} jobs that do not exist anymore in your code. These jobs will fail with a JobNotFoundException (due to a ClassNotFoundException or a MethodNotFoundException)." +
                                "\n\tBelow you can find the method signatures of the jobs that cannot be found anymore: {}",
                        jobStateThatIsNotFound,
                        jobsThatCannotBeFound.stream().map(sign -> "\n\t" + sign + ",").collect(Collectors.joining())
                );
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected exception running `CheckIfAllJobsExistTask`", shouldNotHappenException(e));
        }
    }

    private Set<String> getDistinctRecurringJobSignaturesThatDoNotExistAnymore() {
        return storageProvider.getRecurringJobs().stream()
                .map(AbstractJob::getJobSignature)
                .filter(jobSignature -> !jobExists(jobSignature))
                .collect(toSet());
    }

    private Set<String> getDistinctScheduledJobSignaturesThatDoNotExistAnymore() {
        return storageProvider.getDistinctJobSignatures(StateName.SCHEDULED).stream()
                .filter(jobSignature -> !jobExists(jobSignature))
                .collect(toSet());
    }

    private String jobTypeNotFoundLabel(Set<String> distinctRecurringJobSignaturesThatDoNotExistAnymoreAfterCleanup, Set<String> distinctScheduledJobSignaturesThatDoNotExistAnymoreAfterCleanup) {
        String jobStateThatIsNotFound = "";
        if (!distinctRecurringJobSignaturesThatDoNotExistAnymoreAfterCleanup.isEmpty()) {
            jobStateThatIsNotFound += "RECURRING";
        }
        if (!distinctRecurringJobSignaturesThatDoNotExistAnymoreAfterCleanup.isEmpty() && !distinctScheduledJobSignaturesThatDoNotExistAnymoreAfterCleanup.isEmpty()) {
            jobStateThatIsNotFound += " AND ";
        }
        if (!distinctScheduledJobSignaturesThatDoNotExistAnymoreAfterCleanup.isEmpty()) {
            jobStateThatIsNotFound += "SCHEDULED";
        }
        return jobStateThatIsNotFound;
    }
}
