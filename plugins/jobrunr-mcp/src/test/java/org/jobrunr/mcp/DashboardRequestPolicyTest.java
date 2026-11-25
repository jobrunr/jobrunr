package org.jobrunr.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardRequestPolicyTest {

    private static final String JOB_ID = "0199aaaa-0000-7000-8000-000000000001";

    @Test
    void allowsOnlyCurrentMcpReadRoutes() {
        List<String> currentRoutes = List.of(
                "/api/metadata/id/cluster",
                "/api/problems",
                "/api/servers",
                "/api/version",
                "/api/jobs/" + JOB_ID,
                "/api/jobs?state=FAILED&offset=0&limit=1",
                "/api/jobs?state=SUCCEEDED&offset=100&limit=100&order=updatedAt:DESC",
                "/api/recurring-jobs?offset=0&limit=1",
                "/api/recurring-jobs?offset=10000&limit=200");

        for (String path : currentRoutes) {
            assertThat(DashboardRequestPolicy.isAllowed("GET", path, false)).as(path).isTrue();
        }
    }

    @Test
    void rejectsOtherMetadataAndNonCanonicalOrExpensiveReads() {
        List<String> disallowed = List.of(
                "/api/metadata/credentials/app?format=jsonValue",
                "/api/metadata/id/cluster?format=jsonValue",
                "/api/api/jobs",
                "/api/jobs/../api/version",
                "/api/jobs/%2F",
                "/api/jobs?state=FAILED&offset=0&limit=101",
                "/api/jobs?state=FAILED&offset=10001&limit=1",
                "/api/jobs?state=FAILED&offset=2147483648&limit=1",
                "/api/jobs?state=FAILED&offset=0&limit=1&extra=1",
                "/api/jobs?state=FAILED&offset=0&limit=1%26extra=1",
                "/api/recurring-jobs?offset=0&limit=201",
                "/api/recurring-jobs?offset=10001&limit=1",
                "/api/recurring-jobs",
                "https://example.invalid/api/version",
                "/api/version\nPOST /api/jobs/" + JOB_ID + "/requeue");

        for (String path : disallowed) {
            assertThat(DashboardRequestPolicy.isAllowed("GET", path, true)).as(path).isFalse();
        }
    }

    @Test
    void writesRequireOperateTokenAndExactCurrentRoutes() {
        List<String[]> allowed = List.of(
                new String[]{"POST", "/api/jobs/" + JOB_ID + "/requeue"},
                new String[]{"DELETE", "/api/jobs/" + JOB_ID},
                new String[]{"POST", "/api/recurring-jobs/my-recurring_job(1)/trigger"},
                new String[]{"DELETE", "/api/problems/cpu-allocation-irregularity"});

        for (String[] request : allowed) {
            assertThat(DashboardRequestPolicy.isAllowed(request[0], request[1], false)).as(request[1]).isFalse();
            assertThat(DashboardRequestPolicy.isAllowed(request[0], request[1], true)).as(request[1]).isTrue();
        }
        assertThat(DashboardRequestPolicy.isAllowed("DELETE", "/api/recurring-jobs/my-job", true)).isFalse();
        assertThat(DashboardRequestPolicy.isAllowed("POST", "/api/jobs/" + JOB_ID + "/requeue?force=true", true)).isFalse();
        assertThat(DashboardRequestPolicy.isAllowed("DELETE", "/api/jobs/..", true)).isFalse();
        assertThat(DashboardRequestPolicy.isAllowed("post", "/api/jobs/" + JOB_ID + "/requeue", true)).isFalse();
    }
}
