package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class I2BOperandInstruction extends ZeroOperandInstruction {

    public I2BOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public void load() {
        // not needed
    }

    @Override
    public Object invokeInstruction() {
        return null;
    }
}
