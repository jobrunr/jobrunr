import {AnalyticsCard} from "./analytics-card.js";
import {Storage} from "@mui/icons-material";

const NbrOfBackgroundJobServersCard = ({servers}) => {
    return (
        <AnalyticsCard title="Nbr of servers" icon={Storage} textId="nbr-of-servers-text">
            {servers.length}
        </AnalyticsCard>
    );
};

export default NbrOfBackgroundJobServersCard;