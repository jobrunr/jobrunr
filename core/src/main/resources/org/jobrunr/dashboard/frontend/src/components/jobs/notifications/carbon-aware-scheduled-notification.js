import {EnergySavingsLeaf} from "@mui/icons-material";
import {SwitchableTimeAgo, SwitchableTimeRangeFormatter} from "../../utils/time-ago";
import {isCarbonAwaitingState} from "../../utils/job-utils";
import {JobNotification} from "./job-notification";

const CarbonAwareScheduledNotification = ({state}) => {
    if (!isCarbonAwaitingState(state)) return;

    return (
        <JobNotification
            severity="info"
            icon={<EnergySavingsLeaf fontSize="small" color="success"/>}
        >
            This job is scheduled Carbon Aware <SwitchableTimeRangeFormatter from={new Date(state.from)} to={new Date(state.to)}/> to minimize carbon impact.
        </JobNotification>
    )
};

export default CarbonAwareScheduledNotification;