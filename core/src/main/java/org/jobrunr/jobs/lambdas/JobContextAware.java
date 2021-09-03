package org.jobrunr.jobs.lambdas;

import org.jobrunr.jobs.context.JobContext;

/**
 * Can be used on {@link JobRequestHandler} classes to inject the JobContext before the actual run method is called
 */
public interface JobContextAware {

    void setJobContext(JobContext jobContext);
}
