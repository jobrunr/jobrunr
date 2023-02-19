package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class I2FOperandInstruction extends ZeroOperandInstruction {

    public I2FOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        int intValue = (int) jobDetailsBuilder.getStack().pollLast();
        return (float) intValue;
    }
}
