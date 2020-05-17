package org.jobrunr.jobs.stubs;

import org.jobrunr.server.JobActivator;

import java.util.HashMap;
import java.util.Map;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class SimpleJobActivator implements JobActivator {

    private Map<Class<?>, Object> allServices = new HashMap<>();

    public SimpleJobActivator(Object... services) {
        for (Object service : services) {
            allServices.put(service.getClass(), service);
        }
    }

    @Override
    public <T> T activateJob(Class<T> type) {
        return cast(allServices.get(type));
    }
}
