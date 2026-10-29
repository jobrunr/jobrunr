import {useJobStats} from "../../../hooks/useJobStats";
import {SuffixFreeTimeAgo} from "../../utils/time-ago";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";
import {AccessTime} from "@mui/icons-material";

const EstimatedProcessingTimeCard = () => {
    const [stats, _] = useJobStats();

    return (
        <AnalyticsCard title="Estimated processing time" icon={AccessTime}>
            {stats.estimation.processingDone ? <>All done!</>
                : stats.estimation.estimatedProcessingTimeAvailable
                    ? <SuffixFreeTimeAgo date={new Date(stats.estimation.estimatedProcessingFinishedAt)}/>
                    : <>Calculating...</>
            }
        </AnalyticsCard>
    );
};

export default EstimatedProcessingTimeCard;