package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public abstract class ZeroOperandInstruction extends AbstractJVMInstruction {

    protected ZeroOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load() {
        jobDetailsBuilder.pushInstructionOnStack(this);
    }
}
