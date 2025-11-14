package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jspecify.annotations.Nullable;

public class PopOperandInstruction extends ZeroOperandInstruction {

    public PopOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public void load() {
        // not needed
    }

    @Override
    public @Nullable Object invokeInstruction() {
        return null;
    }

}
