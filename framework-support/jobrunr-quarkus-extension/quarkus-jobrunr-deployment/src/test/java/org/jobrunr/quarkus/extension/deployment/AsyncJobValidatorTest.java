package org.jobrunr.quarkus.extension.deployment;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jobrunr.jobs.annotations.Job;
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
class AsyncJobValidatorTest {
    @Mock
    CombinedIndexBuildItem combinedIndexBuildItem;
    @Mock
    IndexView indexView;

    @BeforeEach
    public void setup() {
        when(combinedIndexBuildItem.getIndex()).thenReturn(indexView);
    }

    @Test
    public void validateShouldNotThrowExceptionForMethodAnnotations() {
        AnnotationTarget nonMethodTarget = mock(AnnotationTarget.class);
        when(nonMethodTarget.kind()).thenReturn(AnnotationTarget.Kind.METHOD);

        AnnotationInstance annotationInstance = mock(AnnotationInstance.class);
        when(annotationInstance.target()).thenReturn(nonMethodTarget);

        when(indexView.getAnnotations((DotName) any())).thenReturn(List.of(annotationInstance));
        assertDoesNotThrow(() -> AsyncJobValidator.validate(combinedIndexBuildItem));
    }

    @Test
    public void validateShouldNotThrowExceptionWhenAsyncJobMethodReturnTypeIsVoid() {
        AnnotationTarget classAnnotationTarget = mock(AnnotationTarget.class);
        when(classAnnotationTarget.kind()).thenReturn(AnnotationTarget.Kind.CLASS);

        ClassInfo classInfo = mock(ClassInfo.class);
        when(classAnnotationTarget.asClass()).thenReturn(classInfo);

        AnnotationInstance annotationInstance = mock(AnnotationInstance.class);
        when(annotationInstance.target()).thenReturn(classAnnotationTarget);

        MethodInfo methodInfo = mock(MethodInfo.class);
        when(methodInfo.hasAnnotation(DotName.createSimple(Job.class.getName()))).thenReturn(true);
        when(methodInfo.returnType()).thenReturn(Type.create(DotName.createSimple("void"), Type.Kind.VOID));

        when(classInfo.methods()).thenReturn(List.of(methodInfo));

        when(indexView.getAnnotations(any(DotName.class))).thenReturn(List.of(annotationInstance));
        assertDoesNotThrow(() -> AsyncJobValidator.validate(combinedIndexBuildItem));
    }

    @Test
    public void validateShouldThrowExceptionForNonVoidReturningAsyncJobMethod() {
        AnnotationTarget classAnnotationTarget = mock(AnnotationTarget.class);
        when(classAnnotationTarget.kind()).thenReturn(AnnotationTarget.Kind.CLASS);

        ClassInfo classInfo = mock(ClassInfo.class);
        when(classInfo.name()).thenReturn(DotName.createSimple("className"));
        when(classAnnotationTarget.asClass()).thenReturn(classInfo);

        AnnotationInstance annotationInstance = mock(AnnotationInstance.class);
        when(annotationInstance.target()).thenReturn(classAnnotationTarget);

        MethodInfo methodInfo = mock(MethodInfo.class);
        when(methodInfo.hasAnnotation(DotName.createSimple(Job.class.getName()))).thenReturn(true);
        when(methodInfo.returnType()).thenReturn(Type.create(DotName.createSimple("java.lang.String"), Type.Kind.CLASS));
        when(methodInfo.name()).thenReturn("myLittleAsyncJobName");

        when(classInfo.methods()).thenReturn(List.of(methodInfo));

        when(indexView.getAnnotations(any(DotName.class))).thenReturn(List.of(annotationInstance));
        assertThatCode(() -> AsyncJobValidator.validate(combinedIndexBuildItem))
                .isInstanceOf(AsyncJobValidator.IllegalAsyncJobAnnotationException.class)
                .hasMessage("An @AsyncJob cannot have a return value. className@myLittleAsyncJobName is defined as an @AsyncJob but has a return value.");
    }

}