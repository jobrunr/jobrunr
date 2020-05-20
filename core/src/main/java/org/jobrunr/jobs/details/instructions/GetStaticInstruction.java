package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;
import org.jobrunr.jobs.details.JobDetailsGeneratorUtils;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class GetStaticInstruction extends VisitFieldInstruction {

    public GetStaticInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    @Override
    public Object invokeInstruction() {
        String className = toFQClassName(owner);
        String methodName = name;

        return JobDetailsGeneratorUtils.getObjectViaStaticField(className, methodName);
    }
}
