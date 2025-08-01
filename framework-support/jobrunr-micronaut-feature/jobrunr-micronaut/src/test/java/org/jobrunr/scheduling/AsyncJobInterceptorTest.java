package org.jobrunr.scheduling;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.server.runner.MockJobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Documented;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncJobInterceptorTest {

    private AsyncJobInterceptor interceptor;
    @Mock
    private JobScheduler jobScheduler;
    @Captor
    ArgumentCaptor<JobDetails> jobDetailsArgumentCaptor;

    @BeforeEach
    void setUp() {
        interceptor = new AsyncJobInterceptor();
        Whitebox.setInternalState(interceptor, "jobScheduler", jobScheduler);
    }

    @Test
    void interceptThrowsIllegalArgumentIfMethodIsNotVoid() {
        var contextMock = invocationContextMockFor("someMethodThatIsNotVoid", Job.class);

        assertThatCode(() -> interceptor.intercept(contextMock)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void interceptDoesNothingIfMethodIsNotAnnotatedWithJob() {
        interceptor.intercept(invocationContextMockFor("someMethodThatIsNotAnnotatedWithJob", Documented.class));
        verify(jobScheduler, times(0)).enqueue(Mockito.isNull(), Mockito.any(JobDetails.class));
    }

    @Test
    void interceptEnqueuesJobDetailsBasedOnMethod() {
        interceptor.intercept(invocationContextMockFor("someMethodWithJobAnnotation", Job.class, "arg1", "arg2"));

        verify(jobScheduler, times(1)).enqueue(Mockito.isNull(), jobDetailsArgumentCaptor.capture());

        assertThat(jobDetailsArgumentCaptor.getValue().getMethodName()).isEqualTo("someMethodWithJobAnnotation");
        assertThat(jobDetailsArgumentCaptor.getValue().getJobParameters().get(0).getObject()).isEqualTo("arg1");
        assertThat(jobDetailsArgumentCaptor.getValue().getJobParameters().get(1).getObject()).isEqualTo("arg2");
    }

    @Test
    void interceptDoesNothingIfJobIsAlreadyRunning() {
        MockJobContext.setUpJobContext(new JobContextMock("org.jobrunr.scheduling.AsyncJobInterceptorTest.someMethodWithJobAnnotation(java.lang.String,java.lang.String)"));
        interceptor.intercept(invocationContextMockFor("someMethodWithJobAnnotation", Job.class, "arg1", "arg2"));

        verify(jobScheduler, times(0)).enqueue(Mockito.isNull(), jobDetailsArgumentCaptor.capture());
    }

    private MethodInvocationContext<Object, Object> invocationContextMockFor(String methodName, Class annotationClass, Object... params) {
        var context = Mockito.mock(MethodInvocationContext.class);
        var method = Mockito.mock(ExecutableMethod.class);
        var returnTypeMock = Mockito.mock(ReturnType.class);

        try {
            lenient().when(returnTypeMock.getType()).thenReturn(this.getClass().getDeclaredMethod(methodName).getReturnType());
            lenient().when(method.getName()).thenReturn(methodName);
            lenient().when(method.isAnnotationPresent(Job.class)).thenReturn(annotationClass.equals(Job.class));
            lenient().when(method.getReturnType()).thenReturn(returnTypeMock);
            lenient().when(context.getExecutableMethod()).thenReturn(method);
            lenient().when(context.getParameterValues()).thenReturn(params);
            lenient().when(context.getDeclaringType()).thenReturn(this.getClass());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }

        return context;
    }

    private class JobContextMock extends JobContext {
        private final String jobSignature;

        public JobContextMock(String signature) {
            this.jobSignature = signature;
        }

        @Override
        public String getJobSignature() {
            return jobSignature;
        }
    }

    private void someMethodThatIsNotAnnotatedWithJob() {
    }

    @Job
    private void someMethodWithJobAnnotation() {

    }

    @Job
    private int someMethodThatIsNotVoid() {
        return 0;
    }
}