package org.jobrunr.quarkus.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.quarkus.annotations.Recurring;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
public class TestService {

    @Recurring(id = "my-recurring-job", cron = "*/15 * * * *")
    @Job(name = "Doing some work")
    public void aRecurringJob() {
        System.out.println("Doing some work every 15 minutes.");
    }

}
