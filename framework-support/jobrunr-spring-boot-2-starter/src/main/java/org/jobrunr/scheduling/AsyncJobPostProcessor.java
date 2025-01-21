package org.jobrunr.scheduling;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.AsyncJob;
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
        Class<?> dynamicType = new ByteBuddy()
                .subclass(bean.getClass())
                .method(ElementMatchers.isAnnotatedWith(Job.class))
                .intercept(MethodDelegation.to(new JobInterceptor(jobScheduler)))
                .make()
                .load(bean.getClass().getClassLoader())
                .getLoaded();
        return dynamicType.getConstructor().newInstance();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}
