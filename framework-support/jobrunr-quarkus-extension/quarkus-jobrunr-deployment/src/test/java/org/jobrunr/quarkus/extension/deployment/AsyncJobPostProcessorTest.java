package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncJobPostProcessorTest {
    @Mock
    RecorderContext recorderContext;
    @Mock
    CombinedIndexBuildItem combinedIndexBuildItem;
    @Mock
    BeanContainerBuildItem beanContainerBuildItem;
    @Mock
    IndexView indexView;

    AsyncJobPostProcessor postProcessor;

    @BeforeEach
    public void setup() {
        when(combinedIndexBuildItem.getIndex()).thenReturn(indexView);
        this.postProcessor = new AsyncJobPostProcessor(recorderContext, combinedIndexBuildItem, beanContainerBuildItem, null);
    }

    @Test
    public void validateShouldNotThrowExForClassAnnotations() {
        AnnotationTarget nonMethodTarget = mock(AnnotationTarget.class);
        when(nonMethodTarget.kind()).thenReturn(AnnotationTarget.Kind.CLASS);

        AnnotationInstance annotationInstance = mock(AnnotationInstance.class);
        when(annotationInstance.target()).thenReturn(nonMethodTarget);

        when(indexView.getAnnotations((DotName) any())).thenReturn(List.of(annotationInstance));
        assertDoesNotThrow(() -> postProcessor.validate());
    }

    @Test
    public void validateShouldNotThrowExWhenAsyncJobMethodReturnTypeIsVoid() {
        MethodInfo methodInfo = mock(MethodInfo.class);
        when(methodInfo.kind()).thenReturn(MethodInfo.Kind.METHOD);
        when(methodInfo.returnType()).thenReturn(Type.create(DotName.createSimple("void"), Type.Kind.VOID));

        ClassInfo declaringClass = mock(ClassInfo.class);
        when(declaringClass.annotations(DotName.createSimple(AsyncJob.class.getName()))).thenReturn(List.of(mock(AnnotationInstance.class)));
        when(methodInfo.declaringClass()).thenReturn(declaringClass);

        AnnotationInstance annotationInstance = mock(AnnotationInstance.class);
        when(annotationInstance.target()).thenReturn(methodInfo);

        when(indexView.getAnnotations(any(DotName.class))).thenReturn(List.of(annotationInstance));
        assertDoesNotThrow(() -> postProcessor.validate());
    }

    @Test
    public void validate_shouldThrow_forNonVoidReturningAsyncJobMethod() {
        MethodInfo methodInfo = mock(MethodInfo.class);
        when(methodInfo.kind()).thenReturn(MethodInfo.Kind.METHOD);
        when(methodInfo.returnType()).thenReturn(Type.create(DotName.createSimple("java.lang.String"), Type.Kind.CLASS));
        when(methodInfo.name()).thenReturn("myLittleAsyncJobName");

        ClassInfo declaringClass = mock(ClassInfo.class);
        when(declaringClass.name()).thenReturn(DotName.createSimple("className"));
        when(declaringClass.annotations(DotName.createSimple(AsyncJob.class.getName()))).thenReturn(List.of(mock(AnnotationInstance.class)));
        when(methodInfo.declaringClass()).thenReturn(declaringClass);

        AnnotationInstance annotationInstance = mock(AnnotationInstance.class);
        when(annotationInstance.target()).thenReturn(methodInfo);

        when(indexView.getAnnotations(any(DotName.class))).thenReturn(List.of(annotationInstance));

        assertThatCode(() -> postProcessor.validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An @AsyncJob cannot have a return value. className@myLittleAsyncJobName is defined as an @AsyncJob but has a return value.");
    }

}