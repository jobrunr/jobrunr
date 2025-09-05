package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.VoidType;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.AsyncJobValidationRecorder;

public class AsyncJobPostProcessor {

    private final RecorderContext recorderContext;
    private final CombinedIndexBuildItem index;
    private final BeanContainerBuildItem beanContainer;
    private final AsyncJobValidationRecorder recorder;

    public AsyncJobPostProcessor(RecorderContext recorderContext, CombinedIndexBuildItem index, BeanContainerBuildItem beanContainer, AsyncJobValidationRecorder recorder) {
        this.recorderContext = recorderContext;
        this.index = index;
        this.beanContainer = beanContainer;
        this.recorder = recorder;
    }

    public void validate() {
        for (AnnotationInstance jobAnnotation : index.getIndex().getAnnotations(DotName.createSimple(Job.class.getName()))) {
            if (isNotAnnotatedOnMethod(jobAnnotation.target())) continue;

            MethodInfo annotationTarget = (MethodInfo) jobAnnotation.target();
            ClassInfo declaringClass = annotationTarget.declaringClass();

            if (isAsyncJobClass(declaringClass) && !annotationTarget.returnType().equals(VoidType.VOID)) {
                throw new IllegalArgumentException("An @AsyncJob cannot have a return value. " + declaringClass.name() + "@" + annotationTarget.name() + " is defined as an @AsyncJob but has a return value.");
            }
        }
    }

    private static boolean isNotAnnotatedOnMethod(AnnotationTarget annotationTarget) {
        return !AnnotationTarget.Kind.METHOD.equals(annotationTarget.kind());
    }

    private static boolean isAsyncJobClass(ClassInfo declaringClass) {
        return declaringClass.annotations(DotName.createSimple(AsyncJob.class.getName())).size() > 0;
    }

}
