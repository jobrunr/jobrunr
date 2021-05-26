package org.jobrunr.dashboard.server.http.handlers;

import org.jobrunr.dashboard.server.http.url.MatchUrl;

import java.util.HashMap;
import java.util.Optional;

public class HttpRequestMethodHandlers extends HashMap<String, HttpRequestHandler> {

    public Optional<String> findMatchingUrl(MatchUrl actualUrl) {
        if (containsKey(actualUrl.getUrl())) return Optional.of(actualUrl.getUrl());
        return keySet()
                .stream()
                .filter(actualUrl::matches)
                .findFirst();
    }
}
