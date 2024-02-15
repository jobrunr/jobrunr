package org.jobrunr.server.tasks;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.NewJobRunrVersionNotification;
import org.jobrunr.utils.JarUtils;
import org.jobrunr.utils.VersionNumber;
import org.jobrunr.utils.annotations.VisibleFor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jobrunr.utils.VersionNumber.v;

public class CheckForNewJobRunrVersion implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForNewJobRunrVersion.class);
    private static final Pattern versionPattern = Pattern.compile("\"latestVersion\"\\s*:\\s*\"(.*)\"");

    private final DashboardNotificationManager dashboardNotificationManager;
    private static boolean isFirstRun;

    public CheckForNewJobRunrVersion(BackgroundJobServer backgroundJobServer) {
        dashboardNotificationManager = backgroundJobServer.getDashboardNotificationManager();
        CheckForNewJobRunrVersion.isFirstRun = true; // why: otherwise latest version API is spammed during testing
    }

    @Override
    public void run() {
        if (isFirstRun) {
            final NewJobRunrVersionNotification newJobRunrVersionNotification = dashboardNotificationManager.getDashboardNotification(NewJobRunrVersionNotification.class);
            if (newJobRunrVersionNotification != null) {
                VersionNumber actualVersion = v(getActualVersion());
                VersionNumber latestVersion = v(newJobRunrVersionNotification.getLatestVersion());
                if (actualVersion.equals(latestVersion)) {
                    dashboardNotificationManager.deleteNotification(NewJobRunrVersionNotification.class);
                }
            }
        } else {
            try {
                VersionNumber latestVersion = v(getLatestVersion());
                VersionNumber actualVersion = v(getActualVersion());
                if (latestVersion.compareTo(actualVersion) > 0) {
                    dashboardNotificationManager.notify(new NewJobRunrVersionNotification(latestVersion.getCompleteVersion()));
                    LOGGER.info("JobRunr Pro version {} is available.", latestVersion);
                } else {
                    dashboardNotificationManager.deleteNotification(NewJobRunrVersionNotification.class);
                }
            } catch (IOException e) {
                LOGGER.info("Unable to check for new JobRunr version:\n {}", e.getMessage());
            }
        }
        CheckForNewJobRunrVersion.isFirstRun = false;
    }

    @VisibleFor("testing")
    static String getLatestVersion() throws IOException {
        URL apiUrl = new URL(getJobRunrVersionUrl());
        HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
        con.setRequestProperty("User-Agent", "JobRunr " + getActualVersion());
        con.setRequestMethod("GET");
        con.setConnectTimeout(2000);
        con.setReadTimeout(2000);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            Matcher matcher = versionPattern.matcher(content);
            final boolean versionIsFound = matcher.find();
            if (versionIsFound) {
                return matcher.group(1).replace("v", "");
            } else {
                throw new IOException("JobRunr API has changed?");
            }
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                throw new IOException(content.toString());
            }
        }
    }

    @VisibleFor("testing")
    static String getJobRunrVersionUrl() {
        return "https://api.jobrunr.io/api/version/jobrunr-pro/latest";
    }

    @VisibleFor("testing")
    static String getActualVersion() {
        return JarUtils.getVersion(JobRunr.class);
    }

    static void resetCheckForNewVersion() {
        CheckForNewJobRunrVersion.isFirstRun = true;
    }
}
