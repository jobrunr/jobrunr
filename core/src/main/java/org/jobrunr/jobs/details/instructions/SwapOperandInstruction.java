package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

import java.util.LinkedList;

public class SwapOperandInstruction extends ZeroOperandInstruction {

    public SwapOperandInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public void load() {
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    @Override
    public Object invokeInstruction() {
        LinkedList<Object> stack = jobDetailsBuilder.getStack();
        Object el1 = stack.get(0);
        Object el2 = stack.get(1);
        stack.set(0, el2);
        stack.set(1, el1);
        return DO_NOT_PUT_ON_STACK;
    }

}
