package org.jobrunr.quarkus.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;

import java.util.List;
import java.util.UUID;

@Path("/")
public class JobRunrFunctionalityResource {

    @Inject
    JobScheduler jobScheduler;

    @Inject
    StorageProvider storageProvider;

    @POST
    @Path("jobs")
    @Produces(MediaType.TEXT_PLAIN)
    public String enqueueJob() {
        final JobId enqueuedJobId = jobScheduler.enqueue(() -> System.out.println("Hello from Quarkus"));
        return "Job Enqueued: " + enqueuedJobId;
    }

    @GET
    @Path("recurring-jobs")
    @Produces(MediaType.TEXT_PLAIN)
    public List<RecurringJob> recurringJobs() {
        final List<RecurringJob> recurringJobs = storageProvider.getRecurringJobs();
        return recurringJobs;
    }

    @GET
    @Path("jobs/{jobId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Job jobById(@PathParam("jobId") String jobId) {
        Job jobById = storageProvider.getJobById(UUID.fromString(jobId));
        return jobById;
    }
}
