package org.jobrunr.quarkus.it;

import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.JobScheduler;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class JobRunrFunctionalityResource {

    @Inject
    JobScheduler jobScheduler;

    @GET
    @Path("enqueue")
    public String doMigrateAuto() {
        final JobId jobId = jobScheduler.enqueue(() -> System.out.println("Hello from Quarkus"));
        return jobId.toString();
    }
}
