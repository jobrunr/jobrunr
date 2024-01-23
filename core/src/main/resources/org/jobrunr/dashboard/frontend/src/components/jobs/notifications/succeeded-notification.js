import {useEffect, useState} from "react";
import serversState from "../../../ServersStateContext";
import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago/lib";
import {JobNotification} from "./job-notification";

const SucceededNotification = ({job}) => {
    const [serverStats, setServerStats] = useState(serversState.getServers());
    useEffect(() => {
        serversState.addListener(setServerStats);
        return () => serversState.removeListener(setServerStats);
    }, [])

    const deleteDuration = serverStats[0].deleteSucceededJobsAfter;
    const deleteDurationInSec = deleteDuration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;

    const succeededState = job.jobHistory[job.jobHistory.length - 1]
    const succeededDate = new Date(succeededState.createdAt);
    const deleteDate = new Date(succeededDate.getTime() + (deleteDurationInSec * 1000));

    return (
        <JobNotification>
            <strong>This job has succeeded.</strong> It will automatically go to the deleted state in <TimeAgo
            date={deleteDate} title={deleteDate.toString()}/>.
        </JobNotification>
    )
};

export default SucceededNotification;