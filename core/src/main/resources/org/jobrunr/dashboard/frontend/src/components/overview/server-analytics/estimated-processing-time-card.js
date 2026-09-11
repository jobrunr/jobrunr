import {useJobStats} from "../../../hooks/useJobStats";
import {SuffixFreeTimeAgo, SwitchableTimeFormatter} from "../../utils/time-ago";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";
import {AccessTime} from "@mui/icons-material";

const EstimatedProcessingTimeCard = () => {
    const [stats, _] = useJobStats();

    return (
        <AnalyticsCard title="Estimated processing time" icon={AccessTime}
                       subtitle={stats.estimation.processingDone ? undefined
                           : <>Estimate end {stats.estimation.estimatedProcessingTimeAvailable ?
                               <SwitchableTimeFormatter date={new Date(stats.estimation.estimatedProcessingFinishedAt)}/> : "..."}</>}>
            {stats.estimation.processingDone ? <>All done!</>
                : stats.estimation.estimatedProcessingTimeAvailable
                    ? <SuffixFreeTimeAgo date={new Date(stats.estimation.estimatedProcessingFinishedAt)}/>
                    : <>Calculating...</>
            }
        </AnalyticsCard>
    );
};

export default EstimatedProcessingTimeCard;