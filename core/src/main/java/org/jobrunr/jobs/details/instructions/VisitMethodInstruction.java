package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public abstract class VisitMethodInstruction extends AbstractJVMInstruction {

    protected String owner;
    protected String name;
    protected String descriptor;
    protected boolean isInterface;

    public VisitMethodInstruction(JobDetailsFinderContext jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(String owner, String name, String descriptor, boolean isInterface) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.isInterface = isInterface;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    protected List<Object> getParametersUsingParamTypes(Class<?>[] paramTypesAsArray) {
        LinkedList<Class<?>> paramTypes = new LinkedList<>(Arrays.asList(paramTypesAsArray));
        List<Object> result = new ArrayList<>();
        while (!paramTypes.isEmpty()) {
            final Class<?> aClass = paramTypes.pollLast();
            result.add(0, jobDetailsBuilder.getStack().pollLast());
        }
        return result;
    }

}
