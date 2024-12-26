import {DismissibleInstanceProblemNotification} from "./dismissible-problem-notification";
import {darken, useTheme} from "@mui/material";
import {ProblemNotification} from "./problem-notification";
import {useState} from "react";

export const LATEST_DISMISSED_API_NOTIFICATION = "latestDismissedApiNotification";

export const getApiNotificationProblem = (apiNotification) => {
    if (!apiNotification) return;
    return {...apiNotification, type: "api-notification"};
}

const getTextColor = (palette, severity) => {
    const coefficient = 0.6;
    switch (severity) {
        case "error":
            return darken(palette.error.light, coefficient);
        case "warning":
            return darken(palette.warning.light, coefficient);
        case "info":
        default:
            return darken(palette.info.light, coefficient);
    }
}

const JobRunrApiNotification = ({problem}) => {
    const handleDismiss = () => {
        localStorage.setItem(LATEST_DISMISSED_API_NOTIFICATION, problem.id);
        problem.reset();
    }

    const theme = useTheme();
    const [iframeHeight, setIframeHeight] = useState(0);
    const severity = problem.severity || "info";
    const isDismissible = severity !== 'error';
    const NotificationComponent = isDismissible ? DismissibleInstanceProblemNotification : ProblemNotification;
    const notificationComponentProps = {
        severity,
        title: problem.title,
        onDismiss: handleDismiss
    }

    if (!isDismissible) delete notificationComponentProps.onDismiss;

    const handleIframeLoad = (e) => {
        setIframeHeight(e.target.contentWindow.document.body.scrollHeight);
    }

    const stylesheets = Array.from(document.querySelectorAll("head > link[rel='stylesheet']"))
        .map(link => `<link href="${link.href}" rel="stylesheet"/>`);

    const srcdoc = `
        <html><head>
        <meta http-equiv="Content-Security-Policy" content="script-src 'none'">
        ${stylesheets.join('\n')}
        <style>body{background-color: initial; font-size: 0.875rem; line-height: 1.43; font-weight: 400; color: ${getTextColor(theme.palette, severity)}}</style>
        <base target="_blank">
        </head>
        <body><div class="MuiAlert-root" style="width: fit-content; max-width: 100%">${problem.body}</div></body>
        </html>
    `;

    return (
        <NotificationComponent {...notificationComponentProps}>
            <iframe srcDoc={srcdoc}
                    sandbox="allow-popups allow-popups-to-escape-sandbox allow-same-origin"
                    style={{overflowY: "auto", height: iframeHeight, maxHeight: "250px", width: "100vw", maxWidth: "100%", border: "none"}}
                    onLoad={handleIframeLoad}
            />
        </NotificationComponent>
    )
};

export default JobRunrApiNotification;