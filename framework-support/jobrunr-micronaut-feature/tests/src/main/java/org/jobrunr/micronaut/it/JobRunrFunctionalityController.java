package org.jobrunr.micronaut.it;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import jakarta.inject.Inject;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;

import java.util.List;
import java.util.UUID;

@Controller("/jobrunr")
public class JobRunrFunctionalityController {

    @Inject
    JobScheduler jobScheduler;

    @Inject
    StorageProvider storageProvider;

    @Post("/jobs")
    @Produces(MediaType.TEXT_PLAIN)
    public String enqueueJob() {
        final JobId enqueuedJobId = jobScheduler.enqueue(() -> System.out.println("Hello from Micronaut"));
        return "Job Enqueued: " + enqueuedJobId;
    }

    @Get("/recurring-jobs")
    public List<RecurringJob> recurringJobs() {
        final List<RecurringJob> recurringJobs = storageProvider.getRecurringJobs();
        return recurringJobs;
    }

    @Get("jobs/{jobId}")
    public Job jobById(@PathVariable("jobId") String jobId) {
        Job jobById = storageProvider.getJobById(UUID.fromString(jobId));
        return jobById;
    }
}
