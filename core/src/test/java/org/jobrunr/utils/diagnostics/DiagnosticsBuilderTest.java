package org.jobrunr.utils.diagnostics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;

class DiagnosticsBuilderTest {

    @Test
    void shouldCreateCorrectMarkdown() {
        String diagnostics = diagnostics()
                .withTitle("Title")
                .withSubTitle("Subtitle")
                .withLine("A line")
                .withBulletedLine("bulleted line")
                .AsMarkDown();

        assertThat(diagnostics)
                .isNotEmpty()
                .contains("## Title")
                .contains("### Subtitle")
                .contains("A line")
                .contains("- bulleted line");
    }
}