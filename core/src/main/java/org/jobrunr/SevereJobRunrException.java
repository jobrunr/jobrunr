package org.jobrunr;

import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;

public class SevereJobRunrException extends JobRunrException {

    private DiagnosticsAware diagnosticsAware;

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
