package org.jobrunr.dashboard.server.http.handlers;

import org.jobrunr.dashboard.server.http.HttpResponse;

import java.util.function.BiConsumer;

public interface ExceptionHandler<T extends Exception> extends BiConsumer<T, HttpResponse> {
}
