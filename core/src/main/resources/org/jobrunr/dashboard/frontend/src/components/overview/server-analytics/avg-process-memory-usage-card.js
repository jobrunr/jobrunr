import {humanFileSize} from "../../../utils/helper-functions";
import {Memory} from "@mui/icons-material";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";
import Tooltip from "@mui/material/Tooltip";
import LinearProgress from "@mui/material/LinearProgress";

const AvgProcessMemoryUsageCard = ({servers}) => {
    let averageProcessMemoryUsage = servers[0].processAllocatedMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processAllocatedMemory, 0) / array.length;
        averageProcessMemoryUsage = average(servers);
    }

    let averageProcessFreeMemory = servers[0].processFreeMemory;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.processFreeMemory, 0) / array.length;
        averageProcessFreeMemory = average(servers);
    }
    let averageProcessMemoryUsagePercentage = (averageProcessMemoryUsage / (averageProcessMemoryUsage + averageProcessFreeMemory)) * 100;

    let highestUsage = {usage: 0, server: null};
    servers.forEach(server => {
        if (server.processAllocatedMemory > highestUsage.usage) {
            highestUsage.usage = server.processAllocatedMemory;
            highestUsage.server = server;
        }
    })

    return (
        <AnalyticsCard title="Avg Process Memory Usage" icon={Memory}
                       sparkline={<LinearProgress
                           variant="determinate" value={averageProcessMemoryUsagePercentage} sx={{width: "100%", borderRadius: 10, my: 1}}
                           color={averageProcessMemoryUsagePercentage > 80 ? "error" : averageProcessMemoryUsagePercentage > 50 ? "warning" : "success"}
                       />}
                       subtitle={<Tooltip title={highestUsage.server ? "On server " + highestUsage.server.name + " (" + highestUsage.server.id + ")" : ""}>
                           Highest {humanFileSize(highestUsage.usage, true)} {highestUsage.server ? `on ${highestUsage.server.name}` : ""}
                       </Tooltip>}
        >
            {humanFileSize(averageProcessMemoryUsage, true)}
        </AnalyticsCard>
    );
};

export default AvgProcessMemoryUsageCard;