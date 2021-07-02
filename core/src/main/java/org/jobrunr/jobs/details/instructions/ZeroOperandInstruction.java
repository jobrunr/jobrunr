package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public abstract class ZeroOperandInstruction extends AbstractJVMInstruction {

    protected ZeroOperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load() {
        jobDetailsBuilder.pushInstructionOnStack(this);
    }
}
