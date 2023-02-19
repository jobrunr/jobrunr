package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class I2LOperandInstruction extends ZeroOperandInstruction {

    public I2LOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        int intValue = (int) jobDetailsBuilder.getStack().pollLast();
        return (long) intValue;
    }
}
