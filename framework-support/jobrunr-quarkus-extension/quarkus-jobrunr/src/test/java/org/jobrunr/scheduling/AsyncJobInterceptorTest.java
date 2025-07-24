package org.jobrunr.scheduling;

import jakarta.interceptor.InvocationContext;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        assertThatCode(() -> interceptor.intercept(invocationContextMockFor("someMethodThatIsNotVoid"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void interceptDoesNothingIfMethodIsNotAnnotatedWithJob() throws Exception {
        interceptor.intercept(invocationContextMockFor("someMethodThatIsNotAnnotatedWithJob"));
        verify(jobScheduler, times(0)).enqueue(Mockito.isNull(), Mockito.any(JobDetails.class));
    }

    @Test
    void interceptEnqueuesJobDetailsBasedOnMethod() throws Exception {
        interceptor.intercept(invocationContextMockFor("someMethodWithJobAnnotation", "arg1", "arg2"));

        verify(jobScheduler, times(1)).enqueue(Mockito.isNull(), jobDetailsArgumentCaptor.capture());

        assertThat(jobDetailsArgumentCaptor.getValue().getMethodName()).isEqualTo("someMethodWithJobAnnotation");
        assertThat(jobDetailsArgumentCaptor.getValue().getJobParameters().get(0).getObject()).isEqualTo("arg1");
        assertThat(jobDetailsArgumentCaptor.getValue().getJobParameters().get(1).getObject()).isEqualTo("arg2");
    }

    @Test
    void interceptDoesNothingIfJobIsAlreadyRunning() throws Exception {
        MockJobContext.setUpJobContext(new JobContextMock("org.jobrunr.scheduling.AsyncJobInterceptorTest.someMethodWithJobAnnotation(java.lang.String,java.lang.String)"));
        interceptor.intercept(invocationContextMockFor("someMethodWithJobAnnotation", "arg1", "arg2"));

        verify(jobScheduler, times(0)).enqueue(Mockito.isNull(), jobDetailsArgumentCaptor.capture());
    }

    private InvocationContext invocationContextMockFor(String methodName, Object... params) throws NoSuchMethodException {
        var context = Mockito.mock(InvocationContext.class);
        when(context.getMethod()).thenReturn(getClass().getDeclaredMethod(methodName));
        when(context.getParameters()).thenReturn(params);
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