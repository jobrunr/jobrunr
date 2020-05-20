package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class LdcInstruction extends AbstractJVMInstruction {

    private Object value;

    public LdcInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        return value;
    }

    public void load(Object value) {
        this.value = value;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }
}
