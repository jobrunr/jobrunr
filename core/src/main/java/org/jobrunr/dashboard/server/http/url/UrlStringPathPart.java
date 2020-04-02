package org.jobrunr.dashboard.server.http.url;

public class UrlStringPathPart implements UrlPathPart {

    private final String part;

    public UrlStringPathPart(String part) {
        this.part = part;
    }

    @Override
    public boolean matches(UrlPathPart pathPart) {
        if (pathPart instanceof UrlStringPathPart) {
            return part.equals(((UrlStringPathPart) pathPart).part);
        }
        return false;
    }

    @Override
    public String part() {
        return part;
    }
}
