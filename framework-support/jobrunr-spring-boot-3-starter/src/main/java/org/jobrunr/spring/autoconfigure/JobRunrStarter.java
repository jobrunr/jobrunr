package org.jobrunr.spring.autoconfigure;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.util.Optional;

public class JobRunrStarter implements DisposableBean {

    private final Optional<BackgroundJobServer> backgroundJobServer;
    private final Optional<JobRunrDashboardWebServer> webServer;

    public JobRunrStarter(Optional<BackgroundJobServer> backgroundJobServer, Optional<JobRunrDashboardWebServer> webServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.webServer = webServer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startup() {
        backgroundJobServer.ifPresent(BackgroundJobServer::start);
        webServer.ifPresent(JobRunrDashboardWebServer::start);
    }

    @Override
    public void destroy() {
        backgroundJobServer.ifPresent(BackgroundJobServer::stop);
        webServer.ifPresent(JobRunrDashboardWebServer::stop);
    }
}
