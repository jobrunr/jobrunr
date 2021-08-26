package org.jobrunr.quarkus.it;

import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static java.util.stream.Collectors.joining;

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
    @Path("jobs/recurring")
    @Produces(MediaType.TEXT_PLAIN)
    public String recurringJobs() {
        final List<RecurringJob> recurringJobs = storageProvider.getRecurringJobs();
        return "Recurring jobs: " + recurringJobs.stream().map(RecurringJob::toString).collect(joining(", "));
    }
}
