import {DeveloperBoard} from "@mui/icons-material";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";
import Tooltip from "@mui/material/Tooltip";
import LinearProgress from "@mui/material/LinearProgress";

const AvgSystemCpuLoadCard = ({servers}) => {
    let averageSystemCpuLoad = servers[0].systemCpuLoad;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.systemCpuLoad, 0) / array.length;
        averageSystemCpuLoad = average(servers);
    }
    let averageSystemCpuLoadPercentage = averageSystemCpuLoad * 100;

    let highestUsage = {usage: 0, server: null};
    servers.forEach(server => {
        if (server.systemCpuLoad > highestUsage.usage) {
            highestUsage.usage = server.systemCpuLoad;
            highestUsage.server = server;
        }
    })

    return (
        <AnalyticsCard title="Avg System Cpu Load" icon={DeveloperBoard}
                       sparkline={<LinearProgress
                           variant="determinate" value={averageSystemCpuLoadPercentage} sx={{width: "100%", borderRadius: 10, my: 1}}
                           color={averageSystemCpuLoadPercentage > 80 ? "error" : averageSystemCpuLoadPercentage > 50 ? "warning" : "success"}
                       />}
                       subtitle={<Tooltip title={highestUsage.server ? ("On server " + highestUsage.server.name + " (" + highestUsage.server.id + ")") : ""}>
                           Highest {(highestUsage.usage * 100).toFixed(2)}% {highestUsage.server ? `on ${highestUsage.server.name}` : ""}
                       </Tooltip>}
        >
            {`${averageSystemCpuLoadPercentage.toFixed(2)} %`}
        </AnalyticsCard>
    );
};

export default AvgSystemCpuLoadCard;