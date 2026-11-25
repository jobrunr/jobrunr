package org.jobrunr.mcp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobRunrMcpPollProtocolTest {

    private static final String ID = "11111111111111111111111111111111";

    @Test
    void parsesAValidCurrentHubLineAndMarksMalformedKnownIds() throws Exception {
        List<JobRunrMcpConnector.PollRequest> requests = JobRunrMcpConnector.parsePollLines(
                ID + " GET /api/version\n"
                        + ID + " GET /api/version extra\n"
                        + ID + "\n"
                        + "not-an-id GET /api/version\n");

        assertThat(requests).hasSize(3);
        assertThat(requests.get(0).malformed()).isFalse();
        assertThat(requests.get(0).path()).isEqualTo("/api/version");
        assertThat(requests.get(1).malformed()).isTrue();
        assertThat(requests.get(2).malformed()).isTrue();
    }

    @Test
    void rejectsPollBatchesLargerThanTheBoundBeforeScheduling() {
        String body = (ID + " GET /api/version\n").repeat(65);

        assertThatThrownBy(() -> JobRunrMcpConnector.parsePollLines(body))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("too many");
    }
}
