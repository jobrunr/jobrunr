package org.jobrunr.jobs.lambdas;

import java.io.Serializable;

public interface JobWithIoc extends Serializable {
    // marker interface to make it serializable and to say that the job needs to be run with an IoC container
}
