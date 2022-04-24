package org.jobrunr.utils.resources;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ClassPathResourceProviderTest {

    @Test
    void canListChildren() {
        try(ClassPathResourceProvider resourceProvider = new ClassPathResourceProvider()) {
            final Stream<String> folderItems = resourceProvider
                    .listAllChildrenOnClasspath(ClassPathResourceProviderTest.class, "somefolder")
                    .map(path -> path.getFileName().toString());

            assertThat(folderItems).contains("file1.txt", "file2.txt");
        }
    }

    @Test
    void canListChildrenInJar() {
        try(ClassPathResourceProvider resourceProvider = new ClassPathResourceProvider()) {
            final Stream<String> folderItems = resourceProvider
                    .listAllChildrenOnClasspath(Test.class)
                    .map(path -> path.getFileName().toString());

            assertThat(folderItems).contains("Test.class", "Tags.class");
        }
    }

    @Test
    void classPathResourceProviderIsThreadsafe() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(3);
        final Thread thread1 = new Thread(() -> useClassPathResourceProvider(atomicInteger, countDownLatch));
        final Thread thread2 = new Thread(() -> useClassPathResourceProvider(atomicInteger, countDownLatch));
        final Thread thread3 = new Thread(() -> useClassPathResourceProvider(atomicInteger, countDownLatch));

        thread1.start();
        thread2.start();
        thread3.start();

        countDownLatch.await(10, TimeUnit.SECONDS);
        assertThat(atomicInteger.get()).isEqualTo(300);
    }

    private void useClassPathResourceProvider(AtomicInteger atomicInteger, CountDownLatch countDownLatch) {
        for(int i = 0; i < 100; i++) {
            try (ClassPathResourceProvider resourceProvider = new ClassPathResourceProvider()) {
                final Stream<String> folderItems = resourceProvider
                        .listAllChildrenOnClasspath(Test.class)
                        .map(path -> path.getFileName().toString());

                assertThat(folderItems).contains("Test.class", "Tags.class");
                atomicInteger.incrementAndGet();
            }
        }
        countDownLatch.countDown();
    }
}