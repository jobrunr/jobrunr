package org.jobrunr.spring.autoconfigure;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.details.postprocess.CGLibPostProcessor;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;

import static java.util.Collections.emptyList;
import static org.jobrunr.JobRunrAssertions.assertThat;

public class CGLibJobDetailsPostProcessorTest {

    private CGLibPostProcessor cgLibPostProcessor = new CGLibPostProcessor();

    @Test
    void testCGLibName() {
        TestService proxy = getTestServiceAsCGLibProxy();

        JobDetails jobDetails = new JobDetails(proxy.getClass().getName(), null, "doWork", emptyList());
        JobDetails actualJobDetails = cgLibPostProcessor.postProcess(jobDetails);

        assertThat(actualJobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasNoArgs();
    }

    private static TestService getTestServiceAsCGLibProxy() {
        Enhancer enhancer = new Enhancer();
        enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
        enhancer.setSuperclass(TestService.class);
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            if (method.getDeclaringClass() != Object.class && method.getReturnType() == String.class) {
                return "Hello from CGLib!";
            } else {
                return proxy.invokeSuper(obj, args);
            }
        });

        TestService proxy = (TestService) enhancer.create();
        return proxy;
    }
}
