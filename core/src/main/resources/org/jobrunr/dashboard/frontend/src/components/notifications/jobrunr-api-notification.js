import {DismissibleNotification} from "./dismissible-notification";

export const LATEST_DISMISSED_API_NOTIFICATION = "latestDismissedApiNotification";

export const getApiNotificationProblem = (apiNotification) => {
    if (!apiNotification) return;
    return {...apiNotification, type: "api-notification"};
}

export const JobRunrApiNotification = ({problem, onDismiss, ...rest}) => {
    const handleDismiss = () => {
        localStorage.setItem(LATEST_DISMISSED_API_NOTIFICATION, problem.id);
        onDismiss();
    }

    return <DismissibleNotification
        severity={problem.severity || "info"}
        title={problem.title}
        date={problem.createdAt}
        read={problem.read}
        onDismiss={handleDismiss}
        {...rest}
    >
        <span dangerouslySetInnerHTML={{__html: problem.body}}/>
    </DismissibleNotification>
};