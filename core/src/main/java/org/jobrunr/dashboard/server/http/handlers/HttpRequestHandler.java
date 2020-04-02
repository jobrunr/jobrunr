package org.jobrunr.dashboard.server.http.handlers;

import org.jobrunr.dashboard.server.http.HttpRequest;
import org.jobrunr.dashboard.server.http.HttpResponse;
import org.jobrunr.utils.exceptions.Exceptions.ThrowingBiConsumer;

public interface HttpRequestHandler extends ThrowingBiConsumer<HttpRequest, HttpResponse> {

}
