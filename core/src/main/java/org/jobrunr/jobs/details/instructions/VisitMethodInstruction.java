package org.jobrunr.jobs.details.instructions;

import org.jobrunr.jobs.details.JobDetailsFinderContext;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

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

    protected List<Object> getParametersUsingParamTypes(Class[] paramTypesAsArray) {
        LinkedList<Class> paramTypes = new LinkedList<>(Arrays.asList(paramTypesAsArray));
        List<Object> result = new ArrayList<>();
        while (!paramTypes.isEmpty()) {
            final Class aClass = paramTypes.pollLast();
            if (aClass.isArray()) {
                result.add(0, getArrayParameter(aClass));
            } else {
                result.add(0, getSimpleParameter());
            }
        }
        return result;
    }

    private <T> T[] getArrayParameter(Class<T> aClass) {
        AbstractJVMInstruction instruction = jobDetailsBuilder.pollLastInstruction();
        List<T> arrayItems = new ArrayList<>();
        while (!(instruction instanceof ANewArrayOperandInstruction)) {
            final T o = cast(instruction.invokeInstruction());
            arrayItems.add(0, o);

            jobDetailsBuilder.pollLastInstruction(); // not needed - it's the int where to store the item in the array
            instruction = jobDetailsBuilder.pollLastInstruction();
        }
        jobDetailsBuilder.pollLastInstruction(); // not needed - it's the array size
        return toArray(aClass, arrayItems);
    }

    private Object getSimpleParameter() {
        return jobDetailsBuilder.pollLastInstruction().invokeInstruction();
    }

    private <T> T[] toArray(Class<T> aClass, List<T> arrayItems) {
        final T[] result = cast(Array.newInstance(aClass.getComponentType(), arrayItems.size()));
        arrayItems.toArray(result);
        return result;
    }
}
