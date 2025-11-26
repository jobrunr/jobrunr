package org.jobrunr.tests.fromhost;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static java.nio.charset.StandardCharsets.UTF_8;


/*
Cannot use Java 11 Http client as it is compiled with Java 8
 */
public class HttpClient {

    public static String getJson(String getUrl) {
        try {
            URL url = URI.create(getUrl).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            try (AutoCloseable ignored = con::disconnect) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), UTF_8));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();
                return content.toString();
            }
        } catch (Exception e) {
            return "{}";
        }
    }

    public static boolean ok(String getUrl) {
        try {
            URL url = URI.create(getUrl).toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            try (AutoCloseable ignored = con::disconnect) {
                int responseCode = con.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
