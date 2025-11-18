package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.details.instructions.AbstractJVMInstruction;
import org.jobrunr.jobs.details.postprocess.CGLibPostProcessor;
import org.jobrunr.jobs.details.postprocess.JobDetailsPostProcessor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

public abstract class JobDetailsBuilder {

    private final Deque<AbstractJVMInstruction> instructions;
    private final LinkedList<Object> stack;
    private final List<Object> localVariables;
    private String jobDetailsClassName;
    private String jobDetailsStaticFieldName;
    private String jobDetailsMethodName;
    private List<JobParameter> jobDetailsJobParameters;
    private List<JobDetailsPostProcessor> jobDetailsPostProcessors;

    protected JobDetailsBuilder(List<Object> localVariables) {
        this(localVariables, null, null);
    }

    protected JobDetailsBuilder(List<Object> localVariables, String className, String methodName) {
        this.instructions = new ArrayDeque<>();
        this.stack = new LinkedList<>();
        this.localVariables = localVariables;

        setClassName(className);
        setMethodName(methodName);
        setJobParameters(new ArrayList<>());
        jobDetailsPostProcessors = singletonList(new CGLibPostProcessor());
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

    public Deque<AbstractJVMInstruction> getInstructions() {
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
        final JobDetails jobDetails = new JobDetails(jobDetailsClassName, jobDetailsStaticFieldName, jobDetailsMethodName, jobDetailsJobParameters);
        return postProcessJobDetails(jobDetails);
    }

    private JobDetails postProcessJobDetails(JobDetails jobDetails) {
        JobDetails currentJobDetails = jobDetails;
        for (JobDetailsPostProcessor postProcessor : getJobDetailsPostProcessors()) {
            currentJobDetails = postProcessor.postProcess(currentJobDetails);
        }
        return currentJobDetails;
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

    public void setClassName(String className) {
        if (jobDetailsStaticFieldName == null) {
            jobDetailsClassName = className;
        }
    }

    public void setStaticFieldName(String name) {
        jobDetailsStaticFieldName = name;
    }

    public void setMethodName(String name) {
        if (name.endsWith("$default") && classExists("kotlin.KotlinVersion")) {
            throw new IllegalArgumentException("Unsupported lambda", new UnsupportedOperationException("You are (probably) using Kotlin default parameter values which is not supported by JobRunr."));
        }
        jobDetailsMethodName = name;
    }

    public void setJobParameters(List<JobParameter> jobParameters) {
        jobDetailsJobParameters = jobParameters;
    }

    List<JobDetailsPostProcessor> getJobDetailsPostProcessors() {
        return jobDetailsPostProcessors;
    }
}
