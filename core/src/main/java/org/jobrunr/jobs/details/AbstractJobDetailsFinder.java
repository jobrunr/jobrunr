package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.*;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

abstract class AbstractJobDetailsFinder extends ClassVisitor {

    protected final JobDetailsBuilder jobDetailsBuilder;

    protected AbstractJobDetailsFinder(JobDetailsBuilder jobDetailsBuilder) {
        super(Opcodes.ASM7);
        this.jobDetailsBuilder = jobDetailsBuilder;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (isLambdaContainingJobDetails(name)) {
            return new MethodVisitor(Opcodes.ASM7) {

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    VisitFieldInstruction instruction = AllJVMInstructions.get(opcode, jobDetailsBuilder);
                    instruction.load(owner, name, descriptor);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                    InvokeDynamicInstruction instruction = AllJVMInstructions.get(Opcodes.INVOKEDYNAMIC, jobDetailsBuilder);
                    instruction.load(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
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
            };
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

    protected abstract boolean isLambdaContainingJobDetails(String name);

    protected abstract InputStream getClassContainingLambdaAsInputStream();

    public JobDetails getJobDetails() {
        return jobDetailsBuilder.getJobDetails();
    }

    protected void parse(InputStream inputStream) {
        try {
            ClassReader parser = new ClassReader(inputStream);
            parser.accept(this, ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            throw shouldNotHappenException(e);
        }
    }

}
