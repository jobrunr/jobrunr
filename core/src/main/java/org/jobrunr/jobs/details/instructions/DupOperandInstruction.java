package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class DupOperandInstruction extends ZeroOperandInstruction {

    public DupOperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public void load() {
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    @Override
    public Object invokeInstruction() {
        if (jobDetailsBuilder.getStack().isEmpty()) return DO_NOT_PUT_ON_STACK;
        return jobDetailsBuilder.getStack().getLast();
    }

}
