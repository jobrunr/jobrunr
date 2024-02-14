package org.jobrunr.utils;

import java.util.Objects;

import static java.util.Optional.ofNullable;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.substringAfter;
import static org.jobrunr.utils.StringUtils.substringBefore;

public class VersionNumber implements Comparable<VersionNumber> {

    public static final VersionNumber JAVA_VERSION = new VersionNumber(System.getProperty("java.version"));
    private final String completeVersion;
    private final String version;
    private final String majorVersion;
    private final String minorVersion;
    private final String patchVersion;
    private final String updateVersion;
    private final String qualifier;

    private VersionNumber(String completeVersion) {
        this.completeVersion = completeVersion;
        this.version = substringBefore(completeVersion, "-");
        this.qualifier = substringAfter(completeVersion, "-");
        String[] split = this.version.split("\\.");
        this.majorVersion = split.length > 0 ? split[0] : "0";
        this.minorVersion = split.length > 1 ? split[1] : "0";
        this.patchVersion = split.length > 2 ? substringBefore(split[2], "_") : "0";
        this.updateVersion = split.length > 2 ? ofNullable(substringAfter(split[2], "_")).orElse("0") : "0";
    }

    public String getCompleteVersion() {
        return completeVersion;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    public String getMinorVersion() {
        return minorVersion;
    }

    public String getPatchVersion() {
        return patchVersion;
    }

    public boolean isOlderOrEqualTo(VersionNumber versionNumber) {
        return equals(versionNumber) || isOlderThan(versionNumber);
    }

    public boolean isNewerOrEqualTo(VersionNumber versionNumber) {
        return equals(versionNumber) || isNewerThan(versionNumber);
    }

    public boolean hasMajorVersionHigherOrEqualTo(int majorVersion) {
        return hasMajorVersionHigherOrEqualTo(Integer.toString(majorVersion));
    }

    public boolean hasMajorVersionHigherOrEqualTo(String majorVersion) {
        return hasMajorVersionHigherOrEqualTo(new VersionNumber(majorVersion));
    }

    public boolean hasMajorVersionHigherOrEqualTo(VersionNumber o) {
        return compareVersionNumber(majorVersion, o.majorVersion) >= 0;
    }

    public boolean hasMajorAndMinorVersionHigherOrEqualTo(String majorAndMinorVersion) {
        return hasMajorAndMinorVersionHigherOrEqualTo(new VersionNumber(majorAndMinorVersion));
    }

    public boolean hasMajorAndMinorVersionHigherOrEqualTo(VersionNumber o) {
        return compareVersionNumber(majorVersion, o.majorVersion) >= 0
                && compareVersionNumber(minorVersion, o.minorVersion) >= 0;
    }

    public boolean hasMajorMinorAndPatchVersionHigherOrEqualTo(String majorMinorAndPatchVersion) {
        return hasMajorMinorAndPatchVersionHigherOrEqualTo(new VersionNumber(majorMinorAndPatchVersion));
    }

    public boolean hasMajorMinorAndPatchVersionHigherOrEqualTo(VersionNumber o) {
        return compareVersionNumber(majorVersion, o.majorVersion) >= 0
                && compareVersionNumber(minorVersion, o.minorVersion) >= 0
                && compareVersionNumber(patchVersion, o.patchVersion) >= 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VersionNumber) {
            return completeVersion.equals(((VersionNumber) obj).completeVersion);
        }
        return false;
    }

    public boolean isOlderThan(Object obj) {
        if (obj instanceof VersionNumber) {
            return compareTo((VersionNumber) obj) < 0;
        }
        return false;
    }

    public boolean isNewerThan(Object obj) {
        return !isOlderThan(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(completeVersion);
    }

    @Override
    public int compareTo(VersionNumber o) {
        int majorVersionComparison = compareVersionNumber(majorVersion, o.majorVersion);
        if (majorVersionComparison != 0) return majorVersionComparison;

        int minorVersionComparison = compareVersionNumber(minorVersion, o.minorVersion);
        if (minorVersionComparison != 0) return minorVersionComparison;

        int patchVersionComparison = compareVersionNumber(patchVersion, o.patchVersion);
        if (patchVersionComparison != 0) return patchVersionComparison;

        int updateVersionComparison = compareVersionNumber(updateVersion, o.updateVersion);
        if (updateVersionComparison != 0) return updateVersionComparison;

        if (isNullOrEmpty(qualifier) && isNullOrEmpty(o.qualifier)) {
            return 0;
        } else if (isNullOrEmpty(qualifier) && isNotNullOrEmpty(o.qualifier)) {
            return 1;
        } else if (isNotNullOrEmpty(qualifier) && isNullOrEmpty(o.qualifier)) {
            return -1;
        } else {
            return qualifier.compareTo(o.qualifier);
        }
    }

    @Override
    public String toString() {
        return completeVersion;
    }

    private int compareVersionNumber(String myself, String other) {
        if (myself.length() != other.length()) return myself.length() - other.length();
        else if (myself.compareTo(other) < 0) return -1;
        else if (myself.compareTo(other) > 0) return 1;
        return 0;
    }

    public static VersionNumber v(String version) {
        return new VersionNumber(version);
    }
}