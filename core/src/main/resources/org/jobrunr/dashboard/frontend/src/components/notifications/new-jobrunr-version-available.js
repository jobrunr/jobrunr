import {DismissibleNotification} from "./dismissible-notification";
import {versionIsNewerThanOther} from "./new-jobrunr-version-utils.js";

export const LATEST_DISMISSED_VERSION_STORAGE_KEY = "latestDismissedVersion";

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
            title={`New JobRunr Version Available`}
            onDismiss={handleDismiss}
            read={read}
            containerId="jobrunr-version-available"
            {...rest}
        >
            <p>JobRunr version {latestVersion} is available. Please upgrade JobRunr as it brings bugfixes,
                performance improvements and new features.
                Full release note available <a target="_blank" href="https://github.com/jobrunr/jobrunr/releases">on GitHub</a>.</p>
            Current version: {currentVersion ?? "UNKNOWN"}
        </DismissibleNotification>
    );
};