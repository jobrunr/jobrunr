import {useEffect, useState} from "react";
import serversState from "../../../ServersStateContext";
import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago/lib";
import {JobNotification} from "./job-notification";

const DeletedNotification = ({job}) => {
    const [serverStats, setServerStats] = useState(serversState.getServers());
    useEffect(() => {
        serversState.addListener(setServerStats);
        return () => serversState.removeListener(setServerStats);
    }, [])

    const deleteDuration = serverStats[0].permanentlyDeleteDeletedJobsAfter;
    const deleteDurationInSec = deleteDuration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;

    const deletedState = job.jobHistory[job.jobHistory.length - 1]
    const deletedDate = new Date(deletedState.createdAt);
    const deleteDate = new Date(deletedDate.getTime() + (deleteDurationInSec * 1000));

    return (
        <JobNotification>
            <strong>This job is deleted.</strong> It will automatically be removed in <TimeAgo date={deleteDate}
                                                                                               title={deleteDate.toString()}/>.
        </JobNotification>
    )
};

export default DeletedNotification;