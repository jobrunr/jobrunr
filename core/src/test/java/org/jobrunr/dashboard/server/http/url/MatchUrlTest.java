package org.jobrunr.dashboard.server.http.url;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MatchUrlTest {

    @Test
    void testNoMatch() {
        boolean matches = new MatchUrl("/api").matches("/dashboard");
        assertThat(matches).isFalse();
    }

    @Test
    void testMatch() {
        boolean matches = new MatchUrl("/api/jobs").matches("/api/jobs");
        assertThat(matches).isTrue();
    }

    @Test
    void testNoMatchWithParamsAndDifferentSize() {
        boolean matches = new MatchUrl("/api/jobs/enqueued/test").matches("/api/jobs/:state/test/extra");
        assertThat(matches).isFalse();
    }

    @Test
    void testMatchWithParams() {
        boolean matches = new MatchUrl("/api/jobs/enqueued/test").matches("/api/jobs/:state/test");
        assertThat(matches).isTrue();
    }

    @Test
    void testNoMatchWithParams() {
        boolean matches = new MatchUrl("/api/jobs/enqueued/wrong").matches("/api/jobs/:state/test");
        assertThat(matches).isFalse();
    }

    @Test
    void testToRequestUrl() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued/test").toRequestUrl("/api/jobs/:state/test");
        assertThat(requestUrl.param(":state")).isEqualTo("enqueued");
    }

    @Test
    void testToRequestUrlWithQueryParams() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued?offset=2").toRequestUrl("/api/jobs/:state");
        assertThat(requestUrl.param(":state")).isEqualTo("enqueued");
    }
}