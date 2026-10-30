package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.details.JobDetailsBuilder;
import org.jobrunr.jobs.details.instructions.AllJVMInstructions;
import org.jobrunr.jobs.details.instructions.InvokeDynamicInstruction;
import org.jobrunr.jobs.details.instructions.LdcInstruction;
import org.jobrunr.jobs.details.instructions.SingleIntOperandInstruction;
import org.jobrunr.jobs.details.instructions.VisitFieldInstruction;
import org.jobrunr.jobs.details.instructions.VisitLocalVariableInstruction;
import org.jobrunr.jobs.details.instructions.VisitMethodInstruction;
import org.jobrunr.jobs.details.instructions.VisitTypeInstruction;
import org.jobrunr.jobs.details.instructions.ZeroOperandInstruction;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Feeds the instructions of one method into the shared {@link AllJVMInstructions} table. Shared by the
 * Java and the Kotlin ASM finders so both read bytecode the same way.
 */
class AsmInstructionsMethodVisitor extends MethodVisitor {

    private final JobDetailsBuilder jobDetailsBuilder;

    AsmInstructionsMethodVisitor(JobDetailsBuilder jobDetailsBuilder) {
        super(Opcodes.ASM7);
        this.jobDetailsBuilder = jobDetailsBuilder;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        VisitFieldInstruction instruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
        instruction.load(owner, name, descriptor);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
        InvokeDynamicInstruction instruction = AllJVMInstructions.get(Opcodes.INVOKEDYNAMIC, jobDetailsBuilder);
        instruction.load(name, descriptor, bootstrapMethodArguments);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        VisitMethodInstruction visitMethodInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
        visitMethodInstruction.load(owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInsn(int opcode) {
        ZeroOperandInstruction zeroOperandInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
        zeroOperandInstruction.load();
    }

    @Override
    public void visitVarInsn(int opcode, int variable) {
        VisitLocalVariableInstruction instruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
        instruction.load(variable);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        SingleIntOperandInstruction singleIntOperandInstruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
        singleIntOperandInstruction.load(operand);
    }

    @Override
    public void visitLdcInsn(Object value) {
        LdcInstruction ldcInstruction = AllJVMInstructions.get(Opcodes.LDC, jobDetailsBuilder);
        ldcInstruction.load(value);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        VisitTypeInstruction instruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
        instruction.load(type);
    }

    @Override
    public void visitIincInsn(int varIndex, int increment) {
        AllJVMInstructions.get(Opcodes.IINC, jobDetailsBuilder);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        AllJVMInstructions.get(opcode, jobDetailsBuilder);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        AllJVMInstructions.get(Opcodes.TABLESWITCH, jobDetailsBuilder);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        AllJVMInstructions.get(Opcodes.LOOKUPSWITCH, jobDetailsBuilder);
    }
}
