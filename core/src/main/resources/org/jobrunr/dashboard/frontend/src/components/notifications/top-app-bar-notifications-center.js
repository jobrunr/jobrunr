import React, {Fragment, useCallback, useContext, useEffect, useMemo, useRef, useState} from "react";
import IconButton from "@mui/material/IconButton";
import {Badge, Divider, Stack} from "@mui/material";
import {MarkEmailRead, Notifications} from "@mui/icons-material";
import {ClickAwayPopper} from "../ui/ClickAwayPopper";
import Paper from "@mui/material/Paper";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import List from "@mui/material/List";
import {SevereJobRunrExceptionProblemNotification} from "./severe-jobrunr-exception-problem";
import {CPUAllocationIrregularityProblemNotification} from "./cpu-allocation-irregularity-problem";
import {getNewVersionProblem, LATEST_DISMISSED_VERSION_STORAGE_KEY, NewJobRunrVersionAvailableNotification} from "./new-jobrunr-version-available";
import {JobNotFoundProblemNotification} from "./job-not-found-problem";
import {Notification} from "./notification";
import {PollIntervalInSecondsIsTooSmallProblemNotification} from "./poll-interval-timebox-is-too-small-problem";
import {getApiNotificationProblem, JobRunrApiNotification, LATEST_DISMISSED_API_NOTIFICATION} from "./jobrunr-api-notification";
import {JobRunrInfoContext} from "../../JobRunrInfoContext";
import {subDaysToDate} from "../../utils/helper-functions";
import {CarbonIntensityApiErrorProblem} from "./carbon-intensity-api-error-problem";

const READ_NOTIFICATIONS_STORAGE_KEY = "readNotifications";

const getReadNotifications = () => {
    const storedReadNotifications = localStorage.getItem(READ_NOTIFICATIONS_STORAGE_KEY);
    const readNotifications = storedReadNotifications ? JSON.parse(storedReadNotifications) : {};
    return parseDatesAndCleanupOlderReadNotifications(readNotifications);
}

const parseDatesAndCleanupOlderReadNotifications = (readNotifications = {}) => {
    const readNotificationIds = Object.keys(readNotifications);
    if (!readNotificationIds.length) return {};

    const aWeekAgo = subDaysToDate(new Date(), 7);
    const cleanedReadNotifications = readNotificationIds.reduce((acc, cur) => {
        const readAt = new Date(readNotifications[cur]);
        if (readAt > aWeekAgo) acc[cur] = readAt;
        return acc;
    }, {})

    storeReadNotifications(cleanedReadNotifications);

    return cleanedReadNotifications;
}

const storeReadNotifications = (readNotifications) => {
    localStorage.setItem(READ_NOTIFICATIONS_STORAGE_KEY, JSON.stringify(readNotifications));
}

const useReadNotifications = () => {
    const [readNotifications, setReadNotifications] = useState(getReadNotifications());

    const toggleStatus = useCallback((id) => {
        setReadNotifications(prev => {
            const copy = {...prev};
            if (id in prev) delete copy[id];
            else copy[id] = new Date();
            storeReadNotifications(copy);
            return copy;
        });
    }, [setReadNotifications]);

    const markAllRead = useCallback((allNotificationIds) => {
        const now = new Date();
        const timestamped = allNotificationIds.reduce((acc, cur) => {
            acc[cur] = now;
            return acc;
        }, {});
        setReadNotifications(timestamped);
        storeReadNotifications(timestamped);
    }, [setReadNotifications]);

    const isRead = useCallback((id) => {
        return id in readNotifications;
    }, [readNotifications]);

    return {toggleStatus, markAllRead, isRead};
}

const problemTypeOrder = {
    "api-notification": 0, "severe-jobrunr-exception": 1, "jobs-not-found": 2, "cpu-allocation-irregularity": 3,
    "poll-interval-in-seconds-is-too-small": 4, "carbon-intensity-api-error": 5, "new-jobrunr-version": 6
};
const getProblemOrder = (type) => problemTypeOrder[type] ?? 99;
const problemCompareFn = (a, b) => {
    return getProblemOrder(a.type) - getProblemOrder(b.type);
}

const hasProblemOfType = (problems, problemType) => {
    return !!problems?.find(({type}) => type === problemType);
}

const getProblemId = (problem) => {
    switch (problem.type) {
        case "api-notification":
            return `${problem.type};${problem.id}`;
        case 'jobs-not-found':
        case 'severe-jobrunr-exception':
        case 'cpu-allocation-irregularity':
        case 'poll-interval-in-seconds-is-too-small':
        case "carbon-intensity-api-error":
            return `${problem.type};${problem.createdAt}`;
        case 'new-jobrunr-version':
            return `${problem.type};${problem.latestVersion}`;
        default:
            return `unknown;${Math.random().toString(36).substring(2, 10)}`;
    }
}

const isNotDismissed = (key, value) => !localStorage.getItem(key)?.endsWith(value);

// why memoized: component re-renders because of job stats changes in TopAppBar
export const TopAppBarNotificationsCenter = React.memo(() => {
    const [clusterProblems, setClusterProblems] = useState([]);
    const [latestVersion, setLatestVersion] = useState();
    const [apiNotification, setApiNotification] = useState();
    const {version: currentVersion} = useContext(JobRunrInfoContext);
    const [isOpen, setIsOpen] = useState(false);
    const popperAnchorEl = useRef(null);
    const {toggleStatus, markAllRead, isRead} = useReadNotifications();

    const resetLatestVersion = useCallback(() => {
        setLatestVersion(undefined);
    }, []);

    const resetApiNotification = useCallback(() => {
        setApiNotification(undefined);
    }, []);

    const reloadProblems = useCallback(() => {
        fetch(`/api/problems`).then(res => res.json())
            .then(problems => setClusterProblems(problems))
            .catch(e => console.error("Failed to reload problems", e))
    }, []);

    useEffect(() => {
        Promise.all([
            fetch("/api/problems").then(res => res.json()),
            fetch("https://api.jobrunr.io/api/version/jobrunr/latest").then(res => res.json()).catch(() => undefined /* ignored */),
            fetch("https://api.jobrunr.io/api/notifications/jobrunr").then(res => res.json()).catch(() => undefined /* ignored */),
        ]).then(([clusterProblems, latestVersion, apiNotification]) => {
            setClusterProblems(clusterProblems);
            setLatestVersion(latestVersion?.["latestVersion"]);
            setApiNotification(apiNotification);
        }).catch(error => console.error(error));
    }, []);

    const problems = useMemo(() => {
            const problems = [...clusterProblems];
            if (latestVersion && isNotDismissed(LATEST_DISMISSED_VERSION_STORAGE_KEY, latestVersion)) {
                const newVersionProblem = getNewVersionProblem(currentVersion, latestVersion);
                if (newVersionProblem) problems.push(newVersionProblem);
            }

            if (apiNotification && isNotDismissed(LATEST_DISMISSED_API_NOTIFICATION, apiNotification["id"])) {
                problems.push(getApiNotificationProblem(apiNotification));
            }

            return problems.map(problem => ({...problem, id: getProblemId(problem)})).sort(problemCompareFn);
        },
        [clusterProblems, latestVersion, currentVersion, apiNotification]
    );

    const problemsWithReadStatus = problems.map(p => ({...p, read: isRead(p.id)}));
    const amountOfUnreadNotifications = problemsWithReadStatus.filter(p => !p.read).length;

    const openNotifications = () => {
        setIsOpen(true);
    };

    const closeNotifications = () => {
        setIsOpen(false);
    };

    const handleMarkAllAsRead = () => {
        markAllRead(problemsWithReadStatus.map(p => p.id));
    }

    const mapToNotificationElement = (problem) => {
        const handleReadStatusToggled = () => toggleStatus(problem.id);
        switch (problem.type) {
            case "api-notification":
                return <JobRunrApiNotification
                    problem={problem}
                    onReadStatusToggled={handleReadStatusToggled}
                    onDismiss={resetApiNotification}/>
            case 'jobs-not-found':
                return <JobNotFoundProblemNotification problem={problem} onReadStatusToggled={handleReadStatusToggled}/>
            case 'severe-jobrunr-exception':
                return <SevereJobRunrExceptionProblemNotification
                    problem={problem}
                    onReadStatusToggled={handleReadStatusToggled}
                    onDismiss={reloadProblems}
                    hasCpuAllocationIrregularity={hasProblemOfType(problems, "cpu-allocation-irregularity")}
                />
            case 'cpu-allocation-irregularity':
                return <CPUAllocationIrregularityProblemNotification
                    problem={problem}
                    onDismiss={reloadProblems}
                    onReadStatusToggled={handleReadStatusToggled}
                />
            case 'poll-interval-in-seconds-is-too-small':
                return <PollIntervalInSecondsIsTooSmallProblemNotification
                    problem={problem}
                    onDismiss={reloadProblems}
                    onReadStatusToggled={handleReadStatusToggled}
                />
            case "carbon-intensity-api-error":
                return <CarbonIntensityApiErrorProblem
                    problem={problem}
                    onDismiss={reloadProblems}
                    onReadStatusToggled={handleReadStatusToggled}
                />
            case 'new-jobrunr-version':
                return <NewJobRunrVersionAvailableNotification
                    problem={problem}
                    onReadStatusToggled={handleReadStatusToggled}
                    onDismiss={resetLatestVersion}
                />
            default:
                return <Notification title="Unknown" onReadStatusToggled={handleReadStatusToggled} read={problem.read}>
                    Received an unexpected notification of type: '{problem.type}'.
                </Notification>
        }
    }

    return <>
        <IconButton
            edge="start"
            color="inherit"
            size="large"
            sx={{marginRight: 2}}
            onClick={openNotifications}
            ref={popperAnchorEl}
        >
            <Badge badgeContent={amountOfUnreadNotifications} max={99} color="secondary" style={{textTransform: "uppercase"}}>
                <Notifications/>
            </Badge>
        </IconButton>
        <ClickAwayPopper isOpen={isOpen} handleClickAway={closeNotifications} anchorEl={popperAnchorEl?.current}>
            <Paper elevation={6}>
                <Box width="80vw" maxWidth={500} maxHeight="70vh" overflow="auto">
                    <Box p={2}>
                        <Stack direction="row" spacing={1} justifyContent="space-between">
                            <Button
                                startIcon={<MarkEmailRead/>}
                                variant="outlined"
                                disabled={!amountOfUnreadNotifications}
                                onClick={handleMarkAllAsRead}
                            >
                                Mark all read
                            </Button>
                            <Button variant="outlined" onClick={closeNotifications}>Close</Button>
                        </Stack>
                    </Box>
                    {problemsWithReadStatus.length
                        ? (
                            <List disablePadding>
                                {problemsWithReadStatus.map((problem) => (
                                    <Fragment key={problem.id}>
                                        <Divider/>
                                        {mapToNotificationElement(problem)}
                                    </Fragment>
                                ))}
                            </List>
                        )
                        : (
                            <Box p={2}>
                                All clear! You can relax, we have no news for you.
                            </Box>
                        )
                    }
                </Box>
            </Paper>
        </ClickAwayPopper>
    </>
});