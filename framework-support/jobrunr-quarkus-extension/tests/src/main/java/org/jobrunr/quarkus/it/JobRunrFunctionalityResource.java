package org.jobrunr.quarkus.it;

import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobScheduler;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class JobRunrFunctionalityResource {

    @Inject
    JobScheduler jobScheduler;

    @GET
    @Path("enqueue")
    @Produces(MediaType.TEXT_PLAIN)
    public String doMigrateAuto() {
        final JobId enqueuedJobId = jobScheduler.enqueue(() -> System.out.println("Hello from Quarkus"));
        return "Job Enqueued: " + enqueuedJobId;
    }
}
