package org.jobrunr.utils.diagnostics;

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
        String indentationResult = "";
        int actualIndentation = indentation;
        while (actualIndentation > 0) {
            indentationResult += "\t";
            actualIndentation--;
        }
        return indentationResult + line + "\n";
    }
}
