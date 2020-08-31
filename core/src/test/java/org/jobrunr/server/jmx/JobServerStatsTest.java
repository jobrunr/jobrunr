package org.jobrunr.server.jmx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.OperatingSystemMXBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// why: see JDK bug JDK8193878
@ExtendWith(MockitoExtension.class)
class JobServerStatsTest {

    private ObjectName objectName;
    @Mock
    private OperatingSystemMXBean operatingSystemMXBean;
    @Mock
    private MBeanServer mBeanServer;

    private JobServerStats jobServerStats;


    @BeforeEach
    void setUp() throws MalformedObjectNameException {
        objectName = new ObjectName("java.lang:type=OperatingSystem");
        when(operatingSystemMXBean.getObjectName()).thenReturn(objectName);
        jobServerStats = new JobServerStats(operatingSystemMXBean, mBeanServer);
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForSystemCpuLoadOnFirstCall_NegativeIsReturned() throws JMException {
        when(mBeanServer.getAttribute(objectName, "SystemCpuLoad")).thenReturn(Double.NaN);

        assertThat(jobServerStats.getSystemCpuLoad()).isEqualTo(-1);
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForSystemCpuLoadOnLaterCalls_CachedValueIsReturned() throws JMException {
        when(mBeanServer.getAttribute(objectName, "SystemCpuLoad")).thenReturn(0.7, Double.NaN, 0.5);

        assertThat(jobServerStats.getSystemCpuLoad()).isEqualTo(0.7);
        assertThat(jobServerStats.getSystemCpuLoad()).isEqualTo(0.7);
        assertThat(jobServerStats.getSystemCpuLoad()).isEqualTo(0.5);
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForProcessCpuLoadOnFirstCall_NegativeIsReturned() throws JMException {
        when(mBeanServer.getAttribute(objectName, "ProcessCpuLoad")).thenReturn(Double.NaN);

        assertThat(jobServerStats.getProcessCpuLoad()).isEqualTo(-1);
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForProcessCpuLoadOnLaterCalls_CachedValueIsReturned() throws JMException {
        when(mBeanServer.getAttribute(objectName, "ProcessCpuLoad")).thenReturn(0.7, Double.NaN, 0.5);

        assertThat(jobServerStats.getProcessCpuLoad()).isEqualTo(0.7);
        assertThat(jobServerStats.getProcessCpuLoad()).isEqualTo(0.7);
        assertThat(jobServerStats.getProcessCpuLoad()).isEqualTo(0.5);
    }
}