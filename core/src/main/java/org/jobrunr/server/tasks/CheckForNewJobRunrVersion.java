package org.jobrunr.server.tasks;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.NewJobRunrVersionNotification;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.metadata.VersionRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckForNewJobRunrVersion implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForNewJobRunrVersion.class);

    private static final Pattern versionPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^,]*)\",");
    private final DashboardNotificationManager dashboardNotificationManager;

    public CheckForNewJobRunrVersion(BackgroundJobServer backgroundJobServer) {
        dashboardNotificationManager = backgroundJobServer.getDashboardNotificationManager();
    }

    @Override
    public void run() {
        try {
            String latestVersion = getLatestVersion();
            String actualVersion = getActualVersion();
            if (latestVersion.compareTo(actualVersion) > 0) {
                dashboardNotificationManager.notify(new NewJobRunrVersionNotification(latestVersion));
                LOGGER.info("JobRunr version " + latestVersion + " is available.");
            } else {
                dashboardNotificationManager.deleteNotification(NewJobRunrVersionNotification.class);
            }
        } catch (IOException e) {
            LOGGER.info("Unable to check for new JobRunr version...");
        }
    }

    @VisibleFor("testing")
    static String getLatestVersion() throws IOException {
        URL url = new URL("https://api.github.com/repos/jobrunr/jobrunr/releases/latest");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(2000);
        con.setReadTimeout(2000);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            Matcher matcher = versionPattern.matcher(content);
            final boolean versionIsFound = matcher.find();
            if (versionIsFound) {
                return matcher.group(1).replace("v", "");
            } else {
                throw new IOException("Github API has changed?");
            }
        }
    }

    @VisibleFor("testing")
    static String getActualVersion() {
        return VersionRetriever.getVersion(JobRunr.class);
    }
}
