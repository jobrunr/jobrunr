import {DismissibleNotification} from "./dismissible-notification";

export const LATEST_DISMISSED_VERSION_STORAGE_KEY = "latestDismissedVersion";

const getVersionParts = (version) => {
    const splitVersion = version?.split("-");
    return {version: splitVersion?.[0], qualifier: splitVersion?.[1]}
}

const versionIsNewerThanOther = (version, otherVersion) => {
    // TODO ignore betas
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

export const getNewVersionProblem = (currentVersion, latestVersion) => {
    if (!latestVersion) return;
    if (!currentVersion || versionIsNewerThanOther(latestVersion, currentVersion)) return {type: "new-jobrunr-version", latestVersion, currentVersion};
}

export const NewJobRunrVersionAvailableNotification = ({problem: {currentVersion, latestVersion, read}, onDismiss, ...rest}) => {
    const handleDismiss = () => {
        localStorage.setItem(LATEST_DISMISSED_VERSION_STORAGE_KEY, latestVersion);
        onDismiss();
    }

    return (
        <DismissibleNotification
            severity="info"
            title="New JobRunr Version Available"
            onDismiss={handleDismiss}
            read={read}
            {...rest}
        >
            JobRunr version {latestVersion} is available. Please upgrade JobRunr as it brings bugfixes,
            performance improvements and new features.<br/>
            Current version: {currentVersion ?? "UNKNOWN"}
        </DismissibleNotification>
    );
};