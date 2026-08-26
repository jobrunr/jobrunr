import {DeveloperBoard} from "@mui/icons-material";
import {AnalyticsCard} from "../../ui/AnalyticsCard.js";

const AvgSystemCpuLoadCard = ({servers}) => {
    let averageSystemCpuLoad = servers[0].systemCpuLoad;
    if (servers.length > 1) {
        const average = (array) => array.reduce((a, b) => a + b.systemCpuLoad, 0) / array.length;
        averageSystemCpuLoad = average(servers);
    }

    return (
        <AnalyticsCard title="Avg System Cpu Load" icon={DeveloperBoard}>
            {`${(averageSystemCpuLoad * 100).toFixed(2)} %`}
        </AnalyticsCard>
    );
};

export default AvgSystemCpuLoadCard;