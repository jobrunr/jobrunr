package org.jobrunr.mcp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The dashboard routes used by the current MCP tools and instance monitor. The hub
 * is a remote service, so a connector does not grant it the dashboard's full API.
 */
final class DashboardRequestPolicy {

    private static final String UUID = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String RECURRING_ID = "[0-9A-Za-z_(),.-]{1,127}";
    private static final String PROBLEM_TYPE = "[a-z0-9-]{1,64}";
    private static final String STATE = "AWAITING|SCHEDULED|ENQUEUED|PROCESSING|FAILED|SUCCEEDED|DELETED";
    private static final int MAX_PATH_LENGTH = 512;

    private static final Pattern JOB_DETAIL = Pattern.compile("/api/jobs/" + UUID);
    private static final Pattern JOB_PAGE = Pattern.compile(
            "/api/jobs\\?state=(" + STATE + ")&offset=([0-9]{1,10})&limit=([0-9]{1,3})(&order=updatedAt:DESC)?");
    private static final Pattern RECURRING_PAGE = Pattern.compile(
            "/api/recurring-jobs\\?offset=([0-9]{1,10})&limit=([0-9]{1,3})");
    private static final Pattern REQUEUE_JOB = Pattern.compile("/api/jobs/" + UUID + "/requeue");
    private static final Pattern TRIGGER_RECURRING = Pattern.compile("/api/recurring-jobs/" + RECURRING_ID + "/trigger");
    private static final Pattern DELETE_PROBLEM = Pattern.compile("/api/problems/" + PROBLEM_TYPE);

    private DashboardRequestPolicy() {
    }

    static boolean isAllowed(String method, String path, boolean canOperate) {
        if (method == null || path == null || path.length() > MAX_PATH_LENGTH) return false;
        if ("GET".equals(method)) return isAllowedGet(path);
        if (!canOperate) return false;
        return ("POST".equals(method) && (REQUEUE_JOB.matcher(path).matches() || TRIGGER_RECURRING.matcher(path).matches()))
                || ("DELETE".equals(method) && (JOB_DETAIL.matcher(path).matches() || DELETE_PROBLEM.matcher(path).matches()));
    }

    private static boolean isAllowedGet(String path) {
        if ("/api/metadata/id/cluster".equals(path)
                || "/api/problems".equals(path)
                || "/api/servers".equals(path)
                || "/api/version".equals(path)
                || JOB_DETAIL.matcher(path).matches()) return true;

        Matcher jobs = JOB_PAGE.matcher(path);
        if (jobs.matches()) return pageBounds(jobs.group(2), jobs.group(3), 100);
        Matcher recurring = RECURRING_PAGE.matcher(path);
        return recurring.matches() && pageBounds(recurring.group(1), recurring.group(2), 200);
    }

    private static boolean pageBounds(String offset, String limit, int maxLimit) {
        try {
            return Long.parseLong(offset) <= Integer.MAX_VALUE
                    && Integer.parseInt(limit) >= 1
                    && Integer.parseInt(limit) <= maxLimit;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
