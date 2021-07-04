package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class AllJVMInstructions {

    private static final Map<Integer, Function<JobDetailsBuilder, AbstractJVMInstruction>> instructions = new HashMap<>();
    private static final Map<Integer, String> unsupportedInstructions = new HashMap<>();

    static {
        instructions.put(Opcodes.AASTORE, AAStoreInstruction::new);
        instructions.put(Opcodes.ALOAD, ALoadOperandInstruction::new);
        instructions.put(Opcodes.ANEWARRAY, ANewArrayOperandInstruction::new);
        instructions.put(Opcodes.ASTORE, AStoreInstruction::new);
        instructions.put(Opcodes.BIPUSH, SingleIntOperandInstruction::new);
        instructions.put(Opcodes.CHECKCAST, CheckCastOperandInstruction::new);
        instructions.put(Opcodes.DLOAD, DLoadOperandInstruction::new);
        instructions.put(Opcodes.FLOAD, FLoadOperandInstruction::new);
        instructions.put(Opcodes.I2B, I2BOperandInstruction::new);
        instructions.put(Opcodes.I2S, I2SOperandInstruction::new);
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

        String mathematicalPerformanceSuffix = " - for performance reasons it is better to do the calculation outside of the job lambda";
        asList(Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD)
                .forEach(instr -> unsupportedInstructions.put(instr, "You are summing two numbers while enqueueing/scheduling jobs" + mathematicalPerformanceSuffix));
        asList(Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB)
                .forEach(instr -> unsupportedInstructions.put(instr, "You are subtracting two numbers while enqueueing/scheduling jobs" + mathematicalPerformanceSuffix));
        asList(Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL)
                .forEach(instr -> unsupportedInstructions.put(instr, "You are multiplying two numbers while enqueueing/scheduling jobs" + mathematicalPerformanceSuffix));
        asList(Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV)
                .forEach(instr -> unsupportedInstructions.put(instr, "You are dividing two numbers while enqueueing/scheduling jobs" + mathematicalPerformanceSuffix));
        asList(Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM)
                .forEach(instr -> unsupportedInstructions.put(instr, "You are calculating the remainder (%) for two numbers while enqueueing/scheduling jobs" + mathematicalPerformanceSuffix));
    }

    private AllJVMInstructions() {

    }

    public static <T extends AbstractJVMInstruction> T get(int opcode, JobDetailsBuilder jobDetailsBuilder) {
        final Function<JobDetailsBuilder, AbstractJVMInstruction> instructionBuilder = instructions.get(opcode);
        if (instructionBuilder == null) {
            if (unsupportedInstructions.containsKey(opcode)) {
                throw new IllegalArgumentException("Unsupported lambda", new UnsupportedOperationException(unsupportedInstructions.get(opcode)));
            }
            throw JobRunrException.shouldNotHappenException(new IllegalArgumentException("Instruction " + opcode + " not found"));
        }
        return cast(instructionBuilder.apply(jobDetailsBuilder));
    }

}
