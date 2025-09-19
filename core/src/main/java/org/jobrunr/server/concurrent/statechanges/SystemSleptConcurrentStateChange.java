package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.server.DesktopUtils.hasSystemSleptRecently;
import static org.jobrunr.server.DesktopUtils.systemSupportsSleepDetection;

public class SystemSleptConcurrentStateChange implements AllowedConcurrentStateChange {

    private static final Logger LOG = LoggerFactory.getLogger(SystemSleptConcurrentStateChange.class);

    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        return systemSupportsSleepDetection() && hasSystemSleptRecently();
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        if (systemSupportsSleepDetection() && hasSystemSleptRecently()) {
            LOG.warn("Jobs were updated concurrently but JobRunr has detected that the system has slept recently (e.g. a laptop where the lid is closed). It will disregard this ConcurrentJobModificationException and use the job from the StorageProvider.");
            return ConcurrentJobModificationResolveResult.succeeded(storageProviderJob);
        }
        throw shouldNotHappenException("Should not happen as matches filter should be filtering out this StateChangeFilter");
    }
}
