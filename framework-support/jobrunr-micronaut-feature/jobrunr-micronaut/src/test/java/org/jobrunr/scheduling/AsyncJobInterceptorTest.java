package org.jobrunr.scheduling;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.server.runner.MockThreadLocalJobContext;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
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
    void interceptProceedsNormallyIfMethodIsNotAnnotatedWithJob() throws Exception {
        var invocationContext = invocationContextMockFor("someMethodThatIsNotAnnotatedWithJob");

        interceptor.intercept(invocationContext);

        verify(invocationContext, times(1)).proceed();
        verify(jobScheduler, times(0)).enqueue(Mockito.isNull(), Mockito.any(JobDetails.class));
    }

    @Test
    void interceptEnqueuesJobDetailsBasedOnMethod() throws Exception {
        var invocationContext = invocationContextMockFor("someMethodWithJobAnnotation", "arg1", "arg2");
        interceptor.intercept(invocationContext);

        verify(invocationContext, times(0)).proceed();
        verify(jobScheduler, times(1)).enqueue(Mockito.isNull(), jobDetailsArgumentCaptor.capture());

        assertThat(jobDetailsArgumentCaptor.getValue().getMethodName()).isEqualTo("someMethodWithJobAnnotation");
        assertThat(jobDetailsArgumentCaptor.getValue().getJobParameters().get(0).getObject()).isEqualTo("arg1");
        assertThat(jobDetailsArgumentCaptor.getValue().getJobParameters().get(1).getObject()).isEqualTo("arg2");
    }

    @Test
    void interceptProceedsNormallyIfJobIsAlreadyRunning() throws Exception {
        try (var ignored = new MockThreadLocalJobContext()) {
            var invocationContext = invocationContextMockFor("someMethodWithJobAnnotation", "arg1", "arg2");
            MockThreadLocalJobContext.setUpJobContext(new JobContextMock("org.jobrunr.scheduling.AsyncJobInterceptorTest.someMethodWithJobAnnotation(java.lang.String, java.lang.String)"));
            interceptor.intercept(invocationContext);

            verify(invocationContext, times(1)).proceed();
            verify(jobScheduler, times(0)).enqueue(Mockito.isNull(), jobDetailsArgumentCaptor.capture());
        }
    }

    private MethodInvocationContext<Object, Object> invocationContextMockFor(String methodName, Object... params) throws NoSuchMethodException {
        var context = Mockito.mock(MethodInvocationContext.class);
        var method = Mockito.mock(ExecutableMethod.class);
        var returnTypeMock = Mockito.mock(ReturnType.class);

        var declaredMethod = this.getClass().getDeclaredMethod(methodName);
        lenient().when(method.getName()).thenReturn(declaredMethod.getName());
        lenient().when(method.isAnnotationPresent(Job.class)).thenReturn(declaredMethod.isAnnotationPresent(Job.class));

        lenient().when(method.getReturnType()).thenReturn(returnTypeMock);
        lenient().when(returnTypeMock.getType()).thenReturn(declaredMethod.getReturnType());

        lenient().when(context.getExecutableMethod()).thenReturn(method);
        lenient().when(context.getParameterValues()).thenReturn(params);
        lenient().when(context.getDeclaringType()).thenReturn(this.getClass());

        return ReflectionUtils.cast(context);
    }

    private static class JobContextMock extends JobContext {
        private final String jobSignature;

        JobContextMock(String signature) {
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