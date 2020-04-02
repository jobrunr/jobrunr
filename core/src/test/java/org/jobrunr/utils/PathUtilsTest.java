package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @Test
    public void canListItems() {
        final Stream<String> folderItems = PathUtils
                .listItems(PathUtils.class, "somefolder")
                .map(path -> path.getFileName().toString());

        assertThat(folderItems).contains("file1.txt", "file2.txt");
    }

    @Test
    public void canListItemsInJar() {
        final Stream<String> folderItems = PathUtils
                .listItems(Test.class)
                .map(path -> path.getFileName().toString());

        assertThat(folderItems).contains("Test.class", "Tags.class");
    }

}