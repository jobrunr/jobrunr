package org.jobrunr.utils.diagnostics;

import static java.util.stream.IntStream.range;

public class DiagnosticsLine implements DiagnosticsItem {

    private final int indentation;
    private final String line;

    public DiagnosticsLine(String line) {
        this.indentation = 0;
        this.line = line;
    }

    public DiagnosticsLine(int indentation, String line) {
        this.indentation = indentation;
        this.line = line;
    }

    @Override
    public String toMarkdown() {
        StringBuilder result = new StringBuilder();
        range(0, indentation).forEach(i -> result.append("\t"));
        result.append(line).append("\n");
        return result.toString();
    }
}
