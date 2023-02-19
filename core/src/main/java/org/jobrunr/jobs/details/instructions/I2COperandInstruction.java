package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class I2COperandInstruction extends ZeroOperandInstruction {

    public I2COperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        int intValue = (int) jobDetailsBuilder.getStack().pollLast();
        return (char) intValue;
    }
}
