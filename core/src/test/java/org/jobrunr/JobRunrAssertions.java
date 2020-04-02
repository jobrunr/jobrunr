package org.jobrunr;

import net.javacrumbs.jsonunit.assertj.JsonAssert;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.assertj.core.api.Assertions;
import org.jobrunr.dashboard.server.http.client.HttpResponseAssert;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobAssert;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobDetailsAssert;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JobRunrAssertions extends Assertions {

    public static JobAssert assertThat(Job job) {
        return JobAssert.assertThat(job);
    }

    public static JobDetailsAssert assertThat(JobDetails jobDetails) {
        return JobDetailsAssert.assertThat(jobDetails);
    }

    public static HttpResponseAssert assertThat(HttpResponse httpResponse) {
        return HttpResponseAssert.assertThat(httpResponse);
    }

    public static JsonAssert.ConfigurableJsonAssert assertThatJson(String json) {
        return JsonAssertions.assertThatJson(json);
    }

    public static String contentOfResource(String resourceName) {
        try {
            return Files.readString(Paths.get(JobRunrAssertions.class.getResource(resourceName).toURI()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
