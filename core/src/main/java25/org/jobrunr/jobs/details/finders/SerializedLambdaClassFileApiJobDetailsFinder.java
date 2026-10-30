package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.details.lambdas.SerializedJobLambda;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class SerializedLambdaClassFileApiJobDetailsFinder extends AbstractClassFileApiJobDetailsFinder {

    private final SerializedJobLambda lambda;

    public SerializedLambdaClassFileApiJobDetailsFinder(SerializedJobLambda lambda, Object... params) {
        super(lambda.createJobDetailsBuilder(params));
        this.lambda = lambda;
        if (lambda.isLambdaBody()) {
            try (InputStream classContainingLambdaInputStream = lambda.getClassContainingLambdaAsInputStream()) {
                parse(classContainingLambdaInputStream.readAllBytes());
            } catch (IOException e) {
                throw shouldNotHappenException(e);
            }
        }
    }

    void parse(byte[] classBytes) {
        ClassModel classModel = ClassFile.of().parse(classBytes);
        MethodModel lambdaMethod = classModel.methods().stream()
                .filter(method -> lambda.matches(method.methodName().stringValue()))
                .findFirst()
                .orElseThrow(() -> shouldNotHappenException(new IllegalStateException("Could not find the lambda method in " + classModel.thisClass().asInternalName())));
        lambdaMethod.code().ifPresent(this::visit);
    }
}
