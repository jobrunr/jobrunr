import StatCard from "./stat-card.js";
import {humanFileSize} from "../../../utils/helper-functions";

const AvgProcessFreeMemoryCard = ({servers}) => {
    let averageProcessFreeMemory = servers[0].processFreeMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processFreeMemory, 0) / array.length;
        averageProcessFreeMemory = average(servers);
    }

    return (
        <StatCard title="Avg Process Free Memory">
            {humanFileSize(averageProcessFreeMemory, true)}
        </StatCard>
    );
};

export default AvgProcessFreeMemoryCard;