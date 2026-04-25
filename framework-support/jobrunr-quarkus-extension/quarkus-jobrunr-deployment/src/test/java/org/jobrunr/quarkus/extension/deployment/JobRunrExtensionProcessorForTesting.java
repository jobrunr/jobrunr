package org.jobrunr.quarkus.extension.deployment;

public class JobRunrExtensionProcessorForTesting extends JobRunrExtensionProcessor {

    private boolean kotlinxSerializationWillBePresent = false;
    private boolean couchbaseClientWillBePresent = true;

    public void setKotlinxSerializationWillBePresent(boolean kotlinxSerializationWillBePresent) {
        this.kotlinxSerializationWillBePresent = kotlinxSerializationWillBePresent;
    }

    public void setCouchbaseClientWillBePresent(boolean couchbaseClientWillBePresent) {
        this.couchbaseClientWillBePresent = couchbaseClientWillBePresent;
    }

    @Override
    protected boolean isKotlinxSerializationAndJobRunrKotlinSupportPresent() {
        return kotlinxSerializationWillBePresent;
    }

    @Override
    protected boolean isCouchbaseClientPresent() {
        return couchbaseClientWillBePresent;
    }
}
