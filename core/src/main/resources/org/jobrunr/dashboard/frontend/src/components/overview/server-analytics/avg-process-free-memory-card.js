import {humanFileSize} from "../../../utils/helper-functions";
import {Memory} from "@mui/icons-material";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";
import LinearProgress from "@mui/material/LinearProgress";

const AvgProcessFreeMemoryCard = ({servers}) => {
    let averageProcessFreeMemory = servers[0].processFreeMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processFreeMemory, 0) / array.length;
        averageProcessFreeMemory = average(servers);
    }

    let averageProcessMemoryUsage = servers[0].processAllocatedMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processAllocatedMemory, 0) / array.length;
        averageProcessMemoryUsage = average(servers);
    }
    let averageProcessAvailableMemoryPercentage = (averageProcessFreeMemory / (averageProcessMemoryUsage + averageProcessFreeMemory)) * 100;

    return (
        <AnalyticsCard title="Avg Process Free Memory" icon={Memory}
                       sparkline={<LinearProgress
                           variant="determinate" value={averageProcessAvailableMemoryPercentage} sx={{width: "100%", borderRadius: 10, my: 1}}
                           color={averageProcessAvailableMemoryPercentage > 80 ? "success" : averageProcessAvailableMemoryPercentage > 50 ? "warning" : "error"}
                       />}
        >
            {humanFileSize(averageProcessFreeMemory, true)}
        </AnalyticsCard>
    );
};

export default AvgProcessFreeMemoryCard;