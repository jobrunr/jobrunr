package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class DupOperandInstruction extends ZeroOperandInstruction {

    public DupOperandInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public void load() {
        // not needed
        //System.out.println("DUP");
        //jobDetailsBuilder.pushInstructionOnStack(this);
    }

    @Override
    public Object invokeInstruction() {
        if (jobDetailsBuilder.getStack().isEmpty()) return DO_NOT_PUT_ON_STACK;
        Object toDup = jobDetailsBuilder.getStack().getLast();
        if (toDup == null) return DO_NOT_PUT_ON_STACK;
        if (toDup.getClass().equals(String.class) || toDup.getClass().isPrimitive()) return DO_NOT_PUT_ON_STACK;
        return toDup;
    }

}
