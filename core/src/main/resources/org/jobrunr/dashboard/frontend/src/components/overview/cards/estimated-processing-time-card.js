import StatCard from "./stat-card.js";
import {useJobStats} from "../../../hooks/useJobStats";
import {SuffixFreeTimeAgo} from "../../utils/time-ago";

const EstimatedProcessingTimeCard = () => {
    const [stats, _] = useJobStats();

    return (
        <StatCard title="Estimated processing time">
            {stats.estimation.processingDone ? <>All done!</>
                : stats.estimation.estimatedProcessingTimeAvailable
                    ? <SuffixFreeTimeAgo date={new Date(stats.estimation.estimatedProcessingFinishedAt)}/>
                    : <>Calculating...</>
            }
        </StatCard>
    );
};

export default EstimatedProcessingTimeCard;