import {DismissibleNotification} from "./dismissible-notification";
import {versionIsNewerThanOther} from "./new-jobrunr-version-utils.js";

export const LATEST_DISMISSED_BETA_VERSION_STORAGE_KEY = "latestDismissedBetaVersion";

export const getNewBetaVersionProblem = (currentVersion, latestVersion) => {
    if (!latestVersion) return;
    if (!currentVersion || versionIsNewerThanOther(latestVersion, currentVersion)) return {type: "new-jobrunr-beta-version", latestVersion, currentVersion};
}

export const NewJobRunrBetaVersionAvailableNotification = ({problem: {currentVersion, latestVersion, read}, onDismiss, ...rest}) => {
    const handleDismiss = () => {
        localStorage.setItem(LATEST_DISMISSED_BETA_VERSION_STORAGE_KEY, latestVersion);
        onDismiss();
    }

    return (
        <DismissibleNotification
            severity="info"
            title={`New JobRunr Beta Available`}
            onDismiss={handleDismiss}
            read={read}
            containerId="jobrunr-beta-version-available"
            {...rest}
        >
            <p>JobRunr version {latestVersion} is live!
                Try it out and share your thoughts by starting a <a target="_blank" href="https://github.com/jobrunr/jobrunr/discussions">
                    discussion on GitHub</a>. Thanks for your contribution!</p>
            Current version: {currentVersion ?? "UNKNOWN"}
        </DismissibleNotification>
    );
};