package org.jobrunr.quarkus.extension.deployment;

public class JobRunrExtensionProcessorForTesting extends JobRunrExtensionProcessor {

    private boolean kotlinxSerializationWillBePresent = false;

    public void setKotlinxSerializationWillBePresent(boolean kotlinxSerializationWillBePresent) {
        this.kotlinxSerializationWillBePresent = kotlinxSerializationWillBePresent;
    }

    @Override
    protected boolean isKotlinxSerializationAndJobRunrKotlinSupportPresent() {
        return kotlinxSerializationWillBePresent;
    }
}
