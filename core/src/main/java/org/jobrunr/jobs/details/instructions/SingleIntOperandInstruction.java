package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class SingleIntOperandInstruction extends AbstractJVMInstruction {

    private int intValue;

    public SingleIntOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(int intValue) {
        this.intValue = intValue;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    @Override
    public Object invokeInstruction() {
        return intValue;
    }
}
