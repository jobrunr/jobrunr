package org.jobrunr.dashboard.server.http.url;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;

class RequestUrlTest {

    @Test
    void testRequestUrl() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThat(requestUrl.getUrl()).isEqualTo("/api/jobs/enqueued?offset=2&limit=2");
    }

    @Test
    void testRequestUrlParams() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThat(requestUrl.getParams()).containsEntry(":state", "enqueued");
    }

    @Test
    void testRequestUrlParamAsUUID() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/17b2c0a0-bf6b-446a-8cea-93246675fe0c").toRequestUrl("/api/jobs/:id");
        assertThat(requestUrl.param(":id", UUID.class)).isEqualTo(UUID.fromString("17b2c0a0-bf6b-446a-8cea-93246675fe0c"));
    }

    @Test
    void testRequestUrlParamAsString() {
        RequestUrl requestUrl = new MatchUrl("/api/problems/some-string").toRequestUrl("/api/problems/:type");
        assertThat(requestUrl.param(":type", String.class)).isEqualTo("some-string");
    }

    @Test
    void testRequestUrlParamAsEnum() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThat(requestUrl.param(":state", StateName.class)).isEqualTo(ENQUEUED);
    }

    @Test
    void testRequestUrlParamAsUnknownClass() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThatIllegalArgumentException().isThrownBy(() -> requestUrl.param(":state", Object.class));
    }

    @Test
    void testRequestUrlQueryParam() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued?present=2").toRequestUrl("/api/jobs/:state");
        assertThat(requestUrl.queryParam("present")).isEqualTo("2");
    }

    @Test
    void testRequestUrlQueryParamWhichIsPresentUsingClass() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs?state=SCHEDULED").toRequestUrl("/api/jobs");
        assertThat(requestUrl.queryParam("state", StateName.class, ENQUEUED)).isEqualTo(SCHEDULED);
    }

    @Test
    void testRequestUrlQueryParamWhichIsNotPresentUsingClass() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs").toRequestUrl("/api/jobs");
        assertThat(requestUrl.queryParam("state", StateName.class, ENQUEUED)).isEqualTo(ENQUEUED);
    }

    @Test
    void testToRequestUrlWithQueryParams() {
        RequestUrl requestUrl = new MatchUrl("/api/jobs/enqueued?offset=2&limit=2&order=updatedAt:DESC").toRequestUrl("/api/jobs/:state");
        PageRequest pageRequest = requestUrl.fromQueryParams(PageRequest.class);
        assertThat(pageRequest.getOffset()).isEqualTo(2);
        assertThat(pageRequest.getLimit()).isEqualTo(2);
        assertThat(pageRequest.getOrder()).isEqualTo("updatedAt:DESC");
    }

}