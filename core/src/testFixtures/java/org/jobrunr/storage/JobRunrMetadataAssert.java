package org.jobrunr.storage;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.time.Instant;

public class JobRunrMetadataAssert extends AbstractAssert<JobRunrMetadataAssert, JobRunrMetadata> {

    protected JobRunrMetadataAssert(JobRunrMetadata metadata) {
        super(metadata, JobRunrMetadataAssert.class);
    }

    public static JobRunrMetadataAssert assertThat(JobRunrMetadata jobRunrMetadata) {
        return new JobRunrMetadataAssert(jobRunrMetadata);
    }

    public JobRunrMetadataAssert hasName(String name) {
        Assertions.assertThat(actual.getName()).isEqualTo(name);
        return this;
    }

    public JobRunrMetadataAssert hasOwner(String owner) {
        Assertions.assertThat(actual.getOwner()).isEqualTo(owner);
        return this;
    }

    public JobRunrMetadataAssert hasValue(String value) {
        Assertions.assertThat(actual.getMetadata()).isEqualTo(value);
        return this;
    }

    public JobRunrMetadataAssert valueContains(String toContain) {
        Assertions.assertThat(actual.getMetadata()).contains(toContain);
        return this;
    }

    public JobRunrMetadataAssert isEqualTo(JobRunrMetadata jobRunrMetadata) {
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(jobRunrMetadata);
        return this;
    }
}
