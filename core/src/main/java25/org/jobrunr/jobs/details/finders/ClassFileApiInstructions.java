package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.jobs.details.instructions.AllJVMInstructions;
import org.jobrunr.jobs.details.instructions.LdcInstruction;
import org.jobrunr.jobs.details.instructions.SingleIntOperandInstruction;
import org.jobrunr.jobs.details.instructions.VisitFieldInstruction;
import org.jobrunr.jobs.details.instructions.VisitLocalVariableInstruction;
import org.jobrunr.jobs.details.instructions.VisitMethodInstruction;
import org.jobrunr.jobs.details.instructions.VisitTypeInstruction;
import org.jobrunr.jobs.details.instructions.ZeroOperandInstruction;

import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.Instruction;
import java.lang.classfile.Opcode;
import java.lang.classfile.instruction.ArrayLoadInstruction;
import java.lang.classfile.instruction.ArrayStoreInstruction;
import java.lang.classfile.instruction.BranchInstruction;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.ConvertInstruction;
import java.lang.classfile.instruction.FieldInstruction;
import java.lang.classfile.instruction.IncrementInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LoadInstruction;
import java.lang.classfile.instruction.LookupSwitchInstruction;
import java.lang.classfile.instruction.MonitorInstruction;
import java.lang.classfile.instruction.NewMultiArrayInstruction;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewPrimitiveArrayInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.NopInstruction;
import java.lang.classfile.instruction.OperatorInstruction;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.classfile.instruction.StackInstruction;
import java.lang.classfile.instruction.StoreInstruction;
import java.lang.classfile.instruction.TableSwitchInstruction;
import java.lang.classfile.instruction.ThrowInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

/**
 * Feeds the instructions of one method into the shared {@link AllJVMInstructions} table. Shared by the
 * Java and the Kotlin Class File API finders so both read bytecode the same way.
 */
class ClassFileApiInstructions {

    private final JobDetailsBuilder jobDetailsBuilder;

    ClassFileApiInstructions(JobDetailsBuilder jobDetailsBuilder) {
        this.jobDetailsBuilder = jobDetailsBuilder;
    }

    void visit(CodeModel codeModel) {
        codeModel.forEach(this::visit);
    }

    private void visit(CodeElement codeElement) {
        if (!(codeElement instanceof Instruction instruction)) return;
        int opcode = genericOpcode(instruction.opcode());

        switch (codeElement) {
            case ConstantInstruction.IntrinsicConstantInstruction _ -> {
                ZeroOperandInstruction zeroOperandInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                zeroOperandInstruction.load();
            }
            case ConstantInstruction.ArgumentConstantInstruction argumentConstantInstruction -> {
                SingleIntOperandInstruction singleIntOperandInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                singleIntOperandInstruction.load(argumentConstantInstruction.constantValue());
            }
            case ConstantInstruction.LoadConstantInstruction loadConstantInstruction -> {
                LdcInstruction ldcInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                ldcInstruction.load(loadConstantInstruction.constantValue());
            }
            case InvokeInstruction invokeInstruction -> {
                VisitMethodInstruction visitMethodInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                visitMethodInstruction.load(invokeInstruction.owner().asInternalName(), invokeInstruction.name().stringValue(), invokeInstruction.type().stringValue(), invokeInstruction.isInterface());
            }
            case FieldInstruction fieldInstruction -> {
                VisitFieldInstruction visitFieldInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                visitFieldInstruction.load(fieldInstruction.owner().asInternalName(), fieldInstruction.name().stringValue(), fieldInstruction.type().stringValue());
            }
            case LoadInstruction loadInstruction -> {
                VisitLocalVariableInstruction visitLocalVariableInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                visitLocalVariableInstruction.load(loadInstruction.slot());
            }
            case StoreInstruction storeInstruction -> {
                VisitLocalVariableInstruction visitLocalVariableInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                visitLocalVariableInstruction.load(storeInstruction.slot());
            }
            case NewObjectInstruction newObjectInstruction -> {
                VisitTypeInstruction visitTypeInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                visitTypeInstruction.load(newObjectInstruction.className().asInternalName());
            }
            case NewReferenceArrayInstruction newReferenceArrayInstruction -> {
                VisitTypeInstruction visitTypeInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                visitTypeInstruction.load(newReferenceArrayInstruction.componentType().asInternalName());
            }
            case TypeCheckInstruction typeCheckInstruction -> {
                VisitTypeInstruction visitTypeInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                visitTypeInstruction.load(typeCheckInstruction.type().asInternalName());
            }
            case InvokeDynamicInstruction invokeDynamicInstruction -> {
                org.jobrunr.jobs.details.instructions.InvokeDynamicInstruction jobRunrInvokeDynamicInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                jobRunrInvokeDynamicInstruction.load(invokeDynamicInstruction.name().stringValue(), invokeDynamicInstruction.type().stringValue(), invokeDynamicInstruction.bootstrapArgs().toArray());
            }
            case ReturnInstruction _, StackInstruction _, ArrayStoreInstruction _, ArrayLoadInstruction _, ConvertInstruction _ -> {
                ZeroOperandInstruction zeroOperandInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                zeroOperandInstruction.load();
            }
            case IncrementInstruction _, BranchInstruction _, TableSwitchInstruction _, LookupSwitchInstruction _,
                 OperatorInstruction _, ThrowInstruction _, NewPrimitiveArrayInstruction _, NewMultiArrayInstruction _,
                 MonitorInstruction _, NopInstruction _ -> AllJVMInstructions.get(opcode, jobDetailsBuilder);
            default ->
                    throw shouldNotHappenException(new IllegalStateException("Unsupported class file instruction " + instruction.opcode() + " (" + codeElement.getClass().getSimpleName() + ")"));
        }
    }

    /**
     * Maps a {@link Opcode} from java.lang.classfile to the equivalent ASM opcode used by
     * {@link AllJVMInstructions}.
     * <p>
     * ASM normalises wide and one-byte forms before its visitor sees them
     * ({@code goto_w}/{@code jsr_w} -> {@code goto}/{@code jsr}, {@code ldc_w}/{@code ldc2_w} -> {@code ldc},
     * {@code iload_0} .. {@code astore_3} -> {@code iload} .. {@code astore}); java.lang.classfile keeps
     * them, so map them back here.
     *
     * @param opcode the java.lang.classfile opcode
     * @return the equivalent ASM opcode
     */
    static int genericOpcode(Opcode opcode) {
        return switch (opcode) {
            case ILOAD_0, ILOAD_1, ILOAD_2, ILOAD_3, ILOAD_W -> Opcode.ILOAD.bytecode();
            case LLOAD_0, LLOAD_1, LLOAD_2, LLOAD_3, LLOAD_W -> Opcode.LLOAD.bytecode();
            case FLOAD_0, FLOAD_1, FLOAD_2, FLOAD_3, FLOAD_W -> Opcode.FLOAD.bytecode();
            case DLOAD_0, DLOAD_1, DLOAD_2, DLOAD_3, DLOAD_W -> Opcode.DLOAD.bytecode();
            case ALOAD_0, ALOAD_1, ALOAD_2, ALOAD_3, ALOAD_W -> Opcode.ALOAD.bytecode();
            case ISTORE_0, ISTORE_1, ISTORE_2, ISTORE_3, ISTORE_W -> Opcode.ISTORE.bytecode();
            case LSTORE_0, LSTORE_1, LSTORE_2, LSTORE_3, LSTORE_W -> Opcode.LSTORE.bytecode();
            case FSTORE_0, FSTORE_1, FSTORE_2, FSTORE_3, FSTORE_W -> Opcode.FSTORE.bytecode();
            case DSTORE_0, DSTORE_1, DSTORE_2, DSTORE_3, DSTORE_W -> Opcode.DSTORE.bytecode();
            case ASTORE_0, ASTORE_1, ASTORE_2, ASTORE_3, ASTORE_W -> Opcode.ASTORE.bytecode();
            case IINC_W -> Opcode.IINC.bytecode();
            case RET_W -> Opcode.RET.bytecode();
            case LDC_W, LDC2_W -> Opcode.LDC.bytecode();
            case GOTO_W -> Opcode.GOTO.bytecode();
            case JSR_W -> Opcode.JSR.bytecode();
            default -> opcode.bytecode();
        };
    }
}
