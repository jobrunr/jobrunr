package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class PopOperandInstruction extends ZeroOperandInstruction {

    public PopOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
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
