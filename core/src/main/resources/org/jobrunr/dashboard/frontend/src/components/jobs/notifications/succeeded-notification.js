import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago/lib";
import {JobNotification} from "./job-notification";
import {useServers} from "../../../hooks/useServers";

const SucceededNotification = ({job}) => {
    const [servers, _] = useServers();

    const deleteDuration = servers[0]?.deleteSucceededJobsAfter;
    const deleteDurationInSec = deleteDuration?.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;

    const succeededState = job.jobHistory[job.jobHistory.length - 1]
    const succeededDate = new Date(succeededState.createdAt);
    const deleteDate = new Date(succeededDate.getTime() + (deleteDurationInSec * 1000));

    return (
        <JobNotification>
            <strong>This job has succeeded.</strong> {servers.length
            ? <>It will automatically go to the deleted state in <TimeAgo
                date={deleteDate} title={deleteDate.toString()}/>.</>
            : <>Please start a background job server to enable automatic move to the deleted state.</>}
        </JobNotification>
    )
};

export default SucceededNotification;