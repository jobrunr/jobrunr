package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class VisitTypeInstruction extends AbstractJVMInstruction {

    private String type;

    public VisitTypeInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(String type) {
        this.type = type;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    @Override
    public Object invokeInstruction() {
        return null;
    }

    public String getType() {
        return type;
    }
}
