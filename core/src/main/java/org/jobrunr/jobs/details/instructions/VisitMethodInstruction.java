package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.jobrunr.utils.reflection.ReflectionUtils.autobox;

public abstract class VisitMethodInstruction extends AbstractJVMInstruction {

    protected String owner;
    protected String name;
    protected String descriptor;
    protected boolean isInterface;

    protected VisitMethodInstruction(JobDetailsBuilder jobDetailsBuilder) {
        super(jobDetailsBuilder);
    }

    public void load(String owner, String name, String descriptor, boolean isInterface) {
        this.owner = owner;
        this.name = name;
        this.descriptor = descriptor;
        this.isInterface = isInterface;
        jobDetailsBuilder.pushInstructionOnStack(this);
    }

    protected boolean isVoidInstruction() {
        return descriptor.endsWith(")V");
    }

    protected List<Object> getParametersUsingParamTypes(Class<?>[] paramTypesAsArray) {
        LinkedList<Class<?>> paramTypes = new LinkedList<>(Arrays.asList(paramTypesAsArray));
        List<Object> result = new ArrayList<>();
        while (!paramTypes.isEmpty()) {
            Class<?> paramType = paramTypes.pollLast();
            result.add(0, autobox(jobDetailsBuilder.getStack().pollLast(), paramType));
        }
        return result;
    }

}
