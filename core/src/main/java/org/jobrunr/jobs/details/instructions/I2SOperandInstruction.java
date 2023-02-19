package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public class I2SOperandInstruction extends ZeroOperandInstruction {

    public I2SOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        int intValue = (int) jobDetailsBuilder.getStack().pollLast();
        return (short) intValue;
    }
}
