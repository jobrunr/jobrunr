package org.jobrunr.utils;

import java.util.Objects;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.substringAfter;
import static org.jobrunr.utils.StringUtils.substringBefore;

public class VersionNumber implements Comparable<VersionNumber> {

    private final String completeVersion;
    private final String version;
    private final int majorVersion;
    private final int minorVersion;
    private final int patchVersion;
    private final String qualifier;

    public VersionNumber(String completeVersion) {
        this.completeVersion = completeVersion;
        this.version = substringBefore(completeVersion, "-");
        this.qualifier = substringAfter(completeVersion, "-");
        String[] split = this.version.split("\\.");
        this.majorVersion = split.length > 0 ? Integer.parseInt(split[0]) : 0;
        this.minorVersion = split.length > 1 ? Integer.parseInt(split[1]) : 0;
        this.patchVersion = split.length > 2 ? Integer.parseInt(split[2]) : 0;
    }

    public String getCompleteVersion() {
        return completeVersion;
    }

    public boolean isOlderOrEqualTo(VersionNumber versionNumber) {
        return equals(versionNumber) || isOlderThan(versionNumber);
    }

    public boolean isNewerOrEqualTo(VersionNumber versionNumber) {
        return equals(versionNumber) || isNewerThan(versionNumber);
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
        if (majorVersion < o.majorVersion) return -1;
        else if (majorVersion > o.majorVersion) return 1;

        if (minorVersion < o.minorVersion) return -1;
        else if (minorVersion > o.minorVersion) return 1;

        if (patchVersion < o.patchVersion) return -1;
        else if (patchVersion > o.patchVersion) return 1;

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

    private String toFullyQualifiedVersion(String version) {
        String newString = "0000000000".concat(version);
        return newString.substring(newString.length() - 10);
    }

    public static VersionNumber of(String version) {
        return new VersionNumber(version);
    }

    public static boolean isOlderOrEqualTo(String baseLine, String actualVersion) {
        return of(baseLine).isOlderOrEqualTo(of(actualVersion));
    }

    public static boolean isNewerOrEqualTo(String actualVersion, String baseLine) {
        return of(actualVersion).isNewerOrEqualTo(of(baseLine));
    }
}
