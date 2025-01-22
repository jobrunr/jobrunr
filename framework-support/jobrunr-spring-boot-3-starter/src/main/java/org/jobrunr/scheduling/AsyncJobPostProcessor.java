package org.jobrunr.scheduling;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

@Component
public class AsyncJobPostProcessor implements BeanPostProcessor, BeanFactoryAware {
    BeanFactory beanFactory;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean.getClass().isAnnotationPresent(AsyncJob.class)) {
            try {
                return applyJobEnhancement(bean);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return bean;
    }

    public Object applyJobEnhancement(Object bean) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        JobScheduler jobScheduler = beanFactory.getBean(JobScheduler.class);
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true); // To proxy classes, not just interfaces
        proxyFactory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
            if (invocation.getMethod().isAnnotationPresent(Job.class)) {
                return new JobInterceptor(jobScheduler).intercept(invocation);
            }
            return invocation.proceed();
        });

        return proxyFactory.getProxy();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}


