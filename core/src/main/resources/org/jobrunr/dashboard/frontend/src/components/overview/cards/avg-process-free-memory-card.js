import {humanFileSize} from "../../../utils/helper-functions";
import {Memory} from "@mui/icons-material";
import {AnalyticsCard} from "./analytics-card.js";

const AvgProcessFreeMemoryCard = ({servers}) => {
    let averageProcessFreeMemory = servers[0].processFreeMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processFreeMemory, 0) / array.length;
        averageProcessFreeMemory = average(servers);
    }

    return (
        <AnalyticsCard title="Avg Process Free Memory" icon={Memory}>
            {humanFileSize(averageProcessFreeMemory, true)}
        </AnalyticsCard>
    );
};

export default AvgProcessFreeMemoryCard;