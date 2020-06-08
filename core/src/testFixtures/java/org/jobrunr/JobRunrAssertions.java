package org.jobrunr;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.javacrumbs.jsonunit.assertj.JsonAssert;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import org.assertj.core.api.Assertions;
import org.jobrunr.dashboard.server.http.client.HttpResponseAssert;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobAssert;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobDetailsAssert;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.RecurringJobAssert;
import org.mockito.internal.util.reflection.Whitebox;

import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class JobRunrAssertions extends Assertions {

    public static JobAssert assertThat(Job job) {
        return JobAssert.assertThat(job);
    }

    public static Job[] withoutLocks(Job... actual) {
        return Arrays.stream(actual).peek(job -> Whitebox.setInternalState(job, "lock", null)).toArray(Job[]::new);
    }

    public static List<Job> withoutLocks(List<Job> actual) {
        return actual.stream().peek(job -> Whitebox.setInternalState(job, "lock", null)).collect(toList());
    }

    public static RecurringJobAssert assertThat(RecurringJob recurringJob) {
        return RecurringJobAssert.assertThat(recurringJob);
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

    public static LoggerAssert assertThat(ListAppender<ILoggingEvent> listAppender) {
        return LoggerAssert.assertThat(listAppender);
    }

    public static String contentOfResource(String resourceName) {
        try {
            return Files.readString(Paths.get(JobRunrAssertions.class.getResource(resourceName).toURI()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
