package org.mockito;

import java.util.List;

public class MockedStaticHolder implements AutoCloseable {

    private final List<MockedStatic<?>> mocks;

    public MockedStaticHolder(MockedStatic<?>... mocks) {
        this.mocks = List.of(mocks);
    }

    @Override
    public void close() {
        mocks.forEach(MockedStatic::close);
    }
}
