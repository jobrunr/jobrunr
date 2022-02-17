package org.jobrunr.utils.resources;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class ClassPathResourceProviderTest {

    @Test
    void canGetPathsOnClasspath() {
        try(ClassPathResourceProvider resourceProvider = new ClassPathResourceProvider()) {
            final List<String> paths = resourceProvider
                    .toPathsOnClasspath("/org/jobrunr/utils/resources/somefolder")
                    .map(Path::toString)
                    .collect(toList());

            assertThat(paths).hasSize(1);
            assertThat(paths.get(0)).endsWith("org/jobrunr/utils/resources/somefolder");
        }
    }

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

}