import {DismissibleNotification} from "./dismissible-notification";
import {Notification} from "./notification";
import {useTheme} from "@mui/material";

export const LATEST_DISMISSED_API_NOTIFICATION = "latestDismissedApiNotification";

export const getApiNotificationProblem = (apiNotification) => {
    if (!apiNotification) return;
    return {...apiNotification, type: "api-notification"};
}

export const JobRunrApiNotification = ({problem, onDismiss, ...rest}) => {
    const theme = useTheme();
    const handleDismiss = () => {
        localStorage.setItem(LATEST_DISMISSED_API_NOTIFICATION, problem.id);
        onDismiss();
    }

    const severity = problem.severity || "info";
    const isDismissible = severity !== 'error';

    const NotificationComponent = isDismissible ? DismissibleNotification : Notification;

    const handleIframeLoad = (e) => {
        e.target.style.height = e.target.contentWindow.document.body.scrollHeight + "px";
    }

    const stylesheets = Array.from(document.querySelectorAll("head > link[rel='stylesheet']"))
        .map(link => `<link href="${link.href}" rel="stylesheet"/>`);


    const srcdoc = `
        <html lang="en"><head>
        <meta name="color-scheme" content="light dark">
        <meta http-equiv="Content-Security-Policy" content="script-src 'none'">
        ${stylesheets.join('\n')}
        <style>body{background-color: initial; color:${theme.palette.text.secondary}; font-size: 0.875rem; line-height: 1.43; font-weight: 400;}</style>
        <base target="_blank">
        </head>
        <body><div style="width: fit-content; max-width: 100%">${problem.body}</div></body>
        </html>
    `;

    return <NotificationComponent
        severity={severity}
        title={problem.title}
        date={problem.createdAt}
        read={problem.read}
        onDismiss={handleDismiss}
        {...rest}
    >
        <iframe srcDoc={srcdoc}
                sandbox="allow-popups allow-popups-to-escape-sandbox allow-same-origin"
                style={{overflowY: "auto", maxHeight: "250px", width: "100vw", maxWidth: "100%", border: "none"}}
                onLoad={handleIframeLoad}
        />
    </NotificationComponent>
};