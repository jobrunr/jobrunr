package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsFinderContext;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class AllJVMInstructions {

    private static final Map<Integer, Function<JobDetailsFinderContext, AbstractJVMInstruction>> instructions = new HashMap<>();

    static {
        instructions.put(Opcodes.AASTORE, AAStoreInstruction::new);
        instructions.put(Opcodes.ALOAD, ALoadOperandInstruction::new);
        instructions.put(Opcodes.ANEWARRAY, ANewArrayOperandInstruction::new);
        instructions.put(Opcodes.ASTORE, AStoreInstruction::new);
        instructions.put(Opcodes.BIPUSH, SingleIntOperandInstruction::new);
        instructions.put(Opcodes.DLOAD, DLoadOperandInstruction::new);
        instructions.put(Opcodes.FLOAD, FLoadOperandInstruction::new);
        instructions.put(Opcodes.ICONST_0, IConst0OperandInstruction::new);
        instructions.put(Opcodes.ICONST_1, IConst1OperandInstruction::new);
        instructions.put(Opcodes.ICONST_2, IConst2OperandInstruction::new);
        instructions.put(Opcodes.ICONST_3, IConst3OperandInstruction::new);
        instructions.put(Opcodes.ICONST_4, IConst4OperandInstruction::new);
        instructions.put(Opcodes.ICONST_5, IConst5OperandInstruction::new);
        instructions.put(Opcodes.INVOKEDYNAMIC, InvokeDynamicInstruction::new);
        instructions.put(Opcodes.INVOKEINTERFACE, InvokeInterfaceInstruction::new);
        instructions.put(Opcodes.INVOKESPECIAL, InvokeSpecialInstruction::new);
        instructions.put(Opcodes.INVOKESTATIC, InvokeStaticInstruction::new);
        instructions.put(Opcodes.INVOKEVIRTUAL, InvokeVirtualInstruction::new);
        instructions.put(Opcodes.ISTORE, IStoreInstruction::new);
        instructions.put(Opcodes.LSTORE, LStoreInstruction::new);
        instructions.put(Opcodes.DSTORE, DStoreInstruction::new);
        instructions.put(Opcodes.FSTORE, FStoreInstruction::new);
        instructions.put(Opcodes.DUP, DupOperandInstruction::new);
        instructions.put(Opcodes.ILOAD, ILoadOperandInstruction::new);
        instructions.put(Opcodes.LCONST_0, LConst0OperandInstruction::new);
        instructions.put(Opcodes.LCONST_1, LConst1OperandInstruction::new);
        instructions.put(Opcodes.LDC, LdcInstruction::new);
        instructions.put(Opcodes.LLOAD, LLoadOperandInstruction::new);
        instructions.put(Opcodes.NEW, NewOperandInstruction::new);
        instructions.put(Opcodes.POP, PopOperandInstruction::new);
        instructions.put(Opcodes.GETFIELD, GetFieldInstruction::new);
        instructions.put(Opcodes.GETSTATIC, GetStaticInstruction::new);
        instructions.put(Opcodes.RETURN, ReturnOperandInstruction::new);
        instructions.put(Opcodes.SIPUSH, SingleIntOperandInstruction::new);
    }

    private AllJVMInstructions() {

    }

    public static <T extends AbstractJVMInstruction> T get(int opcode, JobDetailsFinderContext jobDetailsBuilder) {
        final Function<JobDetailsFinderContext, AbstractJVMInstruction> instructionBuilder = instructions.get(opcode);
        if (instructionBuilder == null) {
            throw JobRunrException.shouldNotHappenException(new IllegalArgumentException("Instruction " + opcode + " not found"));
        }
        return cast(instructionBuilder.apply(jobDetailsBuilder));
    }

}
