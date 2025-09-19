package org.jobrunr.server.tasks.startup;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.RecurringJob.CreatedBy.ANNOTATION;
import static org.jobrunr.utils.CollectionUtils.asSet;
import static org.jobrunr.utils.JobUtils.getRecurringAnnotation;
import static org.jobrunr.utils.JobUtils.jobExists;
import static org.jobrunr.utils.OptionalUtils.isNotPresent;

public class CheckIfAllJobsExistTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(BackgroundJobServer.class);

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
                LOG.warn("JobRunr found {} jobs that do not exist anymore in your code. These jobs will fail with a JobNotFoundException (due to a ClassNotFoundException or a MethodNotFoundException)." +
                                "\n\tBelow you can find the method signatures of the jobs that cannot be found anymore: {}",
                        jobStateThatIsNotFound,
                        jobsThatCannotBeFound.stream().map(sign -> "\n\t" + sign + ",").collect(Collectors.joining())
                );
            }
        } catch (Exception e) {
            LOG.error("Unexpected exception running `CheckIfAllJobsExistTask`", shouldNotHappenException(e));
        }
    }

    private Set<String> getDistinctRecurringJobSignaturesThatDoNotExistAnymore() {
        Set<String> missingRecurringJobSignatures = new HashSet<>();
        for (RecurringJob recurringJob : storageProvider.getRecurringJobs()) {
            if (ANNOTATION.equals(recurringJob.getCreatedBy())) {
                if (!jobExists(recurringJob.getJobSignature())) {
                    storageProvider.deleteRecurringJob(recurringJob.getId());
                    LOG.info("Deleted recurring job {} ({}) as it was created by the @Recurring annotation but does not exist anymore", recurringJob.getId(), recurringJob.getJobSignature());
                } else if (isNotPresent(getRecurringAnnotation(recurringJob.getJobDetails()))) {
                    storageProvider.deleteRecurringJob(recurringJob.getId());
                    LOG.info("Deleted recurring job {} ({}) as it was created by the @Recurring annotation but is not annotated by the  @Recurring annotation anymore", recurringJob.getId(), recurringJob.getJobSignature());
                }
            } else if (!jobExists(recurringJob.getJobSignature())) {
                missingRecurringJobSignatures.add(recurringJob.getJobSignature());
            }
        }
        return missingRecurringJobSignatures;
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
