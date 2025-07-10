import {EnergySavingsLeaf} from "@mui/icons-material";
import {SwitchableTimeFormatter, SwitchableTimeRangeFormatter} from "../../utils/time-ago";
import {JobNotification} from "./job-notification";

const CarbonAwareScheduledNotification = ({job}) => {
    const carbonAwareAwaitingState = job?.jobHistory?.find(h => h["@class"].endsWith("CarbonAwareAwaitingState"));
    if (!carbonAwareAwaitingState) return;
    const scheduledState = job.jobHistory.find(h => h.state === "SCHEDULED");

    function carbonAwareSchedulingDisabled() {
        return scheduledState?.reason.indexOf("Carbon aware scheduling is disabled") >= 0;
    }

    if(carbonAwareSchedulingDisabled()) {
        return (
            <JobNotification
                severity="info"
                icon={<EnergySavingsLeaf fontSize="small" color="success"/>}
            >
                This job is scheduled Carbon Aware but Carbon Aware Processing is disabled: scheduling at <SwitchableTimeFormatter date={new Date(scheduledState.scheduledAt)}/>.
            </JobNotification>

        )
    }

    return (
        <JobNotification
            severity="info"
            icon={<EnergySavingsLeaf fontSize="small" color="success"/>}
        >
            This job is scheduled Carbon Aware <SwitchableTimeRangeFormatter from={new Date(carbonAwareAwaitingState.from)} to={new Date(carbonAwareAwaitingState.to)}/> to minimize carbon impact.
        </JobNotification>
    )
};

export default CarbonAwareScheduledNotification;