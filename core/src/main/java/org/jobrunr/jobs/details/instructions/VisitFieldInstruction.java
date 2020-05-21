package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsFinderContext;

public class VisitFieldInstruction extends AbstractJVMInstruction {

    protected String owner;
    protected String name;
    protected String descriptor;

    public VisitFieldInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        throw new JobRunrException("Are you trying to enqueue a multiline lambda? Performance-wise this is probaly not a good idea and it is not yet supported. If you do think this must be possible, please create a bug report why and provide the code to reproduce this and the stacktrace");
    }

    public void load(String owner, String name, String descriptor) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }
}
