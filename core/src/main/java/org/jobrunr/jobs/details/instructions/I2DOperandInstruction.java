package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class I2DOperandInstruction extends ZeroOperandInstruction {

    public I2DOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        int intValue = (int) jobDetailsBuilder.getStack().pollLast();
        return (double) intValue;
    }
}
