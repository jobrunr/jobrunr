import StatCard from "./stat-card.js";

const AvgSystemCpuLoadCard = ({servers}) => {
    let averageSystemCpuLoad = servers[0].systemCpuLoad;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.systemCpuLoad, 0) / array.length;
        averageSystemCpuLoad = average(servers);
    }

    return (
        <StatCard title="Avg System Cpu Load">
            {`${(averageSystemCpuLoad * 100).toFixed(2)} %`}
        </StatCard>
    );
};

export default AvgSystemCpuLoadCard;