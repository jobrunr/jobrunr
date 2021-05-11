package org.jobrunr;

import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;

import java.io.Serializable;

public class SevereJobRunrException extends JobRunrException {

    private final DiagnosticsAware diagnosticsAware;

    public SevereJobRunrException(String message, DiagnosticsAware diagnosticsAware) {
        super(message);
        this.diagnosticsAware = diagnosticsAware;
    }

    public DiagnosticsBuilder getDiagnostics() {
        return diagnosticsAware.getDiagnosticsInfo();
    }

    public interface DiagnosticsAware extends Serializable {
        DiagnosticsBuilder getDiagnosticsInfo();
    }
}
