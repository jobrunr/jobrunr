import StatCard from "./stat-card.js";
import {humanFileSize} from "../../../utils/helper-functions";

const AvgProcessMemoryUsageCard = ({servers}) => {
    let averageProcessMemoryUsage = servers[0].processAllocatedMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processAllocatedMemory, 0) / array.length;
        averageProcessMemoryUsage = average(servers);
    }

    return (
        <StatCard title="Avg Process Memory Usage">
            {humanFileSize(averageProcessMemoryUsage)}
        </StatCard>
    );
};

export default AvgProcessMemoryUsageCard;