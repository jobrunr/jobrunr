package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class AAStoreInstruction extends ZeroOperandInstruction {

    public AAStoreInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        Object value = jobDetailsBuilder.getStack().pollLast();
        int index = (int) jobDetailsBuilder.getStack().pollLast();
        Object[] array = (Object[]) jobDetailsBuilder.getStack().pollLast();
        array[index] = value;
        return DO_NOT_PUT_ON_STACK;
    }
}
