package org.jobrunr.spring.autoconfigure;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.Optional;

public class JobRunrStarter implements SmartInitializingSingleton, DisposableBean {

    private final Optional<BackgroundJobServer> backgroundJobServer;
    private final Optional<JobRunrDashboardWebServer> webServer;

    public JobRunrStarter(Optional<BackgroundJobServer> backgroundJobServer, Optional<JobRunrDashboardWebServer> webServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.webServer = webServer;
    }

    @Override
    public void afterSingletonsInstantiated() {
        backgroundJobServer.ifPresent(BackgroundJobServer::start);
        webServer.ifPresent(JobRunrDashboardWebServer::start);
    }

    @Override
    public void destroy() {
        backgroundJobServer.ifPresent(BackgroundJobServer::stop);
        webServer.ifPresent(JobRunrDashboardWebServer::stop);
    }
}
