package org.jobrunr.scheduling;

import org.jobrunr.jobs.annotations.AsyncJob;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

@Component
public class AsyncJobPostProcessor implements BeanPostProcessor, BeanFactoryAware {
    private BeanFactory beanFactory;
    private JobInterceptor jobInterceptor;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (AnnotatedElementUtils.hasAnnotation(bean.getClass(), AsyncJob.class)) {
            return applyJobEnhancement(bean);
        }
        return bean;
    }

    public Object applyJobEnhancement(Object bean) {
        if (jobInterceptor == null) {
            JobScheduler jobScheduler = beanFactory.getBean(JobScheduler.class);
            jobInterceptor = new JobInterceptor(jobScheduler);
        }
        ProxyFactory proxyFactory = new ProxyFactory(bean);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(jobInterceptor);
        return proxyFactory.getProxy();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

}


