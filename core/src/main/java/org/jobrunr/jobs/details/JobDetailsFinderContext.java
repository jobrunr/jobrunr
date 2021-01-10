package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.details.instructions.AbstractJVMInstruction;
import org.jobrunr.jobs.lambdas.IocJobLambda;

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class JobDetailsFinderContext {

    private final SerializedLambda serializedLambda;
    private final LinkedList<AbstractJVMInstruction> instructions;
    private final LinkedList<Object> stack;
    private final List<Object> localVariables;
    private String jobDetailsClassName;
    private String jobDetailsStaticFieldName;
    private String jobDetailsMethodName;
    private List<JobParameter> jobDetailsJobParameters;

    public JobDetailsFinderContext(SerializedLambda serializedLambda, Object... params) {
        this.serializedLambda = serializedLambda;
        this.instructions = new LinkedList<>();
        this.stack = new LinkedList<>();
        this.localVariables = initLocalVariables(params);

        init();
    }

    protected void init() {
        setClassName(toFQClassName(serializedLambda.getImplClass()));
        setMethodName(serializedLambda.getImplMethodName());
        setJobParameters(new ArrayList<>());
    }

    public void pushInstructionOnStack(AbstractJVMInstruction jvmInstruction) {
        instructions.add(jvmInstruction);
    }

    public Object getLocalVariable(int nbrInStack) {
        if (nbrInStack < localVariables.size()) {
            return localVariables.get(nbrInStack);
        }
        throw JobRunrException.shouldNotHappenException("Can not find variable " + nbrInStack + " in stack");
    }

    public void addLocalVariable(Object o) {
        this.localVariables.add(o);
    }

    public List<AbstractJVMInstruction> getInstructions() {
        return instructions;
    }

    public AbstractJVMInstruction pollFirstInstruction() {
        return instructions.pollFirst();
    }

    public LinkedList<Object> getStack() {
        return stack;
    }

    public JobDetails getJobDetails() {
        invokeInstructions();
        return new JobDetails(jobDetailsClassName, jobDetailsStaticFieldName, jobDetailsMethodName, jobDetailsJobParameters);
    }

    private void invokeInstructions() {
        if (instructions.isEmpty() && localVariables.size() > 1) { // it is a method reference
            for (int i = 1; i < localVariables.size(); i++) {
                Object variable = localVariables.get(i);
                jobDetailsJobParameters.add(new JobParameter(variable));
            }
        } else {
            AbstractJVMInstruction instruction = pollFirstInstruction();
            while (instruction != null) {
                instruction.invokeInstructionAndPushOnStack();
                instruction = pollFirstInstruction();
            }
        }
    }

    protected List<Object> initLocalVariables(Object[] params) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < serializedLambda.getCapturedArgCount(); i++) {
            final Object capturedArg = serializedLambda.getCapturedArg(i);
            result.add(capturedArg);
            //why: If the local variable at index is of type double or long, it occupies both index and index + 1. See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
            if (capturedArg instanceof Long || capturedArg instanceof Double) {
                result.add(null);
            }
        }
        result.addAll(Arrays.asList(params));
        if (IocJobLambda.class.getName().equals(toFQClassName(serializedLambda.getFunctionalInterfaceClass()))) {
            result.add(null); // will be injected by IoC
        }
        return result;
    }

    public void setClassName(String className) {
        if (jobDetailsStaticFieldName == null) {
            jobDetailsClassName = className;
        }
    }

    public void setStaticFieldName(String name) {
        jobDetailsStaticFieldName = name;
    }

    public void setMethodName(String name) {
        jobDetailsMethodName = name;
    }

    public void setJobParameters(List<JobParameter> jobParameters) {
        jobDetailsJobParameters = jobParameters;
    }
}
