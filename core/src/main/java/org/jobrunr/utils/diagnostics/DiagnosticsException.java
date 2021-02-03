package org.jobrunr.utils.diagnostics;

import static org.jobrunr.utils.exceptions.Exceptions.getStackTraceAsString;

public class DiagnosticsException implements DiagnosticsItem {

    private Exception e;

    public DiagnosticsException(Exception e) {
        this.e = e;
    }

    @Override
    public String toMarkdown() {
        return "```java\n" + getStackTraceAsString(e) + "\n" + "```";
    }
}
