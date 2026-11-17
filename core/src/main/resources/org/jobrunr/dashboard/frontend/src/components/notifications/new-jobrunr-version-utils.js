export const versionIsNewerThanOther = (version, otherVersion) => {
    const versionParts = getVersionParts(version);
    const otherVersionParts = getVersionParts(otherVersion);

    if (!versionParts.version && !otherVersionParts.version) return false;
    if (!otherVersionParts.version) return true;
    if (!versionParts.version) return false;

    if (versionParts.version.localeCompare(otherVersionParts.version, "en", {numeric: true, sensitivity: "base"}) > 0) return true;

    if (!versionParts.qualifier && !otherVersionParts.qualifier) return false;
    if (!versionParts.qualifier) return true;
    if (!otherVersionParts.qualifier) return false;
    return versionParts.qualifier.localeCompare(otherVersionParts.qualifier, "en", {sensitivity: "base"}) > 0;
}

const getVersionParts = (version) => {
    const splitVersion = version?.split("-");
    return {version: splitVersion?.[0], qualifier: splitVersion?.[1]}
}
