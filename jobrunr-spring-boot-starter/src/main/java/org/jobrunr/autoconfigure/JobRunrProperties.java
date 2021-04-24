package org.jobrunr.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties(prefix = "org.jobrunr")
public class JobRunrProperties {

    private Dashboard dashboard = new Dashboard();

    private BackgroundJobServer backgroundJobServer = new BackgroundJobServer();

    private Database database = new Database();

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public BackgroundJobServer getBackgroundJobServer() {
        return backgroundJobServer;
    }

    public void setBackgroundJobServer(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    /**
     * JobRunr JobScheduler related settings
     */
    public static class JobScheduler {

        /**
         * Enables the scheduling of jobs.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * JobRunr BackgroundJobServer related settings
     */
    public static class BackgroundJobServer {

        /**
         * Enables the background processing of jobs.
         */
        private boolean enabled = false;

        /**
         * Sets the workerCount for the BackgroundJobServer which defines the maximum number of jobs that will be run in parallel.
         * By default, this will be determined by the amount of available processor.
         */
        private Integer workerCount;

        /**
         * Set the pollIntervalInSeconds for the BackgroundJobServer to see whether new jobs need to be processed
         */
        private Integer pollIntervalInSeconds = 15;

        /**
         * Sets the duration to wait before changing jobs that are in the SUCCEEDED state to the DELETED state. If a duration suffix
         * is not specified, hours will be used.
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration deleteSucceededJobsAfter = Duration.ofHours(36);

        /**
         * Sets the duration to wait before permanently deleting jobs that are in the DELETED state. If a duration suffix
         * is not specified, hours will be used.
         */
        @DurationUnit(ChronoUnit.HOURS)
        private Duration permanentlyDeleteDeletedJobsAfter = Duration.ofHours(72);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getWorkerCount() {
            return workerCount;
        }

        public void setWorkerCount(Integer workerCount) {
            this.workerCount = workerCount;
        }

        public Integer getPollIntervalInSeconds() {
            return pollIntervalInSeconds;
        }

        public void setPollIntervalInSeconds(Integer pollIntervalInSeconds) {
            this.pollIntervalInSeconds = pollIntervalInSeconds;
        }

        public Duration getDeleteSucceededJobsAfter() {
            return deleteSucceededJobsAfter;
        }

        public void setDeleteSucceededJobsAfter(Duration deleteSucceededJobsAfter) {
            this.deleteSucceededJobsAfter = deleteSucceededJobsAfter;
        }

        public Duration getPermanentlyDeleteDeletedJobsAfter() {
            return permanentlyDeleteDeletedJobsAfter;
        }

        public void setPermanentlyDeleteDeletedJobsAfter(Duration permanentlyDeleteDeletedJobsAfter) {
            this.permanentlyDeleteDeletedJobsAfter = permanentlyDeleteDeletedJobsAfter;
        }
    }

    /**
     * JobRunr dashboard related settings
     */
    public static class Dashboard {

        /**
         * Enables the JobRunr dashboard.
         */
        private boolean enabled = false;

        /**
         * The port on which the Dashboard should run
         */
        private int port = 8000;

        /**
         * The username for the basic authentication which protects the dashboard
         */
        private String username = null;

        /**
         * The password for the basic authentication which protects the dashboard. WARNING: this is insecure as it is in clear text
         */
        private String password = null;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }


        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    /**
     * JobRunr dashboard related settings
     */
    public static class Database {
        /**
         * Allows to skip the creation of the tables - this means you should add them manually or by database migration tools like FlywayDB.
         */
        private boolean skipCreate = false;

        public void setSkipCreate(boolean skipCreate) {
            this.skipCreate = skipCreate;
        }

        public boolean isSkipCreate() {
            return skipCreate;
        }
    }
}
