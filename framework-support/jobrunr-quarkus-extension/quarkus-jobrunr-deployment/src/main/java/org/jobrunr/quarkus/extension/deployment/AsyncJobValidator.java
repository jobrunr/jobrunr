package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.VoidType;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;

public class AsyncJobValidator {

    public static void validate(CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        for (AnnotationInstance asyncJobAnnotation : index.getAnnotations(DotName.createSimple(AsyncJob.class.getName()))) {
            if (asyncJobAnnotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo classInfo = asyncJobAnnotation.target().asClass();
                validateAsyncJobMethods(classInfo);
            }
        }
    }

    private static void validateAsyncJobMethods(ClassInfo classInfo) {
        DotName jobAnnotation = DotName.createSimple(Job.class.getName());

        for (MethodInfo method : classInfo.methods()) {
            if (method.hasAnnotation(jobAnnotation) && !hasVoidAsReturnType(method.returnType())) {
                throw new IllegalAsyncJobAnnotationException("An @AsyncJob cannot have a return value. " + classInfo.name() + "@" + method.name() + " is defined as an @AsyncJob but has a return value.");
            }
        }
    }

    private static boolean hasVoidAsReturnType(Type type) {
        return type.equals(VoidType.VOID);
    }

    public static class IllegalAsyncJobAnnotationException extends RuntimeException {
        public IllegalAsyncJobAnnotationException(String message) {
            super(message);
        }
    }

}
