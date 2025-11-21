package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

import java.util.Deque;

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
        Deque<Object> stack = jobDetailsBuilder.getStack();
        Object el1 = stack.pollFirst();
        Object el2 = stack.pollFirst();
        stack.addFirst(el1); // goes to position 1
        stack.addFirst(el2); // goes to position 0
        return DO_NOT_PUT_ON_STACK;
    }

}
