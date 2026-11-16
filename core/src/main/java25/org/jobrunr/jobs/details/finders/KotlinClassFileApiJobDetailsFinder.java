package org.jobrunr.jobs.details.finders;

import org.jobrunr.jobs.details.KotlinJobDetailsBuilder;
import org.jobrunr.jobs.details.lambdas.KotlinClassBasedLambda;
import org.jobrunr.jobs.lambdas.JobRunrJob;

import java.lang.classfile.Attributes;
import java.lang.classfile.ClassModel;
import java.lang.classfile.MethodModel;
import java.lang.reflect.Field;
import java.util.Optional;


public class KotlinClassFileApiJobDetailsFinder extends AbstractClassFileApiJobDetailsFinder {

    private final JobRunrJob jobRunrJob;
    private final KotlinClassBasedLambda kotlinLambda;

    public KotlinClassFileApiJobDetailsFinder(JobRunrJob jobRunrJob, Object... params) {
        super(new KotlinJobDetailsBuilder(jobRunrJob, params));
        this.jobRunrJob = jobRunrJob;
        this.kotlinLambda = new KotlinClassBasedLambda(jobRunrJob, jobDetailsBuilder);
        Optional<Field> kotlinFunction = kotlinLambda.getKotlinFunction();
        if (kotlinFunction.isPresent()) {
            kotlinLambda.setJobDetailsFromKotlinFunction(kotlinFunction.get());
        } else {
            parseKotlinLambdaClass();
        }
    }

    private void parseKotlinLambdaClass() {
        ClassModel classModel = parseClass(kotlinLambda.getClassContainingLambdaAsInputStream());
        visitMethods(classModel);
        parseNestedClassIfItIsAMethodReference(classModel);
    }

    private void parseNestedClassIfItIsAMethodReference(ClassModel classModel) {
        Optional<String> nestedClassWithMethodReference = classModel.findAttribute(Attributes.innerClasses()).stream()
                .flatMap(innerClasses -> innerClasses.classes().stream())
                .filter(innerClass -> KotlinClassBasedLambda.isMethodReferenceNestedClassCandidate(innerClass.flagsMask()))
                .map(innerClass -> innerClass.innerClass().asInternalName())
                .reduce((_, last) -> last);

        if (nestedClassWithMethodReference.isPresent() && kotlinLambda.isMethodReferenceNestedClass(nestedClassWithMethodReference.get())) {
            String location = "/" + nestedClassWithMethodReference.get() + ".class";
            ClassModel nestedClassModel = parseClass(jobRunrJob.getClass().getResourceAsStream(location));
            visitMethods(nestedClassModel);
            kotlinLambda.trimToSingleInstruction();
        }
    }

    private void visitMethods(ClassModel classModel) {
        for (MethodModel method : classModel.methods()) {
            if (kotlinLambda.matches(method.methodName().stringValue())) {
                method.code().ifPresent(this::visit);
            }
        }
    }

}
