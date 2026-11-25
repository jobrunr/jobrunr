package org.jobrunr.mcp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryHttpExchangeTest {

    @Test
    void neverBuffersMoreThanTheResultLimitEvenIfTheHandlerDeclaresASmallerLength() throws Exception {
        InMemoryHttpExchange exchange = new InMemoryHttpExchange("GET", "/api/version");
        exchange.sendResponseHeaders(200, 1);
        exchange.getResponseBody().write(new byte[InMemoryHttpExchange.MAX_RESPONSE_BYTES]);

        assertThatThrownBy(() -> exchange.getResponseBody().write(1))
                .isInstanceOf(InMemoryHttpExchange.ResponseLimitExceededException.class);
        assertThat(exchange.responseExceededLimit()).isTrue();
        assertThat(exchange.getResponseCode()).isEqualTo(200);
    }
}
