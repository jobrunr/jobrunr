package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jspecify.annotations.Nullable;

public class NullLoadOperandInstruction extends ZeroOperandInstruction {

    public NullLoadOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public @Nullable Object invokeInstruction() {
        return null;
    }
}
