package org.jobrunr.dashboard.server.http.url;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeenyMatchUrlTest {

    @Test
    public void testNoMatch() {
        boolean matches = new TeenyMatchUrl("/api").matches("/dashboard");
        assertThat(matches).isFalse();
    }

    @Test
    public void testMatch() {
        boolean matches = new TeenyMatchUrl("/api/jobs").matches("/api/jobs");
        assertThat(matches).isTrue();
    }

    @Test
    public void testNoMatchWithParamsAndDifferentSize() {
        boolean matches = new TeenyMatchUrl("/api/jobs/enqueued/test").matches("/api/jobs/:state/test/extra");
        assertThat(matches).isFalse();
    }

    @Test
    public void testMatchWithParams() {
        boolean matches = new TeenyMatchUrl("/api/jobs/enqueued/test").matches("/api/jobs/:state/test");
        assertThat(matches).isTrue();
    }

    @Test
    public void testNoMatchWithParams() {
        boolean matches = new TeenyMatchUrl("/api/jobs/enqueued/wrong").matches("/api/jobs/:state/test");
        assertThat(matches).isFalse();
    }

    @Test
    public void testToRequestUrl() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued/test").toRequestUrl("/api/jobs/:state/test");
        assertThat(teenyRequestUrl.param(":state")).isEqualTo("enqueued");
    }

    @Test
    public void testToRequestUrlWithQueryParams() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?offset=2").toRequestUrl("/api/jobs/:state");
        assertThat(teenyRequestUrl.param(":state")).isEqualTo("enqueued");
    }
}