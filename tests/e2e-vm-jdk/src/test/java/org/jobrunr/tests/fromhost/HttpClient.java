package org.jobrunr.tests.fromhost;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/*
Cannot use Java 11 Http client as it is compiled with Java 8
 */
public class HttpClient {

    public static String getJson(String getUrl) {
        try {
            URL url = new URL(getUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            try (AutoCloseable conc = con::disconnect) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
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
}
