package org.jobrunr.micronaut;


import io.micronaut.context.ApplicationContext;
import org.jobrunr.JobRunrAssertions;

public class MicronautAssertions extends JobRunrAssertions {

    public static ApplicationContextAssertions assertThat(ApplicationContext applicationContext) {
        return ApplicationContextAssertions.assertThat(applicationContext);
    }

}
