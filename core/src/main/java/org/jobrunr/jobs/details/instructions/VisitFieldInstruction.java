package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

public abstract class VisitFieldInstruction extends AbstractJVMInstruction {

    protected String owner;
    protected String name;
    protected String descriptor;

    protected VisitFieldInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }
}
