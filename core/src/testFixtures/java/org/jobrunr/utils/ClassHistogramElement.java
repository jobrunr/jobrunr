package org.jobrunr.utils;

import java.util.Comparator;

/**
 * Class histogram element for IR / Java object instrumentation
 */
public final class ClassHistogramElement {
    /**
     * Instance comparator
     */
    public static final Comparator<ClassHistogramElement> COMPARE_INSTANCES = new Comparator<ClassHistogramElement>() {
        @Override
        public int compare(final ClassHistogramElement o1, final ClassHistogramElement o2) {
            return (int) Math.abs(o1.instances - o2.instances);
        }
    };

    /**
     * Bytes comparator
     */
    public static final Comparator<ClassHistogramElement> COMPARE_BYTES = new Comparator<ClassHistogramElement>() {
        @Override
        public int compare(final ClassHistogramElement o1, final ClassHistogramElement o2) {
            return (int) Math.abs(o1.bytes - o2.bytes);
        }
    };

    /**
     * Classname comparator
     */
    public static final Comparator<ClassHistogramElement> COMPARE_CLASSNAMES = new Comparator<ClassHistogramElement>() {
        @Override
        public int compare(final ClassHistogramElement o1, final ClassHistogramElement o2) {
            return o1.clazz.getCanonicalName().compareTo(o2.clazz.getCanonicalName());
        }
    };

    private final Class<?> clazz;
    private long instances;
    private long bytes;

    /**
     * Constructor
     *
     * @param clazz class for which to construct histogram
     */
    public ClassHistogramElement(final Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * Add an instance
     *
     * @param sizeInBytes byte count
     */
    public void addInstance(final long sizeInBytes) {
        instances++;
        this.bytes += sizeInBytes;
    }

    /**
     * Get size in bytes
     *
     * @return size in bytes
     */
    public long getBytes() {
        return bytes;
    }

    /**
     * Get class
     *
     * @return class
     */
    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * Get number of instances
     *
     * @return number of instances
     */
    public long getInstances() {
        return instances;
    }

    @Override
    public String toString() {
        return "ClassHistogramElement[class=" + clazz.getCanonicalName() + ", instances=" + instances + ", bytes=" + bytes + "]";
    }
}
