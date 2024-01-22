import StatCard from "./stat-card.js";

const NbrOfBackgroundJobServersCard = ({servers}) => {
    return (
        <StatCard title="Nbr of servers">
            {servers.length}
        </StatCard>
    );
};

export default NbrOfBackgroundJobServersCard;