package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CheckForNewJobRunrVersion implements Runnable {

    private final StorageProvider storageProvider;
    private final JsonMapper jsonMapper;

    public CheckForNewJobRunrVersion(BackgroundJobServer backgroundJobServer) {
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.jsonMapper = backgroundJobServer.getJsonMapper();
    }

    @Override
    public void run() {
        try {
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
                System.out.println(content);
                final JobRunrGithubVersion deserialize = jsonMapper.deserialize(content.toString(), JobRunrGithubVersion.class);
                System.out.println(deserialize);
            }
        } catch (IOException e) {

        }
    }

    public static class JobRunrGithubVersion {

        public String tag_name;

    }
}
