package org.jobrunr.utils.diagnostics;

public class DiagnosticsBulletedLine extends DiagnosticsLine {

    public DiagnosticsBulletedLine(String line) {
        super("- " + line);
    }

    public DiagnosticsBulletedLine(String name, String value) {
        super("- __" + name + "__: " + value);
    }

    public DiagnosticsBulletedLine(int indentation, String line) {
        super(indentation, "- " + line);
    }

    public DiagnosticsBulletedLine(int indentation, String name, String value) {
        super(indentation, "- __" + name + "__: " + value);
    }
}
