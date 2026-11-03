package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.details.lambdas.SerializedJobLambda;

import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class SerializedLambdaClassFileApiJobDetailsFinder extends AbstractClassFileApiJobDetailsFinder {

    private final SerializedJobLambda lambda;

    public SerializedLambdaClassFileApiJobDetailsFinder(SerializedJobLambda lambda, Object... params) {
        super(lambda.createJobDetailsBuilder(params));
        this.lambda = lambda;
        if (lambda.isLambdaBody()) {
            visitLambdaMethod(parseClass(lambda.getClassContainingLambdaAsInputStream()));
        }
    }

    private void visitLambdaMethod(ClassModel classModel) {
        MethodModel lambdaMethod = classModel.methods().stream()
                .filter(method -> lambda.matches(method.methodName().stringValue()))
                .findFirst()
                .orElseThrow(() -> shouldNotHappenException(new IllegalStateException("Could not find the lambda method in " + classModel.thisClass().asInternalName())));
        lambdaMethod.code().ifPresent(this::visit);
    }
}
