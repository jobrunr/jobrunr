import StatCard from "./stat-card.js";
import {SuffixFreeTimeAgo} from "../../utils/time-ago";

const UptimeCard = ({servers}) => {
    return (
        <StatCard title="Uptime">
            <SuffixFreeTimeAgo date={new Date(servers[0].firstHeartbeat)}/>
        </StatCard>
    );
};

export default UptimeCard;