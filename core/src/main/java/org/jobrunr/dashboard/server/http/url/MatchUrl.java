package org.jobrunr.dashboard.server.http.url;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchUrl {

    private final String url;
    private final List<UrlPathPart> pathParts;

    public MatchUrl(String url) {
        this.url = url;
        String path = url;
        if (path.indexOf('?') > -1) path = path.substring(0, path.indexOf('?'));
        pathParts = Stream.of(path.split("/"))
                .map(this::toUrlPathPart)
                .collect(Collectors.toList());
    }

    public String getUrl() {
        return url;
    }

    private UrlPathPart toUrlPathPart(String part) {
        if (part.startsWith(":")) return new UrlParamPathPart(part);
        return new UrlStringPathPart(part);
    }

    public boolean matches(String matchUrl) {
        if (url.equals(matchUrl)) return true;

        Iterator<UrlPathPart> iter1 = new MatchUrl(matchUrl).pathParts.iterator();
        Iterator<UrlPathPart> iter2 = pathParts.iterator();
        while (iter1.hasNext() && iter2.hasNext())
            if (!(iter1.next().matches(iter2.next()))) return false;

        return !iter1.hasNext() && !iter2.hasNext();
    }

    public RequestUrl toRequestUrl(String matchUrl) {
        Map<String, String> params = new HashMap<>();
        Iterator<UrlPathPart> iter1 = new MatchUrl(matchUrl).pathParts.iterator();
        Iterator<UrlPathPart> iter2 = pathParts.iterator();
        while (iter1.hasNext() && iter2.hasNext()) {
            UrlPathPart matchUrlPathPart = iter1.next();
            UrlPathPart actualUrlPathPart = iter2.next();
            if (matchUrlPathPart instanceof UrlParamPathPart) {
                params.put(matchUrlPathPart.part(), actualUrlPathPart.part());
            }
        }
        return new RequestUrl(url, params);
    }
}
