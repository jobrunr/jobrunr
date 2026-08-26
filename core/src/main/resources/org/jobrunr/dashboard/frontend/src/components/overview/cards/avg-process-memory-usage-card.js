import {humanFileSize} from "../../../utils/helper-functions";
import {Memory} from "@mui/icons-material";
import {AnalyticsCard} from "./analytics-card.js";

const AvgProcessMemoryUsageCard = ({servers}) => {
    let averageProcessMemoryUsage = servers[0].processAllocatedMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processAllocatedMemory, 0) / array.length;
        averageProcessMemoryUsage = average(servers);
    }

    return (
        <AnalyticsCard title="Avg Process Memory Usage" icon={Memory}>
            {humanFileSize(averageProcessMemoryUsage, true)}
        </AnalyticsCard>
    );
};

export default AvgProcessMemoryUsageCard;