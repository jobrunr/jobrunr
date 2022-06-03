package org.jobrunr.server.jmx;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobServerStats {

    private final OperatingSystemMXBean operatingSystemMXBean;
    private final MBeanServer platformMBeanServer;
    private final ConcurrentMap<String, Object> cachedValues = new ConcurrentHashMap<>();

    public JobServerStats() {
        this(getOperatingSystemMXBean(), getPlatformMBeanServer());
    }

    protected JobServerStats(OperatingSystemMXBean operatingSystemMXBean, MBeanServer platformMBeanServer) {
        this.operatingSystemMXBean = operatingSystemMXBean;
        this.platformMBeanServer = platformMBeanServer;
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
        double value = ((Number) getMXBeanValue(name)).doubleValue();
        if (!Double.isNaN(value)) {
            cachedValues.put(name, value);
        }
        return cast(cachedValues.getOrDefault(name, -1.0));
    }

    private Long getMXBeanValueAsLong(String name) {
        long value = ((Number) getMXBeanValue(name)).longValue();
        if (value > 0) {
            cachedValues.put(name, value);
        }
        return cast(cachedValues.getOrDefault(name, -1L));
    }

    // visible for testing
    // see bug JDK-8193878

    <O> O getMXBeanValue(String name) {
        if(platformMBeanServer == null || operatingSystemMXBean == null) return cast(-1);

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
