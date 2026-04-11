package org.jobrunr.server.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobServerStats {

    private final OperatingSystemMXBean operatingSystemMXBean;
    private final MBeanServer platformMBeanServer;
    private static class TimedCachedValue { // would be nice as a record ...
        private static final long EXPIRY_TIME_IN_NANOS = 10_000_000;
        private final long whenCreated = System.nanoTime();
        private final Object value;
        public TimedCachedValue(Object value) {this.value = value;}
        public boolean isNotExpired() { return whenCreated + EXPIRY_TIME_IN_NANOS > System.nanoTime(); }
    }
    private final ConcurrentMap<String, TimedCachedValue> cachedValues = new ConcurrentHashMap<>();
    private final ObjectName objectName;

    public JobServerStats() {
        this(getOperatingSystemMXBean(), getPlatformMBeanServer());
    }

    protected JobServerStats(OperatingSystemMXBean operatingSystemMXBean, MBeanServer platformMBeanServer) {
        this.operatingSystemMXBean = operatingSystemMXBean;
        this.platformMBeanServer = platformMBeanServer;
        this.objectName = operatingSystemMXBean != null ? operatingSystemMXBean.getObjectName() : null;
    }

    public Long getProcessMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public Long getProcessFreeMemory() {
        return Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }

    public Long getProcessAllocatedMemory() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }

    public Long getSystemTotalMemory() {
        return getMXBeanValueAsLong("TotalPhysicalMemorySize");
    }

    public Long getSystemFreeMemory() {
        return getMXBeanValueAsLong("FreePhysicalMemorySize");
    }

    public Double getSystemCpuLoad() {
        return getMXBeanValueAsDouble("SystemCpuLoad");
    }

    public Double getProcessCpuLoad() {
        return getMXBeanValueAsDouble("ProcessCpuLoad");
    }

    private Double getMXBeanValueAsDouble(String name) {
        final TimedCachedValue timedValue =  cachedValues.get(name);
        if (timedValue != null) {
            if (timedValue.isNotExpired()) return (Double) timedValue.value;
            cachedValues.remove(name);
        }
        double value = ((Number) getMXBeanValue(name)).doubleValue();
        if (!Double.isNaN(value)) {
            cachedValues.put(name, new TimedCachedValue(value));
            return value;
        }
        return -1.0;
    }

    private Long getMXBeanValueAsLong(String name) {
        final TimedCachedValue timedValue =  cachedValues.get(name);
        if (timedValue != null) {
            if (timedValue.isNotExpired()) return (Long) timedValue.value;
            cachedValues.remove(name);
        }
        long value = ((Number) getMXBeanValue(name)).longValue();
        if (value > 0) {
            cachedValues.put(name, new TimedCachedValue(value));
            return value;
        }
        return -1L;
    }

    // visible for testing
    // see bug JDK-8193878

    <O> O getMXBeanValue(String name) {
        if (platformMBeanServer == null || operatingSystemMXBean == null) return cast(-1);

        try {
            final Object attribute = platformMBeanServer.getAttribute(operatingSystemMXBean.getObjectName(), name);
            return cast(attribute);
        } catch (Throwable ex) {
            return cast(-1);
        }
    }

    private static OperatingSystemMXBean getOperatingSystemMXBean() {
        try {
            return ManagementFactory.getOperatingSystemMXBean();
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            return null;
        }
    }

    private static MBeanServer getPlatformMBeanServer() {
        try {
            return ManagementFactory.getPlatformMBeanServer();
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            return null;
        }
    }
}
