package org.jobrunr.scheduling;

import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

public class AsyncJobPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private final AsyncJobMethodValidator asyncJobMethodValidator;
    private JobInterceptor jobInterceptor;

    public AsyncJobPostProcessor() {
        this.asyncJobMethodValidator = new AsyncJobMethodValidator();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (AnnotatedElementUtils.hasAnnotation(bean.getClass(), AsyncJob.class)) {
            ReflectionUtils.doWithMethods(bean.getClass(), asyncJobMethodValidator);
            return applyJobEnhancement(bean);
        }
        return bean;
    }

    public Object applyJobEnhancement(Object bean) {
        if (jobInterceptor == null) {
            throw new IllegalStateException("Bean Factory was not set and JobInterceptor could not be created");
        }
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(jobInterceptor);
        return proxyFactory.getProxy();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.jobInterceptor = new JobInterceptor(beanFactory);
    }

    private static class AsyncJobMethodValidator implements ReflectionUtils.MethodCallback {

        @Override
        public void doWith(Method method) throws IllegalArgumentException {
            if (isJobRunrJobWithAReturnValue(method)) {
                throw new IllegalArgumentException("An @AsyncJob cannot have a return value. " + method + " is defined as an @AsyncJob but has a return value.");
            }
        }

        private static boolean isJobRunrJobWithAReturnValue(Method method) {
            return method.isAnnotationPresent(Job.class) && !method.getReturnType().equals(Void.TYPE);
        }
    }
}