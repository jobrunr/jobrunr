package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.VoidType;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.AsyncJobValidationRecorder;

public class AsyncJobPostProcessor {

    private final RecorderContext recorderContext;
    private final CombinedIndexBuildItem index;
    private final AsyncJobValidationRecorder recorder;

    public AsyncJobPostProcessor(RecorderContext recorderContext, CombinedIndexBuildItem index, AsyncJobValidationRecorder recorder) {
        this.recorderContext = recorderContext;
        this.index = index;
        this.recorder = recorder;
    }

    public void validate() {
        for (AnnotationInstance jobAnnotation : index.getIndex().getAnnotations(DotName.createSimple(Job.class.getName()))) {
            AnnotationTarget annotationTarget = jobAnnotation.target();
            if (AnnotationTarget.Kind.METHOD.equals(annotationTarget.kind())) {
                final MethodInfo methodInfo = (MethodInfo) jobAnnotation.target();
                final ClassInfo declaringClass = methodInfo.declaringClass();
                final Type returnType = methodInfo.returnType();

                recorder.validate(hasAsyncJobAnnotationOnClass(declaringClass), hasVoidAsReturnType(returnType), declaringClass.name() + "@" + methodInfo.name());
            }
        }
    }

    private static boolean hasVoidAsReturnType(Type type) {
        return type.equals(VoidType.VOID);
    }

    private static boolean hasAsyncJobAnnotationOnClass(ClassInfo declaringClass) {
        return declaringClass.annotations(DotName.createSimple(AsyncJob.class.getName())).size() > 0;
    }

}
