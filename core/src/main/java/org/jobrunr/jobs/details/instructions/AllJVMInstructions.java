package org.jobrunr.jobs.details.instructions;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.jobs.details.UnsupportedLambdaException;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

/**
 * AllJVMInstructions contains the instructions supported by JobRunr and also all unsupported instructions.
 * <p>
 * <note>Although this class references {@link Opcodes} from ASM, all Opcodes are inlined during compilation.
 * This means that when on Java 25, one does not need to have ASM on the classpath as the Java Class File API is then used.</note>
 */
public class AllJVMInstructions {

    private static final Map<Integer, Function<JobDetailsBuilder, AbstractJVMInstruction>> instructions = new HashMap<>();
    private static final Map<Integer, String> unsupportedInstructions = new HashMap<>();

    static {
        instructions.put(Opcodes.ACONST_NULL, NullLoadOperandInstruction::new);
        instructions.put(Opcodes.AASTORE, AAStoreInstruction::new);
        instructions.put(Opcodes.ALOAD, ALoadOperandInstruction::new);
        instructions.put(Opcodes.ANEWARRAY, ANewArrayOperandInstruction::new);
        instructions.put(Opcodes.ASTORE, AStoreInstruction::new);
        instructions.put(Opcodes.BIPUSH, SingleIntOperandInstruction::new);
        instructions.put(Opcodes.CHECKCAST, CheckCastOperandInstruction::new);
        instructions.put(Opcodes.DLOAD, DLoadOperandInstruction::new);
        instructions.put(Opcodes.FLOAD, FLoadOperandInstruction::new);
        instructions.put(Opcodes.I2B, I2BOperandInstruction::new);
        instructions.put(Opcodes.I2C, I2COperandInstruction::new);
        instructions.put(Opcodes.I2D, I2DOperandInstruction::new);
        instructions.put(Opcodes.I2F, I2FOperandInstruction::new);
        instructions.put(Opcodes.I2L, I2LOperandInstruction::new);
        instructions.put(Opcodes.I2S, I2SOperandInstruction::new);
        instructions.put(Opcodes.ICONST_M1, IConstM1OperandInstruction::new);
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
        instructions.put(Opcodes.SWAP, SwapOperandInstruction::new);
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
        // needed to support Jacoco
        instructions.put(Opcodes.BASTORE, BAStoreOperandInstruction::new);

        registerUnsupported(sumOpcodes(), UnsupportedLambdaException.SUM);
        registerUnsupported(subtractionOpcodes(), UnsupportedLambdaException.SUBTRACTION);
        registerUnsupported(multiplicationOpcodes(), UnsupportedLambdaException.MULTIPLICATION);
        registerUnsupported(divisionOpcodes(), UnsupportedLambdaException.DIVISION);
        registerUnsupported(remainderOpcodes(), UnsupportedLambdaException.REMAINDER);
        registerUnsupported(incrementOpcodes(), UnsupportedLambdaException.INCREMENT);
        registerUnsupported(jumpOpcodes(), UnsupportedLambdaException.JUMP);
        registerUnsupported(switchOpcodes(), UnsupportedLambdaException.SWITCH);
    }

    private AllJVMInstructions() {

    }

    public static <T extends AbstractJVMInstruction> T get(int opcode, JobDetailsBuilder jobDetailsBuilder) {
        final Function<JobDetailsBuilder, AbstractJVMInstruction> instructionBuilder = instructions.get(opcode);
        if (instructionBuilder == null) {
            if (unsupportedInstructions.containsKey(opcode)) {
                throw new UnsupportedLambdaException(unsupportedInstructions.get(opcode));
            }
            throw JobRunrException.shouldNotHappenException(new IllegalArgumentException("Instruction " + opcode + " not found"));
        }
        return cast(instructionBuilder.apply(jobDetailsBuilder));
    }

    private static void registerUnsupported(List<Integer> opcodes, String message) {
        opcodes.forEach(opcode -> unsupportedInstructions.put(opcode, message));
    }

    private static List<Integer> sumOpcodes() {
        return asList(Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD);
    }

    private static List<Integer> subtractionOpcodes() {
        return asList(Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB);
    }

    private static List<Integer> multiplicationOpcodes() {
        return asList(Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL);
    }

    private static List<Integer> divisionOpcodes() {
        return asList(Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV);
    }

    private static List<Integer> remainderOpcodes() {
        return asList(Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM);
    }

    private static List<Integer> incrementOpcodes() {
        return singletonList(Opcodes.IINC);
    }

    private static List<Integer> jumpOpcodes() {
        return asList(Opcodes.IFEQ, Opcodes.IFNE, Opcodes.IFLT, Opcodes.IFGE, Opcodes.IFGT, Opcodes.IFLE,
                Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE, Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE, Opcodes.IF_ICMPGT, Opcodes.IF_ICMPLE,
                Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE, Opcodes.IFNULL, Opcodes.IFNONNULL,
                Opcodes.GOTO, Opcodes.JSR);
    }

    private static List<Integer> switchOpcodes() {
        return asList(Opcodes.TABLESWITCH, Opcodes.LOOKUPSWITCH);
    }

}
