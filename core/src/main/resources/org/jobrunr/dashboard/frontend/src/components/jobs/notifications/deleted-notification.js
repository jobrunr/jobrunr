import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago";
import {JobNotification} from "./job-notification";
import {useServers} from "../../../hooks/useServers";

const DeletedNotification = ({job}) => {
    const [servers, _] = useServers();

    const deleteDuration = servers[0]?.permanentlyDeleteDeletedJobsAfter;
    const deleteDurationInSec = deleteDuration?.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;

    const deletedState = job.jobHistory[job.jobHistory.length - 1]
    const deletedDate = new Date(deletedState.createdAt);
    const deleteDate = new Date(deletedDate.getTime() + (deleteDurationInSec * 1000));

    return (
        <JobNotification>
            <strong>This job is deleted.</strong> {servers.length
            ? <>It will automatically be removed in <TimeAgo date={deleteDate} title={deleteDate.toString()}/>.</>
            : <>Please start a background job server to enable automatic permanent deletion.</>}
        </JobNotification>
    )
};

export default DeletedNotification;