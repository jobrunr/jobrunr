import StatCard from "./stat-card.js";

const NbrOfBackgroundJobServersCard = ({servers}) => {
    return (
        <StatCard title="Nbr of servers" content={servers.length} />
    );
};

export default NbrOfBackgroundJobServersCard;