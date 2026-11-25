package org.jobrunr.mcp;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitedBodySubscriberTest {

    @Test
    void acceptsAResponseAtTheLimit() {
        LimitedBodySubscriber subscriber = new LimitedBodySubscriber(4);
        TestSubscription subscription = new TestSubscription();
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2}), ByteBuffer.wrap(new byte[]{3, 4})));
        subscriber.onComplete();

        assertThat(subscriber.getBody().toCompletableFuture().join()).containsExactly(1, 2, 3, 4);
        assertThat(subscription.cancelled).isFalse();
    }

    @Test
    void cancelsTheTransferBeforeBufferingBytesBeyondTheLimit() {
        LimitedBodySubscriber subscriber = new LimitedBodySubscriber(4);
        TestSubscription subscription = new TestSubscription();
        subscriber.onSubscribe(subscription);
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{1, 2, 3})));
        subscriber.onNext(List.of(ByteBuffer.wrap(new byte[]{4, 5})));

        assertThat(subscription.cancelled).isTrue();
        assertThatThrownBy(() -> subscriber.getBody().toCompletableFuture().join())
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("MCP hub response exceeds");
    }

    private static class TestSubscription implements Flow.Subscription {
        private boolean cancelled;

        @Override
        public void request(long count) {
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
