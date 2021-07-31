package org.jobrunr.server.jmx;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.JobStatsChangeListener;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.time.Instant;

public class JobRunrJMXExtensions implements JobStatsChangeListener, JobStatsMBean {

    private JobStats jobStats;

    public JobRunrJMXExtensions(BackgroundJobServer backgroundJobServer, StorageProvider storageProvider) {
        try {
            onChange(storageProvider.getJobStats());
            storageProvider.addJobStorageOnChangeListener(this);

            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            StandardMBean backgroundJobServerMBean = new StandardMBean(backgroundJobServer, BackgroundJobServerMBean.class);
            StandardMBean backgroundJobServerStatusMBean = new StandardMBean(backgroundJobServer.getServerStatus(), BackgroundJobServerStatusMBean.class);
            StandardMBean backgroundJobServerStatsMBean = new StandardMBean(this, JobStatsMBean.class);
            server.registerMBean(backgroundJobServerMBean, new ObjectName("org.jobrunr.server:type=BackgroundJobServerInfo,name=BackgroundJobServer"));
            server.registerMBean(backgroundJobServerStatusMBean, new ObjectName("org.jobrunr.server:type=BackgroundJobServerInfo,name=BackgroundJobServerStatus"));
            server.registerMBean(backgroundJobServerStatsMBean, new ObjectName("org.jobrunr.server:type=BackgroundJobServerInfo,name=BackgroundJobServerStatistics"));
        } catch (JMException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onChange(JobStats jobStats) {
        this.jobStats = jobStats;
    }

    @Override
    public Instant getTimeStamp() {
        return jobStats.getTimeStamp();
    }

    @Override
    public Long getTotal() {
        return jobStats.getTotal();
    }

    @Override
    public Long getScheduled() {
        return jobStats.getScheduled();
    }

    @Override
    public Long getEnqueued() {
        return jobStats.getEnqueued();
    }

    @Override
    public Long getProcessing() {
        return jobStats.getProcessing();
    }

    @Override
    public Long getFailed() {
        return jobStats.getFailed();
    }

    @Override
    public Long getSucceeded() {
        return jobStats.getSucceeded();
    }

    @Override
    public int getRecurringJobs() {
        return jobStats.getRecurringJobs();
    }

    @Override
    public int getBackgroundJobServers() {
        return jobStats.getBackgroundJobServers();
    }
}
