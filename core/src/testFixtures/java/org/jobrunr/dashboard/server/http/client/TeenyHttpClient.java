package org.jobrunr.dashboard.server.http.client;

import org.jobrunr.utils.exceptions.Exceptions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class TeenyHttpClient {

    private final String baseUri;
    private final HttpClient httpClient;

    public TeenyHttpClient(String baseUri) {
        this.baseUri = baseUri;
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public HttpResponse<String> get(String url) {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + url))
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    public HttpResponse<String> get(String url, Object... params) {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    public HttpResponse<String> delete(String url, Object... params) {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .DELETE()
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }

    private <T> T unchecked(Exceptions.ThrowingSupplier<T> throwingSupplier) {
        try {
            return throwingSupplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse<String> post(String url, Object... params) {
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + String.format(url, params)))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        return unchecked(() -> httpClient.send(httpRequest, BodyHandlers.ofString()));
    }
}
