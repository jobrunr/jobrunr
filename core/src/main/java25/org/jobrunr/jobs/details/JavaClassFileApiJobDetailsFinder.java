package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.instructions.AllJVMInstructions;
import org.jobrunr.jobs.details.instructions.LdcInstruction;
import org.jobrunr.jobs.details.instructions.SingleIntOperandInstruction;
import org.jobrunr.jobs.details.instructions.VisitFieldInstruction;
import org.jobrunr.jobs.details.instructions.VisitLocalVariableInstruction;
import org.jobrunr.jobs.details.instructions.VisitMethodInstruction;
import org.jobrunr.jobs.details.instructions.VisitTypeInstruction;
import org.jobrunr.jobs.details.instructions.ZeroOperandInstruction;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.Instruction;
import java.lang.classfile.MethodModel;
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
import java.lang.invoke.SerializedLambda;

import static java.util.Arrays.stream;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getJavaClassContainingLambdaAsInputStream;
import static org.jobrunr.utils.StringUtils.substringAfter;

class JavaClassFileApiJobDetailsFinder {

    private final JobDetailsBuilder jobDetailsBuilder;

    JavaClassFileApiJobDetailsFinder(JobRunrJob jobRunrJob, SerializedLambda serializedLambda, Object... params) {
        this.jobDetailsBuilder = new JavaJobDetailsBuilder(serializedLambda, params);
        if (isLambda(serializedLambda)) {
            try (InputStream classContainingLambdaInputStream = getJavaClassContainingLambdaAsInputStream(jobRunrJob)) {
                parse(serializedLambda.getImplMethodName(), classContainingLambdaInputStream.readAllBytes());
            } catch (IOException e) {
                throw shouldNotHappenException(e);
            }
        } else if (isKotlinMethodReference(serializedLambda)) {
            this.jobDetailsBuilder.setClassName(serializedLambda.getCapturedArg(0).getClass().getName());
            this.jobDetailsBuilder.setMethodName(serializedLambda.getImplMethodName().contains("$")
                    ? substringAfter(serializedLambda.getImplMethodName(), "$")
                    : serializedLambda.getImplMethodName());
        }
    }

    JobDetails getJobDetails() {
        return jobDetailsBuilder.getJobDetails();
    }

    void parse(String implMethodName, byte[] classBytes) {
        ClassModel classModel = ClassFile.of().parse(classBytes);
        MethodModel lambdaMethod = classModel.methods().stream()
                .filter(method -> method.methodName().stringValue().equals(implMethodName))
                .findFirst()
                .orElseThrow(() -> shouldNotHappenException(new IllegalStateException("Could not find lambda method " + implMethodName + " in " + classModel.thisClass().asInternalName())));
        lambdaMethod.code().ifPresent(codeModel -> codeModel.forEach(this::visit));
    }

    private void visit(CodeElement codeElement) {
        if (!(codeElement instanceof Instruction instruction)) return;
        int opcode = genericOpcode(instruction.opcode());

        switch (codeElement) {
            case ConstantInstruction.IntrinsicConstantInstruction ignored -> {
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

    // ASM normalises the wide and one-byte forms before the visitor sees them (ldc_w/ldc2_w -> ldc,
    // iload_0 .. astore_3 -> iload .. astore); java.lang.classfile keeps them, so map them back
    private static int genericOpcode(Opcode opcode) {
        int bytecode = opcode.bytecode();
        if (bytecode >= 26 && bytecode <= 45) return 21 + (bytecode - 26) / 4;
        if (bytecode >= 59 && bytecode <= 78) return 54 + (bytecode - 59) / 4;
        if (bytecode == 19 || bytecode == 20) return 18;
        return bytecode;
    }

    private static boolean isLambda(SerializedLambda serializedLambda) {
        return serializedLambda.getImplMethodName().startsWith("lambda$")
                || serializedLambda.getImplMethodName().contains("$lambda-")
                || serializedLambda.getImplMethodName().contains("$lambda$");
    }

    private static boolean isKotlinMethodReference(SerializedLambda serializedLambda) {
        return serializedLambda.getCapturedArgCount() == 1
                && stream(serializedLambda.getCapturedArg(0).getClass().getAnnotations())
                .anyMatch(annotation -> "kotlin.Metadata".equals(annotation.annotationType().getName()));
    }
}
