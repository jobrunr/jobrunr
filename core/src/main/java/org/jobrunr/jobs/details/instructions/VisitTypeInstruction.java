package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

public abstract class VisitTypeInstruction extends AbstractJVMInstruction {

    protected String type;

    protected VisitTypeInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(String type) {
        this.type = type;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

}
