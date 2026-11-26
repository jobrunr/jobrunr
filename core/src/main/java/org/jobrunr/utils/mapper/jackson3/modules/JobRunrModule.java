package org.jobrunr.utils.mapper.jackson3.modules;

import tools.jackson.databind.module.SimpleModule;

public class JobRunrModule extends SimpleModule {

    public JobRunrModule() {
        throw new UnsupportedOperationException("You need Java 17 or higher to use Jackson3JsonMapper.");
    }
}
