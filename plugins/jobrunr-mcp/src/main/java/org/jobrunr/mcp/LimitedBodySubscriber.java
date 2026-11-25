package org.jobrunr.mcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Collects an HTTP response while cancelling the transfer as soon as its byte cap is exceeded. */
final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {

    private final int maxBytes;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final CompletableFuture<byte[]> body = new CompletableFuture<>();
    private Flow.Subscription subscription;

    LimitedBodySubscriber(int maxBytes) {
        if (maxBytes < 1) throw new IllegalArgumentException("maxBytes must be positive");
        this.maxBytes = maxBytes;
    }

    static HttpResponse.BodyHandler<byte[]> handler(int maxBytes) {
        return ignored -> new LimitedBodySubscriber(maxBytes);
    }

    @Override
    public CompletionStage<byte[]> getBody() {
        return body;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            subscription.cancel();
            return;
        }
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
        if (body.isDone()) return;
        for (ByteBuffer buffer : buffers) {
            int count = buffer.remaining();
            if (count > maxBytes - output.size()) {
                subscription.cancel();
                body.completeExceptionally(new IOException("MCP hub response exceeds " + maxBytes + " bytes"));
                return;
            }
            byte[] bytes = new byte[count];
            buffer.get(bytes);
            output.write(bytes, 0, bytes.length);
        }
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        body.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        body.complete(output.toByteArray());
    }
}
