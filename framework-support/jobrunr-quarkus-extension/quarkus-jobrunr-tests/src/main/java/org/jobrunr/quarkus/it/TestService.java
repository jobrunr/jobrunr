package org.jobrunr.quarkus.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.jobrunr.jobs.context.JobContext;


@ApplicationScoped
@RegisterForReflection
public class TestService {

    @Recurring(id = "my-recurring-job", cron = "*/15 * * * *")
    @Job(name = "Doing some work")
    public void aRecurringJob() {
        System.out.println("Doing some work every 15 minutes.");
    }

    //why: for some reason, the bytecode recorder gives troubles with the below
    @Recurring(id = "another-recurring-job-with-jobContext", interval = "PT10M")
    @Job(name = "Doing some work with the job context")
    public void anotherRecurringJob(JobContext jobContext) {
        System.out.println("Doing some work every 15 minutes.");
    }

}
