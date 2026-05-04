package org.jobrunr.utils.mapper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.ScheduledState;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonList;

public class JsonMapperValidator {

    public static JsonMapper validateJsonMapper(JsonMapper jsonMapper) {
        try {
            final String serializedJob = jsonMapper.serialize(getJobForTesting());
            testTimeFields(serializedJob);
            testUseFieldsNotMethods(serializedJob);
            testUsePolymorphism(serializedJob);
            testCanConvertBackToJob(jsonMapper, serializedJob);
            return jsonMapper;
        } catch (Exception e) {
            throw new IllegalArgumentException("The JsonMapper you provided cannot be used as it deserializes jobs in an incorrect way.", e);
        }
    }

    private static void testTimeFields(String serializedJob) {
        if (!serializedJob.contains("\"createdAt\":\"2021-10-14T22:00:00Z\""))
            throw new IllegalArgumentException("Timestamps are wrongly formatted for JobRunr. They should be in ISO8601 format.");
    }

    private static void testUseFieldsNotMethods(String serializedJob) {
        if (serializedJob.contains("jobStates") && !serializedJob.contains("jobHistory"))
            throw new IllegalArgumentException("Job Serialization should use fields and not getters/setters.");
    }

    private static void testUsePolymorphism(String serializedJob) {
        if (!serializedJob.contains("\"@class\":\"org.jobrunr.jobs.states.ScheduledState\""))
            throw new IllegalArgumentException("Polymorphism is not supported as no @class annotation is present with fully qualified name of the different Job states.");
    }

    private static void testCanConvertBackToJob(JsonMapper jsonMapper, String serializedJob) {
        try {
            jsonMapper.deserialize(serializedJob, Job.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("The JsonMapper cannot convert jobs from Json.", e);
        }
    }

    private static Job getJobForTesting() {
        final Job job = new Job(
                UUID.randomUUID(),
                5,
                new JobDetails("java.lang.System", "out", "println", singletonList(
                        new JobParameter("java.io.File", new File("/tmp/"))
                )),

                Arrays.asList(
                        new ScheduledState(Instant.ofEpochSecond(1634248800), null, Instant.ofEpochSecond(1634245200)),
                        new EnqueuedState(Instant.ofEpochSecond(1634248800)),
                        new ProcessingState(UUID.fromString("117bbfcf-e6df-45f0-82a7-b88fd8f96c06"), "some host name", Instant.ofEpochSecond(1634248900))),
                new ConcurrentHashMap<>()
        );
        job.setJobName("Some name");
        return job;
    }
}
