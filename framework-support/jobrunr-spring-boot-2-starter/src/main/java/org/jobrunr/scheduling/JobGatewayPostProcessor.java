package org.jobrunr.scheduling;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.JobGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;


public class JobGatewayPostProcessor implements BeanPostProcessor, BeanFactoryAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobGatewayPostProcessor.class);
    private BeanFactory beanFactory;
    private JobGatewayPostProcessor.JobGatewayMethodCallback jobGatewayMethodCallback;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if(bean.getClass().isAnnotationPresent(JobGateway.class)) {
            ReflectionUtils.doWithMethods(bean.getClass(), jobGatewayMethodCallback, method -> method.isAnnotationPresent(Job.class));
        }
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        this.jobGatewayMethodCallback = new JobGatewayPostProcessor.JobGatewayMethodCallback(beanFactory);
    }

    private static class JobGatewayMethodCallback implements ReflectionUtils.MethodCallback {

        private final BeanFactory beanFactory;

        public JobGatewayMethodCallback(BeanFactory beanFactory) {
            this.beanFactory = beanFactory;
        }

        @Override
        public void doWith(Method method) throws IllegalArgumentException {
            LOGGER.info("Scheduled job for: {}", method.getName());
            JobScheduler jobScheduler = beanFactory.getBean(JobScheduler.class);
            String declaringClassName = method.getDeclaringClass().getName();
            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();
            jobScheduler.enqueue(() -> {
                try {
                    // Load the declaring class
                    Class<?> declaringClass = Class.forName(declaringClassName);
                    Method resolvedMethod = declaringClass.getDeclaredMethod(methodName, parameterTypes);

                    if (java.lang.reflect.Modifier.isStatic(resolvedMethod.getModifiers())) {
                        // Directly invoke static methods
                        ReflectionUtils.invokeMethod(resolvedMethod, null);
                    } else {
                        // Create a new instance if the method is not static
                        Object instance = declaringClass.getDeclaredConstructor().newInstance();
                        ReflectionUtils.invokeMethod(resolvedMethod, instance);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to execute job for method {} in class {}: {}", methodName, declaringClassName, e.getMessage(), e);
                }
            });

        }

    }

}

