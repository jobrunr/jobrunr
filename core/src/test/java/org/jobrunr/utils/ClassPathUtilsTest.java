package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class ClassPathUtilsTest {

    @Test
    public void canGetPathsOnClasspath() {
        final List<String> paths = ClassPathUtils
                .toPathsOnClasspath("/org/jobrunr/utils/somefolder")
                .map(Path::toString)
                .collect(toList());

        assertThat(paths).hasSize(1);
        assertThat(paths.get(0)).endsWith("org/jobrunr/utils/somefolder");
    }

    @Test
    public void canListChildren() {
        final Stream<String> folderItems = ClassPathUtils
                .listAllChildrenOnClasspath(ClassPathUtilsTest.class, "somefolder")
                .map(path -> path.getFileName().toString());

        assertThat(folderItems).contains("file1.txt", "file2.txt");
    }

    @Test
    public void canListChildrenInJar() {
        final Stream<String> folderItems = ClassPathUtils
                .listAllChildrenOnClasspath(Test.class)
                .map(path -> path.getFileName().toString());

        assertThat(folderItems).contains("Test.class", "Tags.class");
    }

}