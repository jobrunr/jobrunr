package org.jobrunr.jobs.details;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.AbstractJVMInstruction;
import org.jobrunr.jobs.details.instructions.StoreVariableInstruction;
import org.jobrunr.utils.streams.StreamUtils;

import java.lang.invoke.SerializedLambda;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQClassName;

public class JobDetailsFinderContext {

    private final SerializedLambda serializedLambda;
    private final LinkedList<AbstractJVMInstruction> instructions;
    private final List<Object> localVariables;

    public JobDetailsFinderContext(SerializedLambda serializedLambda, List<Object> params) {
        this(serializedLambda, params.toArray());
    }

    public JobDetailsFinderContext(SerializedLambda serializedLambda, Object... params) {
        this.serializedLambda = serializedLambda;
        this.instructions = new LinkedList<>();
        this.localVariables = initLocalVariables(params);
    }

    public void pushInstructionOnStack(AbstractJVMInstruction jvmInstruction) {
        jvmInstruction.setNbrInStack(instructions.size());
        instructions.add(jvmInstruction);
    }

    public Object getLocalVariable(int nbrInStack) {
        if (nbrInStack < localVariables.size()) {
            return localVariables.get(nbrInStack);
        } else if (hasVariableStoredInInstruction(nbrInStack)) {
            return variableStoredInInstruction(nbrInStack);
        }
        throw JobRunrException.shouldNotHappenException("Can not find variable " + nbrInStack + " in stack");
    }

    public LinkedList<AbstractJVMInstruction> getInstructions() {
        return instructions;
    }

    public AbstractJVMInstruction pollLastInstruction() {
        return instructions.pollLast();
    }

    public JobDetails getJobDetails() {
        if (jobIsCalledViaMethodReference()) {
            return new JobDetails(toFQClassName(serializedLambda.getImplClass()), null, serializedLambda.getImplMethodName(), new ArrayList<>());
        }
        return (JobDetails) pollLastInstruction().invokeInstruction();
    }

    private List<Object> initLocalVariables(Object[] params) {
        List<Object> result = new ArrayList<>();
        for (int i = 0; i < serializedLambda.getCapturedArgCount(); i++) {
            final Object capturedArg = serializedLambda.getCapturedArg(i);
            result.add(capturedArg);
            if (capturedArg instanceof Long || capturedArg instanceof Double) { //why: If the local variable at index is of type double or long, it occupies both index and index + 1. See https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
                result.add(null);
            }
        }
        result.addAll(Arrays.asList(params));
        return result;
    }

    private boolean hasVariableStoredInInstruction(int nbrInStack) {
        return StreamUtils.ofType(instructions, StoreVariableInstruction.class)
                .anyMatch(instruction -> nbrInStack == instruction.getVariable());
    }

    private Object variableStoredInInstruction(int nbrInStack) {
        final StoreVariableInstruction storeVariableInstruction = StreamUtils.ofType(instructions, StoreVariableInstruction.class)
                .filter(instruction -> nbrInStack == instruction.getVariable())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot happen"));
        instructions.remove(storeVariableInstruction);
        return storeVariableInstruction.invokeInstruction();
    }

    private boolean jobIsCalledViaMethodReference() {
        return instructions.isEmpty();
    }
}
