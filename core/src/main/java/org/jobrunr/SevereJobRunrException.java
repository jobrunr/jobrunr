package org.jobrunr;

import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;

public class SevereJobRunrException extends JobRunrException {

    private final DiagnosticsAware diagnosticsAware;

    public SevereJobRunrException(String message, DiagnosticsAware diagnosticsAware) {
        super(message);
        this.diagnosticsAware = diagnosticsAware;
    }

    public DiagnosticsBuilder getDiagnostics() {
        return diagnosticsAware.getDiagnosticsInfo();
    }

    public interface DiagnosticsAware {
        DiagnosticsBuilder getDiagnosticsInfo();
    }
}
