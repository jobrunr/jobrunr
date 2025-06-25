import {EnergySavingsLeaf} from "@mui/icons-material";
import {SwitchableTimeAgo} from "../../utils/time-ago";
import {isCarbonAwaitingState} from "../../utils/job-utils";
import {JobNotification} from "./job-notification";

const CarbonAwareScheduledNotification = ({state}) => {
    if (!isCarbonAwaitingState(state)) return;

    return (
        <JobNotification
            severity="info"
            icon={<EnergySavingsLeaf fontSize="small" color="success"/>}
        >
            This job is scheduled Carbon Aware between <SwitchableTimeAgo date={new Date(state.from)}/> and <SwitchableTimeAgo
            date={new Date(state.to)}/> to minimize carbon impact.
        </JobNotification>
    )
};

export default CarbonAwareScheduledNotification;