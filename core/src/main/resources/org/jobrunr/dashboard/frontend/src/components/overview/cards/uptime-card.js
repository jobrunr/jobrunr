import TimeAgo from "react-timeago/lib";
import StatCard from "./stat-card.js";

const UptimeCard = ({servers}) => {
    const timeAgoFormatter = (a, b, c) => a > 1 ? `${a} ${b}s` : `${a} ${b}`;

    return (
        <StatCard title="Uptime">
            <TimeAgo
                date={new Date(servers[0].firstHeartbeat)}
                title={new Date(servers[0].firstHeartbeat).toString()} formatter={timeAgoFormatter}
            />
        </StatCard>
    );
};

export default UptimeCard;