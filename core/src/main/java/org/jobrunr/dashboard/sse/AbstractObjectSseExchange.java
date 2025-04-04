package org.jobrunr.dashboard.sse;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.sse.SseExchange;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;

public abstract class AbstractObjectSseExchange extends SseExchange {

    private final JsonMapper jsonMapper;

    public AbstractObjectSseExchange(HttpExchange httpExchange, JsonMapper jsonMapper) throws IOException {
        super(httpExchange);
        this.jsonMapper = jsonMapper;
    }

    public String sendObject(Object object) {
        final String message = jsonMapper.serialize(object);
        sendMessage(message);
        return message;
    }

}
