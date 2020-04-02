package org.jobrunr.dashboard.server.http.url;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TeenyRequestUrlTest {

    @Test
    public void testRequestUrl() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThat(teenyRequestUrl.getUrl()).isEqualTo("/api/jobs/enqueued?offset=2&limit=2");
    }

    @Test
    public void testRequestUrlParams() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThat(teenyRequestUrl.getParams()).containsEntry(":state", "enqueued");
    }

    @Test
    public void testRequestUrlParamAsUUID() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/17b2c0a0-bf6b-446a-8cea-93246675fe0c").toRequestUrl("/api/jobs/:id");
        assertThat(teenyRequestUrl.param(":id", UUID.class)).isEqualTo(UUID.fromString("17b2c0a0-bf6b-446a-8cea-93246675fe0c"));
    }

    @Test
    public void testRequestUrlParamAsEnum() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThat(teenyRequestUrl.param(":state", StateName.class)).isEqualTo(StateName.ENQUEUED);
    }

    @Test
    public void testRequestUrlParamAsUnknownClass() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        assertThatIllegalArgumentException().isThrownBy(() -> teenyRequestUrl.param(":state", Object.class));
    }

    @Test
    public void testRequestUrlQueryParam() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?present=2").toRequestUrl("/api/jobs/:state");
        assertThat(teenyRequestUrl.queryParam("present")).isEqualTo("2");
    }

    @Test
    public void testRequestUrlQueryParamAsInt() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?present=2").toRequestUrl("/api/jobs/:state");
        assertThat(teenyRequestUrl.queryParam("present", 3)).isEqualTo(2);
        assertThat(teenyRequestUrl.queryParam("notPresent", 3)).isEqualTo(3);
    }

    @Test
    public void testToRequestUrlWithQueryParams() {
        TeenyRequestUrl teenyRequestUrl = new TeenyMatchUrl("/api/jobs/enqueued?offset=2&limit=2").toRequestUrl("/api/jobs/:state");
        PageRequest pageRequest = teenyRequestUrl.fromQueryParams(PageRequest.class);
        assertThat(pageRequest.getOffset()).isEqualTo(2);
        assertThat(pageRequest.getLimit()).isEqualTo(2);
    }

}