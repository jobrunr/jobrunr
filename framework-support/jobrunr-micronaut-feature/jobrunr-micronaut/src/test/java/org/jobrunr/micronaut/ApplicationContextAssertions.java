package org.jobrunr.micronaut;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.NoSuchBeanException;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class ApplicationContextAssertions extends AbstractAssert<ApplicationContextAssertions, ApplicationContext> {

    protected ApplicationContextAssertions(ApplicationContext applicationContext) {
        super(applicationContext, ApplicationContextAssertions.class);
    }

    public static ApplicationContextAssertions assertThat(ApplicationContext applicationContext) {
        return new ApplicationContextAssertions(applicationContext);
    }

    public ApplicationContextAssertions hasSingleBean(Class<?> type) {
        Assertions.assertThat(actual.getBean(type)).isNotNull();
        return this;
    }

    public ApplicationContextAssertions doesNotHaveBean(Class<?> type) {
        Assertions.assertThatThrownBy(() -> actual.getBean(type)).isInstanceOf(NoSuchBeanException.class);
        return this;
    }
}
